package com.ioannapergamali.mysmartroute.model.enumerations

import androidx.compose.ui.graphics.Color

/** Διαθέσιμα χρώματα οχημάτων με αντίστοιχο χρώμα για προεπισκόπηση. */
enum class VehicleColor(val label: String, val color: Color) {
    /** Μαύρο */
    BLACK("Μαύρο", Color.Black),
    /** Άσπρο */
    WHITE("Άσπρο", Color.White),
    /** Κόκκινο */
    RED("Κόκκινο", Color.Red),
    /** Μπλε */
    BLUE("Μπλε", Color.Blue),
    /** Πράσινο */
    GREEN("Πράσινο", Color(0xFF4CAF50)),
    /** Κίτρινο */
    YELLOW("Κίτρινο", Color(0xFFFFEB3B))
}
