package com.ioannapergamali.mysmartroute

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity

class DropdownActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dropdown)

        val locations = listOf("Σημείο 1", "Σημείο 2", "Σημείο 3")

        val fromAuto = findViewById<AutoCompleteTextView>(R.id.fromAuto)
        val toAuto = findViewById<AutoCompleteTextView>(R.id.toAuto)

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, locations)
        fromAuto.setAdapter(adapter)
        toAuto.setAdapter(adapter)
    }
}
