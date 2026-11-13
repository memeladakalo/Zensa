package com.zensa.api.service.cosmos

import com.azure.cosmos.ConsistencyLevel
import com.azure.cosmos.CosmosClientBuilder
import com.azure.cosmos.CosmosContainer
import com.azure.cosmos.CosmosException
import com.azure.cosmos.models.CosmosItemRequestOptions
import com.azure.cosmos.models.PartitionKey
import com.zensa.api.exception.ServiceUnavailableException
import org.slf4j.LoggerFactory

/**
 * The gamification progress of ONE student, stored as ONE document in Azure Cosmos DB
 * (NoSQL / document database).
 *
 * Document shape (JSON):
 *   { "id": "<userId>", "userId": "<userId>", "type": "gamification-profile",
 *     "points": 120, "level": 2, "currentStreakDays": 3, "longestStreakDays": 5,
 *     "unlockedBadges": ["first_study_session", ...], "updatedAt": "2026-11-07T10:15:30" }
 *
 * Partition key: /userId. Every read and write targets a single logical partition, which is
 * the cheapest and fastest access pattern in Cosmos DB, and a user can only ever touch their
 * own partition (the userId always comes from the verified JWT).
 */
data class GamificationProfile(
    val userId: String,
    val points: Int,
    val level: Int,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val unlockedBadges: List<String>,
    val updatedAt: String
)

interface GamificationStore {
    /** @return the stored profile, or null when this user has nothing stored yet. */
    fun find(userId: String): GamificationProfile?

    /** Creates or replaces (upsert) the user's profile document. */
    fun save(profile: GamificationProfile): GamificationProfile
}

/** Conversion between the Kotlin model and the Cosmos JSON document (pure, unit-tested). */
object GamificationDocuments {
    const val TYPE = "gamification-profile"

    fun toDocument(p: GamificationProfile): Map<String, Any?> = mapOf(
        "id" to p.userId,
        "userId" to p.userId, // partition key path: /userId
        "type" to TYPE,
        "points" to p.points,
        "level" to p.level,
        "currentStreakDays" to p.currentStreakDays,
        "longestStreakDays" to p.longestStreakDays,
        "unlockedBadges" to p.unlockedBadges,
        "updatedAt" to p.updatedAt
    )

    fun fromDocument(doc: Map<*, *>): GamificationProfile = GamificationProfile(
        userId = doc["userId"]?.toString() ?: doc["id"]?.toString().orEmpty(),
        points = (doc["points"] as? Number)?.toInt() ?: 0,
        level = (doc["level"] as? Number)?.toInt() ?: 1,
        currentStreakDays = (doc["currentStreakDays"] as? Number)?.toInt() ?: 0,
        longestStreakDays = (doc["longestStreakDays"] as? Number)?.toInt() ?: 0,
        unlockedBadges = (doc["unlockedBadges"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
        updatedAt = doc["updatedAt"]?.toString().orEmpty()
    )
}

/** Azure Cosmos DB implementation. Endpoint and key come ONLY from environment variables. */
class CosmosGamificationStore(
    private val endpoint: String,
    private val key: String,
    private val databaseName: String,
    private val containerName: String
) : GamificationStore {

    private val log = LoggerFactory.getLogger(javaClass)

    /** Created lazily so the API starts even if Cosmos is briefly unreachable. */
    private val container: CosmosContainer by lazy {
        try {
            val client = CosmosClientBuilder()
                .endpoint(endpoint)
                .key(key)
                .consistencyLevel(ConsistencyLevel.SESSION)
                .gatewayMode() // works through Azure App Service networking
                .buildClient()
            client.createDatabaseIfNotExists(databaseName)
            val database = client.getDatabase(databaseName)
            database.createContainerIfNotExists(containerName, "/userId")
            database.getContainer(containerName)
        } catch (e: Exception) {
            log.error("Cosmos DB unavailable: {}", e.javaClass.simpleName)
            throw ServiceUnavailableException("Cloud database is temporarily unavailable")
        }
    }

    override fun find(userId: String): GamificationProfile? = try {
        val response = container.readItem(userId, PartitionKey(userId), Map::class.java)
        GamificationDocuments.fromDocument(response.item)
    } catch (e: CosmosException) {
        if (e.statusCode == 404) null else {
            log.error("Cosmos read failed: status {}", e.statusCode)
            throw ServiceUnavailableException("Cloud database is temporarily unavailable")
        }
    }

    override fun save(profile: GamificationProfile): GamificationProfile {
        try {
            container.upsertItem(
                GamificationDocuments.toDocument(profile),
                PartitionKey(profile.userId),
                CosmosItemRequestOptions()
            )
        } catch (e: CosmosException) {
            log.error("Cosmos write failed: status {}", e.statusCode)
            throw ServiceUnavailableException("Cloud database is temporarily unavailable")
        }
        return profile
    }
}

/** Used when no Cosmos endpoint/key has been configured. */
class UnconfiguredGamificationStore : GamificationStore {
    private fun fail(): Nothing =
        throw ServiceUnavailableException("Cloud database is not configured on the server")

    override fun find(userId: String): GamificationProfile? = fail()
    override fun save(profile: GamificationProfile): GamificationProfile = fail()
}
