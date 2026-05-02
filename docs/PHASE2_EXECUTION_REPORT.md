# TalkAR Phase 2 Execution Report (Beta-Ready Control Plane)

Date: 2026-05-02

## Summary
Phase 2 implementation is completed in repository scope for:
- release control plane (runtime mode switches),
- provider rollout profile wiring,
- ops alerts and emergency toggles APIs,
- admin operational controls wiring,
- mobile runtime-mode handling,
- backend integration test coverage for controls,
- certification harness scripts/runbook for load/soak/security gates.

## Implemented

### 1) Release-mode switches and rollout policy
- Added runtime policy service:
  - `TALKING_PHOTO_DISABLE_ENQUEUE`
  - `TALKING_PHOTO_FORCE_READY_ONLY`
  - `TALKING_PHOTO_ENABLE_WAV2LIP_FALLBACK`
- Added release profile support:
  - `TALKING_PHOTO_RELEASE_PROFILE=internal|beta`
  - startup policy log on backend boot.
- Effective mode returned as:
  - `runtimeMode: normal | ready_only | enqueue_disabled`

### 2) Backend behavior enforcement
- `GET /api/v1/posters/:id/talking-photo`
  - returns `runtimeMode`
  - enforces ready-only semantics when not ready.
- `POST /api/v1/posters/:id/talking-photo/retry`
  - blocked deterministically in `ready_only` and `enqueue_disabled`.
- Enqueue path in service is skipped when runtime mode disallows generation.

### 3) New ops APIs
- `GET /api/v1/posters/ops/alerts`
  - queue backlog breach
  - provider failure spike
  - failed ratio alert
  - includes runtime mode/profile + metrics snapshot
- `GET /api/v1/posters/ops/toggles`
  - effective toggle state + recent audit trail
- `POST /api/v1/posters/ops/toggles`
  - updates runtime overrides
  - records audit event

### 4) Observability enhancements
- Added artifact-level `lastCorrelationId` persistence and exposure.
- Existing `provider`, `stageTimings`, `lastProcessingDurationMs` retained.

### 5) Admin dashboard wiring
- Alerts page now uses real poster ops alerts endpoint.
- Added emergency runtime controls:
  - disable enqueue
  - ready-only mode
  - enable/disable fallback
- Added runtime chips for quick status view.
- Added correlation ID visibility in image artifact detail drawer.

### 6) Mobile runtime handling
- Extended talking-photo artifact model with `runtimeMode`.
- Controller now exits polling deterministically in `ready_only` / `enqueue_disabled`
  when artifact is not ready, preventing uncontrolled wait loops.

## Validation Results

### Backend
- Build: `npm run build` ✅
- Integration tests:
  - `tests/posters-phase1-integration.test.ts` ✅
  - `tests/posters-phase2-controls.test.ts` ✅
  - `tests/posters-phase2-ops-auth.test.ts` ✅

### Admin dashboard
- Build: `npm run build` ✅

### Mobile app
- Compile: `./gradlew :app:compileDebugKotlin` ✅

### Phase 2 certification artifacts (local execution)
- Security certification report: `docs/phase2-security-report.json` ✅ (`passed=true`)
- Load/soak smoke report: `docs/phase2-load-soak-report.json` ✅
  - profile: `internal-health-smoke`
  - requests: `60`, success rate: `1.0000`, p95 latency: `1ms`

## Remaining Promotion Gates (staging/live execution)

The following are environment/runbook gates and require staging/live execution context:
1. Internal rollout validation on controlled poster set.
2. Beta cohort rollout validation with fallback enabled policy.
3. Non-functional certification runs:
   - full mixed-endpoint load/soak (`backend/scripts/phase2/load-soak-certification.js`)
   - security controls (`backend/scripts/phase2/security-certification.js`)
   - signed URL expiry/access tests
   - worker-auth abuse/replay drills
4. Go/No-Go evidence bundle finalization for beta promotion.
