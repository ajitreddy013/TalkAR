import { Sequelize } from "sequelize";
import dotenv from "dotenv";
import { config } from "../config";
import dns from 'dns';
import { promisify } from 'util';
import { URL } from 'url';

const resolve4 = promisify(dns.resolve4);

dotenv.config();

export const resolveDbHost = async () => {
  if (config.nodeEnv === 'production' && config.dbUrl) {
    try {
      // Parse hostname from connection string
      const dbUrl = new URL(config.dbUrl);
      const hostname = dbUrl.hostname;
      
      // Check if it's already an IP
      const isIp = /^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$/.test(hostname);
      if (isIp) return;

      console.log(`Resolving database host: ${hostname}`);
      const addresses = await resolve4(hostname);
      
      if (addresses && addresses.length > 0) {
        const ip = addresses[0];
        console.log(`Resolved ${hostname} to IPv4: ${ip}`);
        
        // Update Sequelize configuration to use the IP
        (sequelize as any).options.host = ip;
        if ((sequelize.connectionManager as any).config) {
          (sequelize.connectionManager as any).config.host = ip;
        }
        // Also update the pool config if accessible
        if ((sequelize.connectionManager as any).pool && (sequelize.connectionManager as any).pool.options) {
           // pg-pool might not expose options easily to change host on the fly
           // But usually it uses the factory which uses config.
        }
      }
    } catch (error) {
      console.error('Failed to resolve database host to IPv4:', error);
      // Continue and hope for the best (or fallback to default resolution)
    }
  }
};

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
