package com.vayora.extension.ui

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vayora.extension.VayoraApp
import com.vayora.extension.evidence.CaptureResult
import com.vayora.extension.ui.theme.AccentSoft
import com.vayora.extension.ui.theme.Accent
import com.vayora.extension.ui.theme.Raised
import com.vayora.extension.ui.theme.Warning
import kotlinx.coroutines.delay

private fun formatCountdown(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "$m:${s.toString().padStart(2, '0')}"
}

@Composable
fun LockScreen(ui: AuthUi, auth: AuthViewModel) {
    var captureNote by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        val app = context.applicationContext as VayoraApp
        app.evidenceCoordinator.requests.collect { eventId ->
            val result = app.evidenceCoordinator.capture(context, owner, eventId)
            if (result == CaptureResult.SUCCESS) {
                captureNote = "Security photo recorded"
                delay(4000)
                captureNote = null
            }
        }
    }

    val titleAndSubtitle: Pair<String, String> = when (ui.stage) {
        Stage.SETUP -> "Create your passcode" to "Choose a six-digit passcode"
        Stage.SETUP_CONFIRM -> "Confirm your passcode" to "Enter the same six digits again"
        Stage.LOCKED -> "Enter passcode" to "Sentinel by Vayora is locked"
        Stage.CHANGE_CURRENT -> "Current passcode" to "Enter your current passcode"
        Stage.CHANGE_NEW -> "New passcode" to "Choose a new six-digit passcode"
        Stage.CHANGE_CONFIRM -> "Confirm new passcode" to "Enter the new digits again"
        Stage.RECOVERY_NEW -> "Reset passcode" to "Choose a new six-digit passcode"
        Stage.RECOVERY_CONFIRM -> "Confirm new passcode" to "Enter the new digits again"
        Stage.RECOVERY -> "" to ""
        Stage.UNLOCKED -> "" to ""
    }

    val lockText: String? = if (ui.lockSeconds > 0) {
        "Too many attempts. Try again in ${formatCountdown(ui.lockSeconds)}"
    } else {
        null
    }
    val attemptsText: String? = ui.attemptsLeft?.let { "$it attempts remaining before lockout" }
    val info: String = lockText ?: ui.message ?: ui.notice ?: attemptsText
        ?: titleAndSubtitle.second
    val isError = lockText != null || ui.message != null
    val infoColor = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        SentinelLogo(Modifier.width(300.dp).aspectRatio(LogoAspect))
        Spacer(Modifier.height(44.dp))
        Text(titleAndSubtitle.first, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            info,
            style = MaterialTheme.typography.bodyMedium,
            color = infoColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.heightIn(min = 40.dp)
        )
        Spacer(Modifier.height(20.dp))
        PinDots(ui.filled)
        Spacer(Modifier.height(8.dp))
        Text(
            captureNote ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = Warning,
            modifier = Modifier.heightIn(min = 20.dp)
        )
        Spacer(Modifier.height(16.dp))
        Keypad(
            enabled = !ui.busy && ui.lockSeconds <= 0,
            onDigit = { auth.onDigit(it) },
            onDelete = { auth.onDelete() }
        )
        Spacer(Modifier.height(12.dp))
        when (ui.stage) {
            Stage.LOCKED -> TextButton(onClick = { auth.startRecovery() }) {
                Text("Forgot passcode?")
            }
            Stage.CHANGE_CURRENT,
            Stage.CHANGE_NEW,
            Stage.CHANGE_CONFIRM -> TextButton(onClick = { auth.cancelChange() }) {
                Text("Cancel")
            }
            Stage.RECOVERY_NEW,
            Stage.RECOVERY_CONFIRM -> TextButton(onClick = { auth.cancelRecovery() }) {
                Text("Cancel")
            }
            else -> Unit
        }
    }
}

@Composable
private fun PinDots(filled: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        repeat(6) { index ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (index < filled) Accent else Raised)
            )
        }
    }
}

@Composable
private fun Keypad(
    enabled: Boolean,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        listOf("123", "456", "789").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                row.forEach { ch ->
                    KeyButton(ch.toString(), enabled) { onDigit(ch) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            Spacer(Modifier.size(78.dp))
            KeyButton("0", enabled) { onDigit('0') }
            KeyButton("\u232B", enabled) { onDelete() }
        }
    }
}

@Composable
private fun KeyButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(78.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 26.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
