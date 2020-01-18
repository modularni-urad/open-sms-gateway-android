package cz.sazel.android.opensmsgw.model

import cz.sazel.android.opensmsgw.Constants
import java.text.SimpleDateFormat
import java.util.*

enum class LogLevel(level: Int) {
    ERROR(6),
    WARNING(5),
    INFO(4),
    DEBUG(3),
    VERBOSE(2)
}

/**
 * Created on 1/17/20.
 */
data class LogItem(var logLevel: LogLevel, var timestamp: Long, var tag: String, var text: String) {

    private val logLevelChar
        get() = when (logLevel) {
            LogLevel.ERROR -> "E"
            LogLevel.WARNING -> "W"
            LogLevel.INFO -> "I"
            LogLevel.DEBUG -> "D"
            LogLevel.VERBOSE -> "V"
        }

    /**
     * Full date for display in the UI
     */
    val uiDateTime: String
        get() {
            val simpleDF = SimpleDateFormat(Constants.LOG_ITEM_DATEFORMAT, Locale.getDefault())
            return simpleDF.format(Date(timestamp))
        }

    /**
     * Full text for the UI to display the log line.
     */
    val uiText: String
        get() = "${logLevelChar}/${uiDateTime}/${tag}/${text}"
}