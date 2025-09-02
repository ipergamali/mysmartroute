package com.ioannapergamali.mysmartroute.repository

import org.junit.Assert.*
import org.junit.Test

class PointRepositoryTest {

    @Test
    fun getAllPoints_returnsAllPoints() {
        val repo = PointRepository()
        val p1 = Point("1", "Σημείο Α", "")
        val p2 = Point("2", "Σημείο Β", "")
        repo.addPoint(p1)
        repo.addPoint(p2)

        assertEquals(listOf(p1, p2), repo.getAllPoints())
    }

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
        repo.addPoint(Point("1", "Α", "Λεπτομέρειες Α"))
        repo.addPoint(Point("2", "Β", "Λεπτομέρειες Β"))
        repo.addRoute(Route("r", mutableListOf("1", "2")))

        repo.mergePoints("1", "2")

        assertNull(repo.getPoint("2"))
        val merged = repo.getPoint("1")
        assertEquals("Α / Β", merged?.name)
        assertEquals("Λεπτομέρειες Α\nΛεπτομέρειες Β", merged?.details)
        assertEquals(listOf("1", "1"), repo.getRoute("r")?.pointIds)
    }

    @Test
    fun deletePoint_removesPointAndUpdatesRoutes() {
        val repo = PointRepository()
        repo.addPoint(Point("1", "Α", ""))
        repo.addRoute(Route("r", mutableListOf("1")))

        repo.deletePoint("1")

        assertNull(repo.getPoint("1"))
        assertEquals(emptyList<String>(), repo.getRoute("r")?.pointIds)
    }
}
