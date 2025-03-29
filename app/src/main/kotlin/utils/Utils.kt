import java.util.Date
import java.util.UUID
import java.util.Base64
import java.util.Locale

import java.text.SimpleDateFormat
import java.security.SecureRandom

fun generateId(): String = UUID.randomUUID().toString()

fun getCurrentDateTime(): String {
    val formatter: SimpleDateFormat = SimpleDateFormat("M/d/yyyy h:mm a", Locale.US)
    return formatter.format(Date())
}

fun generateBase64RandomBytes(size: Int): String {
    val randomBytes: ByteArray = ByteArray(size)
    SecureRandom().nextBytes(randomBytes)
    return Base64.getEncoder().encodeToString(randomBytes)
}

fun formatBytes(bytes: Long, decimalPlaces: Int = 2): String {
    if (bytes < 1024) return "$bytes B"

    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB")
    val exponent = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val value = bytes / Math.pow(1024.0, exponent.toDouble())

    return String.format("%.${decimalPlaces}f %s", value, units[exponent])
}

fun setConsole(clear: Boolean? = null, title: String? = null) {
    if (clear == null && title == null) {
        return
    }
    val commands = buildString {
        if (clear ?: false) append("cls")
        if (!title.isNullOrEmpty()) {
            if (clear ?: false) append(" &")
            append(" title $title")
        }
    }
    ProcessBuilder("cmd", "/c", commands).inheritIO().start().waitFor()
}

fun displayProgressBar(x: Long, y: Long, operationName: String) {
    val percentage: Int = ((100 * x) / y).toInt()
    val filledLength: Int = (25 * percentage) / 100
    val bar: String = "█".repeat(filledLength) + "-".repeat(25 - filledLength)
    print("\r$operationName: $percentage% │$bar│ (${formatBytes(x)} / ${formatBytes(y)})")
}

fun waitForEnter() {
    print("\nPress enter to continue!")
    readLine()
}

fun takeInput(text: String): String {
    print(text)
    return readLine() ?: ""
}