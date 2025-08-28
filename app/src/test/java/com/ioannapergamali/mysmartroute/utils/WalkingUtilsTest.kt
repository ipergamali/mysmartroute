package com.ioannapergamali.mysmartroute.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class WalkingUtilsTest {
    @Test
    fun walkingDuration_returnsSeconds() {
        val duration = WalkingUtils.walkingDuration(2800.0)
        assertEquals(2000, duration.inWholeSeconds)
    }
}

