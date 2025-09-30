package uz.apphub.location.service

//* Shokirov Begzod  16.09.2025 *//

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import uz.apphub.location.R
import uz.apphub.location.repo.FirebaseRepository
import uz.apphub.location.repo.Settings.GPS
import uz.apphub.location.repo.SettingsRepository
import uz.apphub.location.util.IconController
import uz.apphub.location.util.LocationStatusTracker
import uz.apphub.location.util.NetworkStatusTracker
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : Service() {
    companion object {
        const val ACTION_START = "uz.apphub.location.START"
        const val ACTION_STOP = "uz.apphub.location.STOP"
        private const val CHANNEL_ID = "location_channel"
        private const val LOCATION_UPDATE_INTERVAL_MS = 5000L // 5 soniya
        private const val FASTEST_LOCATION_UPDATE_INTERVAL_MS = 2000L // 2 soniya
        private const val NOTIF_ID = 1002
    }

    @Inject
    lateinit var settingsRepo: SettingsRepository

    @Inject
    lateinit var firebaseRepo: FirebaseRepository

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null // locationListener o'rniga
    private var serviceScope: CoroutineScope? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        observeSettingsAndStatus()
        Log.d("TAGTAG", "LocationService onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForegroundWithNotification()
            ACTION_STOP -> {
                stopLocationUpdates()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }

            else -> startForegroundWithNotification()
        }
        return START_STICKY
    }

    private fun observeSettingsAndStatus() {
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        serviceScope?.launch {
            val context = this@LocationService
            // Sozlamalarni va tarmoq/gps statusini kuzat
            launch {
                NetworkStatusTracker.observe(context).collectLatest { isOnline ->
                    if (isOnline) {
                        startLocationUpdates()
                    } else {
                        stopLocationUpdates()
                    }
                }
            }
            launch {
                settingsRepo.settingsFlow.collectLatest { settings ->
                    IconController.setIconVisible(context, settings.showIcon)
                    if (
                        settings.permission &&
                        settings.gps &&
                        settings.listener &&
                        NetworkStatusTracker.isConnected(context)
                        && LocationStatusTracker.isGpsEnabled(context)
                    ) {
                        startLocationUpdates()
                    } else {
                        stopLocationUpdates()
                    }
                }
            }
            launch {
                LocationStatusTracker.observe(context).collectLatest { isGpsEnabled ->
                    settingsRepo.updateSettings(mapOf(GPS to isGpsEnabled))
                    if (isGpsEnabled) {
                        startLocationUpdates()
                    } else {
                        stopLocationUpdates()
                    }
                }
            }
        }
    }

    private fun startForegroundWithNotification() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Child Location",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
        val notification: Notification =
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.mipmap.map_icon)
                .setOngoing(true)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE) // Muhim
                .build()
        startForeground(NOTIF_ID, notification)
    }

    private fun startLocationUpdates() {
        // Agar allaqachon tinglanayotgan bo'lsa, qayta boshlamaslik
        if (locationCallback != null) {
            Log.d("TAGTAG", "LocationService: Location updates already active.")
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, // Yuqori aniqlik
            LOCATION_UPDATE_INTERVAL_MS
        ).apply {
            setMinUpdateIntervalMillis(FASTEST_LOCATION_UPDATE_INTERVAL_MS)
            // setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            // Ruxsat darajasiga qarab aniqlik
            setWaitForAccurateLocation(true) // Birinchi aniq joylashuvni kutish
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    Log.d(
                        "TAGTAG",
                        "LocationService New location:" +
                                " ${location.latitude}, ${location.longitude}," +
                                " Accuracy: ${location.accuracy}"
                    )
                    firebaseRepo.sendLocation(settingsRepo.getDevicePath(), location)
                } ?: run {
                    Log.w(
                        "TAGTAG",
                        "LocationService LocationResult received but lastLocation is null"
                    )
                    // Bu holat kamdan-kam uchraydi, lekin bo'lishi mumkin
                    // Masalan, joylashuv vaqtincha mavjud bo'lmasa
                    locationResult.locations.firstOrNull()?.let { firstLocation ->
                        Log.d(
                            "TAGTAG",
                            "LocationService Using first location from list:" +
                                    " ${firstLocation.latitude}, ${firstLocation.longitude}"
                        )
                        firebaseRepo.sendLocation(
                            settingsRepo.getDevicePath(),
                            firstLocation
                        )
                    }
                }
            }

            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                if (!locationAvailability.isLocationAvailable) {
                    Log.w("TAGTAG", "LocationService Location is currently unavailable.")
                    // Bu yerda GPS signali yo'qolganligi yoki boshqa muammolar haqida log yozish mumkin
                    // Foydalanuvchiga xabar berish shart emas, chunki FusedLocationProvider o'zi qayta urinadi
                }
            }
        }
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper() // Asosiy oqimda callbacklarni olish uchun
            )
            Log.i("TAGTAG", "LocationService Requested location updates.")
        } catch (unlikely: SecurityException) {
            Log.e("TAGTAG", "LocationService Lost location permission. Reason: $unlikely")
            // Bu holat kamdan-kam, lekin ruxsat bekor qilingan bo'lsa yuz berishi mumkin
            stopSelf() // Xizmatni to'xtatish
        }
    }

    private fun stopLocationUpdates() {
        if (locationCallback != null) {
            Log.d("TAGTAG", "LocationService Stopping location updates.")
            fusedLocationClient.removeLocationUpdates(locationCallback!!)
            locationCallback = null
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        serviceScope?.cancel()
        settingsRepo.stopListening()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}