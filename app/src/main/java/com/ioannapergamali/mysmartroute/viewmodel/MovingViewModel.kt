package com.ioannapergamali.mysmartroute.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioannapergamali.mysmartroute.data.local.MovingEntity
import com.ioannapergamali.mysmartroute.repository.MovingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel που εκθέτει τις μετακινήσεις ως StateFlow.
 */
@HiltViewModel
class MovingViewModel @Inject constructor(
    private val repo: MovingRepository
) : ViewModel() {

    private val _state = MutableStateFlow<List<MovingEntity>>(emptyList())
    val state: StateFlow<List<MovingEntity>> = _state

    fun load(now: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            _state.value = repo.getPendingMovings(now)
        }
    }
}
