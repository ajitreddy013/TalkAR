# ── Stage 1: install deps ────────────────────────────────────────────────────
FROM node:18-alpine AS deps

WORKDIR /app

COPY package*.json ./
RUN npm ci

# ── Stage 2: build React app ──────────────────────────────────────────────────
FROM deps AS builder

COPY . .

# REACT_APP_API_URL can be overridden at build time via --build-arg
ARG REACT_APP_API_URL=http://localhost:3000/api/v1
ENV REACT_APP_API_URL=$REACT_APP_API_URL

RUN npm run build

# ── Stage 3: development (CRA dev server with hot-reload) ─────────────────────
FROM deps AS dev

COPY . .

ENV HOST=0.0.0.0
ENV WATCHPACK_POLLING=true

EXPOSE 3000
CMD ["npm", "start"]

# ── Stage 4: production (static file serving) ─────────────────────────────────
FROM node:18-alpine AS prod

WORKDIR /app

RUN npm install -g serve

COPY --from=builder /app/build ./build

EXPOSE 3000
CMD ["serve", "-s", "build", "-l", "3000"]
