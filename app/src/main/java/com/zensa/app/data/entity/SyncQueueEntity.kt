package com.zensa.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * SyncQueueEntity - one pending offline operation waiting to be sent
 * to the REST API (POST /api/sync).
 *
 * Offline-first pattern: every offline-capable write (create / update /
 * delete) is saved in its own Room table first, then a row is added here.
 * SyncWorker later sends these rows to the server when the device is online.
 * (Android Developers, 2026 - "Build an offline-first app", queued writes.)
 *
 * Design reference: "Sync Queue" table in the Zensa Planning & Design document.
 */
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(

    /** Unique id of this queue row. */
    @PrimaryKey
    val queueId: String,

    /** Which kind of record changed - one of the TYPE_* constants below. */
    val entityType: String,

    /** Local (Room) id of the record that changed. */
    val entityLocalId: String,

    /** CREATE, UPDATE or DELETE - one of the OP_* constants below. */
    val operation: String,

    /** ISO-8601 local date-time (sortable as text, so oldest-first ordering works). */
    val createdAt: String,

    /** How many times sending this operation has failed so far. */
    val retryCount: Int = 0,

    /** Last error message from the server or network layer (never shown raw to the user). */
    val lastError: String? = null,

    /** True once the server has confirmed the operation. */
    val synced: Boolean = false,

    /** True once retryCount reached MAX_RETRIES - the item is parked, not retried. */
    val failed: Boolean = false
) {
    companion object {
        // ---- Entity types ----
        const val TYPE_MODULE = "MODULE"
        const val TYPE_ASSIGNMENT = "ASSIGNMENT"
        const val TYPE_SUBTASK = "SUBTASK"
        const val TYPE_STUDY_SESSION = "STUDY_SESSION"
        const val TYPE_NOTE = "NOTE"

        // ---- Operations ----
        const val OP_CREATE = "CREATE"
        const val OP_UPDATE = "UPDATE"
        const val OP_DELETE = "DELETE"

        /** After this many failed attempts an item is marked failed instead of retried forever. */
        const val MAX_RETRIES = 5
    }
}
