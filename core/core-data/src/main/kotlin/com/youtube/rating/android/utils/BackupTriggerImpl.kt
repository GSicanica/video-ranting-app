package com.youtube.rating.android.utils

class BackupTriggerImpl(
    private val autoBackupManager: AutoBackupManager
) : BackupTrigger {
    override fun trigger(changeType: ChangeType) {
        autoBackupManager.triggerAutoBackup(changeType)
    }
}
