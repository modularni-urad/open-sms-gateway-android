package cz.sazel.android.opensmsgw.util

import android.net.Uri
import java.util.*

/**
 * Checks if url is valid for Websockets.
 * @param url checked url
 * @return true if it's ok
 */
fun isValidWsUrl(url: String?): Boolean {
    return try {
        val uri = Uri.parse(url)
        (uri?.scheme?.toLowerCase(Locale.getDefault()) in listOf("ws", "wss"))
    } catch (e: Exception) {
        false
    }
}

/**
 * Checks if phone number/identifier is ok
 * @param phonenum string to check
 * @return true if it's valid identifier
 */
fun isValidPhonenum(phonenum: String): Boolean = phonenum.matches(Regex("[0-9]{9}"))