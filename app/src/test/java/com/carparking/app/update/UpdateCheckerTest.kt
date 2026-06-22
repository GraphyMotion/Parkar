package com.carparking.app.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun `version superieure detectee correctement`() {
        assertTrue(UpdateChecker.isNewer("1.1.0", "1.0.0"))
        assertTrue(UpdateChecker.isNewer("2.0.0", "1.9.9"))
        assertTrue(UpdateChecker.isNewer("1.0.1", "1.0.0"))
    }

    @Test
    fun `version identique ou inferieure non detectee comme nouvelle`() {
        assertFalse(UpdateChecker.isNewer("1.0.0", "1.0.0"))
        assertFalse(UpdateChecker.isNewer("1.0.0", "1.0.1"))
        assertFalse(UpdateChecker.isNewer("0.9.0", "1.0.0"))
    }

    @Test
    fun `gere les versions a nombre de segments different`() {
        assertTrue(UpdateChecker.isNewer("1.1", "1.0.5"))
        assertFalse(UpdateChecker.isNewer("1.0", "1.0.1"))
        assertTrue(UpdateChecker.isNewer("1.0.0.1", "1.0.0"))
    }

    @Test
    fun `gere les segments non numeriques sans planter`() {
        assertFalse(UpdateChecker.isNewer("abc", "1.0.0"))
    }
}
