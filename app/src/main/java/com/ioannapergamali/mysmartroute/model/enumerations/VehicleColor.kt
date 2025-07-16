package com.ioannapergamali.mysmartroute.model.enumerations

import androidx.compose.ui.graphics.Color

/** Διαθέσιμα χρώματα οχημάτων με αντίστοιχο χρώμα για προεπισκόπηση. */
enum class VehicleColor(val label: String, val color: Color) {
    BLACK("Black", Color.Black),
    WHITE("White", Color.White),
    RED("Red", Color.Red),
    BLUE("Blue", Color.Blue),
    GREEN("Green", Color(0xFF4CAF50)),
    YELLOW("Yellow", Color(0xFFFFEB3B))
}
