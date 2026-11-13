package com.zensa.api.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

/**
 * A phone registered for push notifications (Firebase Cloud Messaging token).
 * One user can have several devices. The token is unique: if a phone is later used by
 * another account, the row is re-assigned to that account (see DeviceController).
 */
@Entity
@Table(name = "device_tokens", indexes = [Index(name = "idx_device_user", columnList = "user_id")])
class DeviceTokenEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "device_id", updatable = false, nullable = false)
    var deviceId: UUID? = null,

    @Column(name = "user_id", nullable = false)
    var userId: UUID? = null,

    @Column(nullable = false, unique = true, length = 512)
    var token: String = "",

    @Column(nullable = false, length = 20)
    var platform: String = "android",

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
