package uz.apphub.location.repo

//* Shokirov Begzod  16.09.2025 *//


import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import javax.inject.Inject

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
                    _settingsFlow.value = AppSettings(
                        permission,
                        gps,
                        listener,
                        showIcon
                    )
                }
                Log.d(
                    "TAGTAG",
                    "Settings repo; listenToSettings: ${_settingsFlow.value}"
                )
            }
    }

    fun stopListening() {
        registration?.remove()
    }

    fun updateSettings(settingsData: Map<String, Any?>) {
        Log.d("TAGTAG", "Settings repo; updateSettings: $settingsData")
        firestore.document(devicePath)
            .set(
                settingsData,
                SetOptions.merge()
            )
    }


    @SuppressLint("HardwareIds")
    private fun getDeviceId(): String {
        val id = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
        val name = android.os.Build.MODEL ?: "Android"
        return "${name}_$id"
    }

    fun getDevicePath() = devicePath
}