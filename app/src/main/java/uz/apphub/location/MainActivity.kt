package uz.apphub.location

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import dagger.hilt.android.AndroidEntryPoint
import uz.apphub.location.service.LocationService
import uz.apphub.location.repo.SettingsRepository
import uz.apphub.location.repo.toMap
import uz.apphub.location.util.LocationStatusTracker
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepo: SettingsRepository

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MainScreen() }
    }

    @Composable
    fun MainScreen() {
        var hasPerm by remember { mutableStateOf(checkAllPermissions()) }
        val settings by settingsRepo.settingsFlow.collectAsState()

        Log.d("TAGTAGTAG", "MainScreen: $settings")

        settingsRepo.updateSettings(settings.copy(
            permission = hasPerm,
            gps = LocationStatusTracker.isGpsEnabled(LocalContext.current)
        ).toMap())

        Column(Modifier.padding(16.dp)) {
            Text("Child Tracker", style = MaterialTheme.typography.headlineSmall)
            if (!hasPerm) {
                Button(onClick = { requestAllPermissions() }, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Ruxsatlarni so'rash")
                }
            } else {
                Button(onClick = { startTrackingService() }, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Yuborishni boshlash")
                }
                Button(onClick = { stopTrackingService() }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("To'xtatish")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Server sozlamalari holati:")
                Text("permission: ${settings.permission}")
                Text("gps: ${settings.gps}")
                Text("listener: ${settings.listener}")
                Text("showIcon: ${settings.showIcon}")
            }
            LaunchedEffect(Unit) { hasPerm = checkAllPermissions() }
        }
    }

    private fun requestAllPermissions() {
        val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            perms.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    private fun checkAllPermissions(): Boolean {
        val fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val bg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED else true
        return fine && coarse && bg
    }

    private fun startTrackingService() {
        val intent = Intent(this, LocationService::class.java).apply { action = LocationService.ACTION_START }
        startForegroundService(intent)
    }

    private fun stopTrackingService() {
        val intent = Intent(this, LocationService::class.java).apply { action = LocationService.ACTION_STOP }
        startService(intent)
    }
}