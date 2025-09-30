package uz.apphub.location.repo

//* Shokirov Begzod  16.09.2025 *//

import android.location.Location
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun sendLocation(devicePath: String, location: Location) {
        Log.d(
            "TAGTAG",
            "Firebase Repository ; sendLocation:$devicePath->" +
                    " ${location.latitude} : ${location.longitude}"
        )
        val data = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "accuracy" to location.accuracy,
            "speed" to location.speed,
            "timestamp" to SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            ).format(Date())
        )
        firestore.document(devicePath).set(
            mapOf("locations" to data),
            SetOptions.merge()
        )
    }
}