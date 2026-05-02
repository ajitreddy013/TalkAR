# TalkAR Phase 3 GA Execution Runbook

Date: 2026-05-02

## Objective
Execute Phase 3 GA rollout with progressive cutover (`10% -> 25% -> 50% -> 100%`) and strict promotion gates.

## Required Environment Matrix

### Backend API (GA/Beta)
- `TALKING_PHOTO_RELEASE_PROFILE=beta|ga`
- `TALKING_PHOTO_SQS_URL=<sqs-url>`
- `AWS_REGION=<aws-region>`
- `AWS_S3_BUCKET=<bucket>`
- `JWT_SECRET=<secret>`
- `TALKING_PHOTO_ENABLE_WAV2LIP_FALLBACK=true|false`
- `TALKING_PHOTO_QUEUE_BACKLOG_THRESHOLD_WARNING=<int>`
- `TALKING_PHOTO_QUEUE_BACKLOG_THRESHOLD_CRITICAL=<int>`
- `TALKING_PHOTO_DLQ_BACKLOG_THRESHOLD=<int>`
- `TALKING_PHOTO_MAX_MESSAGE_AGE_SECONDS_THRESHOLD=<int>`

### Worker
- `TALKING_PHOTO_WORKER_BATCH_SIZE=5`
- `TALKING_PHOTO_WORKER_MAX_CONCURRENCY=2`
- `TALKING_PHOTO_DRAIN_MODE=false`

## Preflight (must pass)
```bash
cd backend
npm run test:phase2
npm run build
npm run phase2:promotion:summary
```

## Catalog Readiness
```bash
cd backend
PHASE3_BASE_URL=<staging-api-base-url> \
PHASE3_ADMIN_TOKEN=<admin-jwt> \
npm run phase3:catalog:sweep | tee ../docs/evidence/phase3-ga/catalog-sweep.json
```

Pass condition:
- no ineligible active posters in sweep report.

## Prewarm Hot Posters
```bash
cd backend
PHASE3_BASE_URL=<staging-api-base-url> \
PHASE3_ADMIN_TOKEN=<admin-jwt> \
PHASE3_PREWARM_LIMIT=20 \
npm run phase3:catalog:prewarm | tee ../docs/evidence/phase3-ga/prewarm-report.json
```

## Progressive Cutover Steps
Create evidence directories:
- `docs/evidence/phase3-ga/step-10`
- `docs/evidence/phase3-ga/step-25`
- `docs/evidence/phase3-ga/step-50`
- `docs/evidence/phase3-ga/step-100`

For each step:
1. apply traffic split
2. hold period (`24h` for 10/25/50, `48h` for 100)
3. collect reports into `step-<N>/`
4. run gate checker:

```bash
cd backend
PHASE3_EVIDENCE_ROOT=../docs/evidence/phase3-ga \
PHASE3_STEP=<10|25|50|100> \
npm run phase3:cutover:gate | tee ../docs/evidence/phase3-ga/step-<N>/cutover-gate-summary.json
```

Promotion only if `canPromote=true`.

## Rollback Controls (immediate)
- ready-only mode:
```bash
curl -X POST "<api>/api/v1/posters/ops/toggles" \
  -H "Authorization: Bearer <admin-jwt>" \
  -H "Content-Type: application/json" \
  -d '{"forceReadyOnly":true}'
```
- disable enqueue:
```bash
curl -X POST "<api>/api/v1/posters/ops/toggles" \
  -H "Authorization: Bearer <admin-jwt>" \
  -H "Content-Type: application/json" \
  -d '{"disableEnqueue":true}'
```
- disable fallback:
```bash
curl -X POST "<api>/api/v1/posters/ops/toggles" \
  -H "Authorization: Bearer <admin-jwt>" \
  -H "Content-Type: application/json" \
  -d '{"enableFallback":false}'
```
- worker drain mode:
  - set `TALKING_PHOTO_DRAIN_MODE=true` on worker deployment to stop new consumption while draining in-flight work.

## Security + Reliability Drills
Execute on staging and production canary:
- signed URL expiry test
- unauthorized artifact access test
- worker auth replay test
- managed provider timeout spike drill
- worker crash loop drill
- queue delay/backlog drill

Attach outcomes under `docs/evidence/phase3-ga/`.

## Final Go/No-Go
Fill:
- `docs/evidence/phase3-ga/go-no-go.md`
- `docs/evidence/phase3-ga/approvals.md`

Required:
- all step gate summaries pass
- no unresolved critical alerts
- evidence bundle complete

