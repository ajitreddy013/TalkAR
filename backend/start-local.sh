#!/bin/bash

# Start backend server with SQLite for local development
# This script unsets DATABASE_URL to force SQLite usage

echo "🚀 Starting TalkAR Backend with SQLite..."
echo "📁 Database: ./database.sqlite"
echo ""

# Unset DATABASE_URL and start server
cd "$(dirname "$0")"
DATABASE_URL="" DB_DIALECT=sqlite NODE_ENV=development npm run dev
