# Changelog

All notable changes to the TC LMS SG Android app (formerly Tertiary Courses SG).
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [1.4] — 2026-11-19 · versionCode 3

Learners and trainers, in step with the iOS app (TC LMS SG 1.4).

### Added
- Trainers can sign in: the classes you teach (WSQ and non-WSQ) appear in My Classes and Calendar next to the classes you attend, marked **Teaching** or **Attending**.
- Trainer class screen: take attendance for each session, show the learner check-in QR code, open your trainer courseware and see how many learners have given feedback.
- Class reminders for learners and trainers: 3 days before a class, the evening before each class day and an hour before it starts. Switch them off in Account.
- Class materials, documents and certificates now open inside the app, with Share and Open in Browser.
- Edit Profile: change your first name, last name and phone number.
- Appearance: choose System, Light or Dark; the choice is saved to your account and the app follows it.

### Changed
- My Classes reloads when you come back to the app (at most once every 5 minutes).
- New permissions: notifications (class reminders) and run-at-startup (to keep reminders after a restart). Reminders are scheduled on the phone; no push service is used.
- Sign-in and About now describe the app as serving learners and trainers.

### Technical
- New API calls: `GET/POST /mobileapi/profile`, `GET /mobileapi/trainer/classes`, `GET /mobileapi/trainer/class`, `POST /mobileapi/trainer/attendance`; every new field is optional, so the app still works against a site that has not been updated.
- Dark colour scheme (Material 3) across every screen; new files `TrainerScreens.kt`, `ProfileScreens.kt`, `DocumentViewer.kt`, `Reminders.kt`.
- `com.google.zxing:core` draws the check-in QR code.

### Play submission
- Production track, full rollout, published through the Play Developer API on 2026-11-19 (corporate account, Tertiary Infotech Academy Pte Ltd). Store listing text updated in the same edit.

## [1.3] — 2026-11-04 · versionCode 2

Rewritten as **TC LMS SG**, the learner app for non-WSQ courses of www.tertiarycourses.com.sg —
a feature-for-feature port of the iOS app (TC LMS SG 1.3).

### Added
- Learner sign-in with an emailed 6-digit code or the website password (`/mobileapi/auth/*`);
  session token stored with EncryptedSharedPreferences; expired sessions return to sign-in.
- **My Classes** — Current / Upcoming / Past tabs, class cards (status, dates, times, mode, trainer,
  pending-payment registrations), pull to refresh.
- **Class detail** — overview with venue and online-class link, **Check In Now** digital attendance,
  class materials opening in the browser, session-by-session attendance, certificate card.
- **Course feedback** — the site's in-house survey rendered from the live template
  (read-only class fields, 1–5 star ratings, text, email, dropdown), submitted once per class.
- **Calendar** — month grid with class-day dots, the selected day's classes, and
  "Add My Classes to Calendar" via the calendar app's insert screen (no calendar permission).
- **Certificates** — View PDF and Share.
- **Billing** — order cards with items, totals, refunds, payment method and invoice numbers.
- **Account** — profile, Browse Courses, website, Send Feedback (WhatsApp), About, Privacy Policy,
  Sign Out, and Delete Account with confirmation.
- Signed-out **Courses** tab: the site's public non-WSQ catalog from `/mobileapi/catalog`, cached on
  device with a bundled offline snapshot; search, category filter, collapsible category sections,
  course detail with View Schedule & Register and WhatsApp enquiry.
- `Site.kt` — the single per-country file (SG / MY / GH builds share every other source file).
- New launcher icon ("TC" over "LMS SG"), adaptive icon, Play icon and feature graphic.
- Debug-only screenshot hooks (`UITEST_SIGNIN`, `START_TAB`, `OPEN_RUN_ID`, `OPEN_SURVEY`,
  `OPEN_BUCKET`, `UITEST_SIGNOUT` intent extras), compiled out of release builds.

### Changed
- App name "Tertiary Courses SG" → "TC LMS SG"; versionName 1.0 → 1.3 to match iOS.
- Explicit `User-Agent: TCLMS-Android/<version> (SG; <applicationId>)` on every request
  (the site's bot filter rejects some default agents).
- IPv4-first DNS ordering so a broken IPv6 route doesn't stall the first request.

### Removed
- The bundled WSQ catalog and the SSG grant calculator (the app now covers non-WSQ courses only).

## [1.0] — 2026-07-06 · versionCode 1

### Added
- Browse and search the Tertiary Courses Singapore WSQ course catalog by keyword and category.
- Course details with fees (before and with 9% GST), funding schemes (WSQ, SFC, SFEC, PSEA, UTAP), and learning outcomes.
- SSG grant calculator: Baseline (50%), MCES (70%), and SME Enhanced Training Support (70%) with SkillsFuture Credit deduction and net fee estimate.
- Check Schedule & Register link to the course page on www.tertiarycourses.com.sg.
- Course enquiry and app feedback via WhatsApp (+65 8866 6375).
- About page with official site, developer, and data source links (TP Gateway, MySkillsFuture).

### Play submission
- First release: closed testing track, All Testers list, Singapore.
