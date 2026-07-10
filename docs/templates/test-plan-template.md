# Test Plan — <Application Name> — <Cohort/Version>

| Field | Value |
|---|---|
| Author | |
| Date | |
| Jira Epic | QAT-XX |
| App version / commit | |
| Environment | Local Docker / Hosted Demo |

## 1. Objective
One paragraph: what this testing effort must establish, and for whom.

## 2. Scope
Feature areas in scope (map each to a Jira Epic):
- Booking flow (QAT: Booking Flow)
- Admin panel (QAT: Admin Panel)
- Contact / messaging (QAT: Contact / Messaging)
- Branding & content (QAT: Branding & Content)

## 3. Out of Scope
List explicitly, with a one-line reason each (e.g., "Performance — covered in Phase 2 bonus track").

## 4. Test Approach
- Techniques used per area: equivalence partitioning, boundary value analysis, decision tables, state transitions, error guessing, exploratory (session-based)
- Test data strategy: where data comes from, how state is reset between tests

## 5. Risks & Assumptions
| # | Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| 1 | Demo environment resets data mid-session | Medium | Medium | Run local Docker; document reset behavior |

## 6. Entry Criteria
- Environment reachable and seeded
- Test cases reviewed and in "Ready" state in QAT

## 7. Exit Criteria
- 100% of planned test cases executed
- No open Critical/Major defects without a documented decision
- All defects logged in BUGS with required fields complete

## 8. Deliverables
- Executed test cases in QAT (status + evidence)
- Defect reports in BUGS
- Session notes in `manual-testing/session-notes/`
- Summary report (pass/fail counts, defect stats, coverage gaps)

## 9. Schedule
| Activity | Effort | Date |
|---|---|---|
| Test case authoring | | |
| Execution round 1 | | |
| Retest / verification | | |
