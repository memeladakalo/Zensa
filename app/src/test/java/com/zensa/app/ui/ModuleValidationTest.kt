package com.zensa.app.ui

import com.zensa.app.data.repository.FileRepository
import com.zensa.app.ui.screens.modules.ModulesViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Small pure functions added in Part 3: module form validation and attachment display names. */
class ModuleValidationTest {

    @Test
    fun `module needs a code and a name`() {
        assertEquals("Module code is required", ModulesViewModel.validate("", "Databases"))
        assertEquals("Module name is required", ModulesViewModel.validate("DB202", "  "))
        assertNull(ModulesViewModel.validate("DB202", "Databases"))
    }

    @Test
    fun `attachment display name strips the folder and the generated id`() {
        val blob = "9f1c2d3e-0000-4000-8000-000000000001/3d6f0e7a-1111-4222-8333-444455556666-lecture notes.pdf"
        assertEquals("lecture notes.pdf", FileRepository.displayName(blob))
    }

    @Test
    fun `plain blob names are shown unchanged`() {
        assertEquals("a.pdf", FileRepository.displayName("user/a.pdf"))
    }
}
