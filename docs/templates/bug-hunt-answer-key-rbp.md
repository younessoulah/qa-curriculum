# 🔒 PRIVATE — Bug Hunt Answer Key — restful-booker-platform — Cohort 01

> Injected on branch `seeded/cohort-01`, one commit per bug, commit message = `chore(RBP-XX): ...`.
> Full injection diffs and repro steps: `docker/seeded-bugs-restful-booker-platform.md`.
> Security-flavored pre-existing gaps (RBP-W03/W04/W05) were patched before seeding —
> see commit `6f1d62d` on `worktree-seed-bug-plan`. RBP-W01/W02 remain as bonus "wild" finds.

## Seeded bugs

| ID | Module | Category | Expected Severity | Difficulty | Location | Injected commit |
|---|---|---|---|---|---|---|
| RBP-01 | booking | Functional | Critical | Medium | `DateCheckValidator.java` | c7eb403 |
| RBP-02 | booking | Functional | Critical | Hard | `BookingDB.java` (checkForBookingConflict) | df801ad |
| RBP-03 | booking | Functional | Major | Hard | `BookingDB.java` (checkForBookingConflict) | 7b95d2d |
| RBP-04 | auth | Functional | Critical | Easy | `AuthService.java` (queryCredentials) | a9446e8 |
| RBP-05 | room | Functional | Major | Medium | (no injection — pre-existing gap, see below) | — |
| RBP-06 | report | Data | Major | Medium | (no injection — pre-existing gap, see below) | — |
| RBP-07 | room | Validation | Major | Medium | `Room.java` (roomPrice) | 6f90939 |
| RBP-08 | booking | Validation | Minor | Medium | `BookingDB.java` (queryBookingsById) | bad5898 |
| RBP-09 | message | Functional | Major | Easy | `MessageService.java` (markAsRead) | 5153b3f |
| RBP-10 | room | Content | Minor | Easy | `Room.java` (type pattern) | 9e81991 |
| RBP-11 | auth | UI/UX | Minor | Medium | `AuthService.java` (deleteToken) | b3b7ab5 |
| RBP-12 | booking (frontend) | UI/UX | Trivial | Easy | `assets/src/components/reservation/BookingForm.tsx` | dc67a62 |

## Wild bugs (bonus finds, not injected — already in the vendored app)

| ID | Module | Severity | Finding |
|---|---|---|---|
| RBP-W01 | booking | Major | Invalid booking dates return `409 Conflict` instead of `400 Bad Request`. |
| RBP-W02 | room | Minor | `getUnavailableRooms` uses a fragile O(n²) removal loop — worth boundary-testing with many overlapping bookings. |

## Per-bug detail

### RBP-01
- **Repro (API):** `POST /booking/` with `checkin == checkout`.
- **Expected:** `400`.
- **Actual:** `201 Created` — verified live, returns a valid booking.

### RBP-02
- **Repro (API):** create booking A, create a genuinely overlapping booking B in the same room, then `PUT /booking/{A}` without changing A's dates.
- **Expected:** the update succeeds (no real conflict with itself).
- **Actual:** any single overlapping match (including itself) is now treated as a conflict, so the update is wrongly rejected.

### RBP-03
- **Repro (API):** book Room 1 with `checkout=2026-08-10`, then book a second guest in Room 1 with `checkin=2026-08-10`.
- **Expected:** allowed (normal hotel turnover — this was a deliberately-tested case, see `DateConflictTest.testNoConflictForOverlapOnCheckoutCheckinDate`, which will now fail if run without `-DskipTests`).
- **Actual:** rejected as a false conflict.

### RBP-04
- **Repro (API):** `POST /auth/login` with `admin` / any wrong password.
- **Expected:** `403`.
- **Actual:** `200` with a usable token — verified live. (Note: the corresponding unit test `AuthServiceTest.testInvalidCredentials` fails with an unrelated 500 rather than the expected 200, because the test's mock for `insertToken` was never stubbed for this code path — the live behavior is the 200 confirmed above, not the test's 500.)

### RBP-05
- **Repro (API):** `POST /room/` with `features` omitted or `null`.
- **Expected:** `400` with a validation message.
- **Actual:** `500 Internal Server Error` (NPE in `RoomDB`'s ResultSet constructor path). Pre-existing gap — no commit needed to inject it, it's already true of the clean app.

### RBP-06
- **Repro:** create a booking with empty `firstname`/`lastname`, then `GET /report/`.
- **Expected:** a graceful fallback in the display name.
- **Actual:** renders as `" - Room: <name>"` / `"null null - Room: <name>"` depending on how the DB stores the empty value. Pre-existing gap — no commit needed.

### RBP-07
- **Repro (API):** `POST /room/` with `roomPrice: 999`.
- **Expected:** accepted (per the model's own documented `@Min(1)`-to-999 range).
- **Actual:** `400`, rejected — verified live, error message literally says "must be less than or equal to 998".

### RBP-08
- **Repro (API):** `GET /booking/?roomid=1` vs `GET /booking/?roomid=2`.
- **Expected:** each returns that room's own bookings.
- **Actual:** off-by-one — verified live, `roomid=1` returned empty and `roomid=2` returned what should have been room 1's bookings.

### RBP-09
- **Repro (API):** `PUT /message/{id}/read`, then `GET /message/` and check the `read` flag.
- **Expected:** `read: true`.
- **Actual:** `PUT` returns `202` (claims success) but `read` stays `false` — verified live.

### RBP-10
- **Repro (API):** `POST /room/` with `"type": "Family"`.
- **Expected:** accepted (per the room type list in the app/README).
- **Actual:** `400`, rejected — verified live.

### RBP-11
- **Repro (API):** log in, `POST /auth/logout`, then reuse the same token on any endpoint (e.g. `GET /message/`).
- **Expected:** `403` after logout.
- **Actual:** logout returns `200` but the token still works afterward — verified live (message list returned `200` with data after logout).

### RBP-12
- **Repro (UI):** complete a booking through the reservation form.
- **Expected:** confirmation shows "check-in - check-out" in that order.
- **Actual:** dates are swapped on the confirmation screen only — the stored booking and API response are correct, this is a pure display bug (Playwright/manual catches it, a Karate API test would not).

## Scoring sheet — <learner name>

| Learner ticket | Matched seeded ID | Valid? | Severity match? | Report quality (0–3) | Notes |
|---|---|---|---|---|---|

**Scoring formula** — same as the shopping-cart answer key, detection rate denominator = 12 (seeded) + 2 (wild, optional bonus per `qa-curriculum-map.md`'s "escapes & extras" rule).
