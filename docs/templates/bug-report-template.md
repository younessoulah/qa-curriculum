# Bug Report Template

**Summary (Jira title) format:** `[Area] Short factual statement of the defect`
> Good: `[Cart] Removing an item removes a different item from the cart`
> Bad: `cart broken`

---

## Environment
- App: <name> @ <URL or localhost port>
- Build/branch: <commit hash or branch>
- Browser + version:
- Viewport / device:

## Preconditions
What state must exist before the steps (logged in? items in cart?).

## Steps to Reproduce
1.
2.
3.

## Expected Result
What should happen, and *why you expect it* (spec, convention, or comparison to other parts of the app).

## Actual Result
What actually happens. Be literal — quote exact UI text and values.

## Evidence
- Screenshot / screen recording attached (mandatory)
- Console errors / network responses if relevant

## Impact Assessment
- **Severity:** Critical / Major / Minor / Trivial — technical impact
- **Frequency:** Always / Intermittent (X out of Y attempts)
- **Workaround:** exists? describe it

## Additional Notes
Related test case: QAT-XX · Possibly related bugs: BUGS-XX

---

### Quality bar (checklist before hitting Create)
- [ ] A stranger could reproduce this from the steps alone
- [ ] One defect per ticket (no bundling)
- [ ] Searched BUGS for duplicates first
- [ ] Severity justified in Impact, not guessed
- [ ] Evidence attached
- [ ] Labels: `bug-hunt`, `cohort-01`; fields Environment, Bug Category, Found In Phase set
