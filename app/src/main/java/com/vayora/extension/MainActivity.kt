package com.vayora.extension

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.vayora.extension.ui.DashboardViewModel
import com.vayora.extension.ui.VayoraRoot
import com.vayora.extension.ui.theme.VayoraTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModel.factory((application as VayoraApp).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VayoraTheme {
                VayoraRoot(viewModel)
            }
        }
    }
}
