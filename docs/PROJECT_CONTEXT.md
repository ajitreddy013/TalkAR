# TalkAR Project Context

Last updated: 2026-04-29
Purpose: Fast project memory for coding sessions. Start here before deep-diving files.

## What this project is
TalkAR is a monorepo for an AR experience that turns recognized images into talking, lip-synced avatars.

Main apps:
- `mobile-app`: Android app (Kotlin, ARCore, Compose, ExoPlayer)
- `backend`: Node.js + TypeScript API and AI orchestration
- `admin-dashboard`: React + TypeScript admin/content/analytics UI

## Monorepo map
- `mobile-app/app/src/main/java/com/talkar/app/`
- `backend/src/`
- `admin-dashboard/src/`
- `docs/` (architecture, setup, deployment, testing docs)
- `docker-compose.yml` (local multi-service orchestration)

## Current architecture (high level)
1. Android app scans/tracks image via ARCore.
2. App calls backend with image/context.
3. Backend orchestrates AI pipeline (script, TTS, lip-sync generation).
4. Backend returns media URLs/metadata.
5. App overlays and plays talking avatar in AR.
6. Admin dashboard manages images/config/settings and monitors usage.

## Backend quick context
Entry point:
- `backend/src/index.ts`

Config:
- `backend/src/config.ts`
- `backend/src/config/database.ts`

Important route modules:
- `backend/src/routes/auth.ts`
- `backend/src/routes/images.ts`
- `backend/src/routes/sync.ts`
- `backend/src/routes/aiPipeline.ts`
- `backend/src/routes/aiConfig.ts`
- `backend/src/routes/analytics.ts`
- `backend/src/routes/settings.ts`
- `backend/src/routes/generateDynamicScript.ts`
- `backend/src/routes/betaFeedbackRoutes.ts`

Important service modules:
- `backend/src/services/aiPipelineService.ts`
- `backend/src/services/optimizedScriptService.ts`
- `backend/src/services/syncService.ts`
- `backend/src/services/enhancedLipSyncService.ts`
- `backend/src/services/analyticsService.ts`
- `backend/src/services/aiConfigService.ts`
- `backend/src/services/performanceMetricsService.ts`

Backend scripts:
- Dev: `cd backend && npm run dev`
- Build: `cd backend && npm run build`
- Test: `cd backend && npm test`

## Admin dashboard quick context
Entry:
- `admin-dashboard/src/index.tsx`
- `admin-dashboard/src/App.tsx`

Main areas:
- `admin-dashboard/src/pages/` (Dashboard, Images, AIConfig, Settings, etc.)
- `admin-dashboard/src/services/` (API clients)
- `admin-dashboard/src/store/` (Redux state)

Scripts:
- Dev: `cd admin-dashboard && npm start`
- Build: `cd admin-dashboard && npm run build`
- Test: `cd admin-dashboard && npm test`

## Mobile app quick context
Entry:
- `mobile-app/app/src/main/java/com/talkar/app/MainActivity.kt`
- `mobile-app/app/src/main/java/com/talkar/app/TalkARApplication.kt`

Important packages:
- `mobile-app/app/src/main/java/com/talkar/app/ar/` (AR/video rendering pipeline)
- `mobile-app/app/src/main/java/com/talkar/app/data/` (API/local/repository/config)
- `mobile-app/app/src/main/java/com/talkar/app/ui/` (screens/components/viewmodels)

Manifest/build config:
- `mobile-app/app/src/main/AndroidManifest.xml`
- `mobile-app/app/build.gradle`

Notable mobile build facts:
- `minSdk 24`, `targetSdk 34`, `compileSdk 34`
- ARCore required (`com.google.ar:core:1.44.0`)
- Build types include `debug`, `beta`, `release`
- API host/protocol/port are variant-configurable via Gradle properties

## Local run options
Docker (recommended for multi-service local setup):
- `docker-compose up -d`

Manual:
- Backend: `cd backend && npm install && npm run dev`
- Admin dashboard: `cd admin-dashboard && npm install && npm start`
- Mobile app: open `mobile-app/` in Android Studio and run app module

## Environment and secrets
- Root `.env` for Docker-related settings.
- Backend env file expected under `backend/.env`.
- Admin dashboard typically uses `admin-dashboard/.env` with API URL.
- Do not commit real secrets/keys.

## Testing and quality
Backend:
- Jest tests under `backend/src/tests` and `backend/tests` patterns.

Admin dashboard:
- React scripts test runner.

Mobile app:
- Unit/UI test dependencies are configured in Gradle.

## Code review graph snapshot
Built on 2026-04-29:
- Files: 287
- Nodes: 2399
- Edges: 18052
- Flows: 211
- Communities: 12

## How to use this file in future sessions
1. Read this file first.
2. Open only the specific subsystem you are changing (`mobile-app`, `backend`, or `admin-dashboard`).
3. Use route/service/package pointers above to jump directly to likely files.
4. Rebuild graph only when structure has changed significantly.

## Suggested maintenance rule
Update this file whenever one of these changes:
- New major route/service/module is added
- Build/run commands change
- Architecture or external AI provider flow changes
- Significant directory reorganization
