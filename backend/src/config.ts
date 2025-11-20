import dotenv from 'dotenv';
import dns from 'dns';

dotenv.config();

// Force IPv4 to avoid ENETUNREACH on IPv6 in some environments (like Render/Supabase)
if (dns.setDefaultResultOrder) {
  dns.setDefaultResultOrder('ipv4first');
}

export const config = {
  port: process.env.PORT || 3000,
  nodeEnv: process.env.NODE_ENV || 'development',
  openaiKey: process.env.OPENAI_API_KEY,
  elevenKey: process.env.ELEVENLABS_API_KEY,
  syncKey: process.env.SYNC_API_KEY,
  dbUrl: process.env.DATABASE_URL,
  frontendOrigin: process.env.FRONTEND_ORIGIN || 'http://localhost:3000',
  jwtSecret: process.env.JWT_SECRET || 'default_secret_please_change',
};
