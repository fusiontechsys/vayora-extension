package com.vayora.extension

import android.app.Application
import com.vayora.extension.cloud.DriveStateStore
import com.vayora.extension.cloud.SyncManager
import com.vayora.extension.cloud.SyncScheduler
import com.vayora.extension.data.SecurityRepository
import com.vayora.extension.data.VayoraDatabase
import com.vayora.extension.security.PasscodeManager

class VayoraApp : Application() {
    val repository: SecurityRepository by lazy {
        SecurityRepository(VayoraDatabase.build(this))
    }
    val passcodeManager: PasscodeManager by lazy {
        PasscodeManager(this)
    }
    val driveState: DriveStateStore by lazy {
        DriveStateStore(this)
    }
    val syncManager: SyncManager by lazy {
        SyncManager(this, repository, driveState)
    }

    override fun onCreate() {
        super.onCreate()
        SyncScheduler.schedulePeriodic(this)
    }
}
