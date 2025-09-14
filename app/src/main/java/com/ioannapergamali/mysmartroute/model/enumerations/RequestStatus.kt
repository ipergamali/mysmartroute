package com.ioannapergamali.mysmartroute.model.enumerations

/**
 * Ελληνικά: Διαθέσιμες καταστάσεις αιτήματος μεταφοράς.
 * English: Possible states of a transport request.
 */
enum class RequestStatus {
    OPEN,
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELED,
    COMPLETED
}
