package uz.apphub.location

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import dagger.hilt.android.AndroidEntryPoint
import uz.apphub.location.repo.AppSettings
import uz.apphub.location.repo.Settings.ALL_PERMISSION
import uz.apphub.location.repo.Settings.PERMISSION
import uz.apphub.location.service.LocationService
import uz.apphub.location.repo.SettingsRepository
import uz.apphub.location.repo.toMap
import uz.apphub.location.ui.theme.LocationTheme
import uz.apphub.location.util.LocationStatusTracker
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepo: SettingsRepository

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        Log.d("TAGTAG", "MainActivity; permissionLauncher: $it")
        settingsRepo.updateSettings(
            mapOf(
                PERMISSION to checkPermissions(),
                ALL_PERMISSION to checkAllPermissions(),
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MainScreen() }
        settingsRepo.updateSettings(
            AppSettings(
                permission = checkPermissions(),
                allPermission = checkAllPermissions(),
                gps = LocationStatusTracker.isGpsEnabled(this),
                devicePath = settingsRepo.getDevicePath().substring(6)
            ).toMap()
        )
        if (!checkAllPermissions()) {
            Log.d("TAGTAG", "MAIN; onCreate: requestAllPermissions")
            requestAllPermissions()
        }
        startTrackingService()
    }

    @Composable
    fun MainScreen() {
        LocationTheme {
            Scaffold(
                modifier = Modifier.fillMaxSize()
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        modifier = Modifier.size(200.dp),
                        painter = painterResource(id = R.drawable.map_image),
                        contentDescription = null
                    )
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
        }
    }

    private fun requestAllPermissions() {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            perms.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    private fun checkAllPermissions(): Boolean {
        val fine = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) ==
                PackageManager.PERMISSION_GRANTED
        val coarse = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) ==
                PackageManager.PERMISSION_GRANTED
        val bg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED else true
        val notifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        else true
        return fine && coarse && bg && notifications
    }

    private fun checkPermissions(): Boolean {
        val fine = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val notifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        else true
        return fine && coarse && notifications
    }

    private fun startTrackingService() {
        Log.d("TAGTAG", "MAIN; startTrackingService")
        val intent = Intent(this, LocationService::class.java).apply {
            action = LocationService.ACTION_START
        }
        startForegroundService(intent)
    }

    private fun stopTrackingService() {
        Log.d("TAGTAG", "MAIN; stopTrackingService")
        val intent =
            Intent(
                this,
                LocationService::class.java
            ).apply {
                action = LocationService.ACTION_STOP
            }
        startService(intent)
    }
}