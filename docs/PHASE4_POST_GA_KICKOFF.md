# TalkAR Phase 4 Post-GA Kickoff

Date: 2026-05-07
Status: ACTIVE
Execution Window: 2026-05-07 to 2026-05-21 (14 days)
Priority Mode: Ops Stability First

## Entry Criteria Validation
- [x] `docs/evidence/phase3-ga/go-no-go.md` finalized with `Final decision: GO`.
- [x] `docs/evidence/phase3-ga/approvals.md` has required signoffs.
- [x] `step-10/25/50/100` cutover gate summaries show `"canPromote": true`.
- [x] Phase 3 closeout status marks ready to move.

## Phase 4 Goals
- Stabilize GA operations for a full business cycle.
- Enforce SLO governance and incident readiness.
- Prove rollback and drain safety under controlled drills.
- Start optimization only after stable Week 1 operations.

## Day 0 Governance Actions (Complete before operations)
- [ ] Mark production readiness phase gate as unblocked for Phase 4.
- [ ] Lock Phase 3 evidence bundle as baseline reference.
- [ ] Start `docs/evidence/phase4-post-ga/phase4-execution-log.md`.

## Week 1 Checklist (Days 1-7)
- [ ] Publish and lock SLO targets (availability, success rate, p95 generation latency).
- [ ] Confirm dashboard panels for queue backlog, DLQ backlog, oldest message age, provider failures, failed ratio.
- [ ] Define alert severities and paging thresholds for each key metric.
- [ ] Run rollback drill (`forceReadyOnly`, `disableEnqueue`, restore) and save drill evidence.
- [ ] Run worker drain-mode drill and verify controlled queue behavior.
- [ ] Publish on-call ownership map and incident escalation routing.
- [ ] Run daily ops review and store daily snapshots.

## Week 2 Checklist (Days 8-14)
- [ ] Confirm no unresolved Sev-1/Sev-2 from Week 1.
- [ ] Execute small-step tuning: worker concurrency, batch size, prewarm limit.
- [ ] Re-validate success rate, p95 latency, and failure ratio after each tuning change.
- [ ] Publish weekly stability summary with top failing posters/providers.

## Exit Criteria
- 14 consecutive days meeting SLO targets.
- No repeating Sev-1/Sev-2 pattern from known Phase 3 risks.
- Optimization changes validated with no reliability regression.

## References
- `docs/PHASE3_CLOSEOUT_STATUS.md`
- `docs/PRODUCTION_READINESS_EXECUTION_PLAN.md`
- `docs/PHASE4_OPS_STABILITY_RUNBOOK.md`
