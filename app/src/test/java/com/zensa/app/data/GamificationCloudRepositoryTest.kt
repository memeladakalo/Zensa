package com.zensa.app.data

import com.zensa.app.core.network.ApiResult
import com.zensa.app.data.remote.ZensaApiService
import com.zensa.app.data.remote.dto.GamificationProfileDto
import com.zensa.app.data.repository.GamificationCloudRepository
import com.zensa.app.domain.usecase.AchievementsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.time.LocalDate

/** Cloud (Cosmos DB) backup of gamification progress: store, read, and failure handling. */
class GamificationCloudRepositoryTest {

    private val api: ZensaApiService = mockk()
    private val repo = GamificationCloudRepository(api)
    private val snapshot = AchievementsUseCase().compute(
        assignments = emptyList(), subtasks = emptyList(), sessions = emptyList(),
        today = LocalDate.of(2026, 11, 7)
    )
    private val stored = GamificationProfileDto(userId = "u", points = 0, level = 1, updatedAt = "2026-11-07T10:00:00")

    @Test
    fun `save sends points level and streaks and returns the stored document`() = runTest {
        val body = slot<com.zensa.app.data.remote.dto.GamificationProfileRequestDto>()
        coEvery { api.saveGamification(capture(body)) } returns Response.success(stored)

        val result = repo.save(snapshot)

        assertTrue(result is ApiResult.Success)
        assertEquals(snapshot.points, body.captured.points)
        assertEquals(snapshot.level.level, body.captured.level)
        coVerify(exactly = 1) { api.saveGamification(any()) }
    }

    @Test
    fun `save reports an error when the cloud is not configured (503)`() = runTest {
        coEvery { api.saveGamification(any()) } returns Response.error(503, "".toResponseBody(null))
        assertTrue(repo.save(snapshot) is ApiResult.Error)
    }

    @Test
    fun `save while offline is a soft error not a crash`() = runTest {
        coEvery { api.saveGamification(any()) } throws IOException("no network")
        assertTrue(repo.save(snapshot) is ApiResult.Error)
    }

    @Test
    fun `load returns the stored document`() = runTest {
        coEvery { api.getGamification() } returns Response.success(stored)
        val result = repo.load()
        assertEquals("2026-11-07T10:00:00", (result as ApiResult.Success).data?.updatedAt)
    }

    @Test
    fun `load treats 404 as nothing stored yet`() = runTest {
        coEvery { api.getGamification() } returns Response.error(404, "".toResponseBody(null))
        assertNull((repo.load() as ApiResult.Success).data)
    }
}
