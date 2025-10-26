import { Sequelize } from "sequelize";
import dotenv from "dotenv";

dotenv.config();

// Allow overriding dialect explicitly to avoid native deps (e.g., sqlite) in certain environments
const effectiveDialect = (process.env.DB_DIALECT as any) || (process.env.NODE_ENV === "production" ? "postgres" : "sqlite");

export const sequelize = new Sequelize({
  dialect: effectiveDialect,
  storage:
    effectiveDialect === "sqlite"
      ? process.env.NODE_ENV === "test"
        ? ":memory:"
        : "./database.sqlite"
      : undefined,
  host: process.env.DB_HOST || "localhost",
  port: parseInt(process.env.DB_PORT || "5432"),
  database: process.env.DB_NAME || "talkar_db",
  username: process.env.DB_USER || "postgres",
  password: process.env.DB_PASSWORD || "password",
  // Enable SQL logging in test/dev to aid debugging; keep quiet otherwise
  logging: process.env.NODE_ENV === "development" || process.env.NODE_ENV === "test" ? console.log : false,
  pool:
    process.env.NODE_ENV === "production"
      ? {
          max: 5,
          min: 0,
          acquire: 30000,
          idle: 10000,
        }
      : undefined,
});

// Test database connection
export const testConnection = async (): Promise<boolean> => {
  try {
    await sequelize.authenticate();
    console.log("Database connection has been established successfully.");
    return true;
  } catch (error) {
    console.error("Unable to connect to the database:", error);
    return false;
  }
};
