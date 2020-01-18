package cz.sazel.android.opensmsgw.util

import android.util.Log
import cz.sazel.android.opensmsgw.Constants
import cz.sazel.android.opensmsgw.model.LogItem
import cz.sazel.android.opensmsgw.model.LogLevel.*

/**
 * Logger that logs to the Logcat and
 */
class Logger {

    /**
     * In-memory log storage of the logger.
     */
    private var internalLog = mutableListOf<LogItem>()

    /**
     * Current contents of the log.
     */
    val contents: List<LogItem>
        get() = internalLog

    /**
     * Listener to listen if anything changes in the log.
     */
    var onLogChanged: ((newLog: List<LogItem>, logItem: LogItem) -> Unit)? = null

    /**
     * If *true*, it will be logged also in the system log (insecure), if *false* it will go only in the app log.
     */
    var logcat = true

    private fun logcat(logCode: () -> Unit): Logger {
        if (logcat) logCode()
        return this
    }

    private fun localLog(logItem: LogItem) {
        val copyLog = internalLog.toMutableList()
        if (copyLog.size > Constants.LOGROTATE_LINES) {
            copyLog.removeAt(0)
        }
        copyLog.add(logItem)
        onLogChanged?.invoke(copyLog, logItem)
        internalLog = copyLog
    }



    /**
     * Logs error (6) message.
     */
    fun e(tag: String, msg: String) =
        logcat { Log.e(tag, msg) }.localLog(LogItem(ERROR,System.currentTimeMillis(),tag,msg))

    /**
     * Logs warning (5) message.
     */
    fun w(tag: String, msg: String) =
        logcat { Log.i(tag, msg) }.localLog(LogItem(WARNING,System.currentTimeMillis(),tag,msg))

    /**
     * Logs info (4) message.
     */
    fun i(tag: String, msg: String) =
        logcat { Log.i(tag, msg) }.localLog(LogItem(INFO,System.currentTimeMillis(),tag,msg))


    /**
     * Logs debug (3) message.
     */
    fun d(tag: String, msg: String) =
        logcat { Log.d(tag, msg) }.localLog(LogItem(DEBUG,System.currentTimeMillis(),tag,msg))

    /**
     * Logs verbose (2) message.
     */
    fun v(tag: String, msg: String) =
        logcat { Log.v(tag, msg) }.localLog(LogItem(VERBOSE,System.currentTimeMillis(),tag,msg))
}