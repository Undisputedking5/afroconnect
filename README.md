# AfroConnect

AfroConnect is an Android marketplace that connects African freelancers with clients, locally and globally.
It is built with Kotlin, Jetpack Compose (Material 3) and Firebase.

## Features

**Everyone**
- Email sign-up and sign-in, password reset, and a choice of **Client** or **Freelancer** role
- Real-time chat with photo sharing, read receipts, unread badges and online/last-seen status
- In-app notifications for new proposals, hires and reviews
- A map of on-site jobs and nearby freelancers (OpenStreetMap, so no API key is needed)
- Profiles with a photo, about section, links and a profile-completion meter
- Light, dark and system themes
- Change password, log out and delete account

**Freelancers**
- Discover jobs with search, category filters, Urgent/Remote/On-site filters and sorting
- "Recommended for you" jobs that match your skills
- Apply with a cover letter and a bid, or withdraw an application
- Save jobs, and track applications under Active, Hired and Closed
- A portfolio gallery, skills, hourly rate, availability and an optional spot on the map
- Ratings and reviews from clients

**Clients**
- A dashboard with your stats and your active and past jobs
- Post, edit, close, reopen and delete jobs, with budget, currency, work type, location pin, deadline and urgency
- Review proposals, message applicants, and hire with one tap
- Mark a job completed and leave a 1–5★ review, which updates the freelancer's rating
- A talent directory with search, availability and sorting, plus a map view

## Download the app (Android)

Every push to `master` builds a signed APK on GitHub Actions and publishes it as the **latest** release:

1. On your phone, open **https://github.com/Undisputedking5/afroconnect/releases/tag/latest**
2. Download `AfroConnect.apk`
3. Open it. If asked, allow your browser to *install unknown apps*.

Builds from other branches are attached to their workflow run as an artifact (**Actions** tab → run → *Artifacts*).
To publish a versioned release, push a tag such as `v2.0.0`.

## Firebase setup (one time)

The app uses the existing Firebase project `africonnect-ab9c9` (`app/google-services.json`). In the Firebase console:

1. **Authentication → Sign-in method**: turn on **Email/Password**.
2. **Realtime Database**: create it if needed, then deploy the rules from this repo.
3. **Storage**: create the default bucket, then deploy the rules.

Deploy the rules with the Firebase CLI:

```bash
npm i -g firebase-tools
firebase login
firebase deploy --only database,storage
```

Or paste `database.rules.json` and `storage.rules` into the console by hand.
**Do not leave the database in test mode**: test mode lets anyone read and write all of your data.

### Data layout

```
users/{uid}                     profile (name, role, skills, rating, location…)
jobs/{jobId}                    job post, with applicants/{uid}: true
proposals/{jobId}/{uid}         cover letter + bid (readable by the job owner and the applicant)
userApplications/{uid}/{jobId}  index of a freelancer's applications
savedJobs/{uid}/{jobId}         bookmarks
chats/{uidA_uidB}/messages      chat messages
conversations/{uid}/{otherUid}  inbox entries
reviews/{freelancerUid}/{id}    client reviews
notifications/{uid}/{id}        in-app notifications
```

Jobs, users and chats created by the earlier version of the app are still read correctly.

## Automated end-to-end test

`.github/workflows/e2e.yml` runs on pull requests and on pushes to `master`. It builds the app pointed at local Firebase emulators, which apply this repo's security rules, so it never touches your real project. It then walks through the whole marketplace flow on an Android emulator: register a client, post a job, register a freelancer, apply, review the proposal, hire, chat and switch to dark theme. The run fails if any step breaks or the app crashes. Screenshots of every step are published to the `ci-screenshots` branch.

## Building locally

Open the project in Android Studio (Ladybug or newer, JDK 21) and run the `app` configuration, or:

```bash
./gradlew assembleDebug
```

## Release signing

CI signs builds with `keystore/debug.keystore`. That key is committed on purpose, so every build installs over the previous one.
**Before publishing on the Google Play Store**, create your own key and keep it secret:

```bash
keytool -genkeypair -v -keystore release.keystore -alias afroconnect -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.keystore   # copy the output
```

Add these repository secrets (**Settings → Secrets and variables → Actions**): `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`. CI then signs with your key automatically.

Play Store notes: the Play Store does not accept apps whose package name starts with `com.example`.
Choose a real ID such as `com.afroconnect.app`, change `applicationId` in `app/build.gradle.kts`, then register that ID as a new Android app in Firebase and replace `google-services.json` with the one it gives you.

## Project structure

```
app/src/main/java/com/example/africonnect/
├── MainActivity.kt          theme, osmdroid and Firebase setup
├── data/Repository.kt       all Firebase reads and writes
├── model/Models.kt          data classes + tolerant snapshot parsing
├── navigation/NavGraph.kt   routes, role-based bottom navigation
├── screens/                 Auth, Home (Discover / Dashboard / Talent), Jobs, Chat, Profile, Map, Settings…
├── ui/components/           reusable Compose components (cards, chips, avatar, fields…)
├── ui/theme/                colours, typography, shapes (light + dark)
├── util/Utils.kt            time/money/geo formatting, location, preferences
└── viewmodel/               AppViewModel (shared session state), AuthViewModel, ChatViewModel
```
