# TalkAR Phase 4 Ops Stability Runbook

Date: 2026-05-07
Owner: Engineering + SRE

## 1) SLO Baseline (Day 1)
Lock these targets in dashboards and daily reports:
- API availability: >= 99.9%
- Artifact generation success rate: >= 95%
- Generation latency p95: <= 6000ms

Required monitoring panels:
- queue backlog
- DLQ backlog
- oldest queue message age
- provider failure count/rate
- failed artifact ratio

## 2) Alert Severity Policy
- `P1/Critical`: API outage, sustained failed artifact ratio spike, severe backlog with user impact.
- `P2/High`: SLO trend breach likely within 1 hour, provider failure spike without full outage.
- `P3/Medium`: non-critical drift requiring same-day remediation.

Paging behavior:
- P1: immediate page to on-call engineer + SRE.
- P2: on-call notification and 30-minute acknowledgement window.
- P3: ticket with same-day triage.

## 3) Rollback Drill Procedure (Day 2-4)
Capture each step output under `docs/evidence/phase4-post-ga/drills/`.

```bash
# snapshot before
curl -s "$PROD_API_BASE_URL/api/v1/posters/ops/stats" -H "Authorization: Bearer $PHASE4_ADMIN_TOKEN" | tee ../docs/evidence/phase4-post-ga/drills/rollback-before-stats.json

# force ready-only
curl -s -X POST "$PROD_API_BASE_URL/api/v1/posters/ops/toggles" \
  -H "Authorization: Bearer $PHASE4_ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"forceReadyOnly":true}' | tee ../docs/evidence/phase4-post-ga/drills/rollback-force-ready-only.json

# disable enqueue
curl -s -X POST "$PROD_API_BASE_URL/api/v1/posters/ops/toggles" \
  -H "Authorization: Bearer $PHASE4_ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"disableEnqueue":true}' | tee ../docs/evidence/phase4-post-ga/drills/rollback-disable-enqueue.json

# restore normal
curl -s -X POST "$PROD_API_BASE_URL/api/v1/posters/ops/toggles" \
  -H "Authorization: Bearer $PHASE4_ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"disableEnqueue":false,"forceReadyOnly":false,"enableFallback":true}' | tee ../docs/evidence/phase4-post-ga/drills/rollback-restore-normal.json

# snapshot after
curl -s "$PROD_API_BASE_URL/api/v1/posters/ops/stats" -H "Authorization: Bearer $PHASE4_ADMIN_TOKEN" | tee ../docs/evidence/phase4-post-ga/drills/rollback-after-stats.json
```

## 4) Drain Mode Drill Procedure
- Set worker env `TALKING_PHOTO_DRAIN_MODE=true`.
- Confirm no new queue consumption while in-flight jobs finish.
- Reset `TALKING_PHOTO_DRAIN_MODE=false`.
- Capture before/after queue and worker health evidence.

## 5) Daily Cadence (Days 1-14)
Every day:
- capture `ops/stats` and `ops/alerts` snapshots
- review error-code distribution
- log incidents, remediations, and status
- append outcomes in `phase4-execution-log.md`

## 6) Week 2 Optimization Gate
Start only if Week 1 has no unresolved Sev-1/Sev-2.

Tuning order:
1. `TALKING_PHOTO_WORKER_MAX_CONCURRENCY`
2. worker batch size
3. prewarm limit

After each change:
- compare success rate, p95, failed ratio vs baseline
- rollback tuning immediately if reliability regresses

## 7) Completion Criteria
- 14 consecutive days within SLO bounds
- no recurring Sev-1/Sev-2 pattern
- tuning verified without reliability regression
