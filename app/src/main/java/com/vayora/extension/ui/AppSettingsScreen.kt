package com.vayora.extension.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vayora.extension.ui.theme.ThemeMode

@Composable
fun AppSettingsScreen(
    themeMode: ThemeMode,
    onThemeMode: (ThemeMode) -> Unit,
    onBack: () -> Unit,
    onSecureLogout: () -> Unit
) {
    val context = LocalContext.current
    var confirm by remember { mutableStateOf(false) }
    val version = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
    } catch (e: Exception) {
        "1.0"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        ScreenHeader("Application Settings", onBack)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Choose light or dark, or follow your phone's setting.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = themeMode == ThemeMode.SYSTEM,
                        onClick = { onThemeMode(ThemeMode.SYSTEM) },
                        label = { Text("System") }
                    )
                    FilterChip(
                        selected = themeMode == ThemeMode.LIGHT,
                        onClick = { onThemeMode(ThemeMode.LIGHT) },
                        label = { Text("Light") }
                    )
                    FilterChip(
                        selected = themeMode == ThemeMode.DARK,
                        onClick = { onThemeMode(ThemeMode.DARK) },
                        label = { Text("Dark") }
                    )
                }
            }
        }

        Panel("Version", "Sentinel by Vayora $version")
        Panel(
            "Local storage",
            "Evidence photos are encrypted with a key held in the Android Keystore before " +
                "they are saved on this phone, and removed from the phone once they have " +
                "uploaded to your Google Drive."
        )
        Panel(
            "Privacy",
            "All Google traffic uses HTTPS. The app contains no analytics, no advertising " +
                "and no third-party services. Evidence goes only to the Google Drive " +
                "account registered in this app."
        )
        Panel(
            "Permissions",
            "Camera for evidence photos, notifications for the monitoring indicator, and " +
                "optional Device Admin and display-over-apps access for failed-attempt " +
                "detection. To fully revoke Google access, remove Sentinel by Vayora in " +
                "your Google Account permissions."
        )
        VOutlinedButton(
            onClick = { confirm = true },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Secure logout") }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Secure logout?") },
            text = {
                Text(
                    "Locks the app, clears cached images and disconnects Google Drive from " +
                        "this app. Files already in Google Drive stay there. Photos not yet " +
                        "uploaded stay encrypted on this phone. Passcode recovery will not " +
                        "work until you connect Google again."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    onSecureLogout()
                }) { Text("Log out") }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text("Cancel") }
            }
        )
    }
}
