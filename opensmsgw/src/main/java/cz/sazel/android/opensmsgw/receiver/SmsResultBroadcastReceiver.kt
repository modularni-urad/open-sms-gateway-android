package cz.sazel.android.opensmsgw.receiver

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import cz.sazel.android.opensmsgw.Constants
import cz.sazel.android.opensmsgw.service.BackgroundService

/**
 * Capturing result from sms sending. Passing it to the BackgroundService.
 */
class SmsResultBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        intent.apply {
            component = ComponentName(context, BackgroundService::class.java)
            putExtra(Constants.EXTRA_RESULT, resultCode)
            context.startService(this)
        }
    }
}