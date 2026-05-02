# TalkAR Phase 2 Certification Runbook

Date: 2026-05-02

## Purpose
Run and capture the required Phase 2 certification gates for Internal/Beta rollout:
- backend integration gates,
- load/soak gate,
- security gate,
- rollback control verification.

## Prerequisites
- Backend running and reachable at `PHASE2_BASE_URL` (default `http://127.0.0.1:4000`).
- Admin JWT available as `PHASE2_ADMIN_TOKEN`.

## 1) Backend Integration Gate
Run:

```bash
cd backend
npm run test:phase2
```

Pass criteria:
- all tests pass, including:
  - `posters-phase1-integration.test.ts`
  - `posters-phase2-controls.test.ts`
  - `posters-phase2-ops-auth.test.ts`

## 2) Load + Soak Gate
Run:

```bash
cd backend
PHASE2_BASE_URL=http://127.0.0.1:4000 \
PHASE2_ADMIN_TOKEN=<admin_jwt> \
PHASE2_CONCURRENCY=20 \
PHASE2_DURATION_SECONDS=60 \
npm run phase2:cert:load-soak | tee ../docs/phase2-load-soak-report.json
```

Pass criteria:
- `latencyMs.p95 <= 6000`
- `totals.successRate >= 0.95`

## 3) Security Gate
Run:

```bash
cd backend
PHASE2_BASE_URL=http://127.0.0.1:4000 \
PHASE2_ADMIN_TOKEN=<admin_jwt> \
npm run phase2:cert:security | tee ../docs/phase2-security-report.json
```

Pass criteria:
- unauthenticated ops alerts/toggles write are blocked (`401`),
- admin read path succeeds (`200`).

## 4) Rollback Toggle Drill
Use admin API:

```bash
curl -X POST "$PHASE2_BASE_URL/api/v1/posters/ops/toggles" \
  -H "Authorization: Bearer $PHASE2_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"disableEnqueue":true,"forceReadyOnly":false,"enableFallback":false}'
```

Then verify:
- `/api/v1/posters/ops/toggles` reflects `runtimeMode=enqueue_disabled`.
- retry endpoint returns deterministic blocked response (`409`).

Repeat for:
- `forceReadyOnly=true`
- `enableFallback=false`

## Evidence Bundle Files
- `docs/phase2-load-soak-report.json`
- `docs/phase2-security-report.json`
- test output from `npm run test:phase2`
- mobile E2E logs/screenshots (device run)
- admin dashboard screenshots for alerts + toggles

