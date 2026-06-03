package com.youtube.rating.android.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.youtube.rating.android.storage.NotesManager
import com.youtube.rating.android.storage.OfflineVideoManager
import com.youtube.rating.android.utils.DataBackup
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupIntegrationTest {

    @Test
    fun backup_includes_notes_and_offline_videos() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext

        val notesManager = GlobalContext.get().get<NotesManager>()
        val offlineManager = OfflineVideoManager(context = ctx)

        // clear existing data to ensure predictability
        notesManager.clearAllAsync()
        offlineManager.clearAllVideosAsync()

        // add a note
        val note = notesManager.addNoteAsync("Integration Test", "Backup note content")
        assertTrue("Note added", note != null)

        // add offline video reference
        val video = offlineManager.saveYouTubeVideoReference(
            youtubeId = "test_video_123",
            title = "Test Video",
            channelName = "Test Channel",
            thumbnailUrl = "",
            category = "test"
        )
        assertTrue("Offline video added", video != null)

        // wait until repository reflects changes (avoid race)
        kotlinx.coroutines.withTimeout(5000) {
            while (notesManager.getAllNotesAsync().isEmpty() || offlineManager.getOfflineVideosAsync().isEmpty()) {
                kotlinx.coroutines.delay(100)
            }
        }

        // create backup
        val result = DataBackup.createBackup(ctx)
        when (result) {
            is com.youtube.rating.android.utils.BackupResult.Success -> {
                val file = java.io.File(result.filePath)
                val text = file.readText()
                val json = JSONObject(text)
                val notesArr = json.optJSONArray("notes")
                val videosArr = json.optJSONArray("offlineVideos")

                assertTrue("Notes present in backup", notesArr != null && notesArr.length() > 0)
                assertTrue("Offline videos present in backup", videosArr != null && videosArr.length() > 0)
            }
            is com.youtube.rating.android.utils.BackupResult.Failed -> {
                assertTrue("Backup failed: ${result.error}", false)
            }
        }
    }
}
