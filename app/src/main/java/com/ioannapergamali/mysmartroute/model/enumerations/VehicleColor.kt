package com.ioannapergamali.mysmartroute.model.enumerations

import androidx.compose.ui.graphics.Color

/**
 * Ελληνικά: Διαθέσιμα χρώματα οχημάτων με αντίστοιχη προεπισκόπηση.
 * English: Available vehicle colors with corresponding preview color.
 */
enum class VehicleColor(val label: String, val color: Color) {
    /** Μαύρο / Black */
    BLACK("Μαύρο", Color.Black),
    /** Άσπρο / White */
    WHITE("Άσπρο", Color.White),
    /** Κόκκινο / Red */
    RED("Κόκκινο", Color.Red),
    /** Μπλε / Blue */
    BLUE("Μπλε", Color.Blue),
    /** Πράσινο / Green */
    GREEN("Πράσινο", Color(0xFF4CAF50)),
    /** Κίτρινο / Yellow */
    YELLOW("Κίτρινο", Color(0xFFFFEB3B))
}
