package id.calltag.app.ui.screens

import id.calltag.app.data.local.entity.EmergencyContact
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal fun EmergencyContact.distanceKmFrom(latitude: Double, longitude: Double): Double? {
    val targetLatitude = this.latitude ?: return null
    val targetLongitude = this.longitude ?: return null
    val latitudeDelta = Math.toRadians(targetLatitude - latitude)
    val longitudeDelta = Math.toRadians(targetLongitude - longitude)
    val chord = sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
        cos(Math.toRadians(latitude)) * cos(Math.toRadians(targetLatitude)) *
        sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
    return 2 * 6371.0 * asin(sqrt(chord.coerceIn(0.0, 1.0)))
}
