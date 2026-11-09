package io.github.veitkramerschoeggl.trainingtracker.domain

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimeInputTest {
    @Test
    fun maskFillsWithUnderscores() {
        assertEquals("__:__", TimeInput.mask(""))
        assertEquals("1_:__", TimeInput.mask("1"))
        assertEquals("18:__", TimeInput.mask("18"))
        assertEquals("18:3_", TimeInput.mask("183"))
        assertEquals("18:30", TimeInput.mask("1830"))
    }

    @Test
    fun sanitizeKeepsFourDigits() {
        assertEquals("1830", TimeInput.sanitize("18:30"))
        assertEquals("1830", TimeInput.sanitize("1830999"))
        assertEquals("12", TimeInput.sanitize("a1b2"))
    }

    @Test
    fun parseAcceptsOnlyRealTimes() {
        assertEquals(LocalTime.of(18, 30), TimeInput.parse("1830"))
        assertEquals(LocalTime.of(0, 0), TimeInput.parse("0000"))
        assertEquals(LocalTime.of(23, 59), TimeInput.parse("2359"))
        assertNull(TimeInput.parse("183"))
        assertNull(TimeInput.parse("2400"))
        assertNull(TimeInput.parse("1860"))
    }
}
