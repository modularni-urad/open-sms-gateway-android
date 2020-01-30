package cz.sazel.android.opensmsgw.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cz.sazel.android.opensmsgw.service.BackgroundService

/**
 * Viewmodel for the MainActivity.
 */
class MainStateVM : ViewModel() {

    val isServiceRunningLD = MutableLiveData<Boolean>()

    fun checkServiceRunning(ctx: Context) {
        isServiceRunningLD.value = BackgroundService.isServiceRunning(ctx)
    }
}