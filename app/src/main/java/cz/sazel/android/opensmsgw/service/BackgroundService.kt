package cz.sazel.android.opensmsgw.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import cz.sazel.android.opensmsgw.App
import cz.sazel.android.opensmsgw.R
import cz.sazel.android.opensmsgw.activity.MainActivity
import cz.sazel.android.opensmsgw.sms.SmsSender
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext


/**
 * Service that handles connection to the server using web sockets and sending SMSes.
 */
class BackgroundService : Service(), CoroutineScope {

    internal class BackgroundServiceBinder(val service: BackgroundService) : Binder()

    private val log by lazy { (application as App).logger }

    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    private val binder = BackgroundServiceBinder(this)

    private val job = SupervisorJob()

    private var smsSender: SmsSender? = null

    private val channelId
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "My Background Service",
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
                CHANNEL_ID
            } else {
                ""
            }

    override fun onBind(intent: Intent?): IBinder? = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val ret = super.onStartCommand(intent, flags, startId)
        log.d(TAG, "service started")
        //starts service in with notification (on foreground), mandatory for a new Android versions
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.service_is_running))
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    0
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT).build()
        startForeground(NOTIFICATION_ID, notification)
        smsSender = SmsSender(this)
        serviceLoop()
        return ret
    }

    private fun serviceLoop() {
        launch(Dispatchers.IO + job) {
            var i = 0
            while (true) {
                delay(100)
                withContext(Dispatchers.Main) {
                    log.w(TAG, "$i")
                }
                i++
            }
        }
    }

    fun reload() {
        log.i(TAG, "Configuration changed, reloading")
    }


    override fun onDestroy() {
        smsSender = null
        super.onDestroy()
        job.cancel()
        log.d(TAG, "service destroyed")
    }

    companion object {
        private const val CHANNEL_ID = "service"
        private const val NOTIFICATION_ID = 1
        private const val TAG = "BackgroundService"

        fun isServiceRunning(ctx: Context): Boolean {
            val manager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (BackgroundService::class.java.name == service.service.className) {
                    return true
                }
            }
            return false
        }
    }

    override val coroutineContext: CoroutineContext = Dispatchers.IO + job
}