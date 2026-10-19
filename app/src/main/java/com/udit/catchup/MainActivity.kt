package com.udit.catchup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.udit.catchup.notify.Notifier
import com.udit.catchup.ui.AppRoot
import com.udit.catchup.ui.theme.CatchUpTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialRoute = intent?.getStringExtra(Notifier.EXTRA_ROUTE)
        setContent {
            CatchUpTheme {
                AppRoot(initialRoute)
            }
        }
    }
}
