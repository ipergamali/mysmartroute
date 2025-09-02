package com.ioannapergamali.mysmartroute.viewmodel

import com.google.android.libraries.places.api.model.Place
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.repository.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Έλεγχος ότι το [UserPointViewModel] δεν περιέχει προκαθορισμένα σημεία
 * και ότι φιλτράρει τα POI με ίδιες συντεταγμένες.
 */
class UserPointViewModelTest {

    @Test
    fun init_hasNoDefaultPoints() {
        val viewModel = UserPointViewModel()

        assertTrue(viewModel.points.value.isEmpty())
    }

    @Test
    fun availablePois_excludesSameCoordinates() {
        val viewModel = UserPointViewModel()
        viewModel.addPoint(Point("1", "A", ""))
        val pois = listOf(
            PoIEntity(id = "1", name = "A", lat = 1.0, lng = 2.0, type = Place.Type.ESTABLISHMENT),
            PoIEntity(id = "2", name = "B", lat = 1.0, lng = 2.0, type = Place.Type.ESTABLISHMENT),
            PoIEntity(id = "3", name = "C", lat = 3.0, lng = 4.0, type = Place.Type.ESTABLISHMENT)
        )

        val available = viewModel.availablePois(pois)

        assertEquals(listOf(pois[2]), available)
    }
}

