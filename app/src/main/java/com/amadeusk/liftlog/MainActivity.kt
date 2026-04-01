package com.amadeusk.liftlog

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.amadeusk.liftlog.data.loadReminderEnabled
import com.amadeusk.liftlog.reminders.DailyReminderScheduler

class MainActivity : ComponentActivity() {

    private val prViewModel: PRViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (loadReminderEnabled(applicationContext)) {
            DailyReminderScheduler.scheduleAllIfEnabled(applicationContext)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED -> { }
                loadReminderEnabled(this) ->
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else -> { }
            }
        }

        setContent {
            LiftLogRoot(viewModel = prViewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        if (loadReminderEnabled(applicationContext)) {
            DailyReminderScheduler.scheduleAllIfEnabled(applicationContext)
        }
    }
}

