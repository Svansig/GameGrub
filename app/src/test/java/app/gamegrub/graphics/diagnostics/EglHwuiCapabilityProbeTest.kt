package app.gamegrub.graphics.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EglHwuiCapabilityProbeTest {
    @Test
    fun parseExtensions_splitsAndTrimsValues() {
        val parsed = EglHwuiCapabilityProbe.parseExtensions("EGL_EXT_buffer_age  EGL_KHR_partial_update   EGL_EXT_buffer_age")

        assertEquals(setOf("EGL_EXT_buffer_age", "EGL_KHR_partial_update"), parsed)
    }

    @Test
    fun hasExtension_returnsExpectedMembership() {
        val extensions = setOf("EGL_EXT_buffer_age", "EGL_KHR_partial_update")

        assertTrue(EglHwuiCapabilityProbe.hasExtension(extensions, "EGL_EXT_buffer_age"))
        assertFalse(EglHwuiCapabilityProbe.hasExtension(extensions, "EGL_EXT_swap_buffers_with_damage"))
    }

    @Test
    fun describeSwapBehavior_formatsKnownValues() {
        assertEquals("UNAVAILABLE", EglHwuiCapabilityProbe.describeSwapBehavior(null))
        assertEquals("PRESERVED", EglHwuiCapabilityProbe.describeSwapBehavior(0x3094))
        assertEquals("0x1234", EglHwuiCapabilityProbe.describeSwapBehavior(0x1234))
    }
}

