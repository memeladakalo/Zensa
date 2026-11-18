Zensa – Calm, Focused Studying

Zensa is an Android study-companion app that helps students organise modules, manage assignments, keep study notes, run focused study sessions and track their progress, all in one place. The name combines Zen and Sense: a calm, organised approach to studying.

Demo video: 
PowerPoint on YouTube: https://studio.youtube.com/video/Smow0cRn1-0/edit 
Live API: https://zensa-api-g0n5.onrender.com
Platform: Android 8.0 (API 26) or higher · Tested on a Samsung Galaxy A04e

Table of contents

	1.	Features
	2.	Screenshots
	3.	Quick start for lecturers and evaluators
	4.	Architecture and technology
	5.	Repository structure
	6.	Google Sign-In (SSO)
	7.	Developer setup
	8.	Database
	9.	API overview
	10.	Testing
	11.	Known limitations
	12.	Security
	13.	Team

Features

Feature	Description
Registration and login	Email and password accounts with input validation. Passwords are hashed with BCrypt and sessions use JWTs.
Google Sign-In (SSO)	Sign in with a Google account using OAuth 2.0 Authorization Code flow with PKCE.
Home Dashboard	Overview of modules, upcoming deadlines and study activity, plus a progress card when gamification is on.
Modules	Create, edit and view modules, each with a code, name, description and colour.
Assignments	Due dates, priority, status and subtasks with completion tracking.
Notes	Create and edit study notes, optionally linked to a module.
Zen Timer	Focused study sessions (25 minutes by default) with pause and resume. Sessions are saved.
Achievements	Points, levels, streaks and badges earned from completed work and study sessions.
Settings	Edit profile name, theme, notifications, study defaults, language, gamification, sync, change password and log out.
Languages	English, isiZulu, Sesotho and isiXhosa.
Offline support	Local Room database with a sync queue that uploads changes when the phone reconnects.

Screenshots

Screenshots of every screen (Splash, Login, Registration, Dashboard, Modules, Assignments, Notes, Zen Timer, Achievements, Settings, Google Sign-In) and of the Render and Railway evidence are included in the project report.

Quick start for lecturers and evaluators

You do not need any development tools to run the app.

	1.	Use the app-debug.apk provided with the submission, or build it yourself (see Build the APK below).
	2.	On the Android phone, allow installs from the source you will use (Files, Chrome or Google Drive).
	3.	Copy the APK to the phone and tap it to install.
	4.	If Google Play Protect asks to scan the app, allow the scan and wait for it to finish.
	5.	Open Zensa, then either register with an email and a password of at least 8 characters, or tap Sign in with Google.

Please note: the backend runs on Render's free plan. The first request after a period of inactivity can take 30-60 seconds while the server wakes up. If you see "Cannot reach the server", open https://zensa-api-g0n5.onrender.com once, wait for a response, then try again.

Build the APK

./gradlew assembleDebug

The file is created at app/build/outputs/apk/debug/app-debug.apk. For Google Sign-In to work in your own build, add your Android OAuth client ID to local.properties first (see Developer setup).

Suggested walkthrough: Register or sign in → add a module → add an assignment with subtasks → write a note → run the Zen Timer → open Achievements → open Settings and change the language.

Architecture and technology

┌───────────────────────────┐
│  Zensa Android app        │
│  Kotlin + Jetpack Compose │
└─────────────┬─────────────┘
              │ HTTPS / JSON (Retrofit)
              ▼
┌───────────────────────────┐
│  Spring Boot REST API     │
│  Hosted on Render         │
└─────────────┬─────────────┘
              │ JDBC
              ▼
┌───────────────────────────┐
│  MySQL database           │
│  Hosted on Railway        │
└───────────────────────────┘

Layer	Technology
Android	Kotlin, Jetpack Compose, Room, DataStore, Retrofit and OkHttp, AppAuth
Backend	Spring Boot 3.4 (Kotlin), Spring Security with JWT, Spring Data JPA and Hibernate
Data	MySQL on Railway
Hosting	Render (API, Docker), Railway (database), GitHub (source control)

Repository structure

Zensa/
├── app/                  Android application (Kotlin, Jetpack Compose)
├── zensa-api/zensa-api/  Spring Boot REST API (deployed to Render with Docker)
├── render.yaml           Render deployment settings
├── store-assets/         App icon and store graphics
└── README.md             This file

Google Sign-In (SSO)

Zensa signs users in with Google using the AppAuth library (Authorization Code flow with PKCE). No client secret is stored on the phone.

	1.	The app opens Google's sign-in page and receives an ID token.
	2.	The app sends the token to POST /api/auth/google.
	3.	The API checks the token's signature, issuer, audience and expiry against Google's public keys, then returns a normal Zensa JWT.

Configuration (for developers)

	•	In Google Cloud Console, create an Android OAuth client with package name com.zensa.app and your debug SHA-1 (./gradlew signingReport).
	•	In the client's Advanced settings, enable custom URI scheme.
	•	Put the client ID in local.properties as zensa.google.clientId= (see below).
	•	Set the same client ID as GOOGLE_CLIENT_IDS in the Render environment.

Developer setup

Prerequisites: Android Studio (latest stable), JDK 21, a MySQL database, and a Google Cloud project with an Android OAuth client.

Android app

	1.	Clone the repository.
	2.	Create local.properties in the project root. It is gitignored, so never commit it:

sdk.dir=C\:\\path\\to\\Android\\Sdk
zensa.google.clientId=YOUR_ANDROID_CLIENT_ID.apps.googleusercontent.com

	3.	Open the project in Android Studio and sync Gradle.
	4.	Run the app. It connects to the live API through BuildConfig.API_BASE_URL.

Backend (local)

cd zensa-api/zensa-api
./gradlew bootRun

Variable	Description
DB_URL	JDBC URL of the MySQL database
DB_USERNAME	Database user
DB_PASSWORD	Database password
JWT_SECRET	Secret of at least 32 characters
GOOGLE_CLIENT_IDS	Comma-separated Google client IDs allowed for SSO

The tables are created automatically by Hibernate (ddl-auto=update) on first start.

Database

Table	Purpose
users	Accounts: name, unique email, BCrypt password hash
modules	A student's modules
assignments	Assignments with due date, priority and status
subtasks	Smaller tasks belonging to an assignment
notes	Study notes
study_sessions	Zen Timer sessions
device_tokens	Firebase push tokens (used by push notifications)

Every record belongs to a user, so each student only sees their own data.

API overview

Base URL: https://zensa-api-g0n5.onrender.com. All endpoints except /api/auth/** need a Bearer JWT.

Area	Endpoints
Auth	POST /api/auth/register, POST /api/auth/login, POST /api/auth/google
Profile	GET/PUT /api/users/me, PUT /api/users/me/password
Modules	GET/POST /api/modules, PUT/DELETE /api/modules/{id}
Assignments	GET/POST /api/assignments, PUT/DELETE /api/assignments/{id}
Subtasks	GET/POST /api/assignments/{id}/subtasks, PUT/DELETE /api/subtasks/{id}
Notes	GET/POST /api/notes, PUT/DELETE /api/notes/{id}
Study sessions	GET/POST /api/study-sessions, DELETE /api/study-sessions/{id}
Sync and gamification	POST /api/sync, GET/PUT /api/gamification/me

Testing

Testing was done manually on a physical Android phone, and the results are recorded in the project report. Cases covered include:

	•	Registration and login with valid details
	•	Login with incorrect credentials (access denied with a clear message)
	•	Invalid email and password validation messages
	•	Google Sign-In
	•	Installing and launching the app on a physical device
	•	Database record counts for each table

Unit and instrumented tests are in app/src/test and app/src/androidTest.

Known limitations

	•	Cold start: the free Render plan sleeps when idle, so the first request can take up to a minute.
	•	Push notifications: implemented in the app, but not enabled on the hosted server because Firebase server credentials are not configured.
	•	The app is installed directly from an APK and is not published on the Google Play Store.

Security

	•	No secrets are committed. local.properties, google-services.json, keystores and service-account files are gitignored.
	•	The JWT secret and database credentials live only in environment variables on Render and Railway.
	•	Passwords are stored as BCrypt hashes. Google ID tokens are verified on the server and never stored.

Team

Name	Main responsibility
Sapho Mahlathi	Zensa Android application and mobile testing
Thembela Dakalo Memela	Compassion Cloud website, planning, documentation and testing

Zensa – Calm, Focused Studying. This project was built for educational purposes.
