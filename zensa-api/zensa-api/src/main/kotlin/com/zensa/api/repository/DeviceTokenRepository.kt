package com.zensa.api.repository

import com.zensa.api.entity.DeviceTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DeviceTokenRepository : JpaRepository<DeviceTokenEntity, UUID> {
    fun findAllByUserId(userId: UUID): List<DeviceTokenEntity>
    fun findByToken(token: String): DeviceTokenEntity?
}
