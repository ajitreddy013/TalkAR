# Wav2Lip Worker Auth Contract

This defines the request verification contract for TalkAR backend -> self-hosted Wav2Lip worker calls.

## Endpoint
- `POST /generate`

## Required Request Body
- `posterId: string`
- `dialogueId: string`
- `text: string`
- `language: string`
- `version: number`
- `correlationId: string`

## Headers
1. `x-correlation-id`
- Trace identifier propagated from API -> queue -> worker.

2. `x-worker-auth-token` (optional but recommended)
- Shared secret token.
- Must match `WAV2LIP_WORKER_AUTH_TOKEN` on worker.

3. `x-worker-timestamp` + `x-worker-nonce` + `x-worker-signature` (optional but recommended)
- Signature format:
  - `signature = HMAC_SHA256(signingSecret, "${timestamp}.${nonce}.${rawJsonBody}")`
- Timestamp must be within skew window (`WAV2LIP_WORKER_MAX_SKEW_MS`, default 5m).
- Nonce must be unique per signed request within the skew window (replay protection).
- Signing secret key: `WAV2LIP_WORKER_SIGNING_SECRET`.

If signing secret is configured on worker, both signature headers are required.

## Success Response
```json
{
  "success": true,
  "videoUrl": "https://.../artifact.mp4",
  "provider": "self_hosted_wav2lip",
  "correlationId": "..."
}
```

## Error Response
```json
{
  "success": false,
  "error": "UNAUTHORIZED_WORKER_REQUEST",
  "message": "..."
}
```

## Reference Implementation
- Middleware: `backend/src/middleware/workerAuth.ts`
- Worker route template: `backend/src/scripts/wav2lipWorkerTemplate.ts`
- Local worker entrypoint: `backend/src/scripts/wav2lipWorker.ts`

## Local Development
1. Start the backend API.
2. Set the worker env vars:
   - `WAV2LIP_WORKER_AUTH_TOKEN`
   - `WAV2LIP_WORKER_SIGNING_SECRET` if you want signed requests
   - `WAV2LIP_WORKER_URL=http://127.0.0.1:8088`
   - `WAV2LIP_PATH=/path/to/Wav2Lip`
   - `WAV2LIP_CHECKPOINT_PATH=/path/to/Wav2Lip/checkpoints/wav2lip_gan.pth`
3. Run the worker:
   - `cd backend && npm run dev:wav2lip-worker`
4. Ensure `ffmpeg` and Python are available on the machine.

The worker generates audio from the script text using Google Translate TTS by default, then runs the configured Wav2Lip inference script and serves the generated MP4 from `/artifacts/...`.

## Replay Store Backends
- `WAV2LIP_NONCE_STORE=memory` (default): single-instance nonce replay protection.
- `WAV2LIP_NONCE_STORE=redis`: multi-instance replay protection.
  - Requires `WAV2LIP_NONCE_REDIS_URL`.

## Security Telemetry
- Worker auth metrics are exposed at:
  - `GET /api/v1/performance/worker-auth`
- Includes:
  - total auth requests, successes, failures
  - failure counts by reason
  - recent failure events with correlation IDs
- Worker auth health endpoint:
  - `GET /api/v1/performance/worker-auth/health`
  - Returns `200` when healthy, `503` when thresholds are breached.
  - Threshold env vars:
    - `WORKER_AUTH_HEALTH_WINDOW_MINUTES` (default `5`)
    - `WORKER_AUTH_FAILURE_RATE_THRESHOLD` (default `0.25`)
    - `WORKER_AUTH_HEALTH_MIN_REQUESTS` (default `20`)
    - `WORKER_AUTH_REASON_COUNT_THRESHOLD` (default `20`)
