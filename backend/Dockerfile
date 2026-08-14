# ── Stage 1: install deps ────────────────────────────────────────────────────
FROM node:18-alpine AS deps

WORKDIR /app

# Native build tools needed for some npm packages (e.g. bcrypt, sqlite3)
RUN apk add --no-cache python3 make g++

COPY package*.json ./
RUN npm ci

# ── Stage 2: build TypeScript ─────────────────────────────────────────────────
FROM deps AS builder

COPY tsconfig.json ./
COPY src/ ./src/

RUN npm run build

# Prune dev dependencies from the install used for build
RUN npm prune --omit=dev

# ── Stage 3: development (hot-reload via ts-node-dev) ─────────────────────────
FROM deps AS dev

# Copy full source so ts-node-dev can watch files
COPY . .

EXPOSE 3000
CMD ["npm", "run", "dev"]

# ── Stage 4: production ───────────────────────────────────────────────────────
FROM node:18-alpine AS prod

WORKDIR /app

# Only copy the pruned node_modules and compiled output
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/package*.json ./

EXPOSE 3000
CMD ["npm", "start"]
