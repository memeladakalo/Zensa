# Zensa Part 3 — Server connect, languages, gamification, SSO, offline sync

## What this package includes

### Android (`app/`)
- **Server connection**: `BuildConfig.API_BASE_URL` → `http://10.0.2.2:8080/` (emulator). Change for device LAN IP or HTTPS.
- **Room + repositories**: Modules, assignments, subtasks, notes, study sessions, achievements, sync queue.
- **Offline sync**: WorkManager `SyncWorker` + `SyncScheduler` (periodic + constrained to network).
- **Screens & navigation**: Splash, Login, Register, Dashboard, Modules, Assignments, Notes, Zen Timer, Achievements, Settings via `ZensaNavHost`.
- **Languages**: English (`values`), IsiZulu (`values-zu`), Sesotho (`values-nso`), IsiXhosa (`values-xh`). Switch via Settings / `LocaleManager`.
- **Gamification**: Achievements + XP/level/streak models (`domain/gamification`).
- **SSO**: Google Sign-In helper (`core/sso/GoogleSsoHelper`). Set `WEB_CLIENT_ID`, then exchange `idToken` with backend `/auth/sso`.
- **Firebase push**: `ZensaFirebaseMessagingService` + notification channel. Replace `app/google-services.json` with a real Firebase project file.
- **Application class**: `ZensaApplication` initializes ServiceLocator, WorkManager sync, notification channel.

### Backend (`zensa-api/zensa-api/`)
- Spring Boot 3 + JWT (`zensa.jwt.secret` required, ≥32 chars).
- MySQL (or H2 for tests).
- Part 3 stubs: **Azure Blob** (`AzureBlobService`), **Cosmos DB** (`CosmosSyncStore`), SSO config keys, Firebase credentials path.
- Existing JWT auth, modules, assignments, notes, study sessions, sync endpoints.

## Open in Android Studio

1. Unzip this archive.
2. Open the **root folder that contains `settings.gradle.kts` and `app/`** (this directory).
3. Sync Gradle (JDK 17 recommended for the Android module).
4. Emulator: run backend first, then Run app.
5. Physical device: set `API_BASE_URL` in `app/build.gradle.kts` to your PC’s LAN IP, e.g. `"http://192.168.1.10:8080/"`.

## Run the Spring Boot API

```bash
cd zensa-api/zensa-api
export JWT_SECRET="$(openssl rand -base64 48)"
export DB_PASSWORD="your_mysql_password"   # or use H2 for quick tests
./gradlew bootRun
```

API listens on `http://localhost:8080/`.

Optional cloud env vars:
- `AZURE_STORAGE_CONNECTION_STRING`, `AZURE_STORAGE_CONTAINER`
- `AZURE_COSMOS_URI`, `AZURE_COSMOS_KEY`, `AZURE_COSMOS_DATABASE`
- `GOOGLE_SSO_CLIENT_ID`
- `FIREBASE_CREDENTIALS_PATH`

## Gradle dependencies (Android)

Already declared in `gradle/libs.versions.toml` + `app/build.gradle.kts`:
- Room, WorkManager, Retrofit, OkHttp, DataStore, Security Crypto
- Compose Material3, Navigation
- Firebase BOM (Messaging, Analytics)
- Play Services Auth + Credentials / Google ID (SSO)

Replace `app/google-services.json` with a real file from Firebase Console before shipping push. Until then the app still builds and runs offline/local features.

## Checklist

| Item | Status |
|------|--------|
| Application class | `ZensaApplication` present |
| Room + DAOs + repositories | Present |
| WorkManager offline sync | Present |
| Screens + navigation | Present |
| EN / ZU / NSO / XH strings | Present |
| Gamification (XP + achievements) | Present |
| SSO (Google) | Helper + manifest ready |
| Firebase push | Service + channel |
| Spring Boot + JWT | Present |
| Azure Blob / Cosmos hooks | Config + service stubs |

## Notes

- Cleartext HTTP is enabled for emulator/dev only.
- SSO and FCM need real Google/Firebase project credentials.
- Azure/Cosmos stubs no-op when env vars are empty so local MySQL + JWT still works.
