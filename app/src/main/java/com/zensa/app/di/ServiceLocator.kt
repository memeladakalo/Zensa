package com.zensa.app.di


import android.content.Context
import com.zensa.app.BuildConfig
import com.zensa.app.auth.SsoManager
import com.zensa.app.core.network.NetworkMonitor
import com.zensa.app.notifications.PushTokenRegistrar
import com.zensa.app.data.local.ZensaDatabase
import com.zensa.app.data.preferences.TokenStorage
import com.zensa.app.data.preferences.UserPreferences
import com.zensa.app.data.remote.AuthInterceptor
import com.zensa.app.data.remote.ZensaApiService
import com.zensa.app.data.repository.AchievementRepository
import com.zensa.app.data.repository.AssignmentRepository
import com.zensa.app.data.repository.AuthRepository
import com.zensa.app.data.repository.FileRepository
import com.zensa.app.data.repository.GamificationCloudRepository
import com.zensa.app.data.repository.ModuleRepository
import com.zensa.app.data.repository.NoteRepository
import com.zensa.app.data.repository.SettingsRepository
import com.zensa.app.data.repository.StudySessionRepository
import com.zensa.app.data.repository.SubtaskRepository
import com.zensa.app.data.repository.SyncRepository
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit


object ServiceLocator {

    private lateinit var tokenStorageInstance: TokenStorage
    private lateinit var userPreferences: UserPreferences
    private lateinit var database: ZensaDatabase
    private lateinit var api: ZensaApiService
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var ssoManagerInstance: SsoManager

    /** Application-lifetime scope for work that must outlive a screen (e.g. logout clean-up). */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var authRepository: AuthRepository
        private set
    lateinit var moduleRepository: ModuleRepository
        private set
    lateinit var assignmentRepository: AssignmentRepository
        private set
    lateinit var subtaskRepository: SubtaskRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var studySessionRepository: StudySessionRepository
        private set
    lateinit var noteRepository: NoteRepository
        private set
    lateinit var achievementRepository: AchievementRepository
        private set
    lateinit var syncRepository: SyncRepository
        private set
    lateinit var gamificationCloudRepository: GamificationCloudRepository
        private set
    lateinit var fileRepository: FileRepository
        private set
    lateinit var pushTokenRegistrar: PushTokenRegistrar
        private set

    fun init(context: Context) {
        if (::tokenStorageInstance.isInitialized) return
        val app = context.applicationContext

        tokenStorageInstance = TokenStorage(app)
        userPreferences = UserPreferences(app)
        database = ZensaDatabase.get(app)
        networkMonitor = NetworkMonitor(app)
        ssoManagerInstance = SsoManager(app)

        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        // Logging: request/response BODIES contain passwords and JWTs, so they must
        // never be logged. Debug builds log only the request line + status (BASIC),
        // release builds log nothing, and the Authorization header is always redacted.
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                    else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStorageInstance))
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        api = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ZensaApiService::class.java)

        authRepository = AuthRepository(api, tokenStorageInstance)

        moduleRepository = ModuleRepository(
            dao = database.moduleDao(),
            api = api
        )

        assignmentRepository = AssignmentRepository(
            dao = database.assignmentDao(),
            moduleDao = database.moduleDao(),
            api = api
        )

        subtaskRepository = SubtaskRepository(
            dao = database.subtaskDao(),
            assignmentDao = database.assignmentDao(),
            api = api
        )

        settingsRepository = SettingsRepository(
            preferences = userPreferences,
            api = api
        )

        studySessionRepository = StudySessionRepository(
            dao = database.studySessionDao(),
            moduleDao = database.moduleDao(),
            api = api
        )

        noteRepository = NoteRepository(
            dao = database.noteDao(),
            moduleDao = database.moduleDao(),
            api = api
        )

        achievementRepository = AchievementRepository(
            dao = database.achievementDao()
        )

        syncRepository = SyncRepository(
            appContext = app,
            queueDao = database.syncQueueDao(),
            moduleDao = database.moduleDao(),
            assignmentDao = database.assignmentDao(),
            subtaskDao = database.subtaskDao(),
            studySessionDao = database.studySessionDao(),
            noteDao = database.noteDao(),
            api = api,
            networkMonitor = networkMonitor
        )

        gamificationCloudRepository = GamificationCloudRepository(api)
        fileRepository = FileRepository(api, app.contentResolver)
        pushTokenRegistrar = PushTokenRegistrar(app, api, tokenStorageInstance)

        // Wire the sync repository back into the other repositories so
        // every failed server write is queued for background sync.
        moduleRepository.syncRepository = syncRepository
        assignmentRepository.syncRepository = syncRepository
        subtaskRepository.syncRepository = syncRepository
        studySessionRepository.syncRepository = syncRepository
        noteRepository.syncRepository = syncRepository
    }

    /**
     * Logs the user out. The push token is unregistered first (best effort, max 4 s) so the
     * next person using this phone does not receive this user's notifications; the local
     * session (JWT) is ALWAYS cleared afterwards, even if the server cannot be reached.
     */
    fun logout() {
        appScope.launch {
            withTimeoutOrNull(4_000) {
                try { pushTokenRegistrar.unregisterCurrentToken() } catch (_: Exception) { }
            }
            tokenStorageInstance.clear()
        }
    }

    fun tokenStorage(): TokenStorage = tokenStorageInstance
    fun ssoManager(): SsoManager = ssoManagerInstance
    fun userPreferences(): UserPreferences = userPreferences
    fun networkMonitor(): NetworkMonitor = networkMonitor
}
