package uz.apphub.location.service

//* Shokirov Begzod  16.09.2025 *//

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import uz.apphub.location.R
import uz.apphub.location.repo.FirebaseRepository
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
        private const val NOTIF_ID = 1002
    }

    @Inject lateinit var settingsRepo: SettingsRepository
    @Inject lateinit var firebaseRepo: FirebaseRepository

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var serviceScope: CoroutineScope? = null

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        observeSettingsAndStatus()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForegroundWithNotification()
            ACTION_STOP -> {
                stopLocationUpdates()
                stopForeground(true)
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
                    // Internet yo‘qligi bo‘lsa joylashuv uzatma
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
        }
    }

    private fun startForegroundWithNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(CHANNEL_ID, "Child Location", NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Joylashuv yuborilmoqda")
            .setContentText("Telefon joylashuvi serverga yuborilmoqda")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, notification)
    }

    private fun startLocationUpdates() {
        if (::locationCallback.isInitialized) return // allaqachon boshlangan
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { loc -> sendLocation(loc) }
            }
        }
        val request = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 5f
        }
        try {
            fusedClient.requestLocationUpdates(request, locationCallback, null)
        } catch (_: SecurityException) {}
    }

    private fun stopLocationUpdates() {
        if (::locationCallback.isInitialized) {
            fusedClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun sendLocation(location: Location) {
        val data = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "accuracy" to location.accuracy,
            "timestamp" to System.currentTimeMillis()
        )
        firebaseRepo.sendLocation(settingsRepo.getDevicePath(), data)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        serviceScope?.cancel()
        settingsRepo.stopListening()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}