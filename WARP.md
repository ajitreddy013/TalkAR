# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

Project overview
- Monorepo with three apps:
  - backend: Node.js + TypeScript Express API with PostgreSQL (Sequelize), AWS S3, external Sync API
  - admin-dashboard: React + TypeScript (Create React App)
  - mobile-app: Android (Kotlin, ARCore, Jetpack Compose)
- Containerization: docker-compose for local dev; k8s manifests in k8s/
- CI: .github/workflows/ci.yml defines canonical build/lint/test steps

Getting started
- Environment setup
  - Backend: cp backend/env.example backend/.env and configure values
  - Admin dashboard: create admin-dashboard/.env with REACT_APP_API_URL (see docs/SETUP.md)
  - Mobile app: update ApiClient.kt base URL and any required keys in Android Studio
- Recommended quick start: docker-compose up -d (starts postgres, backend, admin dashboard)

Common commands
- Top-level (Docker)
  - Start local stack: docker-compose up -d
  - Rebuild images: docker-compose build
  - Validate compose config: docker-compose config

- Backend (Node.js/TypeScript)
  - Install deps: (from backend/) npm install
  - Dev server (ts-node-dev): npm run dev
  - Build TypeScript: npm run build
  - Start (from dist/): npm start
  - Lint: npm run lint
  - Lint and fix: npm run lint:fix
  - Tests (Jest via ts-jest):
    - All tests: npm test
    - Watch: npm run test:watch
    - Coverage: npm run test:coverage
    - Unit only: npm run test:unit
    - Integration only: npm run test:integration
    - Performance only: npm run test:performance
    - Run a single test file: npm test -- tests/authService.test.ts
    - Run tests by name: npm test -- -t "should return 200 on health"
  - Typical env for tests (when needed): DATABASE_URL=postgresql://<user>:<pass>@localhost:5432/<db>

- Admin dashboard (React + CRA)
  - Install deps: (from admin-dashboard/) npm install
  - Dev server: npm start  (serves at http://localhost:3001)
  - Build: npm run build
  - Tests (Jest via react-scripts):
    - All tests with coverage (CI-style): npm test -- --coverage --watchAll=false
    - Run a single test file: npm test -- Dialogues.test.tsx
    - Run tests by name: npm test -- -t "renders dashboard"
  - Note: No explicit lint script is defined; react-scripts surfaces linting during build/test.

- Mobile app (Android / Gradle)
  - From mobile-app/ (use ./gradlew if wrapper exists, otherwise gradle)
  - Unit tests: ./gradlew test
  - Android lint: ./gradlew lint
  - Assemble debug APK: ./gradlew assembleDebug
  - Assemble release APK: ./gradlew assembleRelease
  - Instrumented tests (device/emulator): ./gradlew connectedAndroidTest
  - Clean: ./gradlew clean
  - Example single test: ./gradlew testDebugUnitTest --tests com.talkar.app.ExampleUnitTest

Key ports and URLs
- Backend API base (local): http://localhost:3000/api/v1
- Admin dashboard (local): http://localhost:3001
- Health endpoint: http://localhost:3000/health

High-level architecture
- Mobile app (Android/Kotlin)
  - MVVM + Repository pattern
  - Layers: data (api, local Room), models, repository; UI (components, screens, theme), viewmodels
  - AR: ARCore for image recognition; Compose for UI; Retrofit/OkHttp for networking; Coil for images
  - Pulls content and sync jobs from backend; displays AR overlays, plays generated lip-synced videos

- Backend (Node/TypeScript/Express)
  - Entry: backend/src/index.ts sets middleware (helmet, cors, morgan), mounts routes under /api/v1
  - Layers:
    - config: database.ts initializes Sequelize (PostgreSQL)
    - middleware: auth, validation, error handling, 404
    - models: Sequelize models (e.g., Image)
    - routes: images, sync (lip-sync generation), admin, auth
    - services: syncService (external Sync API), uploadService (S3), authService
  - Data flow: Admin dashboard and mobile app call REST endpoints; backend persists to Postgres and media to S3; delegates lip-sync job creation to Sync API; surfaces status and assets

- Admin dashboard (React/TS)
  - Structure: components (dialogs, layout), pages (Dashboard, Images, Dialogues, Analytics, Settings), services (api/auth/dialogue/image), store (Redux Toolkit slices)
  - Functions: content management (upload images, manage dialogues), analytics, settings, auth
  - Communicates with backend via Axios through src/services/*

- Orchestration and deployment
  - docker-compose.yml (local): services for postgres, backend, admin-dashboard with volume mounts for live dev
  - k8s/ manifests: Deployments, Services, HPA (backend), Ingress (frontend), StatefulSet + Secret (database)
  - CI (GitHub Actions): jobs for backend/frontend/mobile tests, security scan, Docker build; refer to .github/workflows/ci.yml for exact steps and flags

Environment configuration
- Backend (.env) — see backend/env.example
  - DATABASE_URL or individual DB_* vars; PORT; JWT_SECRET; AWS_*; SYNC_API_URL; SYNC_API_KEY; CORS_ORIGIN
- Admin dashboard (.env)
  - REACT_APP_API_URL (e.g., http://localhost:3000/api/v1)
- Mobile app
  - Update API base URL in mobile-app/app/src/main/java/com/talkar/app/data/api/ApiClient.kt

References
- README.md: high-level overview, quick start, tech stack
- docs/SETUP.md: detailed setup for all environments and example env vars
- docs/ARCHITECTURE.md: deeper structural breakdown and data flows
- docs/API.md: REST endpoints for images, dialogues, sync operations
- .github/workflows/ci.yml: authoritative build/lint/test steps and flags
