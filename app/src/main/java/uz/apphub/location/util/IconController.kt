package uz.apphub.location.util

//* Shokirov Begzod  16.09.2025 *//

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object IconController {
    private const val ALIAS_NAME = "uz.apphub.location.AliasMain"

    fun setIconVisible(context: Context, visible: Boolean) {
        val pm = context.packageManager
        val comp = ComponentName(context, ALIAS_NAME)
        pm.setComponentEnabledSetting(
            comp,
            if (visible) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}