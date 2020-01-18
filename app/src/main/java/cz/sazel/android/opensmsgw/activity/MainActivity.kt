package cz.sazel.android.opensmsgw.activity

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import cz.sazel.android.opensmsgw.App
import cz.sazel.android.opensmsgw.R
import cz.sazel.android.opensmsgw.adapter.LogItemAdapter
import cz.sazel.android.opensmsgw.adapter.diff_callbacks.LogItemDiffCallback
import cz.sazel.android.opensmsgw.service.BackgroundService
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

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            log.d(TAG, "service connected")
            val binder = service as BackgroundService.BackgroundServiceBinder
            backgroundService = binder.service
        }

    }

    private val intentForService: Intent by lazy {
        return@lazy Intent().apply {
            component = ComponentName(this@MainActivity, BackgroundService::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindService(intentForService, serviceConnection, 0)
        mainStateVM.isServiceRunningLD.observe(this, Observer { isRunning ->
            btConnect.text = getString(if (isRunning) R.string.stop else R.string.start)
            tvStatus.text =
                if (isRunning) "${getString(R.string.sms_sent)}: ?" else getString(R.string.service_not_running)
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
                val diffResult = DiffUtil.calculateDiff(LogItemDiffCallback(log.contents, newLog))
                rvLog.adapter?.apply {
                    diffResult.dispatchUpdatesTo(this)

                    if (cbFollowLog.isChecked) handler.postDelayed(
                        { rvLog.smoothScrollToPosition(itemCount) },
                        100
                    )
                }
            }
    }

    override fun onDestroy() {
        if (backgroundService != null) unbindService(serviceConnection)
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        log.d(TAG, "returned from PrefsActivity")
        if (requestCode == REQUEST_PREFERENCES && resultCode == RESULT_OK) {
            backgroundService?.reload()
        }
    }

    companion object {
        const val TAG = "MainActivity"
        const val REQUEST_PREFERENCES = 1
    }

}
