# Worker Auth Incident Runbook

## Purpose
Operational playbook for detecting, triaging, and mitigating Wav2Lip worker authentication and replay-abuse incidents.

## Health and Telemetry Endpoints
- Worker auth telemetry:
  - `GET /api/v1/performance/worker-auth`
- Worker auth health:
  - `GET /api/v1/performance/worker-auth/health`
  - `200` = healthy, `503` = threshold breach
- Global health (strict worker auth mode):
  - `GET /health?workerAuthStrict=true`
  - returns `503` when worker-auth health is unhealthy

## Alert Threshold Configuration
- `WORKER_AUTH_HEALTH_WINDOW_MINUTES` (default `5`)
- `WORKER_AUTH_FAILURE_RATE_THRESHOLD` (default `0.25`)
- `WORKER_AUTH_HEALTH_MIN_REQUESTS` (default `20`)
- `WORKER_AUTH_REASON_COUNT_THRESHOLD` (default `20`)

Recommended initial production values:
- window: `5`
- failure rate threshold: `0.10`
- min requests: `50`
- reason count threshold: `25`

## Alerting Rules
Trigger an alert when any condition is true:
- `/api/v1/performance/worker-auth/health` returns `503` for 3 consecutive checks.
- `failureRate > threshold` with `requestsInWindow >= minimumRequests`.
- `reasonsOverThreshold` is non-empty.

## Triage Checklist
1. Confirm signal quality:
- Validate current traffic volume (`requestsInWindow`) to avoid low-volume false positives.
- Check top failure reason from `failuresByReason`.

2. Determine blast radius:
- Confirm if talking-photo generation failures are rising (`status=failed` artifacts).
- Check queue backlog and worker throughput.

3. Correlate by request identity:
- Inspect `recentFailures` and `correlationId`.
- Match with backend logs for `worker-auth` warnings.

## Mitigation Actions
1. Token/signature mismatch spikes:
- Rotate `WAV2LIP_WORKER_AUTH_TOKEN`.
- Rotate `WAV2LIP_WORKER_SIGNING_SECRET`.
- Ensure backend and worker deploys use same secret versions.

2. Replay/nonce spikes:
- Switch nonce store to Redis in multi-instance mode:
  - `WAV2LIP_NONCE_STORE=redis`
  - `WAV2LIP_NONCE_REDIS_URL=...`
- Verify synchronized system clocks (NTP).

3. Timestamp expiry spikes:
- Check clock drift between backend and worker hosts.
- Increase `WAV2LIP_WORKER_MAX_SKEW_MS` temporarily if needed.

4. Sustained attack conditions:
- Reduce public exposure of worker endpoint (private network / WAF / allowlist).
- Tighten retry rate limits:
  - `TALKING_PHOTO_RETRY_RATE_LIMIT_MAX`
- Temporarily disable self-hosted fallback if needed:
  - `TALKING_PHOTO_ENABLE_WAV2LIP_FALLBACK=false`

## Recovery Criteria
- Worker-auth health endpoint stable at `200` for 30+ minutes.
- Failure rate below threshold for 3 consecutive windows.
- No reason in `reasonsOverThreshold`.
- Artifact readiness and generation success return to baseline.

## Post-Incident Actions
- Record root cause and exact failure pattern.
- Update thresholds if too noisy/too lax.
- Add regression test for discovered failure mode.
- If secret mismatch caused outage, document secret rollout order.
