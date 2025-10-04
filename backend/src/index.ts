import express from "express";
import cors from "cors";
import helmet from "helmet";
import morgan from "morgan";
import dotenv from "dotenv";
import { sequelize } from "./config/database";
import { errorHandler } from "./middleware/errorHandler";
import { notFound } from "./middleware/notFound";
import imageRoutes from "./routes/images";
import syncRoutes from "./routes/sync";
import adminRoutes from "./routes/admin";
import authRoutes from "./routes/auth";
import multiImageRoutes from "./routes/multiImageRoutes";

// Load environment variables
dotenv.config();

const app = express();
const PORT = process.env.PORT || 3000;

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
    origin: ["http://localhost:3000", "http://localhost:3001"],
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
app.use("/api/multi-images", multiImageRoutes);

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

    await sequelize.sync({ alter: true });
    console.log("Database synchronized.");

    app.listen(Number(PORT), "0.0.0.0", () => {
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
