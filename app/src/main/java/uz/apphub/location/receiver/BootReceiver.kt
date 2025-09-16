package uz.apphub.location.receiver

//* Shokirov Begzod  16.09.2025 *//

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import uz.apphub.location.service.LocationService

/**
 * Telefon yoqilganda (BOOT_COMPLETED) servisni avtomatik ishga tushiradi.
 * Manifestga receiver sifatida qo'shilgan bo'lishi kerak.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, LocationService::class.java).apply {
                action = LocationService.ACTION_START
            }
            context.startForegroundService(serviceIntent)
        }
    }
}