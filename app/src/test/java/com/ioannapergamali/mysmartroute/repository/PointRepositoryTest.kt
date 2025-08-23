package com.ioannapergamali.mysmartroute.repository

import org.junit.Assert.*
import org.junit.Test

class PointRepositoryTest {

    @Test
    fun getAllPointNames_returnsAll() {
        val repo = PointRepository()
        repo.addPoint(Point("1", "Σημείο Α", ""))
        repo.addPoint(Point("2", "Σημείο Β", ""))

        val names = repo.getAllPointNames()
        assertEquals(listOf("Σημείο Α", "Σημείο Β"), names)
    }

    @Test
    fun updatePoint_changesValues() {
        val repo = PointRepository()
        repo.addPoint(Point("1", "Παλαιό", "Περιγραφή"))

        repo.updatePoint("1", "Νέο", "Νέα περιγραφή")
        val point = repo.getPoint("1")
        assertEquals("Νέο", point?.name)
        assertEquals("Νέα περιγραφή", point?.details)
    }

    @Test
    fun mergePoints_updatesRoutesAndRemovesMergedPoint() {
        val repo = PointRepository()
        repo.addPoint(Point("1", "Α", ""))
        repo.addPoint(Point("2", "Β", ""))
        repo.addRoute(Route("r", mutableListOf("1", "2")))

        repo.mergePoints("1", "2")

        assertNull(repo.getPoint("2"))
        assertEquals("Α / Β", repo.getPoint("1")?.name)
        assertEquals(listOf("1", "1"), repo.getRoute("r")?.pointIds)
    }
}
