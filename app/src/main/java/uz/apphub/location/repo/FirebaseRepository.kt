package uz.apphub.location.repo

//* Shokirov Begzod  16.09.2025 *//

import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun sendLocation(devicePath: String, locationData: Map<String, Any?>) {
        firestore.collection("$devicePath/locations")
            .add(locationData)
    }
}