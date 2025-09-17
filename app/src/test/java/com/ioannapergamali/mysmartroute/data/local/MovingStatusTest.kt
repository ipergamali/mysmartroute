package com.ioannapergamali.mysmartroute.data.local

import kotlin.test.Test
import kotlin.test.assertEquals

class MovingStatusTest {
    private val now = 1_000_000L
    private val past = now - 10_000L
    private val future = now + 10_000L

    @Test
    fun completedStatus_returnsCompleted() {
        val moving = moving(status = "completed", date = future)
        assertEquals(MovingStatus.COMPLETED, moving.movingStatus(now))
    }

    @Test
    fun acceptedFuture_returnsPending() {
        val moving = moving(status = "accepted", date = future)
        assertEquals(MovingStatus.PENDING, moving.movingStatus(now))
    }

    @Test
    fun acceptedPast_returnsUnsuccessful() {
        val moving = moving(status = "accepted", date = past)
        assertEquals(MovingStatus.UNSUCCESSFUL, moving.movingStatus(now))
    }

    @Test
    fun rejectedFuture_returnsUnsuccessful() {
        val moving = moving(status = "rejected", date = future)
        assertEquals(MovingStatus.UNSUCCESSFUL, moving.movingStatus(now))
    }

    @Test
    fun unknownFuture_returnsPending() {
        val moving = moving(status = "in_progress", date = future)
        assertEquals(MovingStatus.PENDING, moving.movingStatus(now))
    }

    private fun moving(status: String, date: Long): MovingEntity =
        MovingEntity(id = "id_$status", status = status, date = date)
}
