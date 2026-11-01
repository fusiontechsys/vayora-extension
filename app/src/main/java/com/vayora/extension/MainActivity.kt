package com.vayora.extension

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.vayora.extension.ui.AuthViewModel
import com.vayora.extension.ui.DashboardViewModel
import com.vayora.extension.ui.VayoraRoot
import com.vayora.extension.ui.theme.VayoraTheme

class MainActivity : ComponentActivity() {

    private val dashboardViewModel: DashboardViewModel by viewModels {
        DashboardViewModel.factory((application as VayoraApp).repository)
    }

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModel.factory((application as VayoraApp).passcodeManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        setContent {
            VayoraTheme {
                VayoraRoot(dashboardViewModel, authViewModel)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            authViewModel.lock()
        }
    }
}
