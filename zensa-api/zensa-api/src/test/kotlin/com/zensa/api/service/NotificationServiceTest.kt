package com.zensa.api.service

import com.zensa.api.entity.DeviceTokenEntity
import com.zensa.api.exception.ServiceUnavailableException
import com.zensa.api.repository.DeviceTokenRepository
import com.zensa.api.service.push.NotificationService
import com.zensa.api.service.push.PushResult
import com.zensa.api.service.push.PushSender
import com.zensa.api.service.push.UnconfiguredPushSender
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import java.util.UUID

/** Push delivery logic with a fake sender (no Firebase needed). */
@DataJpaTest
class NotificationServiceTest @Autowired constructor(private val devices: DeviceTokenRepository) {

    private class FakeSender(private val results: Map<String, PushResult>) : PushSender {
        override val isConfigured = true
        val sentTo = mutableListOf<String>()
        override fun send(token: String, title: String, body: String, data: Map<String, String>): PushResult {
            sentTo += token
            return results[token] ?: PushResult.SENT
        }
    }

    private val owner = UUID.randomUUID()

    @Test
    fun `sends to every device of the user and counts them`() {
        devices.save(DeviceTokenEntity(userId = owner, token = "tok-a"))
        devices.save(DeviceTokenEntity(userId = owner, token = "tok-b"))
        devices.save(DeviceTokenEntity(userId = UUID.randomUUID(), token = "tok-other-user"))
        val sender = FakeSender(emptyMap())

        val sent = NotificationService(devices, sender).sendToUser(owner, "T", "B", "test")

        assertEquals(2, sent)
        assertEquals(setOf("tok-a", "tok-b"), sender.sentTo.toSet())
    }

    @Test
    fun `unregistered tokens are deleted`() {
        devices.save(DeviceTokenEntity(userId = owner, token = "dead"))
        devices.save(DeviceTokenEntity(userId = owner, token = "alive"))

        val sent = NotificationService(devices, FakeSender(mapOf("dead" to PushResult.INVALID_TOKEN)))
            .sendToUser(owner, "T", "B", "test")

        assertEquals(1, sent)
        assertEquals(listOf("alive"), devices.findAllByUserId(owner).map { it.token })
    }

    @Test
    fun `failed sends are not counted and keep the token`() {
        devices.save(DeviceTokenEntity(userId = owner, token = "flaky"))
        val sent = NotificationService(devices, FakeSender(mapOf("flaky" to PushResult.FAILED)))
            .sendToUser(owner, "T", "B", "test")
        assertEquals(0, sent)
        assertEquals(1, devices.findAllByUserId(owner).size)
    }

    @Test
    fun `unconfigured firebase gives a 503 style exception`() {
        assertThrows(ServiceUnavailableException::class.java) {
            NotificationService(devices, UnconfiguredPushSender()).sendToUser(owner, "T", "B", "test")
        }
    }
}
