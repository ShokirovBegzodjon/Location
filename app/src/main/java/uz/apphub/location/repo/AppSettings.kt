package uz.apphub.location.repo

//* Shokirov Begzod  16.09.2025 *//

data class AppSettings(
    val permission: Boolean = false,
    val gps: Boolean = false,
    val listener: Boolean = true,
    val showIcon: Boolean = true
)

fun AppSettings.toMap(): Map<String, Any> {
    return mapOf(
        "permission" to permission,
        "gps" to gps,
        "listener" to listener,
        "showIcon" to showIcon
    )
}