package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.AppDateTimeEntity
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Διαχειρίζεται την αποθηκευμένη ημερομηνία/ώρα. */
class AppDateTimeViewModel : ViewModel() {
    private val _dateTime = MutableStateFlow<Long?>(null)
    val dateTime: StateFlow<Long?> = _dateTime
    private val firestore = FirebaseFirestore.getInstance()

    fun load(context: Context) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).appDateTimeDao()
            _dateTime.value = dao.getDateTime()?.timestamp
        }
    }

    fun save(context: Context, millis: Long) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).appDateTimeDao()
            dao.insert(AppDateTimeEntity(timestamp = millis))
            _dateTime.value = millis

            val data = mapOf("timestamp" to millis)
            firestore
                .collection("app_datetime")
                .document("1")
                .set(data)
                .await()
        }
    }

    fun reset(context: Context) {
        save(context, System.currentTimeMillis())
    }
}
