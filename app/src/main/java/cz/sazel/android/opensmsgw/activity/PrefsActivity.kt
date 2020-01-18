package cz.sazel.android.opensmsgw.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cz.sazel.android.opensmsgw.R
import kotlinx.android.synthetic.main.activity_prefs.*

class PrefsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prefs)
        setSupportActionBar(toolbar)
    }
}