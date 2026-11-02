package com.vayora.extension

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.vayora.extension.monitor.MonitorService
import com.vayora.extension.ui.AuthViewModel
import com.vayora.extension.ui.CloudViewModel
import com.vayora.extension.ui.DashboardViewModel
import com.vayora.extension.ui.GalleryViewModel
import com.vayora.extension.ui.MonitorViewModel
import com.vayora.extension.ui.VayoraRoot
import com.vayora.extension.ui.theme.VayoraTheme

class MainActivity : ComponentActivity() {

    private val dashboardViewModel: DashboardViewModel by viewModels {
        val app = application as VayoraApp
        DashboardViewModel.factory(app.repository, app.driveState)
    }

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModel.factory((application as VayoraApp).passcodeManager)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        setContent {
            VayoraTheme {
                VayoraRoot(
                    dashboardViewModel,
                    authViewModel,
                    cloudViewModel,
                    galleryViewModel,
                    monitorViewModel
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val app = application as VayoraApp
        if (app.monitorSettings.state.value.enabled && !MonitorService.running.value) {
            MonitorService.start(this)
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            authViewModel.lock()
            galleryViewModel.clear()
        }
    }
}
