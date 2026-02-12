import dotenv from 'dotenv';
import path from 'path';

// Try loading from default location
dotenv.config();

// Try loading from root if in dist (fallback)
dotenv.config({ path: path.join(__dirname, '../../.env') });

console.log('--- CONFIG LOADING ---');
console.log('ENV AI_PROVIDER:', process.env.AI_PROVIDER);
console.log('ENV GROQCLOUD_API_KEY present:', !!process.env.GROQCLOUD_API_KEY);
console.log('----------------------');

export const config = {
  port: process.env.PORT || 3000,
  nodeEnv: process.env.NODE_ENV || 'development',
  // openaiKey: removed
  // elevenKey: removed
  syncKey: process.env.SYNC_API_KEY,
  dbUrl: process.env.DATABASE_URL,
  frontendOrigin: process.env.FRONTEND_ORIGIN || 'http://localhost:3000',
  jwtSecret: process.env.JWT_SECRET || 'default_secret_please_change',
  baseUrl: process.env.BASE_URL || `http://localhost:${process.env.PORT || 4000}`,
  aiProvider: process.env.AI_PROVIDER || 'groq',
  groqKey: process.env.GROQCLOUD_API_KEY,
};
