import pkg from 'pg';
import { config } from './config';

const { Pool } = pkg;

export const pool = new Pool({
  connectionString: config.dbUrl,
  ssl: config.nodeEnv === 'production' ? { rejectUnauthorized: false } : undefined,
});

// Test the connection
pool.on('connect', () => {
  console.log('Connected to the database via pg pool');
});

pool.on('error', (err) => {
  console.error('Unexpected error on idle client', err);
  process.exit(-1);
});
