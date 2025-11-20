import { Sequelize } from "sequelize";
import dotenv from "dotenv";
import { config } from "../config";

dotenv.config();

// Allow overriding dialect explicitly to avoid native deps (e.g., sqlite) in certain environments
const effectiveDialect = (process.env.DB_DIALECT as any) || (config.nodeEnv === "production" ? "postgres" : "sqlite");

const sequelizeOptions: any = {
  dialect: effectiveDialect,
  logging: config.nodeEnv === "development" || config.nodeEnv === "test" ? console.log : false,
  pool: config.nodeEnv === "production"
      ? {
          max: 5,
          min: 0,
          acquire: 30000,
          idle: 10000,
        }
      : undefined,
  dialectOptions: config.nodeEnv === "production" ? {
    ssl: {
      require: true,
      rejectUnauthorized: false
    }
  } : undefined
};

if (effectiveDialect === "sqlite") {
  sequelizeOptions.storage = config.nodeEnv === "test" ? ":memory:" : "./database.sqlite";
}

export const sequelize = config.dbUrl 
  ? new Sequelize(config.dbUrl, sequelizeOptions)
  : new Sequelize({
      ...sequelizeOptions,
      host: process.env.DB_HOST || "localhost",
      port: parseInt(process.env.DB_PORT || "5432"),
      database: process.env.DB_NAME || "talkar_db",
      username: process.env.DB_USER || "postgres",
      password: process.env.DB_PASSWORD || "password",
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
