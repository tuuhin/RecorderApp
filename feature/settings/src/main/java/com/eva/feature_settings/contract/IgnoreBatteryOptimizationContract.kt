package com.eva.feature_settings.contract

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContract

internal class IgnoreBatteryOptimizationContract : ActivityResultContract<Unit, Unit>() {

    @SuppressLint("BatteryLife")
    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .apply {
                data = Uri.fromParts("package", context.packageName, null)

                addCategory(Intent.CATEGORY_DEFAULT)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }
    }

    override fun parseResult(resultCode: Int, intent: Intent?) {
        return
    }
}
