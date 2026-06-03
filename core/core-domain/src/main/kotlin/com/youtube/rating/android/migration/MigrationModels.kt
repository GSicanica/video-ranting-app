package com.youtube.rating.android.migration

/**
 * Result of migration operation.
 *
 * Kept in commonMain so the domain layer can depend on it without Android types.
 */
sealed class MigrationResult {
    data object AlreadyMigrated : MigrationResult()
    data class Success(val stats: MigrationStats) : MigrationResult()
    data class Failed(val error: String) : MigrationResult()
}

/**
 * Statistics about migrated data.
 */
data class MigrationStats(
    var notesMigrated: Int = 0,
    var favoritesMigrated: Int = 0,
    var offlineVideosMigrated: Int = 0
) {
    val totalMigrated: Int
        get() = notesMigrated + favoritesMigrated + offlineVideosMigrated

    override fun toString(): String =
        "Notes: $notesMigrated, Favorites: $favoritesMigrated, Offline Videos: $offlineVideosMigrated (Total: $totalMigrated)"
}

