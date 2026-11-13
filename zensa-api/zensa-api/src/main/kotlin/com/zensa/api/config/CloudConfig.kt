package com.zensa.api.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.zensa.api.service.cosmos.CosmosGamificationStore
import com.zensa.api.service.cosmos.GamificationStore
import com.zensa.api.service.cosmos.UnconfiguredGamificationStore
import com.zensa.api.service.push.FirebasePushSender
import com.zensa.api.service.push.PushSender
import com.zensa.api.service.push.UnconfiguredPushSender
import com.zensa.api.service.storage.AzureBlobStorageService
import com.zensa.api.service.storage.BlobStorageService
import com.zensa.api.service.storage.UnconfiguredBlobStorageService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import java.io.FileInputStream
import java.io.InputStream

/**
 * CloudConfig - creates the optional cloud services from ENVIRONMENT VARIABLES only.
 *
 * Every service falls back to an "Unconfigured" implementation that answers HTTP 503 with a
 * clear message. That way the API always starts (local development, GitHub Actions tests) and
 * a missing credential is never hard-coded or guessed.
 *
 *   Firebase push   FIREBASE_CREDENTIALS_JSON  (or FIREBASE_CREDENTIALS_PATH)
 *   Blob Storage    AZURE_STORAGE_CONNECTION_STRING  (+ AZURE_STORAGE_CONTAINER)
 *   Cosmos DB       COSMOS_ENDPOINT + COSMOS_KEY  (+ COSMOS_DATABASE, COSMOS_CONTAINER)
 */
@Configuration
@EnableScheduling
class CloudConfig {

    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun pushSender(
        @Value("\${zensa.firebase.credentials-json:}") credentialsJson: String,
        @Value("\${zensa.firebase.credentials-path:}") credentialsPath: String
    ): PushSender {
        val stream: InputStream = when {
            credentialsJson.isNotBlank() -> credentialsJson.byteInputStream()
            credentialsPath.isNotBlank() -> FileInputStream(credentialsPath)
            else -> {
                log.info("Firebase not configured: push notifications disabled")
                return UnconfiguredPushSender()
            }
        }
        return try {
            val app = FirebaseApp.getApps().firstOrNull { it.name == APP_NAME }
                ?: FirebaseApp.initializeApp(
                    FirebaseOptions.builder()
                        .setCredentials(stream.use { GoogleCredentials.fromStream(it) })
                        .build(),
                    APP_NAME
                )
            log.info("Firebase configured: push notifications enabled")
            FirebasePushSender(FirebaseMessaging.getInstance(app))
        } catch (e: Exception) {
            // Bad credentials must not stop the whole API from starting.
            log.error("Firebase credentials could not be loaded: {}", e.javaClass.simpleName)
            UnconfiguredPushSender()
        }
    }

    @Bean
    fun blobStorageService(
        @Value("\${zensa.azure.storage.connection-string:}") connectionString: String,
        @Value("\${zensa.azure.storage.container:zensa-user-files}") container: String
    ): BlobStorageService =
        if (connectionString.isBlank()) {
            log.info("Azure Blob Storage not configured")
            UnconfiguredBlobStorageService()
        } else {
            AzureBlobStorageService(connectionString, container)
        }

    @Bean
    fun gamificationStore(
        @Value("\${zensa.cosmos.endpoint:}") endpoint: String,
        @Value("\${zensa.cosmos.key:}") key: String,
        @Value("\${zensa.cosmos.database:zensa}") database: String,
        @Value("\${zensa.cosmos.container:gamification}") container: String
    ): GamificationStore =
        if (endpoint.isBlank() || key.isBlank()) {
            log.info("Azure Cosmos DB not configured")
            UnconfiguredGamificationStore()
        } else {
            CosmosGamificationStore(endpoint, key, database, container)
        }

    private companion object {
        const val APP_NAME = "zensa"
    }
}
