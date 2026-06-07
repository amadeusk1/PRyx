# Privacy Policy  
Last updated: June 2026

PRyx (“the App”) is a personal fitness tracking application for Android. Most of your data stays on your device. This Privacy Policy explains what information the App handles, where it is stored, and when anything is sent over the internet.

---

## 1. Summary

- **No account required.** PRyx does not ask you to sign up or log in.
- **Your training log is local by default.** Personal records (PRs) and bodyweight entries are stored on your device.
- **The Live Leaderboard is optional.** Data is only sent to a server if you choose to submit a PR for review.
- **No ads, no analytics, no sale of data.** PRyx does not use advertising or third-party analytics SDKs.

---

## 2. Information Stored Locally on Your Device

The App stores the following data in private app storage or preferences on your device:

### 2.1 Fitness data
- Personal records: exercise name, weight, reps, and date
- Bodyweight entries: weight and date

### 2.2 App preferences
- Unit preference (kg or lb)
- Dark theme preference
- Dashboard layout and home-screen lift selections
- Daily reminder settings (on/off, including optional “aggressive” reminder style)
- Confirmation that you are 18+ when enabling aggressive reminders (stored as a yes/no flag only)

This local data is used to display your progress, graphs, dashboard, tools, and reminders inside the App.

Local data is **not** uploaded automatically. It remains on your device unless you use the optional Live Leaderboard feature described below, uninstall the App, or your device backup settings copy app data elsewhere (see Section 6).

---

## 3. Information Sent When You Use the Live Leaderboard (Optional)

If you choose to submit a PR to the **Live Leaderboard**, the App sends a request over the internet to a server operated by the developer (`https://www.amadeusk.dev/pryx/`).

You control what is submitted. A submission may include:

- Display name (entered by you)
- Exercise (bench, squat, or deadlift)
- Weight and reps
- Optional notes
- Optional proof media (image or video), encoded for upload

The App may also:

- Check the review status of a submission you sent
- Download the list of **accepted** leaderboard entries (name, exercise, and weight) for display in the App

**Important:**
- Submitting to the Live Leaderboard is entirely optional.
- Your local PR log and bodyweight history are **not** synced to the server.
- Proof media and display names you submit may be reviewed manually before appearing on the leaderboard.
- Do not submit information you do not want associated with your chosen display name on a public leaderboard.

---

## 4. Information We Do Not Collect

PRyx does **not**:

- Require or create user accounts
- Collect email addresses, phone numbers, or precise location inside the App
- Run third-party advertising
- Use third-party analytics, crash reporting, or attribution SDKs
- Sell or rent your data

Standard open-source networking libraries (used only to communicate with the Live Leaderboard server when you submit or view leaderboard data) do not independently collect your personal information for their own purposes.

---

## 5. Permissions

The App may request the following Android permissions:

| Permission | Purpose |
|------------|---------|
| **Internet** | Live Leaderboard submissions and viewing accepted entries |
| **Post notifications** | Optional daily training reminders |
| **Vibrate** | Notification feedback |
| **Receive boot completed** | Reschedule reminders after device restart |
| **Schedule exact alarm** | Deliver reminders at scheduled times |

You can disable notifications in the App’s settings. Reminder-related permissions are only used for that feature.

When submitting leaderboard proof, the App uses the system file picker so you can choose an image or video from your device. The App reads only the file you select for that submission.

---

## 6. How Your Data Is Stored and Deleted

### 6.1 Local storage
Fitness data and preferences are stored in the App’s internal storage and private preferences. This data is:

- Private to the App
- Not accessible by other apps under normal Android security
- Removed when you uninstall PRyx (unless restored from a device backup)

### 6.2 Device backup
The App allows Android backup (`allowBackup` is enabled). Depending on your device and Google account settings, local app data **may** be included in encrypted device backups or device-to-device transfers. This is controlled by your operating system, not by PRyx.

### 6.3 Live Leaderboard server data
Data you submit to the Live Leaderboard is stored on the developer’s server for review and, if accepted, public display on the leaderboard. If you want a submission removed, contact the developer (Section 10).

---

## 7. How Your Data Is Used

- **Local data** is used only to operate the App on your device (logging, charts, dashboard, calculators, reminders).
- **Leaderboard submissions** are used to review proof, show submission status to you, and display accepted entries on the Live Leaderboard.

PRyx does not use your data for advertising or profiling.

---

## 8. Children’s Privacy

PRyx is intended for a general audience. The App does not knowingly collect personal information from children through accounts or profiles.

The optional Live Leaderboard allows users to enter a display name and upload proof media. Parents and guardians should supervise minors using that feature.

Enabling “aggressive” reminder notifications requires confirming that you are at least 18 years old. That confirmation is stored locally as a preference flag only.

---

## 9. Data Security

Local data benefits from Android’s app sandbox. Internet submissions use standard HTTPS transport to the developer’s server.

No method of storage or transmission is completely secure. You are responsible for securing your device (screen lock, backups, and what you choose to submit to the Live Leaderboard).

---

## 10. Your Choices and Rights

You can:

- Add, edit, and delete local PR and bodyweight entries inside the App
- Turn reminders on or off in settings
- Avoid the Live Leaderboard entirely if you do not want to send data online
- Uninstall the App to remove local data from the device (subject to backup/restore behavior on your device)
- Contact the developer to request removal of a Live Leaderboard submission

Because there are no accounts, the developer cannot identify or modify your **local** training log remotely.

---

## 11. Changes to This Policy

This Privacy Policy may be updated when App features or legal requirements change. The “Last updated” date at the top will be revised accordingly. Updated versions may also be published with the App listing or project documentation.

---

## 12. Contact

If you have questions about this Privacy Policy or want to request removal of a Live Leaderboard submission:

**Email:** contact@amadeusk.dev  
**Developer:** Amadeus Kaczmarek

---

PRyx is built to keep your everyday training log private on your device. The Live Leaderboard is an optional, user-initiated feature for sharing verified lifts when you choose to participate.
