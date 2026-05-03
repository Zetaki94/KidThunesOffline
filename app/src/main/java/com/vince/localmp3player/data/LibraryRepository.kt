package com.vince.localmp3player.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.Locale

class LibraryRepository(
    private val context: Context,
) {
    private val resolver: ContentResolver = context.contentResolver
    private val cacheStore = LibraryCacheStore(context)
    private val artworkGenerator = SoundboardArtworkGenerator(context)

    suspend fun persistRootPermission(uri: Uri, flags: Int) = withContext(Dispatchers.IO) {
        val persistableFlags = flags and
            (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        resolver.takePersistableUriPermission(uri, persistableFlags)
    }

    suspend fun loadCachedLibrary(rootUri: String?): LibrarySnapshot? = withContext(Dispatchers.IO) {
        if (rootUri.isNullOrBlank()) return@withContext null
        cacheStore.loadLatest(rootUri)
    }

    suspend fun scanLibrary(rootUri: String?): LibrarySnapshot = withContext(Dispatchers.IO) {
        if (rootUri.isNullOrBlank()) {
            return@withContext LibrarySnapshot(rootUri = null)
        }

        val root = DocumentFile.fromTreeUri(context, Uri.parse(rootUri))
            ?: return@withContext LibrarySnapshot(rootUri = rootUri)

        val fingerprint = computeFingerprint(root)
        cacheStore.load(rootUri, fingerprint)?.let { return@withContext it }

        val rootFolders = root.listFiles().filter { it.isDirectory }
        val musicContainer = rootFolders.findFolderByAliases(musicFolderAliases)
        val soundContainer = rootFolders.findFolderByAliases(soundFolderAliases)

        val musicFolders = buildList {
            musicContainer?.listFiles()
                ?.filter { it.isDirectory }
                ?.sortedByName()
                ?.let(::addAll)

            rootFolders
                .filterNot { folder ->
                    folder.sameDocumentAs(musicContainer) || folder.sameDocumentAs(soundContainer)
                }
                .sortedByName()
                .let(::addAll)
        }

        val musicScan = musicFolders.map { scanCategory(it, LibrarySection.MUSIC) }

        val soundScan = when {
            soundContainer == null -> emptyList()
            soundContainer.listFiles().any { it.isDirectory } -> {
                soundContainer.listFiles()
                    .filter { it.isDirectory }
                    .sortedByName()
                    .map { scanCategory(it, LibrarySection.SOUNDBOARD) }
            }

            else -> listOf(scanCategory(soundContainer, LibrarySection.SOUNDBOARD))
        }

        val snapshot = LibrarySnapshot(
            rootUri = rootUri,
            musicCategories = musicScan.map { it.category },
            musicTracks = musicScan.flatMap { it.items },
            soundCategories = soundScan.map { it.category },
            soundPads = soundScan.flatMap { it.items },
        )
        cacheStore.save(snapshot, fingerprint)
        snapshot
    }

    suspend fun createCategory(
        rootUri: String?,
        section: LibrarySection,
        rawName: String,
    ): OperationResult = withContext(Dispatchers.IO) {
        val safeName = sanitizeBaseName(rawName)
        if (safeName.isBlank()) {
            return@withContext OperationResult(false, "Le nom de catégorie est vide.")
        }

        val root = rootUri.toRootDocument() ?: return@withContext OperationResult(
            success = false,
            message = "Choisis d'abord un dossier racine.",
        )

        val parentFolder = when (section) {
            LibrarySection.MUSIC -> findMusicParent(root)
            LibrarySection.SOUNDBOARD -> findOrCreateSoundParent(root)
        } ?: return@withContext OperationResult(false, "Impossible d'accéder au dossier parent.")

        if (parentFolder.listFiles().any { it.isDirectory && it.name.equals(safeName, ignoreCase = true) }) {
            return@withContext OperationResult(false, "Cette catégorie existe déjà.")
        }

        val created = parentFolder.createDirectory(safeName)
        if (created == null) {
            OperationResult(false, "La création a échoué.")
        } else {
            OperationResult(true, "Catégorie \"$safeName\" créée.")
        }
    }

    suspend fun renameTrackPair(
        item: LibraryAudioItem,
        rawNewBaseName: String,
    ): OperationResult = withContext(Dispatchers.IO) {
        val newBaseName = sanitizeBaseName(rawNewBaseName)
        if (newBaseName.isBlank()) {
            return@withContext OperationResult(false, "Le nouveau nom est vide.")
        }

        if (newBaseName.equals(item.baseName, ignoreCase = true)) {
            return@withContext OperationResult(true, "Le nom est déjà à jour.", item.id)
        }

        val parentFolder = item.folderUri.toDocument() ?: return@withContext OperationResult(
            false,
            "Impossible d'accéder au dossier parent.",
        )

        val newAudioFileName = "$newBaseName.${item.audioExtension}"
        val newImageFileName = item.imageExtension?.let { "$newBaseName.$it" }

        val conflictingFile = parentFolder.listFiles().firstOrNull { document ->
            val documentName = document.name.orEmpty()
            val sameAudioName = documentName.equals(newAudioFileName, ignoreCase = true) &&
                document.uri.toString() != item.audioUri
            val sameImageName = !item.imageUri.isNullOrBlank() &&
                !newImageFileName.isNullOrBlank() &&
                documentName.equals(newImageFileName, ignoreCase = true) &&
                document.uri.toString() != item.imageUri
            sameAudioName || sameImageName
        }
        if (conflictingFile != null) {
            return@withContext OperationResult(false, "Un fichier porte déjà ce nom dans cette catégorie.")
        }

        val audioSourceDocument = item.audioUri.toSingleDocument() ?: return@withContext OperationResult(
            false,
            "Impossible d'accéder au fichier audio.",
        )
        val originalAudioName = audioSourceDocument.name ?: "${item.baseName}.${item.audioExtension}"
        val renamedAudioUri = renameDocumentUri(Uri.parse(item.audioUri), newAudioFileName)
            ?: return@withContext OperationResult(
                false,
                "Le renommage du fichier audio a échoué. Vérifie les droits d'accès au dossier.",
            )

        if (!item.imageUri.isNullOrBlank() && !item.imageExtension.isNullOrBlank() && !newImageFileName.isNullOrBlank()) {
            val imageUri = Uri.parse(item.imageUri)
            val renamedImageUri = renameDocumentUri(imageUri, newImageFileName)
            if (renamedImageUri == null) {
                val audioRolledBack = renameDocumentUri(renamedAudioUri, originalAudioName) != null
                return@withContext OperationResult(
                    false,
                    if (audioRolledBack) {
                        "Le renommage de l'image associée a échoué. Le fichier audio a été restauré."
                    } else {
                        "Le renommage de l'image associée a échoué et l'audio n'a pas pu être restauré automatiquement."
                    },
                )
            }
        }

        OperationResult(
            success = true,
            message = "\"${item.title}\" a été renommé.",
            updatedTrackId = renamedAudioUri.toString(),
        )
    }

    suspend fun moveTrackPair(
        item: LibraryAudioItem,
        destinationFolderUri: String,
    ): OperationResult = withContext(Dispatchers.IO) {
        if (item.folderUri == destinationFolderUri) {
            return@withContext OperationResult(false, "La musique est déjà dans cette catégorie.")
        }

        val destinationUri = Uri.parse(destinationFolderUri)
        val audioTargetUri = createDocument(
            parentDocumentUri = destinationUri,
            mimeType = guessAudioMime(item.audioExtension),
            displayName = "${item.baseName}.${item.audioExtension}",
        ) ?: return@withContext OperationResult(false, "Impossible de créer le fichier cible.")

        val audioCopied = copyUri(Uri.parse(item.audioUri), audioTargetUri)
        if (!audioCopied) {
            DocumentFile.fromSingleUri(context, audioTargetUri)?.delete()
            return@withContext OperationResult(false, "La copie du fichier audio a échoué.")
        }

        var movedImageTargetUri: Uri? = null
        if (!item.imageUri.isNullOrBlank() && !item.imageExtension.isNullOrBlank()) {
            val imageTargetUri = createDocument(
                parentDocumentUri = destinationUri,
                mimeType = guessImageMime(item.imageExtension),
                displayName = "${item.baseName}.${item.imageExtension}",
            )

            if (imageTargetUri != null && copyUri(Uri.parse(item.imageUri), imageTargetUri)) {
                movedImageTargetUri = imageTargetUri
            } else {
                DocumentFile.fromSingleUri(context, audioTargetUri)?.delete()
                imageTargetUri?.let { DocumentFile.fromSingleUri(context, it)?.delete() }
                return@withContext OperationResult(false, "La copie de la cover a échoué.")
            }
        }

        item.audioUri.toSingleDocument()?.delete()
        if (movedImageTargetUri != null) {
            item.imageUri?.toSingleDocument()?.delete()
        }

        OperationResult(
            success = true,
            message = "\"${item.title}\" a été déplacé.",
            updatedTrackId = audioTargetUri.toString(),
        )
    }

    suspend fun deleteItemPair(
        item: LibraryAudioItem,
    ): OperationResult = withContext(Dispatchers.IO) {
        val audioDeletion = deleteUriWithDiagnostics(
            uriString = item.audioUri,
            label = "fichier audio",
            tolerateMissing = false,
        )
        if (!audioDeletion.success) {
            return@withContext OperationResult(
                false,
                "La suppression de \"${item.title}\" a échoué : ${audioDeletion.message}",
            )
        }

        val imageDeletion = deleteUriWithDiagnostics(
            uriString = item.imageUri,
            label = "image associée",
            tolerateMissing = true,
        )

        val message = when {
            imageDeletion.success -> "\"${item.title}\" a été supprimé."
            else -> "\"${item.title}\" a été supprimé, mais pas l'image associée : ${imageDeletion.message}"
        }

        OperationResult(
            success = true,
            message = message,
        )
    }

    suspend fun deleteCategory(
        category: CategoryEntry,
    ): OperationResult = withContext(Dispatchers.IO) {
        val categoryFolder = category.folderUri.toDocument() ?: return@withContext OperationResult(
            false,
            "Impossible d'accéder à la catégorie.",
        )

        if (deleteRecursively(categoryFolder)) {
            OperationResult(true, "La catégorie \"${category.name}\" a été supprimée.")
        } else {
            OperationResult(false, "La suppression de la catégorie \"${category.name}\" a échoué.")
        }
    }

    suspend fun resolveRecordingFolderUri(
        rootUri: String?,
        preferredCategoryFolderUri: String?,
    ): String? = withContext(Dispatchers.IO) {
        if (!preferredCategoryFolderUri.isNullOrBlank()) {
            return@withContext preferredCategoryFolderUri
        }

        val root = rootUri.toRootDocument() ?: return@withContext null
        val soundParent = findOrCreateSoundParent(root) ?: return@withContext null
        val existing = soundParent.listFiles().firstOrNull {
            it.isDirectory && it.name.equals(defaultRecordingCategoryName, ignoreCase = true)
        }
        val recordingFolder = existing ?: soundParent.createDirectory(defaultRecordingCategoryName)
        recordingFolder?.uri?.toString()
    }

    private fun scanCategory(
        categoryFolder: DocumentFile,
        section: LibrarySection,
    ): ScannedCategory {
        val files = categoryFolder.listFiles().toList()
        val imagesByStem = files
            .filter(::isImageFile)
            .groupBy { it.name.stemKey() }

        val items = files
            .filter(::isAudioFile)
            .sortedByName()
            .map { audioFile ->
                val baseName = audioFile.name.fileStem()
                val imageFile = imagesByStem[baseName.stemKey()]?.firstOrNull()
                val resolvedImageUri = imageFile?.uri?.toString()
                    ?: if (section == LibrarySection.SOUNDBOARD) {
                        artworkGenerator.ensurePlaceholderFor(
                            itemId = audioFile.uri.toString(),
                            title = baseName.prettyLabel(),
                        )
                    } else {
                        null
                    }

                LibraryAudioItem(
                    id = audioFile.uri.toString(),
                    title = baseName.prettyLabel(),
                    baseName = baseName,
                    audioUri = audioFile.uri.toString(),
                    imageUri = resolvedImageUri,
                    categoryId = categoryFolder.uri.toString(),
                    categoryName = categoryFolder.name.orEmpty().prettyLabel(),
                    folderUri = categoryFolder.uri.toString(),
                    section = section,
                    audioExtension = audioFile.name.fileExtension(),
                    imageExtension = imageFile?.name?.fileExtension(),
                    durationMs = audioFile.uri.readDurationMs(),
                )
            }

        val explicitCoverUri = imagesByStem["cover"]?.firstOrNull()?.uri?.toString()
            ?: imagesByStem["folder"]?.firstOrNull()?.uri?.toString()
            ?: items.firstOrNull()?.imageUri

        return ScannedCategory(
            category = CategoryEntry(
                id = categoryFolder.uri.toString(),
                name = categoryFolder.name.orEmpty().prettyLabel(),
                coverUri = explicitCoverUri,
                itemCount = items.size,
                section = section,
                folderUri = categoryFolder.uri.toString(),
            ),
            items = items,
        )
    }

    private fun computeFingerprint(root: DocumentFile): String {
        val rootFolders = root.listFiles().filter { it.isDirectory }
        val musicContainer = rootFolders.findFolderByAliases(musicFolderAliases)
        val soundContainer = rootFolders.findFolderByAliases(soundFolderAliases)

        val signature = buildString {
            appendDocumentSignature(root, this)

            musicContainer?.let {
                appendDocumentSignature(it, this)
                it.listFiles()
                    .filter { child -> child.isDirectory }
                    .sortedByName()
                    .forEach { category -> appendCategorySignature(category, this) }
            }

            rootFolders
                .filterNot { folder -> folder.sameDocumentAs(musicContainer) || folder.sameDocumentAs(soundContainer) }
                .sortedByName()
                .forEach { category -> appendCategorySignature(category, this) }

            soundContainer?.let {
                appendDocumentSignature(it, this)
                val soundChildren = it.listFiles()
                if (soundChildren.any(DocumentFile::isDirectory)) {
                    soundChildren
                        .filter { child -> child.isDirectory }
                        .sortedByName()
                        .forEach { category -> appendCategorySignature(category, this) }
                } else {
                    appendCategorySignature(it, this)
                }
            }
        }

        return signature.sha256()
    }

    private fun appendCategorySignature(folder: DocumentFile, builder: StringBuilder) {
        appendDocumentSignature(folder, builder)
        folder.listFiles()
            .filter { child -> child.isDirectory || isAudioFile(child) || isImageFile(child) }
            .sortedByName()
            .forEach { child -> appendDocumentSignature(child, builder) }
    }

    private fun appendDocumentSignature(document: DocumentFile, builder: StringBuilder) {
        builder.append(document.uri)
            .append('|')
            .append(document.name.orEmpty())
            .append('|')
            .append(document.isDirectory)
            .append('|')
            .append(document.length())
            .append('|')
            .append(document.lastModified())
            .append('\n')
    }

    private fun findMusicParent(root: DocumentFile): DocumentFile {
        val folders = root.listFiles().filter { it.isDirectory }
        return folders.findFolderByAliases(musicFolderAliases) ?: root
    }

    private fun findOrCreateSoundParent(root: DocumentFile): DocumentFile? {
        val folders = root.listFiles().filter { it.isDirectory }
        return folders.findFolderByAliases(soundFolderAliases)
            ?: root.createDirectory(defaultSoundFolderName)
    }

    private fun isAudioFile(file: DocumentFile): Boolean {
        if (!file.isFile) return false
        return file.name.fileExtension() in audioExtensions
    }

    private fun isImageFile(file: DocumentFile): Boolean {
        if (!file.isFile) return false
        return file.name.fileExtension() in imageExtensions
    }

    private fun createDocument(
        parentDocumentUri: Uri,
        mimeType: String,
        displayName: String,
    ): Uri? {
        return runCatching {
            DocumentsContract.createDocument(resolver, parentDocumentUri, mimeType, displayName)
        }.getOrNull()
    }

    private fun copyUri(sourceUri: Uri, destinationUri: Uri): Boolean {
        val input = runCatching { resolver.openInputStream(sourceUri) }.getOrNull() ?: return false
        val output = runCatching { resolver.openOutputStream(destinationUri, "w") }.getOrNull()

        if (output == null) {
            input.close()
            return false
        }

        return input.useCopyTo(output)
    }

    private fun renameDocumentUri(sourceUri: Uri, displayName: String): Uri? {
        return runCatching {
            DocumentsContract.renameDocument(resolver, sourceUri, displayName)
        }.getOrNull()
    }

    private fun deleteUriWithDiagnostics(
        uriString: String?,
        label: String,
        tolerateMissing: Boolean,
    ): DeleteResult {
        if (uriString.isNullOrBlank()) return DeleteResult(success = true)

        val uri = Uri.parse(uriString)
        return when (uri.scheme?.lowercase(Locale.getDefault())) {
            "file" -> deleteFileUri(uri, label, tolerateMissing)
            else -> deleteDocumentUri(uri, label, tolerateMissing)
        }
    }

    private fun deleteFileUri(
        uri: Uri,
        label: String,
        tolerateMissing: Boolean,
    ): DeleteResult {
        val filePath = uri.path ?: return DeleteResult(
            success = false,
            message = "chemin introuvable pour le $label",
        )
        val file = File(filePath)
        if (!file.exists()) {
            return if (tolerateMissing) {
                DeleteResult(success = true)
            } else {
                DeleteResult(success = false, message = "le $label n'existe plus")
            }
        }
        if (!file.canWrite()) {
            return DeleteResult(success = false, message = "aucun droit d'écriture sur le $label")
        }
        return if (file.delete()) {
            DeleteResult(success = true)
        } else {
            DeleteResult(success = false, message = "le $label n'a pas pu être supprimé")
        }
    }

    private fun deleteDocumentUri(
        uri: Uri,
        label: String,
        tolerateMissing: Boolean,
    ): DeleteResult {
        val document = DocumentFile.fromSingleUri(context, uri)
            ?: DocumentFile.fromTreeUri(context, uri)
            ?: return DeleteResult(
                success = false,
                message = "accès impossible au chemin du $label",
            )

        if (!document.exists()) {
            return if (tolerateMissing) {
                DeleteResult(success = true)
            } else {
                DeleteResult(success = false, message = "le $label est introuvable")
            }
        }
        if (!document.canWrite()) {
            return DeleteResult(
                success = false,
                message = "l'application n'a pas l'autorisation de supprimer le $label",
            )
        }

        val deleted = runCatching { DocumentsContract.deleteDocument(resolver, uri) }.getOrNull()
            ?: document.delete()

        return if (deleted) {
            DeleteResult(success = true)
        } else {
            DeleteResult(success = false, message = "le $label n'a pas pu être supprimé")
        }
    }

    private fun InputStream.useCopyTo(output: OutputStream): Boolean {
        return use { input ->
            output.use { out ->
                input.copyTo(out)
                out.flush()
                true
            }
        }
    }

    private fun String?.toRootDocument(): DocumentFile? {
        if (this.isNullOrBlank()) return null
        return DocumentFile.fromTreeUri(context, Uri.parse(this))
    }

    private fun String?.toSingleDocument(): DocumentFile? {
        if (this.isNullOrBlank()) return null
        return DocumentFile.fromSingleUri(context, Uri.parse(this))
    }

    private fun String?.toDocument(): DocumentFile? {
        if (this.isNullOrBlank()) return null
        val uri = Uri.parse(this)
        return DocumentFile.fromTreeUri(context, uri) ?: DocumentFile.fromSingleUri(context, uri)
    }

    private fun Uri.readDurationMs(): Long? {
        return runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, this)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
            } finally {
                retriever.release()
            }
        }.getOrNull()
    }

    private fun String?.stemKey(): String {
        return fileStem().trim().lowercase(Locale.getDefault())
    }

    private fun String?.fileStem(): String {
        val raw = this.orEmpty()
        val dotIndex = raw.lastIndexOf('.')
        return if (dotIndex > 0) raw.substring(0, dotIndex) else raw
    }

    private fun String?.fileExtension(): String {
        val raw = this.orEmpty()
        val dotIndex = raw.lastIndexOf('.')
        if (dotIndex == -1 || dotIndex == raw.lastIndex) return ""
        return raw.substring(dotIndex + 1).lowercase(Locale.getDefault())
    }

    private fun String.prettyLabel(): String {
        return replace('_', ' ')
            .replace('-', ' ')
            .trim()
            .ifBlank { "Sans nom" }
    }

    private fun sanitizeBaseName(rawName: String): String {
        return rawName.trim()
            .replace(forbiddenCharactersRegex, "")
            .replace("\\s+".toRegex(), " ")
    }

    private fun List<DocumentFile>.findFolderByAliases(aliases: Set<String>): DocumentFile? {
        return firstOrNull { folder ->
            aliases.contains(folder.name.orEmpty().lowercase(Locale.getDefault()))
        }
    }

    private fun DocumentFile.sameDocumentAs(other: DocumentFile?): Boolean {
        return other != null && uri == other.uri
    }

    private fun deleteRecursively(document: DocumentFile): Boolean {
        val childrenDeleted = if (document.isDirectory) {
            document.listFiles().all(::deleteRecursively)
        } else {
            true
        }
        return childrenDeleted && document.delete()
    }

    private fun Iterable<DocumentFile>.sortedByName(): List<DocumentFile> {
        return sortedBy { it.name.orEmpty().lowercase(Locale.getDefault()) }
    }

    private fun String.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private data class ScannedCategory(
        val category: CategoryEntry,
        val items: List<LibraryAudioItem>,
    )

    private data class DeleteResult(
        val success: Boolean,
        val message: String? = null,
    )

    private companion object {
        val audioExtensions = setOf("mp3", "m4a", "aac", "wav", "ogg")
        val imageExtensions = setOf("jpg", "jpeg", "png", "webp")
        val musicFolderAliases = setOf("music", "musique")
        val soundFolderAliases = setOf(
            "boiteasons",
            "boite_a_sons",
            "boite a sons",
            "boite à sons",
            "soundboard",
            "sounds",
        )
        val forbiddenCharactersRegex = Regex("""[\\/:*?"<>|]""")
        const val defaultSoundFolderName = "BoiteASons"
        const val defaultRecordingCategoryName = "Enregistrements"

        fun guessAudioMime(extension: String): String {
            return when (extension.lowercase(Locale.getDefault())) {
                "m4a", "aac" -> "audio/mp4"
                "wav" -> "audio/wav"
                "ogg" -> "audio/ogg"
                else -> "audio/mpeg"
            }
        }

        fun guessImageMime(extension: String): String {
            return when (extension.lowercase(Locale.getDefault())) {
                "png" -> "image/png"
                "webp" -> "image/webp"
                else -> "image/jpeg"
            }
        }
    }
}
