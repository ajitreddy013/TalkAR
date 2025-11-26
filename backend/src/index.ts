import "./bootstrap";
import express, { Request, Response, NextFunction } from "express";
import cors from "cors";
import helmet from "helmet";
import morgan from "morgan";
import dotenv from "dotenv";
import { createServer } from "http";
import { Server } from "socket.io";
import rateLimit from "express-rate-limit"; // Rate limiting
import { sequelize } from "./config/database";
import { errorHandler } from "./middleware/errorHandler";
import { notFound } from "./middleware/notFound";
import { defineAssociations } from "./models/associations";
import logger from "./utils/logger"; // Structured logger
import imageRoutes from "./routes/images";
import syncRoutes from "./routes/sync";
import adminRoutes from "./routes/admin";
import authRoutes from "./routes/auth";
import multiImageRoutes from "./routes/multiImageRoutes";
import avatarRoutes from "./routes/avatars";
import lipSyncRoutes from "./routes/lipSync";
import scriptRoutes from "./routes/scripts";
import enhancedLipSyncRoutes from "./routes/enhancedLipSync";
import analyticsRoutes from "./routes/analytics";
import aiPipelineRoutes from "./routes/aiPipeline";
import aiConfigRoutes from "./routes/aiConfig";
import performanceRoutes from "./routes/performance";
import feedbackRoutes from "./routes/feedback";
import settingsRoutes from "./routes/settings";
import generateDynamicScriptRoutes from "./routes/generateDynamicScript";
import betaFeedbackRoutes from "./routes/betaFeedbackRoutes";
import http from "http";
import { SimpleCache } from "./utils/simpleCache";
import path from "path";
import { AnalyticsWorker } from "./services/analyticsWorker";

import { config } from "./config";

// Load environment variables
dotenv.config();

const app = express();
const PORT = config.port;

// Enable HTTP keep-alive
const agent = new http.Agent({ keepAlive: true });
// Store in app for use in routes
app.set('httpAgent', agent);

// Create LRU cache for video content
const videoCache = new SimpleCache<string, any>({
  max: 3, // Cache last 3 videos
  ttl: 1000 * 60 * 5, // 5 minutes TTL
});
app.set('videoCache', videoCache);

// Middleware
app.use(
  helmet({
    contentSecurityPolicy: {
      directives: {
        defaultSrc: ["'self'"],
        imgSrc: [
          "'self'",
          "data:",
          "blob:",
          "http://localhost:3000",
          "http://localhost:3001",
          "https://*.supabase.co", // Allow Supabase storage
        ],
        scriptSrc: ["'self'"],
        styleSrc: ["'self'", "'unsafe-inline'"],
      },
    },
    crossOriginResourcePolicy: { policy: "cross-origin" },
  })
);
app.use(
  cors({
    origin: [config.frontendOrigin, "http://localhost:3000", "http://localhost:3001", "http://localhost:3002"],
    methods: ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
    allowedHeaders: ["Content-Type", "Authorization"],
    credentials: true,
  })
);
app.use(morgan("combined"));
app.use(express.json({ limit: "10mb" }));
app.use(express.urlencoded({ extended: true, limit: "10mb" }));

// Rate Limiting
const limiter = rateLimit({
  windowMs: 1 * 60 * 1000, // 1 minute
  max: 60, // Limit each IP to 60 requests per windowMs
  standardHeaders: true,
  legacyHeaders: false,
  message: "Too many requests from this IP, please try again after a minute",
  handler: (req, res, next, options) => {
    logger.warn(`Rate limit exceeded for IP: ${req.ip}`);
    res.status(options.statusCode).send(options.message);
  }
});

// Apply rate limiting to all requests
app.use(limiter);

// Request logging
app.use((req, res, next) => {
  logger.http(`${req.method} ${req.url}`);
  next();
});

// Serve uploaded files statically with CORS headers
app.use(
  "/uploads",
  (req: Request, res: Response, next: NextFunction) => {
    res.header("Access-Control-Allow-Origin", "*");
    res.header("Access-Control-Allow-Methods", "GET");
    res.header(
      "Access-Control-Allow-Headers",
      "Origin, X-Requested-With, Content-Type, Accept"
    );
    next();
  },
  express.static("uploads")
);

// Serve admin-dashboard production build (if present)
const adminBuildPath = path.join(__dirname, "..", "admin-dashboard", "build");
try {
  app.use(express.static(adminBuildPath));
  // For any other routes not handled by API, serve the React app
  app.get("/", (req: Request, res: Response) => {
    res.sendFile(path.join(adminBuildPath, "index.html"));
  });
} catch (e) {
  // ignore if build not present in dev environment
}

// Routes
app.use("/api/v1/auth", authRoutes);
app.use("/api/v1/images", imageRoutes);
app.use("/api/v1/sync", syncRoutes);
app.use("/api/v1/admin", adminRoutes);
app.use("/api/v1/ai-pipeline", aiPipelineRoutes);
app.use("/api/v1/ai-config", aiConfigRoutes);
app.use("/api/v1/performance", performanceRoutes);
app.use("/api/v1/feedback", feedbackRoutes);
app.use("/api/v1/settings", settingsRoutes);
app.use("/api/v1/generate-dynamic-script", generateDynamicScriptRoutes);
app.use("/api/v1/beta-feedback", betaFeedbackRoutes);

// Legacy routes (for backward compatibility)
app.use("/api/multi-images", multiImageRoutes);
app.use("/api/v1/avatars", avatarRoutes);
app.use("/api/v1/lipsync", lipSyncRoutes);
app.use("/api/v1/scripts", scriptRoutes);
app.use("/api/v1/enhanced-lipsync", enhancedLipSyncRoutes);
app.use("/api/v1/analytics", analyticsRoutes);

// Health check
import { pool } from "./db";

app.get("/health", async (req: Request, res: Response) => {
  try {
    await pool.query('SELECT 1');
    res.json({
      status: "OK",
      timestamp: new Date().toISOString(),
      version: "1.0.0",
      db: "connected"
    });
  } catch (e: any) {
    res.status(500).json({ 
      status: "error", 
      error: e.message,
      timestamp: new Date().toISOString()
    });
  }
});

// Error handling
app.use(notFound);
app.use(errorHandler);

// Database connection and server startup
const startServer = async () => {
  try {
    await sequelize.authenticate();
    console.log("Database connection established successfully.");

    // Define model associations
    defineAssociations();
    console.log("Model associations defined.");

    await sequelize.sync({ alter: true });
    console.log("Database synchronized.");

    // Create HTTP server and Socket.IO server
    const server = createServer(app);
    const io = new Server(server, {
      cors: {
        origin: ["http://localhost:3000", "http://localhost:3001", "http://localhost:3002"],
        credentials: true
      }
    });

    // Store io instance in app for access in routes
    app.set('io', io);

    // Handle socket connections
    io.on('connection', (socket) => {
      console.log('Client connected for real-time config updates');
      
      socket.on('disconnect', () => {
        console.log('Client disconnected');
      });
    });

    server.listen(Number(PORT), "0.0.0.0", () => {
      console.log(`Server is running on port ${PORT}`);
      console.log(`Environment: ${process.env.NODE_ENV}`);
      console.log(`Server accessible at: http://0.0.0.0:${PORT}`);
      console.log(`Local access: http://localhost:${PORT}`);
      console.log(`Network access: http://10.17.5.127:${PORT}`);
    });
  } catch (error) {
    console.error("Unable to start server:", error);
    process.exit(1);
  }
};

// Start analytics worker
if (process.env.NODE_ENV !== 'test') {
  AnalyticsWorker.start();
  startServer();
}

export default app;