package cz.sazel.android.opensmsgw.sms

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import cz.sazel.android.opensmsgw.App
import cz.sazel.android.opensmsgw.Constants
import cz.sazel.android.opensmsgw.receiver.SmsResultBroadcastReceiver
import cz.sazel.android.opensmsgw.service.BackgroundService
import kotlin.random.Random

/**
 * SMS sending utility class.
 */
class SmsSender(val context: Context, val seed: Long) {


    private val log by lazy { (context.applicationContext as App).logger }
    private val smsManager by lazy { SmsManager.getDefault() }
    private var nextSmsNumber = Random(seed).nextLong(0, Long.MAX_VALUE - 1)

    /**
     * Sends a SMS message.
     * @param to target's phone number
     * @param body text of the
     * @return unique id of sms
     */
    fun sendMessage(to: String, body: String): Long {

        val sentIntent = Intent(context, SmsResultBroadcastReceiver::class.java).apply {
            action = BackgroundService.ACTION_SMS_SENT
            putExtra(Constants.EXTRA_SMS_ID, nextSmsNumber)
        }
        val deliveredIntent = Intent(context, SmsResultBroadcastReceiver::class.java).apply {
            action = BackgroundService.ACTION_SMS_DELIVERED
            putExtra(Constants.EXTRA_SMS_ID, nextSmsNumber)
        }
        val num = nextSmsNumber
        log.i(TAG, "SMS [$num] to:${to} body:${body}")
        try {
            smsManager.sendTextMessage(
                to,
                null,
                body,
                PendingIntent.getBroadcast(
                    context,
                    0,
                    sentIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                ),
                PendingIntent.getBroadcast(
                    context,
                    0,
                    deliveredIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            nextSmsNumber = (nextSmsNumber + 1) % Long.MAX_VALUE

        } catch (e: Exception) {
            log.e(TAG, "error sending sms [$num] ${e.message}")
            return -1
        }
        return num
    }

    companion object {
        const val TAG = "SmsSender"
    }

}
