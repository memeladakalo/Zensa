package com.zensa.api.cloud

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * Optional Cosmos DB document store for high-volume sync payloads / analytics.
 * Falls back to JPA/MySQL when not configured.
 */
@Service
class CosmosSyncStore(
    @Value("\${azure.cosmos.uri:}") private val uri: String,
    @Value("\${azure.cosmos.key:}") private val key: String,
    @Value("\${azure.cosmos.database:zensa}") private val database: String
) {
    fun isConfigured(): Boolean = uri.isNotBlank() && key.isNotBlank()

    fun writeDocument(container: String, id: String, json: String) {
        if (!isConfigured()) return
        // Production: CosmosClient.createItem(...)
    }
}
