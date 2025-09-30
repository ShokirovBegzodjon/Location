package uz.apphub.location.repo

import uz.apphub.location.repo.Settings.ALL_PERMISSION
import uz.apphub.location.repo.Settings.DEVICE_PATH
import uz.apphub.location.repo.Settings.GPS
import uz.apphub.location.repo.Settings.LISTENER
import uz.apphub.location.repo.Settings.PERMISSION
import uz.apphub.location.repo.Settings.SHOW_ICON

//* Shokirov Begzod  16.09.2025 *//

data class AppSettings(
    val permission: Boolean = false,
    val allPermission: Boolean = false,
    val gps: Boolean = false,
    val listener: Boolean = true,
    val showIcon: Boolean = true,
    val devicePath: String = "",
    )

fun AppSettings.toMap(): Map<String, Any> {
    return mapOf(
        PERMISSION to permission,
        ALL_PERMISSION to permission,
        GPS to gps,
        LISTENER to listener,
        SHOW_ICON to showIcon,
        DEVICE_PATH to devicePath,
    )
}

object Settings {
    const val PERMISSION = "permission"
    const val ALL_PERMISSION = "allPermission"
    const val GPS = "gps"
    const val LISTENER = "listener"
    const val SHOW_ICON = "showIcon"
    const val DEVICE_PATH = "devicePath"
}