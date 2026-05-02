# TalkAR Production Readiness Plan (Backend + Frontend + Mobile)

## Current Phase Gate (As of 2026-05-02)
- Current phase: **Phase 3 (GA rollout)**
- Phase 3 completion: **NOT COMPLETE**
- Next phase: **Phase 4 (Post-GA stabilization and optimization)**
- Move decision: **BLOCKED until Phase 3 evidence gates are fully passed**

### Required to move to next phase
- `docs/evidence/phase3-ga/step-10/cutover-gate-summary.json` with `"canPromote": true`
- `docs/evidence/phase3-ga/step-25/cutover-gate-summary.json` with `"canPromote": true`
- `docs/evidence/phase3-ga/step-50/cutover-gate-summary.json` with `"canPromote": true`
- `docs/evidence/phase3-ga/step-100/cutover-gate-summary.json` with `"canPromote": true`
- Per-step evidence (`load-soak-report.json`, `security-report.json`, `mobile-matrix.json`, `alerts-summary.json`) for 10/25/50/100
- `docs/evidence/phase3-ga/go-no-go.md` finalized with `Final decision: GO`
- `docs/evidence/phase3-ga/approvals.md` signed by Engineering, SRE/Platform, Product, QA

### Current blocker snapshot
- Only partial step-10 artifacts are present in repository evidence.
- Step-25/50/100 gate summaries are not present.
- Final go/no-go and required approvals are not finalized.
- Detailed gate-by-gate status is tracked in `docs/PHASE3_CLOSEOUT_STATUS.md`.
- Step-by-step execution commands are in `docs/PHASE3_COMPLETION_PLAYBOOK.md`.

## Summary
Build a production-grade, end-to-end “scan poster → detect from backend catalog → generate/fetch talking artifact → stable AR talking overlay” system with **hybrid lip-sync (day-1)**, **AWS deployment target**, and **reliability-first release scope**.

Implementation will be done in 8 ordered tracks so each track closes specific gaps before the next one starts.

## Implementation Changes (Ordered, Decision-Complete)

1. **Define Canonical Product Flow + Contracts (P0)**
- Lock one runtime flow for app:
  1. App loads active poster index from backend.
  2. ARCore detects known poster.
  3. App requests `GET /posters/:id/talking-photo`.
  4. If `ready`: play artifact immediately.
  5. If `queued/processing/failed`: app shows deterministic UI state and retry policy.
- Standardize artifact status enum: `queued | processing | ready | failed`.
- Standardize error code enum: `ARTIFACT_NOT_READY | NO_FACE_IN_POSTER | NO_DEFAULT_SCRIPT | PROVIDER_TIMEOUT | PROVIDER_FAILED | NETWORK_UNAVAILABLE`.
- Add artifact versioning contract: cache key = `posterId + artifactVersion`.

2. **Backend Production Pipeline (P0)**
- Replace in-process `setTimeout` generation with durable async jobs (AWS SQS + worker service).
- Split backend responsibilities:
  - API service: auth, poster CRUD, artifact status API.
  - Worker service: preprocess, TTS, lip-sync provider orchestration, storage upload, DB updates.
- Implement **hybrid provider strategy**:
  - Primary provider adapter: managed lip-sync API.
  - Secondary provider adapter: self-hosted Wav2Lip GPU worker.
  - Provider fallback policy: primary timeout/failure ⇒ secondary once ⇒ mark failed with error code.
- Persist stage-level timing and failure reason in DB for observability.
- Remove mock-only behavior from production route path; keep mock behind explicit `NODE_ENV=development` + feature flag.

3. **Poster Preprocessing + Data Quality Gate (P0)**
- On poster create/update, run async preprocessing:
  - Face presence detection.
  - Face box + normalized lip ROI extraction.
  - Confidence scoring and minimum threshold.
- Store per-poster metadata used by runtime alignment (not generated ad hoc during play).
- Prevent “talking” eligibility for posters failing quality checks; surface reason in admin dashboard.
- Ensure inactive posters are excluded from mobile poster index response.

4. **Storage, CDN, and Artifact Lifecycle (P0)**
- Store generated videos/artifacts in S3.
- Serve via CloudFront signed URLs (short TTL, refreshable).
- Add artifact lifecycle rules:
  - Keep latest N versions per poster.
  - Cleanup old failed/intermediate assets.
- Mobile download endpoint should return signed URL + expiry metadata.

5. **Mobile App Runtime Hardening (P0)**
- Keep existing flow entrypoint (`MainActivity -> TalkARScreen -> TalkingPhotoScreen`) but stabilize runtime behavior:
  - Poster index refresh on app start + periodic background sync.
  - Single active poster session policy with clean reset.
  - Status polling/backoff policy for `queued/processing`.
  - Deterministic state UI: scanning, preparing, ready, lost-tracking, retryable error.
- Replace heuristic transform path with ARCore projection-based overlay placement:
  - Use image pose + camera projection/view matrices every frame.
  - Map normalized lip ROI into screen quad.
  - Apply temporal smoothing + outlier rejection.
- Ensure tracking-loss behavior:
  - pause media when unstable/lost,
  - resume from last position on recovery,
  - hard timeout to reset session if tracking does not recover.

6. **Frontend (Admin Dashboard) Production Controls (P1)**
- Add artifact operations panel per poster:
  - preprocess status,
  - artifact generation status/version/provider,
  - last failure reason,
  - retry action.
- Add quality gate visibility:
  - face detected?,
  - confidence score,
  - “eligible for talking photo” flag.
- Add operational dashboard metrics widgets:
  - generation success rate,
  - median generation time,
  - provider failure breakdown,
  - queue backlog.

7. **Reliability, Security, and Ops (P0)**
- Add idempotency key for generation enqueue requests.
- Add distributed tracing/correlation IDs across API→queue→worker.
- Implement rate limiting for generation triggers.
- Secrets management via AWS Secrets Manager/SSM.
- SLO baseline:
  - artifact generation success rate target,
  - p95 generation time target,
  - API availability target.
- Alerts:
  - queue backlog threshold,
  - provider failure spike,
  - worker crash loop,
  - rising `failed` artifact ratio.

8. **Release Strategy (P0/P1)**
- Phase A (internal): managed provider only, fallback disabled but wired.
- Phase B (beta): enable fallback provider, limited poster set, monitored rollout.
- Phase C (GA): full catalog, autoscaling workers, dashboard ops enabled.
- Add rollback switches:
  - disable generation enqueue,
  - force cached-ready-only mode for mobile,
  - disable secondary provider quickly.

## Public API / Interface Additions
- `GET /api/v1/posters/:id/talking-photo` response contract finalized:
  - `{ imageId, status, version, videoUrl, lipLandmarks, posterFaceBox, confidence, errorCode, errorMessage, updatedAt }`
- New/updated endpoints:
  - `POST /api/v1/posters/:id/talking-photo/retry` (idempotent enqueue)
  - `GET /api/v1/posters/index` (mobile-optimized active poster metadata including eligibility/version hash)
  - `GET /api/v1/posters/:id/preprocess-status` (admin visibility)
- Worker/provider interface:
  - `generateTalkingArtifact({ posterId, script, language, voiceId, preferredProvider }) -> { status, artifactUrl, landmarks, confidence, provider, timings }`

## Test Plan (Must-Pass)
1. **Backend integration**
- Poster upload triggers preprocess job and persists quality metadata.
- Talking-photo enqueue creates one job per idempotency key.
- Managed-provider failure triggers secondary fallback exactly once.
- Artifact status transitions validly and terminal states are durable.

2. **Mobile integration/E2E**
- App launch shows camera preview reliably.
- Known backend poster detection triggers artifact fetch/poll.
- `ready` artifact starts playback and overlays at lip region.
- Slight camera movement keeps lip region aligned and stable.
- Out-of-frame pauses playback; reframe resumes.
- Failed artifact shows retryable UX with correct code mapping.

3. **Frontend/Admin**
- Operator can see preprocessing/artifact statuses and retry safely.
- Dashboard metrics reflect live pipeline outcomes.

4. **Non-functional**
- Load test concurrent scans + artifact requests.
- Soak test worker stability and queue drain behavior.
- Security tests for signed URL expiry and unauthorized artifact access.

## Assumptions and Defaults
- Chosen decisions:
  - Lip-sync strategy: **Hybrid from day 1**.
  - Infra target: **AWS stack**.
  - Release priority: **Reliability first**.
- Existing Android ARCore + Compose architecture remains and is hardened, not rewritten.
- Existing admin dashboard remains React/TS and is extended for ops workflows.
