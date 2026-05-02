# Phase 2 to Phase 3 Handoff Checklist

Date: 2026-05-02

## Goal
Single checklist to decide if TalkAR can move from Phase 2 to Phase 3.

## A) Repo Gates (must pass)
- [ ] Backend phase-2 tests pass:
  - `cd backend && npm run test:phase2`
- [ ] Backend build passes:
  - `cd backend && npm run build`
- [ ] Security report exists and passes:
  - `docs/phase2-security-report.json` with `"passed": true`
- [ ] Load/soak report exists and passes thresholds:
  - `docs/phase2-load-soak-report.json`
  - `latencyMs.p95 <= 6000`
  - `totals.successRate >= 0.95`
- [ ] Promotion summary passes:
  - `cd backend && npm run phase2:promotion:summary`
  - must output `"readyForPhase3": true`

## B) Staging/Beta Promotion Gates (must pass before GA/Phase 3 release execution)
- [ ] Internal profile rollout verified on controlled poster set.
- [ ] Beta profile rollout verified with fallback enabled on limited cohort.
- [ ] Rollback drills completed:
  - disable enqueue
  - ready-only mode
  - disable fallback
- [ ] Security drills completed in staging:
  - signed URL expiry
  - unauthorized artifact access rejection
  - worker-auth replay/abuse rejection
- [ ] Mobile runtime matrix completed on real devices:
  - startup camera preview
  - poster detection + status polling
  - stable lip overlay on slight movement
  - tracking loss pause + recovery resume

## C) Evidence Bundle (attach before signoff)
- [ ] `docs/phase2-security-report.json`
- [ ] `docs/phase2-load-soak-report.json`
- [ ] `npm run test:phase2` output log
- [ ] Staging alert screenshots (`/ops/alerts`, admin dashboard)
- [ ] Toggle drill logs (before/after runtime mode states)
- [ ] Mobile E2E screenshots/video clips
- [ ] Incident/recovery notes (if any)

## Decision
- Move to Phase 3 only when all items in A + B + C are checked.

