package com.vayora.extension.monitor

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

object AdminHelper {

    private fun component(context: Context) =
        ComponentName(context, VayoraAdminReceiver::class.java)

    private fun manager(context: Context) =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    fun isActive(context: Context): Boolean =
        manager(context).isAdminActive(component(context))

    fun requestIntent(context: Context): Intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component(context))
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Lets Sentinel by Vayora be told when a wrong lock-screen PIN, pattern or " +
                    "password is entered. It cannot lock, wipe or change anything on your phone."
            )
        }

    fun remove(context: Context) {
        manager(context).removeActiveAdmin(component(context))
    }
}
