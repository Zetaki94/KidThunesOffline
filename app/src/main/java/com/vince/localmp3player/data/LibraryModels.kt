package com.vince.localmp3player.data

enum class LibrarySection {
    MUSIC,
    SOUNDBOARD,
}

data class CategoryEntry(
    val id: String,
    val name: String,
    val coverUri: String?,
    val itemCount: Int,
    val section: LibrarySection,
    val folderUri: String,
)

data class LibraryAudioItem(
    val id: String,
    val title: String,
    val baseName: String,
    val audioUri: String,
    val imageUri: String?,
    val categoryId: String,
    val categoryName: String,
    val folderUri: String,
    val section: LibrarySection,
    val audioExtension: String,
    val imageExtension: String?,
    val durationMs: Long?,
)

data class LibrarySnapshot(
    val rootUri: String?,
    val musicCategories: List<CategoryEntry> = emptyList(),
    val musicTracks: List<LibraryAudioItem> = emptyList(),
    val soundCategories: List<CategoryEntry> = emptyList(),
    val soundPads: List<LibraryAudioItem> = emptyList(),
) {
    val hasRoot: Boolean = !rootUri.isNullOrBlank()
    val isEmpty: Boolean = musicCategories.isEmpty() && musicTracks.isEmpty() && soundPads.isEmpty()
}

data class AppSettings(
    val rootUri: String? = null,
    val adminPin: String = DEFAULT_ADMIN_PIN,
    val favoriteTrackIds: Set<String> = emptySet(),
    val recentTrackIds: List<String> = emptyList(),
    val lastMusicCategoryId: String? = null,
    val lastSoundCategoryId: String? = null,
)

data class OperationResult(
    val success: Boolean,
    val message: String,
    val updatedTrackId: String? = null,
)

const val DEFAULT_ADMIN_PIN = "1234"
