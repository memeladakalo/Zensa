package com.zensa.api.service.storage

import com.zensa.api.exception.FileRejectedException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.util.UUID

/** Blob Storage rules: validation of uploads and per-user ownership of blob names. */
class FileRulesTest {

    private val pdf = "%PDF-1.7 test".toByteArray()
    private val user = UUID.randomUUID()

    @Test
    fun `valid pdf is accepted and typed from its extension`() {
        val result = FileRules.validate("notes.pdf", pdf)
        assertEquals("application/pdf", result.contentType)
        assertEquals("notes.pdf", result.safeName)
    }

    @Test
    fun `executable extension is rejected`() {
        val ex = assertThrows(FileRejectedException::class.java) {
            FileRules.validate("virus.exe", "MZ".toByteArray())
        }
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.status)
    }

    @Test
    fun `renamed file whose content does not match is rejected`() {
        assertThrows(FileRejectedException::class.java) {
            FileRules.validate("fake.pdf", "just some text".toByteArray())
        }
    }

    @Test
    fun `empty file is rejected`() {
        assertThrows(FileRejectedException::class.java) { FileRules.validate("a.pdf", ByteArray(0)) }
    }

    @Test
    fun `file over ten megabytes is rejected with 413`() {
        val big = ByteArray((FileRules.MAX_BYTES + 1).toInt()).also {
            "%PDF".toByteArray().copyInto(it)
        }
        val ex = assertThrows(FileRejectedException::class.java) { FileRules.validate("big.pdf", big) }
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, ex.status)
    }

    @Test
    fun `png jpeg docx and txt signatures are recognised`() {
        FileRules.validate("a.png", byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D))
        FileRules.validate("a.jpg", byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()))
        FileRules.validate("a.docx", byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x14))
        FileRules.validate("a.txt", "hello".toByteArray())
    }

    @Test
    fun `text file containing binary NUL bytes is rejected`() {
        assertThrows(FileRejectedException::class.java) {
            FileRules.validate("a.txt", byteArrayOf(0x68, 0x00, 0x69))
        }
    }

    @Test
    fun `file names are sanitised`() {
        assertEquals("passwd", FileRules.sanitizeName("../../etc/passwd"))
        assertEquals("my_file_.pdf", FileRules.sanitizeName("my<file>.pdf"))
        assertEquals("attachment", FileRules.sanitizeName("   "))
    }

    @Test
    fun `blob names start with the owner id`() {
        assertTrue(FileRules.newBlobName(user, "x.pdf").startsWith("$user/"))
    }

    @Test
    fun `ownership accepts own files only`() {
        assertTrue(FileRules.isOwnedBy(user, "$user/abc-notes.pdf"))
        assertFalse(FileRules.isOwnedBy(user, "${UUID.randomUUID()}/abc-notes.pdf"))
    }

    @Test
    fun `ownership rejects traversal and nested paths`() {
        assertFalse(FileRules.isOwnedBy(user, "$user/../other/x.pdf"))
        assertFalse(FileRules.isOwnedBy(user, "$user/sub/x.pdf"))
        assertFalse(FileRules.isOwnedBy(user, "$user\\x.pdf"))
    }
}
