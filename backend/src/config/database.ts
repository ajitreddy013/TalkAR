import { Sequelize } from "sequelize";
import dotenv from "dotenv";

dotenv.config();

export const sequelize = new Sequelize({
  dialect: process.env.NODE_ENV === "production" ? "postgres" : "sqlite",
  storage:
    process.env.NODE_ENV === "production" ? undefined : "./database.sqlite",
  host: process.env.DB_HOST || "localhost",
  port: parseInt(process.env.DB_PORT || "5432"),
  database: process.env.DB_NAME || "talkar_db",
  username: process.env.DB_USER || "postgres",
  password: process.env.DB_PASSWORD || "password",
  logging: process.env.NODE_ENV === "development" ? console.log : false,
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
