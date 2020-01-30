package cz.sazel.android.opensmsgw.util

/**
 * Created on 1/20/20.
 */

/**
 * Returns string explanation for SMS sent result codes.
 */
fun getResultCodeText(resultCode: Int) = when (resultCode) {
    -1 -> "OK"
    1 -> "ERROR_GENERIC_FAILURE"
    5 -> "ERROR_LIMIT_EXCEEDED"
    4 -> "ERROR_NO_SERVICE"
    3 -> "ERROR_NULL_PDU"
    2 -> "ERROR_RADIO_OFF"
    8 -> "ERROR_SHORT_CODE_NEVER_ALLOWED"
    9 -> "ERROR_SHORT_CODE_NOT_ALLOWED"
    else -> "OTHER_ERROR"
}


fun getDuration(timeStart: Long, timeEnd: Long): String {
    val dur = (timeEnd - timeStart) / 1000
    return String.format("%d:%02d:%02d", dur / 3600, (dur % 3600) / 60, (dur % 60))
}