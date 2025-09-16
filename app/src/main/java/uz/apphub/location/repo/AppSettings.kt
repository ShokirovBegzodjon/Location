package uz.apphub.location.repo

//* Shokirov Begzod  16.09.2025 *//


import android.annotation.SuppressLint
import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class AppSettings(
    val permission: Boolean = false,
    val gps: Boolean = false,
    val listener: Boolean = false,
    val showIcon: Boolean = true
)

class SettingsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) {
    private val devicePath = "users/${getDeviceId()}"

    private val _settingsFlow = MutableStateFlow(AppSettings())

    val settingsFlow: StateFlow<AppSettings> = _settingsFlow

    private var registration: ListenerRegistration? = null

    init {
        listenToSettings()
    }

    private fun listenToSettings() {
        registration?.remove()
        registration = firestore.document(devicePath)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val permission = snapshot.getBoolean("permission") ?: false
                    val gps = snapshot.getBoolean("gps") ?: false
                    val listener = snapshot.getBoolean("listener") ?: false
                    val showIcon = snapshot.getBoolean("showIcon") ?: true
                    _settingsFlow.value = AppSettings(permission, gps, listener, showIcon)
                }
            }
    }

    fun stopListening() {
        registration?.remove()
    }

    @SuppressLint("HardwareIds")
    private fun getDeviceId(): String {
        val id = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID)
        val name = android.os.Build.MODEL ?: "Android"
        return "${name}_$id"
    }

    fun getDevicePath() = devicePath
}