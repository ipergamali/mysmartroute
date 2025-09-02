package com.ioannapergamali.mysmartroute.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WalkingUtilsTest {
    @Test
    fun walkingDuration_returnsSeconds() {
        val duration = WalkingUtils.walkingDuration(2800.0)
        assertEquals(2000, duration.inWholeSeconds)
    }

    @Test
    fun walkingDuration_negativeDistance_throws() {
        assertFailsWith<IllegalArgumentException> {
            WalkingUtils.walkingDuration(-1.0)
        }
    }

    @Test
    fun walkingDuration_zeroSpeed_throws() {
        assertFailsWith<IllegalArgumentException> {
            WalkingUtils.walkingDuration(1000.0, 0.0)
        }
    }
}

