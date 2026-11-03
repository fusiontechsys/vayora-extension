package com.vayora.extension

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.activity.enableEdgeToEdge
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.vayora.extension.evidence.CameraCapture
import com.vayora.extension.monitor.MonitorService
import com.vayora.extension.security.ExternalFlowGrace
import com.vayora.extension.ui.AuthViewModel
import com.vayora.extension.ui.CloudViewModel
import com.vayora.extension.ui.DashboardViewModel
import com.vayora.extension.ui.GalleryViewModel
import com.vayora.extension.ui.MonitorViewModel
import com.vayora.extension.ui.RecoveryViewModel
import com.vayora.extension.ui.VayoraRoot
import com.vayora.extension.ui.theme.VayoraTheme

class MainActivity : ComponentActivity() {

    private val dashboardViewModel: DashboardViewModel by viewModels {
        val app = application as VayoraApp
        DashboardViewModel.factory(app.repository, app.driveState, app.recoveryManager)
    }

    private val authViewModel: AuthViewModel by viewModels {
        val app = application as VayoraApp
        AuthViewModel.factory(app.passcodeManager) { failures ->
            app.evidenceCoordinator.onWrongPasscode(failures)
        }
    }

    private val cloudViewModel: CloudViewModel by viewModels {
        CloudViewModel.factory(application as VayoraApp)
    }

    private val galleryViewModel: GalleryViewModel by viewModels {
        GalleryViewModel.factory(application as VayoraApp)
    }

    private val monitorViewModel: MonitorViewModel by viewModels {
        MonitorViewModel.factory(application as VayoraApp)
    }

    private val recoveryViewModel: RecoveryViewModel by viewModels {
        RecoveryViewModel.factory(application as VayoraApp)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        val app = application as VayoraApp
        setContent {
            val themeMode by app.themeSettings.mode.collectAsStateWithLifecycle()
            VayoraTheme(themeMode) {
                VayoraRoot(
                    dashboardViewModel,
                    authViewModel,
                    cloudViewModel,
                    galleryViewModel,
                    monitorViewModel,
                    recoveryViewModel,
                    app.themeSettings
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        authViewModel.onForeground()
        val app = application as VayoraApp
        if (!app.monitorSettings.state.value.enabled) return
        if (!MonitorService.running.value) {
            MonitorService.start(this, true)
        } else {
            val evidence = app.evidenceCoordinator.settings.state.value
            val wantsCamera = evidence.captureOnUnlock || evidence.captureOnSystemFail
            if (wantsCamera && !MonitorService.cameraActive.value &&
                CameraCapture.hasPermission(this)
            ) {
                // The service only gets background camera access when it is started
                // while this screen is visible, so restart it now.
                MonitorService.stop(this)
                MonitorService.start(this, true)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ExternalFlowGrace.end()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            authViewModel.lockFromBackground()
            galleryViewModel.clear()
        }
    }
}
