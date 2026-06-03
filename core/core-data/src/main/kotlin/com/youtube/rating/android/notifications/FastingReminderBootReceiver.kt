package com.youtube.rating.android.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.youtube.rating.android.storage.FastingManager
import com.youtube.rating.android.utils.FastingReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FastingReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_LOCKED_BOOT_COMPLETED) return

        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val manager = FastingManager.getInstance(appContext)
                FastingReminderScheduler.schedule(appContext, manager.getReminderSettings())
            } finally {
                pending.finish()
            }
        }
    }
}
