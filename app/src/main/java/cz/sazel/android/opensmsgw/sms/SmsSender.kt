package cz.sazel.android.opensmsgw.sms

import android.content.Context
import cz.sazel.android.opensmsgw.App

/**
 * SMS sending utility class.
 */
class SmsSender(val context: Context) {


    private val log by lazy { (context.applicationContext as App).logger }

    /**
     * Sends a SMS message.
     * @param to target's phone number
     * @param body text of the SMS
     */
    fun sendMessage(to: String, body: String) {
        log.v(TAG, "SMS to:${to} body:${body}")
    }

    companion object {
        const val TAG = "SmsSender"
    }

}
