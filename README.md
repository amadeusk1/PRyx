# PRyx 
PRyx is a simple, offline personal record (PR) and bodyweight tracking application for Android.  
Built with Kotlin and Jetpack Compose, it focuses on clarity, speed, and minimal interaction—so you can log your progress without friction.
https://play.google.com/store/apps/details?id=com.amadeusk.liftlog

---

## Overview

PRyx allows users to log strength training data and visualize progress through clean, lightweight graphs.  
All data is stored **locally on the device** using internal storage, ensuring full privacy and reliable offline use.

The application includes two primary tracking features:

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

### User Interface
- Jetpack Compose UI using Material 3 components  
- Clean, minimal layout focused on usability  
- Dialog-based forms for adding and editing entries  
- Floating action button for quick data entry

### Data Storage
- Uses Android internal storage for persistence  
- Separate files for:
  - PR entries  
  - Bodyweight entries  
  - Unit preferences  
- No accounts, no cloud sync, and no network dependency

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
