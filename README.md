# PRyx 
PRyx is a simple strength training companion for Android. It lets you log personal records (PRs) and bodyweight locally, see clean progress graphs on a dashboard, and (optionally) compare your lifts on a live leaderboard.  
Built with Kotlin and Jetpack Compose, it focuses on clarity, speed, and minimal interaction—so you can log your progress without friction.
https://play.google.com/store/apps/details?id=com.amadeusk.liftlog

---

## Overview

PRyx allows users to log strength training data and visualize progress through clean, lightweight graphs and a dashboard-style home screen.  

- Core PR and bodyweight data is stored **locally on the device** using internal storage, ensuring privacy and reliable offline use.
- An optional **live leaderboard** lets you submit proof-backed PRs for comparison; this feature uses a remote API but does not require an account.

The application includes three primary areas:

- Dashboard with at-a-glance progress  
- Personal Records (PRs) for any exercise  
- Daily bodyweight entries  

Users can add, edit, delete, and filter entries directly from the interface.

---

## Project structure

The codebase is organized to keep screens, UI components, charts, and utilities separate:

- `app/src/main/java/com/amadeusk/liftlog/LiftLogRoot.kt`: App scaffold, navigation, and main state wiring
- `app/src/main/java/com/amadeusk/liftlog/ui/screens/`: Screens (Dashboard, PR/Bodyweight tabs live in `LiftLogRoot`, Tools, Info, Leaderboard)
- `app/src/main/java/com/amadeusk/liftlog/ui/components/`: Reusable UI building blocks (selectors, list items, dialogs, date input)
- `app/src/main/java/com/amadeusk/liftlog/ui/charts/`: Chart rendering (`ProfessionalLineChart`, `ExerciseGraph`, `BodyWeightGraph`)
- `app/src/main/java/com/amadeusk/liftlog/util/UiUtils.kt`: Shared enums + helpers (ranges, filters, units, daily quote, streak)
- `app/src/main/java/com/amadeusk/liftlog/data/`: Data models + local persistence (PRs, bodyweight, user settings)

---

## Features

### Dashboard
- Daily quote card with lifter-focused motivation  
- Current daily streak (based on recent PR or bodyweight logs)  
- Lifts section with quick graphs for Bench, Squat, and Deadlift  
- Bodyweight preview graph with latest value  
- “This Week” snapshot (exercises tracked, average intensity, volume vs last week, bodyweight change, strength trend, fatigue estimate)  
- Shortcuts to Tools and the Leaderboard  

### PR Tracking
- Record exercise name, weight, reps, and date  
- Edit and delete existing entries  
- Filter PRs by:
  - This month  
  - This year  
  - All time  
- View progress on an automatically generated line graph for the selected exercise

### Bodyweight Tracking
- Log daily bodyweight with date  
- Edit and delete entries  
- Filter by:
  - This month  
  - This year  
  - All time  
- Graph displays automatically once enough data points are available

### Live Leaderboard (optional)
- Submit PRs with required proof (image or short video)  
- Server-side validation before a PR is accepted  
- See how your lifts compare on a live leaderboard  
- Uses the same kg/lb unit selection as the rest of the app  

### Tools & Calculators
- TDEE (maintenance calories)  
- One-rep max estimates  
- Protein and body fat–related tools  

### User Interface
- Jetpack Compose UI using Material 3 components  
- Dashboard-first layout focused on at-a-glance progress  
- Custom line charts with smooth draw-in animations and tooltips  
- Dialog-based forms for adding and editing entries  
- Floating action button for quick data entry

### Data Storage & Privacy
- Uses Android internal storage for core persistence  
- Separate files for:
  - PR entries  
  - Bodyweight entries  
  - Unit and theme preferences  
- No accounts and no cloud sync for your personal log history  
- The live leaderboard feature communicates with a remote API only when you choose to submit or view leaderboard data

---

## Screenshots

(Place images in `/screenshots` and update paths as needed.)

- PRs screen  
  `screenshots/pryx_prs.png`

- Add PR dialog  
  `screenshots/pryx_add_pr.png`

- Bodyweight screen  
  `screenshots/pryx_bodyweight.png`

---

## Technology Stack

- Kotlin  
- Jetpack Compose  
- Material 3  
- File-based persistence using Android internal storage  
- Date handling via `java.time.LocalDate`  
- Custom graph rendering using Compose `Canvas`
