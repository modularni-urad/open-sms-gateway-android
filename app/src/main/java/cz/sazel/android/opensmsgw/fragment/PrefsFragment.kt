package cz.sazel.android.opensmsgw.fragment

import android.app.Activity.RESULT_OK
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import cz.sazel.android.opensmsgw.App
import cz.sazel.android.opensmsgw.R
import cz.sazel.android.opensmsgw.util.isValidPhonenum
import cz.sazel.android.opensmsgw.util.isValidWsUrl

/**
 * Created on 1/17/20.
 */
class PrefsFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var prefWsURL: EditTextPreference
    private lateinit var prefWsPhonenum: EditTextPreference
    private val log by lazy { (activity!!.application as App).logger }

    private fun simpleErrorDialog(text: String) {
        AlertDialog.Builder(context!!)
            .setMessage(text)
            .setPositiveButton(android.R.string.ok, null).show()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)

        //val prefs = preferenceScreen.sharedPreferences

        with(preferenceScreen) {
            prefWsURL = findPreference(getString(R.string.pref_url))!!
            prefWsURL.setSummaryOrNothing(prefWsURL.text)
            prefWsURL.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    if (!isValidWsUrl(newValue as String)) {
                        simpleErrorDialog(getString(R.string.error_wss_url))
                        false
                    } else true
                }
            prefWsPhonenum = findPreference(getString(R.string.pref_phonenum))!!
            prefWsPhonenum.setSummaryOrNothing(prefWsPhonenum.text)
            prefWsPhonenum.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    if ((newValue as String?).isNullOrEmpty()) {
                        simpleErrorDialog(getString(R.string.error_phonenum_empty))
                        false
                    } else if (!isValidPhonenum(newValue as String)) {
                        simpleErrorDialog(getString(R.string.error_invalid_msisdn))
                        false
                    } else true
                }
        }

    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    /**
     * Sets summary text on the preference.
     * @param value value to be set
     */
    private fun EditTextPreference.setSummaryOrNothing(value: String?) {
        summary = if (value.isNullOrBlank()) getString(R.string.nothing_set) else value
    }

    /**
     * Called when some preference changes. Useful for setting summary for the EditTextPreferences.
     */
    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        when (key) {
            getString(R.string.pref_url) -> prefWsURL.setSummaryOrNothing(
                prefs.getString(
                    getString(R.string.pref_url),
                    ""
                )
            )
            getString(R.string.pref_phonenum) -> prefWsPhonenum.setSummaryOrNothing(
                prefs.getString(
                    getString(R.string.pref_phonenum),
                    ""
                )
            )
            else -> {
                log.d(TAG, "preference with key'$key' changed")
            }
        }
        activity?.setResult(RESULT_OK)
    }

    companion object {
        const val TAG = "PrefsActivity"
    }
}