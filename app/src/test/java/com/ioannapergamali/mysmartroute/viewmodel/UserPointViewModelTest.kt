package com.ioannapergamali.mysmartroute.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Έλεγχος ότι το [UserPointViewModel] φορτώνει δύο ενδεικτικά σημεία κατά την αρχικοποίηση.
 */
class UserPointViewModelTest {

    @Test
    fun init_preloadsDefaultPoints() {
        val viewModel = UserPointViewModel()

        val names = viewModel.points.value.map { it.name }
        assertEquals(listOf("Πλατεία Συντάγματος", "Ακρόπολη"), names)
    }
}

