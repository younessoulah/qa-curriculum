⚠️ **AUTHOR-ONLY — DO NOT DISTRIBUTE TO LEARNERS.** Same rule as `seeded-bugs-shopping-cart.md` — see that file §0 for repo-hygiene guidance before publishing a learner-facing fork.

# Seeded-bug catalog — restful-booker-platform

Fork of mwinteringham/restful-booker-platform (`docker/restful-booker-platform`), a
multi-service Spring Boot app: `auth`, `booking`, `room`, `message`, `report`,
`branding`, plus a Next.js frontend (`assets`). Login: `admin` / `password`.

Unlike the shopping-cart app, this one **already ships with real, non-trivial defects**
in the vendored code — some clearly intentional (mwinteringham built this app to be
practiced on), some that look like plain bugs. Section 1 catalogs those as free/found-in-the-wild
bugs. Section 2 is the new catalog to seed on top, mirroring the SB-XX process
(branch `seeded/cohort-01-rbp`, one commit per bug, ID as commit message).

Because this app has both a UI (assets/) and 6 backing REST APIs, it's a good fit for
the capstone phase (manual + API + UI on one app) — keep that in mind when picking which
bugs are UI-discoverable vs API-only, so the manual-testing track and the Karate track
don't accidentally require the same discovery.

## 1. Pre-existing bugs already in the vendored code (RBP-W01..W05 — "wild")

Use these as bonus/escape-testing material, or patch them before cohort 1 if you want
the catalog in §2 to be the sole source of truth. Recommend keeping **W01 and W02** (real
functional bugs, safe to leave) and **patching W03–W05** (auth/security gaps you don't
want in a shared training environment even locally) unless you're deliberately running
the security-flavored stretch track in curriculum §9.

| ID | Module | Severity | Finding |
|---|---|---|---|
| RBP-W01 | booking | Major | Wrong HTTP status: invalid booking dates return `409 Conflict` instead of `400 Bad Request` — `BookingService.java` ~L97-99, ~L121-123. |
| RBP-W02 | room | Minor | `getUnavailableRooms` (`RoomService.java` ~L92-104) removes matched rooms from a list while iterating a fresh inner loop each pass — O(n²) and fragile; worth a boundary test with many overlapping bookings to see if it ever under-removes. |
| RBP-W03 | message | **Critical (security)** | `GET /message`, `GET /message/{id}`, `GET /message/count` (`MessageController.java` ~L22-41) have **no auth check** while `DELETE`/`PUT .../read` do (~L50-62). Any unauthenticated caller can read every contact-form submission, which includes name/email/phone (`Message` model ~L17-42). |
| RBP-W04 | booking | **Critical (security)** | `BookingDB.java` `queryBookingsById` (~L85) and `queryBookingSummariesById` (~L189) / `queryByDate` (~L201) build SQL via raw string concatenation of the `roomid`/date request params instead of `PreparedStatement` binding, unlike every other query in the file — classic SQLi. `roomid` comes straight from `@RequestParam` on `GET /booking` and `GET /booking/summary`. |
| RBP-W05 | report | Major | `ReportController.java`: `GET /report` requires a `@CookieValue("token")` with no `required=false` (~L17, so a missing cookie throws a raw container 400 instead of the app's normal 403 shape), while `GET /report/room/{id}` (~L24) has **no token parameter at all** — fully unauthenticated admin report data, and it transitively hits booking's unauthenticated `/summary` endpoint (RBP-W04's sibling). |

## 2. Bugs to seed (RBP-01..RBP-12)

| ID | Module | Category | Severity | Difficulty | Summary |
|---|---|---|---|---|---|
| RBP-01 | booking | Functional | Critical | Medium | Same-day checkin/checkout is silently accepted |
| RBP-02 | booking | Functional | Critical | Hard | Updating a booking can flag it as conflicting with itself |
| RBP-03 | room | Functional | Major | Hard | Room turnover conflict rule breaks — back-to-back bookings on checkout day get rejected |
| RBP-04 | auth | Functional | Critical | Easy | Wrong/expired credentials are accepted as valid |
| RBP-05 | room | Functional | Major | Medium | Creating a room with no `features` crashes the request (NPE → 500) |
| RBP-06 | report | Data | Major | Medium | Guest name renders literally as `null null` when a booking has empty name fields |
| RBP-07 | room | Validation | Major | Medium | Room price validation upper bound is off — `999` is silently rejected |
| RBP-08 | booking | Validation | Minor | Medium | Booking list `roomid` filter matches the wrong room |
| RBP-09 | message | Functional | Major | Easy | Marking a message as read doesn't persist — it reverts to unread |
| RBP-10 | room | Content | Minor | Easy | Room type validation pattern silently drops one valid type from the dropdown/allowlist |
| RBP-11 | auth | UI/UX | Minor | Medium | Logout doesn't actually invalidate the token server-side |
| RBP-12 | booking | UI/UX | Trivial | Easy | Booking confirmation on the frontend shows checkin/checkout dates swapped |

> Severity/category spread deliberately mirrors the shopping-cart catalog (roughly
> 2 critical / 5 major / 4 minor / 1 trivial) so a combined bug-hunt scoring rubric
> across both apps stays comparable.

## 3. Per-bug injection detail

### RBP-01 — Same-day checkin/checkout silently accepted
**File:** `booking/.../DateCheckValidator.java` (~L17). This is the entire date-range rule.
```diff
- return checkin.compareTo(checkout) < 0;
+ return checkin.compareTo(checkout) <= 0;
```
**Repro (API):** `POST /booking` with `checkin == checkout` → expect `400`, actual `200 Created`. Learner needs boundary-value testing (equivalence partitioning day 1 of Phase 1) to catch this; it's invisible without testing the exact boundary.

### RBP-02 — Update can conflict with itself
**File:** `booking/.../BookingDB.java`, `checkForBookingConflict` (~L166-172), the branch that's supposed to exclude the booking's own ID when checking for conflicts during an update.
```diff
- if (bookingIds.size() == 1 && bookingIds.get(0).equals(excludeId)) {
-     return false;
- }
+ if (bookingIds.size() == 1) {
+     return false;
+ }
```
(Adjust to whatever the actual self-exclusion variable is named at that line — the point is to drop the `.equals(excludeId)` guard so a booking of size 1 is always treated as "no conflict," which actually makes single-room updates *falsely pass* even when they do conflict with a different booking. If you want the failure mode to go the other way — self always flagged as conflicting — instead force the branch to `return true` for `bookingIds.size() >= 1`.)
**Repro (API):** `PUT /booking/{id}` on an existing booking without changing its dates → should succeed (no real conflict), verify against a second, deliberately overlapping booking to confirm the check still fires correctly for genuine conflicts. Hard because it requires a 3-request sequence (create A, create B, update A) to expose.

### RBP-03 — Turnover conflict rule breaks
**File:** `booking/.../BookingDB.java` (~L142-173), the `+1` day adjustment on `checkin` that intentionally allows a new guest's checkin to equal a prior guest's checkout date without flagging a conflict.
```diff
- LocalDate adjustedCheckin = checkin.plusDays(1);
+ LocalDate adjustedCheckin = checkin;
```
**Repro (API):** book Room 1 checkout=`2026-08-10`; book a second guest in Room 1 checkin=`2026-08-10` → currently allowed (correct hotel turnover behavior), after this change it's rejected as a false conflict. Confirmed as intentional-and-tested via `DateConflictTest.testNoConflictForOverlapOnCheckoutCheckinDate` — breaking it will fail that existing unit test, so this is also a good exercise in "does the seeded bug get caught by CI," which you may want either way depending on whether you want this bug to also demonstrate a red pipeline.

### RBP-04 — Wrong credentials accepted
**File:** `auth/.../AuthService.java`, the branch in `verify`/credential check (~L29-37) that returns the OK/FORBIDDEN decision.
```diff
- return credentialsMatch ? HttpStatus.OK : HttpStatus.FORBIDDEN;
+ return HttpStatus.OK;
```
(Match to the actual conditional structure at that line — the point is to make the login endpoint stop rejecting bad credentials.) Easiest and most severe bug in the set — flag it Critical/Easy because a single login attempt with a wrong password exposes it, but it also breaks the admin-only paths across every other module (auth token is shared), so grade it as one bug, not five.
**Repro:** `POST /auth/login` with `admin`/`wrong-password` → expect `403`, actual `200` with a usable token.

### RBP-05 — Creating a room with no features crashes
**File:** `room/.../RoomDB.java`, the `Room(ResultSet)` constructor / insert path (~L74-82) assumes a non-null `features` array; `room/.../Room.java` model has no `@NotNull` on `features` (confirmed gap, ~L24-41 area has `@Pattern`/`@Min`/`@Max` but nothing on `description`/`image`/`features`).
Rather than *adding* a bug here, this NPE already exists as a gap — to make it deterministically reproducible for a bug hunt, add a test-data path that exercises it: seed `room/seed.sql` (or leave it to the learner to discover via `POST /room` with `"features": null` or `features` omitted entirely).
**Repro (API):** `POST /room` with a valid payload but `features` omitted → expect `400` with a validation message, actual `500 Internal Server Error`.

### RBP-06 — Guest name renders as "null null"
**File:** `report/.../ReportService.java`, `getAllRoomsReport` (~L36), the display-name concatenation.
No diff needed to introduce the bug — it's already present (`b.getFirstname() + " " + b.getLastname() + " - Room: " + r.getRoomName()` with no null-guards). To make it deterministically findable, seed one booking via `booking/seed.sql` (or have the learner create one via the API) with empty-string or missing `firstname`/`lastname`.
**Repro:** create a booking with `firstname=""`, `lastname=""` → `GET /report` shows `" - Room: <name>"` (or `"null null - Room: <name>"` depending on whether the DB stores empty string vs null) instead of a graceful fallback.

### RBP-07 — Room price upper bound off
**File:** `room/.../Room.java`, `roomPrice` validation (~L40-41).
```diff
- @Min(1)
- @Max(999)
+ @Min(1)
+ @Max(998)
```
**Repro (API):** `POST /room` with `roomPrice: 999` → per the model's own documented range this should be accepted, actual `400`. Classic boundary-value bug, deliberately placed for the equivalence-partitioning/BVA exercises in Phase 1.

### RBP-08 — Booking list `roomid` filter matches the wrong room
**File:** `booking/.../BookingController.java`, `GET /` list handler (~L24) forwarding `roomid` to the DB query, or `BookingDB.java`'s `queryBookingsById` (~L85 — already flagged as W04's concatenation site; if you patch W04's SQLi by switching to a `PreparedStatement`, this is the natural place to instead seed an off-by-one on the bound parameter, e.g. binding `roomid - 1`).
```diff
- statement.setInt(1, roomid);
+ statement.setInt(1, roomid - 1);
```
**Repro (API):** `GET /booking?roomid=3` returns bookings for room `2`. Only seed this if you've patched W04 first — don't stack a functional bug on top of an unpatched SQL-injection code path, it'll confuse which one the learner is meant to find.

### RBP-09 — Marking a message as read doesn't persist
**File:** `message/.../MessageService.java`, `markAsRead` (paired with the auth-checked path ~L62-82 per the earlier survey).
```diff
  public boolean markAsRead(int id, String token) {
      if (!authRequest.postCheckAuth(token)) return false;
-     return messageDB.updateReadStatus(id, true);
+     messageDB.updateReadStatus(id, true);
+     return true;
  }
```
i.e. the method reports success without actually persisting the update (swap in whatever the real DB call signature is — the point is to decouple the return value from the actual write, e.g. by not awaiting/checking the DB call's own success).
**Repro (UI/API):** `PUT /message/{id}/read` returns `200`, but `GET /message/{id}` immediately after still shows `read: false`; in the admin UI the message reverts to "unread" styling on refresh.

### RBP-10 — Room type allowlist silently drops one valid type
**File:** `room/.../Room.java`, `type` validation (~L24).
```diff
- @Pattern(regexp = "Single|Double|Twin|Family|Suite")
+ @Pattern(regexp = "Single|Double|Twin|Suite")
```
**Repro (API/UI):** `POST /room` with `"type": "Family"` → per the README/UI dropdown this should be a valid type, actual `400`. Good Content/Minor bug because it's an allowlist regression, easy to spot once a learner tries every dropdown option (equivalence partitioning again).

### RBP-11 — Logout doesn't invalidate the token
**File:** `auth/.../AuthService.java`, `deleteToken` (~L39-47).
```diff
  public void logout(String token) {
-     authDB.deleteToken(token);
+     // token intentionally left valid
  }
```
**Repro (API):** `POST /auth/logout` returns `200`; immediately after, reuse the same token on any admin endpoint (e.g. `DELETE /room/{id}`) → expect `403`, actual succeeds. Good session-management finding for the manual-testing charter on the admin panel.

### RBP-12 — Frontend swaps checkin/checkout on the confirmation screen
**File:** `assets/` (Next.js) — the booking confirmation component that renders the two dates after a successful `POST /booking`. Swap which field renders in which label:
```diff
- <span>Check-in: {booking.checkin}</span>
- <span>Check-out: {booking.checkout}</span>
+ <span>Check-in: {booking.checkout}</span>
+ <span>Check-out: {booking.checkin}</span>
```
(Locate the exact component under `assets/` — it wasn't in scope of the backend survey; grep `assets/` for `checkin` / `checkout` JSX usage before seeding.) Trivial/UI-only — the API response and stored data are correct, only the confirmation screen lies, so this is a pure UI bug that a Playwright/manual check would catch but a Karate API test would not, which is deliberately the point (cross-track differentiation, same as the SB catalog's UI-only bugs).

## 4. Notes for the answer key / rubric

- Mirror `docs/templates/bug-hunt-answer-key.md`'s table structure for RBP-01..RBP-12
  once injected; add a second "Wild bugs" section for whichever of W01-W05 you decide
  to leave in, so scoring can tell seeded finds from wild finds apart (same "escapes &
  extras" bonus mechanic already defined there: +2%/bug, cap +6%).
- RBP-04 (auth bypass) is high blast-radius — verify no other seeded bug's repro steps
  assume auth actually works before finalizing the set for a given cohort branch.
- RBP-03 will fail an existing unit test (`DateConflictTest`) if injected as-is; decide
  per cohort whether you want the CI pipeline red (extra "read a failing test" exercise)
  or want to also adjust/skip that test on the seeded branch.
