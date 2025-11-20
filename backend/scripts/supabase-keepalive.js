#!/usr/bin/env node
const path = require("path");
const fs = require("fs");
const dotenv = require("dotenv");

// Load backend .env explicitly
const envPath = path.join(__dirname, "..", ".env");
if (fs.existsSync(envPath)) {
  dotenv.config({ path: envPath });
} else {
  dotenv.config();
}

const SUPABASE_URL = process.env.SUPABASE_URL;
const SUPABASE_KEY =
  process.env.SUPABASE_SERVICE_KEY || process.env.SUPABASE_ANON_KEY;

if (!SUPABASE_URL || !SUPABASE_KEY) {
  console.error("Supabase URL or key not found in backend .env. Aborting.");
  process.exit(1);
}

async function keepalive() {
  try {
    const endpoint = `${SUPABASE_URL.replace(
      /\/$/,
      ""
    )}/rest/v1/ai_configs?select=id&limit=1`;

    const res = await fetch(endpoint, {
      method: "GET",
      headers: {
        apikey: SUPABASE_KEY,
        Authorization: `Bearer ${SUPABASE_KEY}`,
        Accept: "application/json",
      },
    });

    if (!res.ok) {
      console.error(
        `Supabase keepalive request failed: ${res.status} ${res.statusText}`
      );
      process.exitCode = 2;
      return;
    }

    // We intentionally don't print response body (may contain nothing or sensitive values).
    console.log("Supabase keepalive: SUCCESS");
  } catch (err) {
    console.error(
      "Supabase keepalive error:",
      err && err.message ? err.message : err
    );
    process.exitCode = 3;
  }
}

keepalive();
