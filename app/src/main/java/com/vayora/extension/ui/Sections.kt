package com.vayora.extension.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

data class Section(
    val title: String,
    val blurb: String,
    val phase: Int,
    val icon: ImageVector
)

val Sections: List<Section> = listOf(
    Section("Device Security", "Unlock and authentication monitoring", 5, Icons.Filled.Warning),
    Section("Security Activity", "Chronological event timeline", 5, Icons.Filled.DateRange),
    Section("Cloud Evidence", "Drive uploads and sync status", 3, Icons.Filled.Refresh),
    Section("Photo Gallery", "Evidence photos streamed from Drive", 4, Icons.Filled.AccountBox),
    Section("Google Account", "Registered account and Drive access", 3, Icons.Filled.AccountCircle),
    Section("Passcode Settings", "Six-digit passcode and lockouts", 2, Icons.Filled.Lock),
    Section("Recovery Settings", "Google-verified passcode recovery", 7, Icons.Filled.Email),
    Section("Application Settings", "Preferences and secure logout", 7, Icons.Filled.Settings)
)
