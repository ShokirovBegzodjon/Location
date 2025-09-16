package uz.apphub.location.util

//* Shokirov Begzod  16.09.2025 *//

import android.content.Context
import android.location.LocationManager

object LocationStatusTracker {
    fun isGpsEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }
}
