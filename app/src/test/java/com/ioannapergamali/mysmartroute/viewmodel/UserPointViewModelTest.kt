package com.ioannapergamali.mysmartroute.viewmodel

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Έλεγχος ότι το [UserPointViewModel] δεν περιέχει προκαθορισμένα σημεία.
 */
class UserPointViewModelTest {

    @Test
    fun init_hasNoDefaultPoints() {
        val viewModel = UserPointViewModel()

        assertTrue(viewModel.points.value.isEmpty())
    }
}

