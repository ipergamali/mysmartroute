package com.ioannapergamali.mysmartroute.utils

/**
 * Επιστρέφει true μόνο όταν η λίστα των POI έχει αλλάξει από προσθήκη ή αφαίρεση.
 * Η σειρά των στοιχείων αγνοείται, έτσι ώστε απλές αναδιατάξεις να μην θεωρούνται αλλαγές.
 */
fun havePoiMembershipChanged(
    originalPoiIds: List<String>,
    currentPoiIds: List<String>
): Boolean {
    if (originalPoiIds.size != currentPoiIds.size) return true

    val originalCounts = originalPoiIds.groupingBy { it }.eachCount()
    val currentCounts = currentPoiIds.groupingBy { it }.eachCount()

    return originalCounts != currentCounts
}
