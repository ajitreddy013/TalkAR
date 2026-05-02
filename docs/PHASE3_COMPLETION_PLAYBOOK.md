# TalkAR Phase 3 Completion Playbook

Date: 2026-05-02
Purpose: Complete all remaining Phase 3 GA gates and move safely to Phase 4.

## Current Status
- Phase 3 GA is **not complete** yet.
- The rollout is currently blocked at step 10 because the per-step evidence files are missing.
- The evidence files in `docs/evidence/phase3-ga/` now contain a documented `NO-GO` decision and blocked approvals state.
- Phase 4 must remain blocked until every item in this playbook is true.

## 1) Definition of Done (must all be true)
- `docs/evidence/phase3-ga/prewarm-report.json` exists with successful output.
- `docs/evidence/phase3-ga/step-10/cutover-gate-summary.json` has `"canPromote": true`.
- `docs/evidence/phase3-ga/step-25/cutover-gate-summary.json` has `"canPromote": true`.
- `docs/evidence/phase3-ga/step-50/cutover-gate-summary.json` has `"canPromote": true`.
- `docs/evidence/phase3-ga/step-100/cutover-gate-summary.json` has `"canPromote": true`.
- For each step (`10/25/50/100`), these files exist:
  - `load-soak-report.json`
  - `security-report.json`
  - `mobile-matrix.json`
  - `alerts-summary.json`
- `docs/evidence/phase3-ga/go-no-go.md` finalized with `Final decision: GO`.
- `docs/evidence/phase3-ga/approvals.md` signed by Engineering, SRE/Platform, Product, Security.

## 1.1) What is already present
- `docs/evidence/phase3-ga/prewarm-report.json` exists, but it still needs a real successful output body.
- `docs/evidence/phase3-ga/go-no-go.md` is finalized as `NO-GO` with current blockers listed.
- `docs/evidence/phase3-ga/approvals.md` is finalized as blocked pending Phase 3 gate completion.
- `docs/evidence/phase3-ga/step-10/cutover-gate-summary.json` currently fails because load, security, mobile, and alerts evidence are missing.

## 2) Prerequisites
Run from `backend/` unless noted.

### 2.1 Required env vars
```bash
export STAGING_API_BASE_URL="https://<staging-api-domain>"
export PROD_API_BASE_URL="https://<prod-api-domain>"
export PHASE3_EVIDENCE_ROOT="../docs/evidence/phase3-ga"
```

### 2.2 Rotate admin token and set a fresh one (recommended)
```bash
export JWT_SECRET="<jwt-secret>"
NEW_TOKEN=$(node -e "const jwt=require('jsonwebtoken'); console.log(jwt.sign({id:'phase3-admin',role:'admin'}, process.env.JWT_SECRET, {expiresIn:'2h'}));")
export PHASE3_ADMIN_TOKEN="$NEW_TOKEN"
```

If the staging service is unavailable or the token cannot be validated, stop here and fix connectivity before collecting rollout evidence.

### 2.3 Quick connectivity checks
```bash
curl -s "$STAGING_API_BASE_URL/api/health" | jq .
curl -s "$STAGING_API_BASE_URL/api/v1/posters/ops/stats" -H "Authorization: Bearer $PHASE3_ADMIN_TOKEN" | jq .
```

If these fail, fix URL/token before continuing.

## 3) Repo preflight (must pass)
```bash
npm run test:phase2
npm run build
npm run phase2:promotion:summary
```

## 4) Refresh baseline evidence
### 4.1 Catalog sweep
```bash
PHASE3_BASE_URL="$STAGING_API_BASE_URL" \
PHASE3_ADMIN_TOKEN="$PHASE3_ADMIN_TOKEN" \
npm run phase3:catalog:sweep | tee "$PHASE3_EVIDENCE_ROOT/catalog-sweep.json"
```

### 4.2 Prewarm (retry-friendly)
If prior timeout happened, lower scope and retry:
```bash
PHASE3_BASE_URL="$STAGING_API_BASE_URL" \
PHASE3_ADMIN_TOKEN="$PHASE3_ADMIN_TOKEN" \
PHASE3_PREWARM_LIMIT=10 \
npm run phase3:catalog:prewarm | tee "$PHASE3_EVIDENCE_ROOT/prewarm-report.json"
```

If successful, optionally run again with `PHASE3_PREWARM_LIMIT=20`.

## 5) Collect per-step evidence and run gates
Repeat for each step: `10`, `25`, `50`, `100`.

For each step, the intended order is:
1. Capture alerts summary.
2. Save load/soak evidence.
3. Save security drill evidence.
4. Save mobile matrix evidence.
5. Run the cutover gate.
6. Proceed only when the gate reports `canPromote: true`.

### 5.1 Capture alerts summary
```bash
STEP=<10|25|50|100>
mkdir -p "$PHASE3_EVIDENCE_ROOT/step-$STEP"

curl -s "$STAGING_API_BASE_URL/api/v1/posters/ops/alerts" \
  -H "Authorization: Bearer $PHASE3_ADMIN_TOKEN" \
  | tee "$PHASE3_EVIDENCE_ROOT/step-$STEP/alerts-summary.json"
```

### 5.2 Add load/soak report
Store real test output JSON at:
```bash
$PHASE3_EVIDENCE_ROOT/step-$STEP/load-soak-report.json
```
Minimum thresholds expected by gate logic:
- success rate high enough for promotion
- p95 generation latency within target

### 5.3 Add security report
Store security drill output JSON at:
```bash
$PHASE3_EVIDENCE_ROOT/step-$STEP/security-report.json
```
Must indicate pass.

### 5.4 Add mobile matrix report
Store mobile validation JSON at:
```bash
$PHASE3_EVIDENCE_ROOT/step-$STEP/mobile-matrix.json
```
Must cover detection, overlay stability, loss/recovery, retry UX.

### 5.5 Run cutover gate
```bash
PHASE3_EVIDENCE_ROOT="$PHASE3_EVIDENCE_ROOT" \
PHASE3_STEP="$STEP" \
npm run phase3:cutover:gate | tee "$PHASE3_EVIDENCE_ROOT/step-$STEP/cutover-gate-summary.json"
```

### 5.6 Promotion decision per step
Proceed only if gate file contains:
```json
{"canPromote": true}
```
If false: remediate missing/failed checks, re-run 5.1-5.5 for that step.

### 5.7 Step completion checklist
Mark a step complete only when all of these are true:
- `alerts-summary.json` exists for that step.
- `load-soak-report.json` exists for that step.
- `security-report.json` exists for that step.
- `mobile-matrix.json` exists for that step.
- `cutover-gate-summary.json` exists and contains `"canPromote": true`.

## 6) Final go/no-go and approvals
### 6.1 Finalize decision
Edit:
- `docs/evidence/phase3-ga/go-no-go.md`

Set final decision to `GO` only when all four step gates are true. The current record is `NO-GO` until the missing evidence is collected.
Include the exact evidence links and a short rationale for why Phase 4 can start when the gates do pass.

### 6.2 Collect signoffs
Edit:
- `docs/evidence/phase3-ga/approvals.md`

Required approvals:
- Engineering lead
- SRE/Platform
- Product owner
- Security reviewer

Use one approval entry per role with name, UTC timestamp, decision, and notes. The current record should remain blocked until the Phase 3 gates pass.

## 7) One-command verification sweep
Run from `backend/`:

```bash
for s in 10 25 50 100; do
  echo "=== step-$s ==="
  test -f "$PHASE3_EVIDENCE_ROOT/step-$s/cutover-gate-summary.json" && echo "gate file: ok" || echo "gate file: missing"
  rg -n '"canPromote"\s*:\s*true' "$PHASE3_EVIDENCE_ROOT/step-$s/cutover-gate-summary.json" && echo "canPromote: true" || echo "canPromote: false"
done

for s in 10 25 50 100; do
  for f in load-soak-report.json security-report.json mobile-matrix.json alerts-summary.json; do
    test -f "$PHASE3_EVIDENCE_ROOT/step-$s/$f" || echo "missing step-$s/$f"
  done
done

rg -n 'Final decision:\s*GO' "$PHASE3_EVIDENCE_ROOT/go-no-go.md" || echo 'go-no-go not finalized as GO'
rg -n 'APPROVED' "$PHASE3_EVIDENCE_ROOT/approvals.md" || echo 'approvals missing APPROVED entries'
```

## 8) Exit criteria to move Phase 4
Move to Phase 4 only after:
- all 4 gate summaries are `canPromote=true`
- all required per-step evidence files exist
- go/no-go is `GO`
- approvals are complete

Then update:
- `docs/PHASE3_CLOSEOUT_STATUS.md` to `READY TO MOVE TO PHASE 4`
- `docs/PRODUCTION_READINESS_EXECUTION_PLAN.md` phase gate to unblocked

Also archive the final evidence bundle and keep the rollback toggles available until Phase 4 stabilization is complete.

## 9) Rollback controls (keep ready during rollout)
```bash
# force ready-only
curl -s -X POST "$PROD_API_BASE_URL/api/v1/posters/ops/toggles" \
  -H "Authorization: Bearer $PHASE3_ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"forceReadyOnly":true}' | jq .

# disable enqueue
curl -s -X POST "$PROD_API_BASE_URL/api/v1/posters/ops/toggles" \
  -H "Authorization: Bearer $PHASE3_ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"disableEnqueue":true}' | jq .

# disable fallback
curl -s -X POST "$PROD_API_BASE_URL/api/v1/posters/ops/toggles" \
  -H "Authorization: Bearer $PHASE3_ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"enableFallback":false}' | jq .

# restore normal
curl -s -X POST "$PROD_API_BASE_URL/api/v1/posters/ops/toggles" \
  -H "Authorization: Bearer $PHASE3_ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"disableEnqueue":false,"forceReadyOnly":false,"enableFallback":true}' | jq .
```
