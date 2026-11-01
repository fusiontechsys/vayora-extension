package com.vayora.extension

import android.app.Application
import com.vayora.extension.data.SecurityRepository
import com.vayora.extension.data.VayoraDatabase

class VayoraApp : Application() {
    val repository: SecurityRepository by lazy {
        SecurityRepository(VayoraDatabase.build(this))
    }
}
