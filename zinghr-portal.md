# ZingHR Portal — Login & Attendance API

## Login

**URL:** `https://portal.zinghr.com/2015/pages/authentication/login.aspx`

| Field | Value |
|-------|-------|
| Company Code | `IIITB` |
| Employee Code | `2010` |
| Password | `Farman@@5` |

After submit → redirects to `https://zingnext.zinghr.com/portal`

---

## API Base

```
https://mservices.zinghr.com
```

Auth uses token-based headers. On every request, the portal sends:
- `authToken` — JWT from login session
- `subscriptionName` — `IIITB`

Token refresh endpoint:
```
POST https://mservices.zinghr.com/zingauth/api/Auth/RefreshToken
Body: {"subscriptionName":"IIITB","authToken":"<token>"}
```

---

## Punch In / Punch Out APIs

### Status Check (GET — fires on dashboard load)
```
GET https://mservices.zinghr.com/tna/api/v2/PunchIn/GetPunchInStatusWithLastSwipe
```
Returns current punch state + last swipe info.

### Attendance Widget Data (POST — fires on dashboard load)
```
POST https://mservices.zinghr.com/tna/api/v1/Tna/EmployeeAttendanceWidget
Body: {"attendanceDate":"2026-04-21","dashboard":"true"}
```
Returns today's attendance summary shown in the widget.

### Attendance Field Config (GET — fires on dashboard load)
```
GET https://mservices.zinghr.com/tna/api/v1/tna/GetAttendanceFieldConfig
```
Returns field configuration for the TNA module.

### Punch In / Punch Out Action (inferred — NOT clicked)
Based on the TNA service prefix pattern, the action endpoint is expected at:
```
POST https://mservices.zinghr.com/tna/api/v2/PunchIn/PunchIn
POST https://mservices.zinghr.com/tna/api/v2/PunchIn/PunchOut
```
These were not triggered — discovered via network inspection without clicking the buttons.

---

## Notes

- All TNA endpoints live under `/tna/api/`
- Auth endpoints live under `/zingauth/api/`
- Portal frontend: `zingnext.zinghr.com` — backend services: `mservices.zinghr.com`

---

## Automation Update (2026-04-21)

### Problem observed
- `PunchInPunchOut` returned HTTP `200`, but automation occasionally interpreted direction incorrectly (or failed before punch due to fragile action inference from UI/status text).

### Latest decision (UI-only mode)
- Do **not** call attendance APIs directly from automation.
- Automation must interact only with what is visible inside WebView (on-screen punch controls).
- Root cause of `Status: API probe result: {}` was async JS (`async` IIFE) in `evaluateJavascript`; WebView surfaced unresolved Promise output (`{}`) instead of final value.

### Fix applied in app automation
- File updated: `app/src/main/java/com/example/zingauto/MainActivity.kt`
- Removed direct `fetch()` API automation path from dashboard step.
- Added synchronous UI interaction script that:
	- finds visible, clickable `Punch In` / `Punch Out` controls,
	- scrolls to target,
	- dispatches pointer/click events,
	- triggers native `.click()` fallback,
	- returns deterministic `clicked:punchIn|punchOut|unknown` or `needs_details:*`.

### Why this is safer
- Matches real user interaction path in WebView and avoids backend/CORS/session edge cases from direct API calls.
- Avoids async Promise serialization issue in `evaluateJavascript`.

### Quick validation checklist
1. Login manually once and reach dashboard.
2. Run automation once when currently **not punched in**; expect message containing `Punched In`.
3. Run again after some time; expect message containing `Punched Out`.
4. If message mismatches expected state, verify account/session token belongs to the same employee code.

---

## Browser Automation Run (2026-04-21)

### Request executed
- Logged in to portal using documented credentials.
- Reached dashboard at `https://zingnext.zinghr.com/portal`.
- Clicked the on-screen `Punch Out` control in the attendance widget.

### Observed result
- Dashboard remained accessible after click.
- Attendance widget showed `Last Swipe: Today at 4:50 PM`.
- A full-page capture was saved as `punchout-run-2026-04-21.png`.

### Notes
- Punch control text still rendered as `Punch Out` after the click in this run.
- The updated `Last Swipe` timestamp indicates the action was triggered on the dashboard UI path.

---

## Android Rebuild Run (2026-04-21)

### User action
- Existing automation activity file was deleted.
- Recreated `app/src/main/java/com/example/zingauto/MainActivity.kt` from scratch.

### New implementation behavior
- Maintains account list UI (add/delete credentials, run automation).
- Opens `https://portal.zinghr.com/2015/pages/authentication/login.aspx` for each account.
- Waits for login form readiness, fills company/employee/password, and clicks login.
- Waits for dashboard detection (URL and DOM checks).
- Finds visible `Punch Out`, dispatches pointer/click events, and verifies `Last Swipe` text.
- Clears cookies between accounts and continues sequentially.

### Validation
- Kotlin diagnostics for `MainActivity.kt` show no editor errors.

---

## Android UI Target Update (2026-04-21)

### Requirement applied
- Automation now clicks the attendance action button shown in the card (the right-side button next to `Last Swipe`) without searching for `Punch In` or `Punch Out` text.

### Implementation change
- On dashboard, automation scrolls slowly in visible steps until `Last Swipe` marker is found.
- It identifies the attendance card around that marker and clicks the right-most visible clickable control inside that card.
- Keeps UI-only interaction path and avoids API calls.

### Expected visible behavior
- Step-by-step status updates while scrolling.
- Action click occurs on the attendance card button regardless of whether label currently says Punch In or Punch Out.

---

## Hold + Update Wait (2026-04-21)

### Requirement applied
- Added a visible hold before clicking the attendance action button so the target is clearly shown before interaction.
- Added post-click polling that waits for `Last Swipe` text to change on the punch card.

### Implementation details
- If exact button container `div.MuiBox-root.jss175.jss166` exists, automation uses it first.
- Otherwise, it falls back to attendance-card context selection around `Last Swipe` and clicks the right-side action control.
- Wait window: 10 checks every 2 seconds for `Last Swipe` update.

---

## Debug Fix: Premature Completion (2026-04-21)

### Root cause
- Automation finalized account flow even when punch verification failed because session cleanup/advance ran regardless of update confirmation.

### Fix applied
- `runAutomationForCredential` now returns success/failure.
- Automation only clears cookies and moves to next account when success is confirmed.
- On failure, it stays on the same account/session for retry instead of ending.
- Added up to 3 click attempts and dual verification signals:
	- `Last Swipe` change
	- attendance action label change (for cases where swipe text is delayed).
