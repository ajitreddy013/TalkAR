import express from "express";
import cors from "cors";
import helmet from "helmet";
import morgan from "morgan";
import dotenv from "dotenv";
import { createServer } from "http";
import { Server } from "socket.io";
import { sequelize } from "./config/database";
import { errorHandler } from "./middleware/errorHandler";
import { notFound } from "./middleware/notFound";
import { defineAssociations } from "./models/associations";
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
import http from "http";
import LRU from "lru-cache";

// Load environment variables
dotenv.config();

const app = express();
const PORT = process.env.PORT || 3000;

// Enable HTTP keep-alive
const agent = new http.Agent({ keepAlive: true });
// Store in app for use in routes
app.set('httpAgent', agent);

// Create LRU cache for video content
const videoCache = new LRU<string, any>({
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
    origin: ["http://localhost:3000", "http://localhost:3001", "http://localhost:3002"],
    credentials: true,
  })
);
app.use(morgan("combined"));
app.use(express.json({ limit: "10mb" }));
app.use(express.urlencoded({ extended: true, limit: "10mb" }));

// Serve uploaded files statically with CORS headers
app.use(
  "/uploads",
  (req, res, next) => {
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
import path from "path";
const adminBuildPath = path.join(__dirname, "..", "admin-dashboard", "build");
try {
  app.use(express.static(adminBuildPath));
  // For any other routes not handled by API, serve the React app
  app.get("/", (req, res) => {
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

// Legacy routes (for backward compatibility)
app.use("/api/multi-images", multiImageRoutes);
app.use("/api/v1/avatars", avatarRoutes);
app.use("/api/v1/lipsync", lipSyncRoutes);
app.use("/api/v1/scripts", scriptRoutes);
app.use("/api/v1/enhanced-lipsync", enhancedLipSyncRoutes);
app.use("/api/v1/analytics", analyticsRoutes);

// Health check
app.get("/health", (req, res) => {
  res.json({
    status: "OK",
    timestamp: new Date().toISOString(),
    version: "1.0.0",
  });
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

startServer();

export default app;