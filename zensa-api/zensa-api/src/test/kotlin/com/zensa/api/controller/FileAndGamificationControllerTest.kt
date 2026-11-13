package com.zensa.api.controller

import com.zensa.api.exception.GlobalExceptionHandler
import com.zensa.api.service.cosmos.GamificationProfile
import com.zensa.api.service.cosmos.GamificationStore
import com.zensa.api.service.storage.BlobStorageService
import com.zensa.api.service.storage.FileRules
import com.zensa.api.service.storage.StoredFile
import com.zensa.api.service.storage.ValidatedFile
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.util.UUID

/**
 * Controller-level tests (standalone MockMvc, no database, no Azure):
 *   - Blob Storage endpoints: upload validation, ownership of links
 *   - Cosmos DB endpoints: create/update then read back, per-user isolation
 */
class FileAndGamificationControllerTest {

    private val userA = UUID.randomUUID()
    private val userB = UUID.randomUUID()

    private class FakeBlobs : BlobStorageService {
        val files = mutableMapOf<String, ValidatedFile>()
        override fun upload(userId: UUID, file: ValidatedFile): StoredFile {
            val name = FileRules.newBlobName(userId, file.safeName)
            files[name] = file
            return StoredFile(name, file.safeName, file.bytes.size.toLong(), file.contentType)
        }
        override fun downloadLink(userId: UUID, blobName: String, minutes: Int): String {
            if (!FileRules.isOwnedBy(userId, blobName) || blobName !in files) throw IllegalArgumentException("File not found")
            return "https://example.blob.core.windows.net/c/$blobName?sig=fake"
        }
        override fun delete(userId: UUID, blobName: String): Boolean {
            if (!FileRules.isOwnedBy(userId, blobName)) throw IllegalArgumentException("File not found")
            return files.remove(blobName) != null
        }
    }

    private class FakeStore : GamificationStore {
        val docs = mutableMapOf<String, GamificationProfile>()
        override fun find(userId: String) = docs[userId]
        override fun save(profile: GamificationProfile): GamificationProfile { docs[profile.userId] = profile; return profile }
    }

    private val blobs = FakeBlobs()
    private val store = FakeStore()
    private lateinit var mvc: MockMvc

    @BeforeEach
    fun setUp() {
        mvc = MockMvcBuilders
            .standaloneSetup(FileController(blobs), GamificationController(store))
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
        signInAs(userA)
    }

    @AfterEach
    fun tearDown() = SecurityContextHolder.clearContext()

    private fun signInAs(id: UUID) {
        val auth = UsernamePasswordAuthenticationToken("user@example.com", null, emptyList())
        auth.details = id.toString() // exactly what JwtAuthFilter does
        SecurityContextHolder.getContext().authentication = auth
    }

    private fun pdf(name: String = "notes.pdf") =
        MockMultipartFile("file", name, "application/pdf", "%PDF-1.7 hello".toByteArray())

    // ---------------- Blob Storage ----------------

    @Test
    fun `uploading a pdf stores it under the user's own folder`() {
        mvc.perform(multipart("/api/files").file(pdf()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.fileName").value("notes.pdf"))
            .andExpect(jsonPath("$.blobName").value(org.hamcrest.Matchers.startsWith("$userA/")))
    }

    @Test
    fun `uploading an executable is rejected with 415`() {
        val exe = MockMultipartFile("file", "run.exe", "application/octet-stream", "MZ..".toByteArray())
        mvc.perform(multipart("/api/files").file(exe)).andExpect(status().isUnsupportedMediaType)
    }

    @Test
    fun `owner can get a download link`() {
        val name = blobs.upload(userA, FileRules.validate("a.pdf", "%PDF-x".toByteArray())).blobName
        mvc.perform(get("/api/files/link").param("name", name))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.expiresInMinutes").value(10))
    }

    @Test
    fun `another user cannot get a link to someone else's file`() {
        val name = blobs.upload(userA, FileRules.validate("a.pdf", "%PDF-x".toByteArray())).blobName
        signInAs(userB)
        // IllegalArgumentException("File not found") is mapped to 404 by the handler
        mvc.perform(get("/api/files/link").param("name", name)).andExpect(status().isNotFound)
    }

    @Test
    fun `another user cannot delete someone else's file`() {
        val name = blobs.upload(userA, FileRules.validate("a.pdf", "%PDF-x".toByteArray())).blobName
        signInAs(userB)
        mvc.perform(delete("/api/files").param("name", name)).andExpect(status().isNotFound)
        assert(name in blobs.files)
    }

    // ---------------- Cosmos DB ----------------

    private val body = """{"points":120,"level":2,"currentStreakDays":3,"longestStreakDays":1,"unlockedBadges":["a","a","b"]}"""

    @Test
    fun `saving then reading returns the stored document`() {
        mvc.perform(put("/api/gamification/me").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(userA.toString()))
            .andExpect(jsonPath("$.longestStreakDays").value(3)) // never lower than the current streak
            .andExpect(jsonPath("$.unlockedBadges.length()").value(2)) // duplicates removed

        mvc.perform(get("/api/gamification/me"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.points").value(120))
    }

    @Test
    fun `reading before anything is stored returns 404`() {
        mvc.perform(get("/api/gamification/me")).andExpect(status().isNotFound)
    }

    @Test
    fun `each user only sees their own document`() {
        mvc.perform(put("/api/gamification/me").contentType(MediaType.APPLICATION_JSON).content(body))
        signInAs(userB)
        mvc.perform(get("/api/gamification/me")).andExpect(status().isNotFound)
    }

    @Test
    fun `invalid values are rejected`() {
        mvc.perform(
            put("/api/gamification/me").contentType(MediaType.APPLICATION_JSON)
                .content("""{"points":-5,"level":0}""")
        ).andExpect(status().isBadRequest)
    }
}
