package cz.sazel.android.opensmsgw

import android.app.Application
import androidx.preference.PreferenceManager
import cz.sazel.android.opensmsgw.util.Logger

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
    val defaultPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

}