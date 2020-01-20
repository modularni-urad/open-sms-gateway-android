package cz.sazel.android.opensmsgw.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import cz.sazel.android.opensmsgw.App
import cz.sazel.android.opensmsgw.R
import cz.sazel.android.opensmsgw.adapter.LogItemAdapter
import cz.sazel.android.opensmsgw.adapter.diff_callbacks.LogItemDiffCallback
import cz.sazel.android.opensmsgw.service.BackgroundService
import cz.sazel.android.opensmsgw.util.getDuration
import cz.sazel.android.opensmsgw.viewmodel.MainStateVM
import kotlinx.android.synthetic.main.activity_main.*


/**
 * Startup activity.
 */
class MainActivity : FragmentActivity() {

    private val log by lazy { (application as App).logger }

    private val mainStateVM: MainStateVM by viewModels()

    private val handler = Handler()

    private var backgroundService: BackgroundService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            log.d(TAG, "service disconnected")
            backgroundService = null
        }

        @SuppressLint("SetTextI18n")
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            log.d(TAG, "service connected")
            val binder = service as BackgroundService.BackgroundServiceBinder
            backgroundService = binder.service
            backgroundService?.onUpdateCounters = {
                val uptime = getDuration(it.startedAt, System.currentTimeMillis())
                runOnUiThread {
                    tvStatus.text =
                        "r:${it.smsRequests} ok:${it.smsSentOk} e!:${it.smsSentFailed} del:${it.smsDelivered}\nuptime $uptime"
                }
            }
        }

    }

    private val intentForService: Intent by lazy {
        return@lazy Intent().apply {
            component = ComponentName(this@MainActivity, BackgroundService::class.java)
            action = BackgroundService.ACTION_START
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindService(intentForService, serviceConnection, 0)
        mainStateVM.isServiceRunningLD.observe(this, Observer { isRunning ->
            btConnect.text = getString(if (isRunning) R.string.stop else R.string.start)
            if (!isRunning) tvStatus.text = getString(R.string.service_not_running)
        })
        mainStateVM.checkServiceRunning(this)
        btConnect.setOnClickListener {
            if (mainStateVM.isServiceRunningLD.value!!) {
                stopService(intentForService)
            } else {
                startService(intentForService)
                bindService(intentForService, serviceConnection, 0)
            }
            mainStateVM.checkServiceRunning(this)
        }
        btSetup.setOnClickListener {
            //starts preference activity, waits for result in
            startActivityForResult(
                Intent(this@MainActivity, PrefsActivity::class.java),
                REQUEST_PREFERENCES
            )
        }

        val lm = LinearLayoutManager(this@MainActivity).apply {
            stackFromEnd = true
            reverseLayout = true
        }

        rvLog.apply {
            adapter = LogItemAdapter(log)
            layoutManager = lm
        }


        // handles d
        log.onLogChanged =
            { newLog, _ ->
                rvLog.adapter?.apply {
                    runOnUiThread {
                        val diffResult =
                            DiffUtil.calculateDiff(LogItemDiffCallback(log.contents, newLog))
                        diffResult.dispatchUpdatesTo(this@apply)
                        if (cbFollowLog.isChecked) rvLog.scrollToPosition(itemCount)
                    }
                }
            }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            )
            != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.SEND_SMS),
                REQUEST_PERMISSION_SEND_SMS
            )
        }

    }

    override fun onDestroy() {
        log.onLogChanged = null
        if (backgroundService != null) unbindService(serviceConnection)
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        log.d(TAG, "returned from PrefsActivity")
        if (requestCode == REQUEST_PREFERENCES && resultCode == RESULT_OK) {
            backgroundService?.reload()
        } else if (requestCode == REQUEST_PERMISSION_SEND_SMS && resultCode == RESULT_OK) {
            log.d(TAG, "sms permissions granted")
        }
    }

    companion object {
        const val TAG = "MainActivity"
        const val REQUEST_PREFERENCES = 1
        const val REQUEST_PERMISSION_SEND_SMS = 2
    }

}
