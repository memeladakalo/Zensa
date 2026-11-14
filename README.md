Zensa – Student Productivity & Study Management App
Tagline: Calm, Focused Studying.

About the Project
Zensa is an Android student productivity and study management application. It helps students organise modules, manage assignments, take study notes, run focused study sessions, track progress, and earn achievements.

The name Zensa combines Zen and Sense — a calm, organised approach to studying.

Key Features
Feature	Description
Authentication	Email/password + Google Sign-In (OAuth 2.0 / PKCE)
Dashboard	Overview of modules, assignments, and study activity
Modules	Create and manage academic modules
Assignments	Track deadlines and subtasks
Study Notes	Create and organise notes
Zen Timer	Focused study sessions
Achievements	Progress and milestone tracking
Offline Support	Local Room database with sync
Push Notifications	Deadline reminders (Firebase Cloud Messaging)
Tech Stack
Android
Kotlin + Jetpack Compose
Room (local database)
DataStore (preferences)
Retrofit + OkHttp
AppAuth (Google Sign-In with PKCE)
Backend
Spring Boot 3.4 (Kotlin)
Spring Security + JWT
Spring Data JPA + Hibernate
MySQL
Infrastructure
Backend hosting: Render (free tier)
Database: Railway MySQL (public TCP proxy)
Source control: GitHub
Architecture
┌─────────────────────────┐
│   Zensa Android App     │
│   (Kotlin / Compose)    │
└───────────┬─────────────┘
            │ HTTPS / JSON
            ▼
┌─────────────────────────┐
│  Spring Boot API        │
│  (Render)               │
│  https://zensa-api-g0n5 │
│  .onrender.com          │
└───────────┬─────────────┘
            │ JDBC
            ▼
┌─────────────────────────┐
│  MySQL Database         │
│  (Railway)              │
└─────────────────────────┘
For Lecturers / Evaluators
If you only need to run the app (no development setup required):

Download the latest app-debug.apk (or the release APK provided with the submission).
On your Android phone:
Open Settings → Security (or Settings → Apps → Special app access).
Allow installation from the source you will use (Files / Chrome / etc.).
Transfer the APK to the phone (USB, email, Google Drive, etc.).
Open the APK file and tap Install.
Open the Zensa app.
Note: The app talks to a live backend hosted on Render. The first request after a period of inactivity can take 30–60 seconds while the free-tier server wakes up. This is normal.

The Google Sign-In option requires a Google account. Email/password registration is also available.

Getting Started
Prerequisites
Android Studio (latest stable)
JDK 21
A Google Cloud project with an OAuth Android client ID
Railway MySQL instance (or any MySQL) with public access
Render account (for backend deployment)
Android App Setup
Clone the repository.
Create local.properties in the project root (do not commit it):
sdk.dir=C\:\\path\\to\\Android\\Sdk
zensa.google.clientId=YOUR_ANDROID_CLIENT_ID.apps.googleusercontent.com
Open the project in Android Studio and sync Gradle.
The app uses BuildConfig.API_BASE_URL (currently pointing at the live Render service).
Backend Setup (Local)
cd zensa-api/zensa-api
./gradlew bootRun
Required environment variables:

Variable	Description
DB_URL	JDBC URL to MySQL
DB_USERNAME	Database user
DB_PASSWORD	Database password
JWT_SECRET	At least 32 characters
GOOGLE_CLIENT_IDS	Comma-separated Google client IDs
Deployed Environment
The live API is hosted on Render:

https://zensa-api-g0n5.onrender.com

Database credentials are stored as environment variables on the Render service (never committed).

Google Sign-In
The app uses the AppAuth library with the Authorization Code + PKCE flow. No client secret is required on the device.

Client ID is injected at build time from local.properties → BuildConfig.GOOGLE_CLIENT_ID
Redirect scheme is derived automatically (com.googleusercontent.apps.<client-id-prefix>)
Security Notes
No secrets are committed to the repository.
local.properties, keystore files, and service-account JSON files are gitignored.
JWT secret and database credentials live only in environment variables on Render / Railway.
Project Structure
Zensa/
├── app/                          # Android application
│   └── src/main/java/com/zensa/app/
├── zensa-api/zensa-api/          # Spring Boot backend
│   └── src/main/kotlin/com/zensa/api/
├── local.properties              # Local config (gitignored)
└── README.md
License
This project is for educational / portfolio use.


**Zensa – Calm, Focused Studying.**
