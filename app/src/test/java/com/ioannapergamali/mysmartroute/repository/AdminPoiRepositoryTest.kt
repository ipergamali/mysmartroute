package com.ioannapergamali.mysmartroute.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.android.libraries.places.api.model.Place
import com.ioannapergamali.mysmartroute.data.local.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Έλεγχοι για το [AdminPoiRepository].
 */
@RunWith(RobolectricTestRunner::class)
class AdminPoiRepositoryTest {
    private lateinit var db: MySmartRouteDatabase
    private lateinit var repo: AdminPoiRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MySmartRouteDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = AdminPoiRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun mergePois_updatesRoutesAndRemovesPoi() = runBlocking {
        val type = PoiTypeEntity(id = Place.Type.ESTABLISHMENT.name, name = "")
        db.poiTypeDao().insertAll(listOf(type))
        val poi1 = PoIEntity(id = "1", name = "A", type = Place.Type.ESTABLISHMENT)
        val poi2 = PoIEntity(id = "2", name = "B", type = Place.Type.ESTABLISHMENT)
        db.poIDao().insertAll(listOf(poi1, poi2))
        db.routeDao().insert(RouteEntity(id = "r", userId = "u", name = "r", startPoiId = "1", endPoiId = "2"))
        db.routePointDao().insert(RoutePointEntity(routeId = "r", position = 0, poiId = "2"))

        repo.mergePois("1", "2")

        assertNull(db.poIDao().findById("2"))
        assertEquals("1", db.routeDao().findById("r")?.endPoiId)
        val points = db.routePointDao().getPointsForRoute("r").first()
        assertTrue(points.all { it.poiId == "1" })
    }
}

