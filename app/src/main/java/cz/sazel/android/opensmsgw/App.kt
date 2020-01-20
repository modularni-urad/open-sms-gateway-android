package cz.sazel.android.opensmsgw

import android.app.Application
import androidx.preference.PreferenceManager
import cz.sazel.android.opensmsgw.util.Logger
import kotlin.random.Random

/**
 * Created on 1/17/20.
 */
class App : Application() {
    /**
     * Shared logger for the application
     */
    val logger = Logger().apply { logcat = BuildConfig.LOGCAT }



    /**
     * SharedPreferences for the application.
     */
    val defaultPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this).apply {
            edit().apply {
                //following code creates random identifier on the first start of the app
                if (getString(getString(R.string.pref_phonenum), "").isNullOrBlank()) {
                    val randomNum = Random(System.currentTimeMillis()).nextLong(0, 999999999)
                    putString(
                        getString(R.string.pref_phonenum),
                        randomNum.toString().padStart(9, '0')
                    )
                }
            }.apply()
        }
    }

}