package cz.sazel.android.opensmsgw.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import cz.sazel.android.opensmsgw.App
import cz.sazel.android.opensmsgw.Constants
import cz.sazel.android.opensmsgw.R
import cz.sazel.android.opensmsgw.activity.MainActivity
import cz.sazel.android.opensmsgw.sms.SmsSender
import cz.sazel.android.opensmsgw.util.getResultCodeText
import io.socket.engineio.client.Socket
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URISyntaxException
import java.util.*
import kotlin.coroutines.CoroutineContext


/**
 * Service that handles connection to the server using web sockets and sending SMSes.
 */
class BackgroundService : Service(), CoroutineScope {

    internal class BackgroundServiceBinder(val service: BackgroundService) : Binder()

    private val log by lazy {
        (application as App).logger
    }


    private val prefs by lazy { (application as App).defaultPreferences }

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

    private var socket: Socket? = null
    private var disconnecting = false

    data class Counters(
        var smsRequests: Long = 0L,
        var smsSentOk: Long = 0L,
        var smsSentFailed: Long = 0L,
        var smsDelivered: Long = 0L,
        val startedAt: Long = System.currentTimeMillis()
    )

    private var counters = Counters()

    /**
     * Set this to listen for the update of the counters.
     */
    var onUpdateCounters: ((Counters) -> Unit)? = null

    override fun onBind(intent: Intent?): IBinder? = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val ret = super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> {
                log.d(TAG, "service started")
                counters = Counters()
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
                smsSender = SmsSender(this, Random(System.currentTimeMillis()).nextLong())
                connect()
                serviceLoop()
                log.d(TAG, "onStartCommand() finished")
            }
            ACTION_SMS_SENT -> {
                val resultCode = intent.getIntExtra(Constants.EXTRA_RESULT, Activity.RESULT_OK)
                val smsId = intent.getLongExtra(Constants.EXTRA_SMS_ID, 0)
                if (resultCode > 0) {
                    log.e(
                        TAG,
                        "sms [$smsId] send failed, result ${getResultCodeText(resultCode)}"
                    )
                    counters.smsSentFailed++
                } else {
                    log.v(TAG, "sms $smsId sent ok")
                    counters.smsSentOk++
                }

            }
            ACTION_SMS_DELIVERED -> {
                log.d(
                    TAG,
                    "sms ${intent.getLongExtra(
                        Constants.EXTRA_SMS_ID,
                        0
                    )} delivered, result $intent"
                )
                counters.smsDelivered++
            }
            else -> log.w(TAG, "invalid service action <${intent?.action}")
        }
        return ret
    }

    private fun serviceLoop() {
        launch(Dispatchers.IO + job) {
            while (true) {

                onUpdateCounters?.invoke(counters)
                delay(Constants.COUNTER_UPDATE_DELAY)
            }
        }
    }

    fun reconnect() {
        log.v(TAG, "Reconnecting")
        disconnect()
        launch(Dispatchers.Main) {
            delay(Constants.RECONNECT_DELAY)
            connect()
        }
    }

    /**
     * Connects to the server.
     */
    private fun connect() {
        if (socket != null) throw IllegalStateException("already connected")
        val uriWithoutNum = prefs.getString(getString(R.string.pref_url), "")
        val num = prefs.getString(getString(R.string.pref_phonenum), "")
        try {
            val uri = Uri.parse(uriWithoutNum).buildUpon().apply {
                appendQueryParameter(Constants.SUBSCRIBE_QUERY_NUM, num)
            }.build()
            if (uri.scheme in arrayOf("http", "ws")) log.w(
                TAG,
                "unencrypted insecure connection, don't use in production!"
            )
            socket = Socket(uri.toString(), object : Socket.Options() {
                init {
                    transports = arrayOf("websocket")
                }
            })
            socket?.on(
                Socket.EVENT_OPEN
            ) {
                log.v(TAG, "engine.io socket open to <$uri>")
            }
                ?.on(Socket.EVENT_MESSAGE) {
                    try {
                        val obj = JSONObject(it[0] as String)
                        val to = obj.getString(Constants.SMSREQUEST_PARAM_NUM)
                        val msg = obj.getString(Constants.SMSREQUEST_PARAM_MSG)
                        counters.smsRequests++
                        val smsId = smsSender!!.sendMessage(to, msg)
                        if (smsId > 0) {
                            log.d(TAG, "sms send_result ok")
                            socket?.send("ok")
                        } else {
                            log.d(TAG, "sms send_result error")
                            socket?.send("error")
                        }
                    } catch (e: Exception) {
                        log.e(TAG, "bad: ${e.message}")
                        counters.smsSentFailed++
                        try {
                            socket?.send("error")
                        } catch (e: Exception) {
                            log.e(TAG, "bad: ${e.message}")
                        }
                    }
                }?.on(Socket.EVENT_CLOSE) {
                    log.d(TAG, "engine.io closed <$uriWithoutNum>")
                    if (!disconnecting) {
                        log.w(TAG, "unexpected disconnect, reconnecting")
                        socket = null
                        connect()
                    }
                    disconnecting = false
                }?.on(Socket.EVENT_ERROR) {
                    log.e(
                        TAG,
                        "error ${it.filterIsInstance<Exception>().fold<Exception, String>(
                            "",
                            { acc, e -> "$acc${e.cause?.message} \n" })}"
                    )
                }
            log.v(TAG, "opening connection to <$uri>")
            socket?.open()
            log.d(TAG, "after connect, starting service loop")

        } catch (e: URISyntaxException) {
            log.e(
                TAG,
                "Invalid URI for connection <$uriWithoutNum>"
            )
        } catch (e: UnsupportedOperationException) {
            log.e(
                TAG,
                "Invalid URI for connection <$uriWithoutNum> and num parameter <${num}>"
            )
        } catch (e: Exception) {
            log.e(TAG, "Error <${e.message}>")
            if (socket != null) socket?.close()
        }
    }

    private fun disconnect() {
        disconnecting = true
        if (socket != null) socket?.close()
        socket = null
    }


    override fun onDestroy() {
        log.d(TAG, "service stopped, destroying service")
        disconnect()
        smsSender = null
        super.onDestroy()
        job.cancel()
        log.d(TAG, "service destroyed")
    }

    companion object {
        private const val CHANNEL_ID = "service"
        private const val NOTIFICATION_ID = 1
        private const val TAG = "BackgroundService"
        val ACTION_START = "${BackgroundService::class.java.name}.ACTION_START"
        val ACTION_SMS_SENT = "${BackgroundService::class.java.name}.ACTION_SMS_SENT"
        val ACTION_SMS_DELIVERED = "${BackgroundService::class.java.name}.ACTION_SMS_DELIVERED"


        @Suppress("DEPRECATION")
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

    override val coroutineContext: CoroutineContext = Dispatchers.Unconfined + job
}