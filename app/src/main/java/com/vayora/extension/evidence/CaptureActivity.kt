package com.vayora.extension.evidence

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.vayora.extension.VayoraApp
import com.vayora.extension.ui.theme.Blue300
import com.vayora.extension.ui.theme.VayoraTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Visible screen shown over the lock screen while a security photo is taken.
 * Android only lets an app use the camera from a visible screen, so this is
 * deliberately not hidden.
 */
class CaptureActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        setContent {
            VayoraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "VAYORA EXTENSION",
                            style = MaterialTheme.typography.labelMedium,
                            color = Blue300
                        )
                        Text(
                            "Security photo being recorded",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        if (eventId < 0) {
            finish()
            return
        }
        val app = application as VayoraApp
        lifecycleScope.launch {
            app.evidenceCoordinator.capture(this@CaptureActivity, this@CaptureActivity, eventId)
            delay(600)
            finish()
        }
    }

    companion object {
        const val EXTRA_EVENT_ID = "event_id"
    }
}
