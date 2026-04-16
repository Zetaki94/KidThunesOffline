package com.vince.localmp3player.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PauseCircleFilled
import androidx.compose.material.icons.rounded.PlayCircleFilled
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.vince.localmp3player.MainUiState
import com.vince.localmp3player.MainViewModel
import com.vince.localmp3player.data.CategoryEntry
import com.vince.localmp3player.data.LibraryAudioItem
import com.vince.localmp3player.data.LibrarySection
import com.vince.localmp3player.player.PlayerUiState
import com.vince.localmp3player.player.RecordingUiState
import com.vince.localmp3player.player.RepeatSetting
import com.vince.localmp3player.ui.theme.Butter
import com.vince.localmp3player.ui.theme.CardCream
import com.vince.localmp3player.ui.theme.CardStroke
import com.vince.localmp3player.ui.theme.HoneyBackground
import com.vince.localmp3player.ui.theme.Ink
import com.vince.localmp3player.ui.theme.MeadowGreen
import com.vince.localmp3player.ui.theme.MeadowGreenDark
import com.vince.localmp3player.ui.theme.Mint
import com.vince.localmp3player.ui.theme.SoftInk
import com.vince.localmp3player.ui.theme.SoundRed
import com.vince.localmp3player.ui.theme.SoundRedDark
import com.vince.localmp3player.ui.theme.StoryBlue
import com.vince.localmp3player.ui.theme.StoryBlueDark

private enum class AppRoute(val route: String) {
    HOME("home"),
    MUSIC("music"),
    SOUND("sound"),
}

private enum class ProtectedAction {
    PICK_ROOT_FOLDER,
    CHANGE_PARENT_PIN,
    TOGGLE_SOUND_RECORDING,
    DELETE_SELECTED_MUSIC_CATEGORY,
    DELETE_SELECTED_SOUND_CATEGORY,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KidTunesApp(
    viewModel: MainViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val recordingState by viewModel.recordingState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    var protectedAction by remember { mutableStateOf<ProtectedAction?>(null) }
    var trackForActions by remember { mutableStateOf<LibraryAudioItem?>(null) }
    var trackForRename by remember { mutableStateOf<LibraryAudioItem?>(null) }
    var trackForMove by remember { mutableStateOf<LibraryAudioItem?>(null) }
    var createCategorySection by remember { mutableStateOf<LibrarySection?>(null) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showRecentSheet by remember { mutableStateOf(false) }
    var showFavoritesSheet by remember { mutableStateOf(false) }
    var showPinChangeDialog by remember { mutableStateOf(false) }
    var soundPadForActions by remember { mutableStateOf<LibraryAudioItem?>(null) }
    var itemForDeletion by remember { mutableStateOf<LibraryAudioItem?>(null) }
    var pendingRecordingEnabled by remember { mutableStateOf<Boolean?>(null) }
    var wasRecording by remember { mutableStateOf(false) }

    val rootPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data?.data != null) {
            val flags = data.flags and
                (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            viewModel.onRootFolderPicked(data.data!!, flags)
        }
    }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.startRecording(uiState.selectedSoundCategoryId)
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(recordingState.isRecording) {
        if (wasRecording && !recordingState.isRecording) {
            viewModel.handleRecordingEnded()
        }
        wasRecording = recordingState.isRecording
    }

    val recentTracks = remember(uiState.recentIds, uiState.library.musicTracks) {
        uiState.recentIds.mapNotNull { recentId ->
            uiState.library.musicTracks.firstOrNull { it.id == recentId }
        }
    }

    val favoriteTracks = remember(uiState.favoriteIds, uiState.library.musicTracks) {
        uiState.library.musicTracks.filter { track -> uiState.favoriteIds.contains(track.id) }
    }

    val pendingRecordedSound = remember(uiState.pendingRecordedSoundId, uiState.library.soundPads) {
        uiState.pendingRecordedSoundId?.let { pendingId ->
            uiState.library.soundPads.firstOrNull { it.id == pendingId }
        }
    }

    if (protectedAction != null) {
        PinDialog(
            title = "Code parent",
            subtitle = "Entre le code pour modifier les options sensibles.",
            onDismiss = {
                if (protectedAction == ProtectedAction.TOGGLE_SOUND_RECORDING) {
                    pendingRecordingEnabled = null
                }
                protectedAction = null
            },
            onValidate = { enteredPin ->
                if (viewModel.verifyPin(enteredPin)) {
                    when (protectedAction) {
                        ProtectedAction.PICK_ROOT_FOLDER -> {
                            rootPickerLauncher.launch(
                                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                                    uiState.settings.rootUri?.let {
                                        putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse(it))
                                    }
                                },
                            )
                            protectedAction = null
                        }

                        ProtectedAction.CHANGE_PARENT_PIN -> {
                            showPinChangeDialog = true
                            protectedAction = null
                        }

                        ProtectedAction.TOGGLE_SOUND_RECORDING -> {
                            pendingRecordingEnabled?.let(viewModel::setSoundboardRecordingEnabled)
                            pendingRecordingEnabled = null
                            protectedAction = null
                        }

                        ProtectedAction.DELETE_SELECTED_MUSIC_CATEGORY -> {
                            uiState.library.musicCategories
                                .firstOrNull { it.id == uiState.selectedMusicCategoryId }
                                ?.let(viewModel::deleteCategory)
                            protectedAction = null
                        }

                        ProtectedAction.DELETE_SELECTED_SOUND_CATEGORY -> {
                            uiState.library.soundCategories
                                .firstOrNull { it.id == uiState.selectedSoundCategoryId }
                                ?.let(viewModel::deleteCategory)
                            protectedAction = null
                        }

                        null -> Unit
                    }
                    true
                } else {
                    false
                }
            },
        )
    }

    if (createCategorySection != null) {
        TextEntryDialog(
            title = if (createCategorySection == LibrarySection.MUSIC) {
                "Nouvelle categorie musique"
            } else {
                "Nouvelle categorie sons"
            },
            initialValue = "",
            confirmLabel = "Creer",
            placeholder = "Nom de la categorie",
            onDismiss = { createCategorySection = null },
            onConfirm = { name ->
                viewModel.createCategory(createCategorySection!!, name)
                createCategorySection = null
            },
        )
    }

    if (showPinChangeDialog) {
        TextEntryDialog(
            title = "Nouveau code parent",
            initialValue = "",
            confirmLabel = "Enregistrer",
            placeholder = "Ex: 2580",
            onDismiss = { showPinChangeDialog = false },
            onConfirm = { newPin ->
                viewModel.changePin(newPin)
                showPinChangeDialog = false
            },
        )
    }

    val renameTarget = trackForRename ?: pendingRecordedSound

    if (renameTarget != null) {
        TextEntryDialog(
            title = if (pendingRecordedSound?.id == renameTarget.id) "Nommer le nouveau son" else "Renommer",
            initialValue = renameTarget.baseName,
            confirmLabel = if (pendingRecordedSound?.id == renameTarget.id) "Enregistrer" else "Appliquer",
            placeholder = "Nouveau nom",
            onDismiss = {
                if (pendingRecordedSound?.id == renameTarget.id) {
                    viewModel.dismissPendingRecordedRename()
                } else {
                    trackForRename = null
                }
            },
            onConfirm = { newName ->
                viewModel.renameTrack(renameTarget, newName)
                if (pendingRecordedSound?.id == renameTarget.id) {
                    viewModel.dismissPendingRecordedRename()
                } else {
                    trackForRename = null
                }
            },
        )
    }

    if (trackForMove != null) {
        MoveTrackDialog(
            track = trackForMove!!,
            destinations = uiState.library.musicCategories.filter { it.id != trackForMove!!.categoryId },
            onDismiss = { trackForMove = null },
            onMove = { destination ->
                viewModel.moveTrack(trackForMove!!, destination.folderUri)
                trackForMove = null
            },
        )
    }

    if (showQueueSheet) {
        ModalBottomSheet(onDismissRequest = { showQueueSheet = false }) {
            QueueSheet(
                playerState = playerState,
                favorites = uiState.favoriteIds,
                onPlayIndex = {
                    viewModel.playQueueIndex(it)
                    showQueueSheet = false
                },
                onRemoveIndex = viewModel::removeTrackFromQueue,
                onFavoriteClick = viewModel::toggleFavorite,
            )
        }
    }

    if (showRecentSheet) {
        ModalBottomSheet(onDismissRequest = { showRecentSheet = false }) {
            RecentSheet(
                tracks = recentTracks,
                favorites = uiState.favoriteIds,
                onTrackClick = { track ->
                    viewModel.playTrack(track)
                    showRecentSheet = false
                },
                onFavoriteClick = viewModel::toggleFavorite,
            )
        }
    }

    if (showFavoritesSheet) {
        ModalBottomSheet(onDismissRequest = { showFavoritesSheet = false }) {
            FavoritesSheet(
                tracks = favoriteTracks,
                onTrackClick = { track ->
                    viewModel.playTrack(track)
                    showFavoritesSheet = false
                },
                onFavoriteClick = viewModel::toggleFavorite,
            )
        }
    }

    if (trackForActions != null) {
        ModalBottomSheet(onDismissRequest = { trackForActions = null }) {
            TrackActionSheet(
                item = trackForActions!!,
                onRename = {
                    trackForRename = trackForActions
                    trackForActions = null
                },
                onMove = {
                    trackForMove = trackForActions
                    trackForActions = null
                },
            )
        }
    }

    if (soundPadForActions != null) {
        ModalBottomSheet(onDismissRequest = { soundPadForActions = null }) {
            SoundPadActionSheet(
                item = soundPadForActions!!,
                onRename = {
                    trackForRename = soundPadForActions
                    soundPadForActions = null
                },
                onDelete = {
                    itemForDeletion = soundPadForActions
                    soundPadForActions = null
                },
            )
        }
    }

    if (itemForDeletion != null) {
        AlertDialog(
            onDismissRequest = { itemForDeletion = null },
            title = { Text("Supprimer") },
            text = { Text("Supprimer \"${itemForDeletion!!.title}\" ?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteItem(itemForDeletion!!)
                        itemForDeletion = null
                    },
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemForDeletion = null }) {
                    Text("Annuler")
                }
            },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = HoneyBackground,
        bottomBar = {
            PlayerBar(
                playerState = playerState,
                onSeek = viewModel::seekTo,
                onPlayPause = viewModel::togglePlayPause,
                onNext = viewModel::skipNext,
                onPrevious = viewModel::skipPrevious,
                onRepeat = viewModel::cycleRepeatMode,
                onShuffle = viewModel::toggleShuffle,
                onQueue = { showQueueSheet = true },
            )
        },
    ) { innerPadding ->
        AppNavigator(
            navController = navController,
            uiState = uiState,
            recordingState = recordingState,
            contentPadding = innerPadding,
            onOpenMusic = { navController.navigate(AppRoute.MUSIC.route) },
            onOpenSoundboard = { navController.navigate(AppRoute.SOUND.route) },
            onGoHome = {
                navController.navigate(AppRoute.HOME.route) {
                    popUpTo(AppRoute.HOME.route) { inclusive = false }
                    launchSingleTop = true
                }
            },
            onSelectMusicCategory = viewModel::selectMusicCategory,
            onSelectSoundCategory = viewModel::selectSoundCategory,
            onMusicSearchChange = viewModel::setMusicSearch,
            onSoundSearchChange = viewModel::setSoundSearch,
            onTrackClick = viewModel::playTrack,
            onEnqueueTrack = viewModel::enqueueTrack,
            onTrackLongPress = { trackForActions = it },
            onRecentClick = { showRecentSheet = true },
            onFavoritesClick = { showFavoritesSheet = true },
            onFavoriteClick = viewModel::toggleFavorite,
            onPickRootFolder = {
                if (uiState.settings.rootUri.isNullOrBlank()) {
                    rootPickerLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE))
                } else {
                    protectedAction = ProtectedAction.PICK_ROOT_FOLDER
                }
            },
            onCreateMusicCategory = { createCategorySection = LibrarySection.MUSIC },
            onDeleteMusicCategory = { protectedAction = ProtectedAction.DELETE_SELECTED_MUSIC_CATEGORY },
            onCreateSoundCategory = { createCategorySection = LibrarySection.SOUNDBOARD },
            onDeleteSoundCategory = { protectedAction = ProtectedAction.DELETE_SELECTED_SOUND_CATEGORY },
            onChangePin = { protectedAction = ProtectedAction.CHANGE_PARENT_PIN },
            onSoundClick = viewModel::playSoundEffect,
            onSoundLongPress = { soundPadForActions = it },
            onToggleSoundRecording = { enabled ->
                pendingRecordingEnabled = enabled
                protectedAction = ProtectedAction.TOGGLE_SOUND_RECORDING
            },
            onRecordClick = {
                if (recordingState.isRecording) {
                    viewModel.stopRecording()
                } else {
                    recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
        )
    }
}

@Composable
private fun AppNavigator(
    navController: NavHostController,
    uiState: MainUiState,
    recordingState: RecordingUiState,
    contentPadding: PaddingValues,
    onOpenMusic: () -> Unit,
    onOpenSoundboard: () -> Unit,
    onGoHome: () -> Unit,
    onSelectMusicCategory: (String?) -> Unit,
    onSelectSoundCategory: (String?) -> Unit,
    onMusicSearchChange: (String) -> Unit,
    onSoundSearchChange: (String) -> Unit,
    onTrackClick: (LibraryAudioItem) -> Unit,
    onEnqueueTrack: (LibraryAudioItem) -> Unit,
    onTrackLongPress: (LibraryAudioItem) -> Unit,
    onRecentClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onFavoriteClick: (LibraryAudioItem) -> Unit,
    onPickRootFolder: () -> Unit,
    onCreateMusicCategory: () -> Unit,
    onDeleteMusicCategory: () -> Unit,
    onCreateSoundCategory: () -> Unit,
    onDeleteSoundCategory: () -> Unit,
    onChangePin: () -> Unit,
    onSoundClick: (LibraryAudioItem) -> Unit,
    onSoundLongPress: (LibraryAudioItem) -> Unit,
    onToggleSoundRecording: (Boolean) -> Unit,
    onRecordClick: () -> Unit,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppRoute.HOME.route

    NavHost(
        navController = navController,
        startDestination = AppRoute.HOME.route,
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        composable(AppRoute.HOME.route) {
            HomeScreen(
                uiState = uiState,
                onOpenMusic = onOpenMusic,
                onOpenSoundboard = onOpenSoundboard,
                onPickRootFolder = onPickRootFolder,
                onChangePin = onChangePin,
            )
        }

        composable(AppRoute.MUSIC.route) {
            MusicScreen(
                uiState = uiState,
                onBack = {
                    if (uiState.selectedMusicCategoryId != null) onSelectMusicCategory(null) else onGoHome()
                },
                onHome = onGoHome,
                onSearchChange = onMusicSearchChange,
                onCategoryClick = onSelectMusicCategory,
                onTrackClick = onTrackClick,
                onEnqueueTrack = onEnqueueTrack,
                onTrackLongPress = onTrackLongPress,
                onFavoriteClick = onFavoriteClick,
                onOpenRecent = onRecentClick,
                onOpenFavorites = onFavoritesClick,
                onCreateCategory = onCreateMusicCategory,
                onDeleteCategory = onDeleteMusicCategory,
                isCurrentRoute = currentRoute == AppRoute.MUSIC.route,
            )
        }

        composable(AppRoute.SOUND.route) {
            SoundboardScreen(
                uiState = uiState,
                recordingState = recordingState,
                onBack = {
                    if (uiState.selectedSoundCategoryId != null) onSelectSoundCategory(null) else onGoHome()
                },
                onHome = onGoHome,
                onSearchChange = onSoundSearchChange,
                onCategoryClick = onSelectSoundCategory,
                onPlaySound = onSoundClick,
                onLongPressSound = onSoundLongPress,
                onCreateCategory = onCreateSoundCategory,
                onDeleteCategory = onDeleteSoundCategory,
                onToggleRecording = onToggleSoundRecording,
                onRecordClick = onRecordClick,
            )
        }
    }
}

@Composable
private fun HomeScreen(
    uiState: MainUiState,
    onOpenMusic: () -> Unit,
    onOpenSoundboard: () -> Unit,
    onPickRootFolder: () -> Unit,
    onChangePin: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    ScreenContainer(
        brush = Brush.verticalGradient(listOf(Mint, HoneyBackground)),
    ) {
        HeaderCard(
            title = "Accueil",
            accent = MeadowGreen,
            accentDark = MeadowGreenDark,
            leading = { Spacer(modifier = Modifier.size(46.dp)) },
            trailing = {
                Box {
                    IconBadgeButton(
                        icon = Icons.Rounded.Settings,
                        background = CardCream,
                        onClick = { menuExpanded = true },
                    )
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Changer le dossier racine") },
                            leadingIcon = { Icon(Icons.Rounded.FolderOpen, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onPickRootFolder()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Changer le code parent") },
                            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onChangePin()
                            },
                        )
                    }
                }
            },
        )

        Spacer(modifier = Modifier.height(18.dp))

        if (uiState.settings.rootUri.isNullOrBlank()) {
            OutlinedButton(
                onClick = onPickRootFolder,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
            ) {
                Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Choisir le dossier racine")
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        FeatureCard(
            title = "Musique",
            subtitle = "Categories, pochettes et playlists locales",
            icon = Icons.Rounded.MenuBook,
            gradient = Brush.horizontalGradient(listOf(MeadowGreen, StoryBlue)),
            onClick = onOpenMusic,
        )

        Spacer(modifier = Modifier.height(16.dp))

        FeatureCard(
            title = "Boite a sons",
            subtitle = "Petits boutons, bruitages et enregistrements courts",
            icon = Icons.Rounded.Widgets,
            gradient = Brush.horizontalGradient(listOf(SoundRed, SoundRedDark)),
            onClick = onOpenSoundboard,
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MusicScreen(
    uiState: MainUiState,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onSearchChange: (String) -> Unit,
    onCategoryClick: (String?) -> Unit,
    onTrackClick: (LibraryAudioItem) -> Unit,
    onEnqueueTrack: (LibraryAudioItem) -> Unit,
    onTrackLongPress: (LibraryAudioItem) -> Unit,
    onFavoriteClick: (LibraryAudioItem) -> Unit,
    onOpenRecent: () -> Unit,
    onOpenFavorites: () -> Unit,
    onCreateCategory: () -> Unit,
    onDeleteCategory: () -> Unit,
    isCurrentRoute: Boolean,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val selectedCategory = uiState.library.musicCategories.firstOrNull { it.id == uiState.selectedMusicCategoryId }
    val visibleTracks = remember(selectedCategory, uiState.musicSearch, uiState.library.musicTracks) {
        when {
            selectedCategory != null -> uiState.library.musicTracks.filter {
                it.categoryId == selectedCategory.id && it.matchesQuery(uiState.musicSearch)
            }

            uiState.musicSearch.isNotBlank() -> uiState.library.musicTracks.filter { it.matchesQuery(uiState.musicSearch) }
            else -> emptyList()
        }
    }

    val visibleCategories = remember(uiState.library.musicCategories, uiState.musicSearch) {
        if (uiState.musicSearch.isBlank()) {
            uiState.library.musicCategories
        } else {
            uiState.library.musicCategories.filter { category ->
                category.name.contains(uiState.musicSearch, ignoreCase = true)
            }
        }
    }

    ScreenContainer(
        brush = Brush.verticalGradient(listOf(Color(0xFFF3F6FF), HoneyBackground)),
    ) {
        HeaderCard(
            title = if (selectedCategory == null) "Musique" else selectedCategory.name,
            accent = StoryBlue,
            accentDark = StoryBlueDark,
            leading = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconBadgeButton(
                        icon = Icons.Rounded.ArrowBack,
                        background = CardCream,
                        onClick = onBack,
                    )
                    IconBadgeButton(
                        icon = Icons.Rounded.Home,
                        background = CardCream,
                        onClick = onHome,
                    )
                }
            },
            trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconBadgeButton(
                        icon = Icons.Rounded.History,
                        background = CardCream,
                        onClick = onOpenRecent,
                    )
                    IconBadgeButton(
                        icon = Icons.Rounded.Favorite,
                        background = CardCream,
                        onClick = onOpenFavorites,
                    )
                    Box {
                        IconBadgeButton(
                            icon = Icons.Rounded.Settings,
                            background = CardCream,
                            onClick = { menuExpanded = true },
                        )
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Creer une categorie") },
                                leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onCreateCategory()
                                },
                            )
                            if (selectedCategory != null) {
                                DropdownMenuItem(
                                    text = { Text("Supprimer cette categorie") },
                                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onDeleteCategory()
                                    },
                                )
                            }
                        }
                    }
                }
            },
        )

        Spacer(modifier = Modifier.height(14.dp))

        SearchField(
            value = uiState.musicSearch,
            onValueChange = onSearchChange,
            placeholder = "Chercher une musique",
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {
            uiState.isLoading && isCurrentRoute -> {
                LoadingCard(label = "Chargement de la bibliotheque...")
            }

            uiState.library.musicCategories.isEmpty() && uiState.library.musicTracks.isEmpty() -> {
                EmptyStateCard(
                    icon = Icons.Rounded.MenuBook,
                    title = "Aucune musique trouvee",
                    body = "Place des dossiers de categories et des fichiers mp3/m4a dans ton dossier racine.",
                )
            }

            selectedCategory == null && uiState.musicSearch.isBlank() -> {
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(visibleCategories, key = { it.id }) { category ->
                        CategoryTile(
                            category = category,
                            icon = Icons.Rounded.MenuBook,
                            onClick = { onCategoryClick(category.id) },
                        )
                    }
                }
            }

            else -> {
                Text(
                    text = if (selectedCategory == null) "Resultats" else "Musiques",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (visibleTracks.isEmpty()) {
                    EmptyStateCard(
                        icon = Icons.Rounded.Search,
                        title = if (selectedCategory != null && uiState.musicSearch.isBlank()) {
                            "Categorie vide"
                        } else {
                            "Aucun resultat"
                        },
                        body = if (selectedCategory != null && uiState.musicSearch.isBlank()) {
                            "Cette categorie est vide pour le moment."
                        } else {
                            "Essaie un autre mot-cle ou retourne a la liste des categories."
                        },
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        items(visibleTracks, key = { it.id }) { track ->
                            TrackQueueSwipeRow(
                                onEnqueue = { onEnqueueTrack(track) },
                            ) {
                                TrackRow(
                                    item = track,
                                    subtitle = if (selectedCategory == null) track.categoryName else formatDuration(track.durationMs),
                                    isFavorite = uiState.favoriteIds.contains(track.id),
                                    onClick = { onTrackClick(track) },
                                    onLongPress = { onTrackLongPress(track) },
                                    onFavoriteClick = { onFavoriteClick(track) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundboardScreen(
    uiState: MainUiState,
    recordingState: RecordingUiState,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onSearchChange: (String) -> Unit,
    onCategoryClick: (String?) -> Unit,
    onPlaySound: (LibraryAudioItem) -> Unit,
    onLongPressSound: (LibraryAudioItem) -> Unit,
    onCreateCategory: () -> Unit,
    onDeleteCategory: () -> Unit,
    onToggleRecording: (Boolean) -> Unit,
    onRecordClick: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val selectedCategory = uiState.library.soundCategories.firstOrNull { it.id == uiState.selectedSoundCategoryId }
    val visiblePads = remember(uiState.library.soundPads, uiState.soundSearch, uiState.selectedSoundCategoryId) {
        uiState.library.soundPads.filter { pad ->
            (uiState.selectedSoundCategoryId == null || pad.categoryId == uiState.selectedSoundCategoryId) &&
                pad.matchesQuery(uiState.soundSearch)
        }
    }

    ScreenContainer(
        brush = Brush.verticalGradient(listOf(Color(0xFFEFF5FF), HoneyBackground)),
    ) {
        HeaderCard(
            title = selectedCategory?.name ?: "Boite a sons",
            accent = StoryBlue,
            accentDark = StoryBlueDark,
            leading = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconBadgeButton(
                        icon = Icons.Rounded.ArrowBack,
                        background = CardCream,
                        onClick = onBack,
                    )
                    IconBadgeButton(
                        icon = Icons.Rounded.Home,
                        background = CardCream,
                        onClick = onHome,
                    )
                }
            },
            trailing = {
                Box {
                    IconBadgeButton(
                        icon = Icons.Rounded.Settings,
                        background = CardCream,
                        onClick = { menuExpanded = true },
                    )
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Creer une categorie") },
                            leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onCreateCategory()
                            },
                        )
                        if (selectedCategory != null) {
                            DropdownMenuItem(
                                text = { Text("Supprimer cette categorie") },
                                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteCategory()
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Autoriser l'enregistrement") },
                            leadingIcon = { Icon(Icons.Rounded.Mic, contentDescription = null) },
                            trailingIcon = {
                                Switch(
                                    checked = uiState.settings.soundboardRecordingEnabled,
                                    onCheckedChange = { enabled ->
                                        menuExpanded = false
                                        onToggleRecording(enabled)
                                    },
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onToggleRecording(!uiState.settings.soundboardRecordingEnabled)
                            },
                        )
                    }
                }
            },
        )

        Spacer(modifier = Modifier.height(14.dp))

        SearchField(
            value = uiState.soundSearch,
            onValueChange = onSearchChange,
            placeholder = "Chercher un son",
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (uiState.library.soundCategories.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = uiState.selectedSoundCategoryId == null,
                        onClick = { onCategoryClick(null) },
                        label = { Text("Tous") },
                    )
                }
                items(uiState.library.soundCategories, key = { it.id }) { category ->
                    FilterChip(
                        selected = uiState.selectedSoundCategoryId == category.id,
                        onClick = { onCategoryClick(category.id) },
                        label = { Text(category.name) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (visiblePads.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Rounded.Widgets,
                title = if (uiState.library.soundPads.isEmpty()) "Aucun son disponible" else "Aucun resultat",
                body = if (uiState.library.soundPads.isEmpty()) {
                    "Ajoute des sons courts dans un dossier de la boite a sons. Chaque fichier peut avoir une image du meme nom."
                } else {
                    "Essaie un autre mot-cle ou une autre categorie."
                },
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f, fill = false),
            ) {
                items(visiblePads, key = { it.id }) { pad ->
                    SoundPad(
                        item = pad,
                        onClick = { onPlaySound(pad) },
                        onLongPress = { onLongPressSound(pad) },
                    )
                }
            }
        }

        if (uiState.settings.soundboardRecordingEnabled) {
            Spacer(modifier = Modifier.height(18.dp))
            RecordButton(
                recordingState = recordingState,
                onClick = onRecordClick,
            )
        }
    }
}

@Composable
private fun ScreenContainer(
    brush: Brush,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        content = content,
    )
}

@Composable
private fun HeaderCard(
    title: String,
    accent: Color,
    accentDark: Color,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.horizontalGradient(listOf(accentDark, accent)))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            leading()
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = CardCream,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            trailing()
        }
    }
}

@Composable
private fun IconBadge(
    icon: ImageVector,
    background: Color,
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Ink)
    }
}

@Composable
private fun IconBadgeButton(
    icon: ImageVector,
    background: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Ink)
    }
}

@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: Brush,
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .background(gradient)
                .padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CardCream,
                    modifier = Modifier.size(50.dp),
                )
            }
            Spacer(modifier = Modifier.width(18.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = CardCream,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = CardCream.copy(alpha = 0.92f),
                )
            }
        }
    }
}

@Composable
private fun HomeStatsCard(
    uiState: MainUiState,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = CardCream),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Bibliotheque",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(10.dp))
            HomeStatLine("Categories musique", uiState.library.musicCategories.size.toString())
            HomeStatLine("Musiques", uiState.library.musicTracks.size.toString())
            HomeStatLine("Sons courts", uiState.library.soundPads.size.toString())
        }
    }
}

@Composable
private fun HomeStatLine(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = SoftInk)
        Text(text = value, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun InfoBanner(
    text: String,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Butter.copy(alpha = 0.92f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(22.dp),
    )
}

@Composable
private fun LoadingCard(
    label: String,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = CardCream),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator()
            Text(text = label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    body: String,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = CardCream),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, tint = StoryBlue, modifier = Modifier.size(42.dp))
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Text(text = body, style = MaterialTheme.typography.bodyLarge, color = SoftInk)
        }
    }
}
@Composable
private fun CategoryTile(
    category: CategoryEntry,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = CardCream),
        border = androidx.compose.foundation.BorderStroke(2.dp, CardStroke),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            ArtworkBox(
                imageUri = category.coverUri,
                fallbackIcon = icon,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "${category.itemCount} pistes", color = SoftInk)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackQueueSwipeRow(
    onEnqueue: () -> Unit,
    content: @Composable () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                onEnqueue()
            }
            false
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            val active = dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(26.dp))
                    .background(if (active) MeadowGreen else MeadowGreen.copy(alpha = 0.35f))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QueueMusic,
                        contentDescription = null,
                        tint = CardCream,
                    )
                    Text(
                        text = "Ajouter a la file",
                        color = CardCream,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        },
        content = { content() },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackRow(
    item: LibraryAudioItem,
    subtitle: String,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onFavoriteClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = CardCream),
        border = androidx.compose.foundation.BorderStroke(2.dp, CardStroke),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongPress,
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArtworkBox(
                imageUri = item.imageUri,
                fallbackIcon = Icons.Rounded.MusicNote,
                modifier = Modifier.size(76.dp),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = SoftInk,
                )
            }
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) SoundRed else SoftInk,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SoundPad(
    item: LibraryAudioItem,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardCream),
        border = androidx.compose.foundation.BorderStroke(2.dp, CardStroke),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress,
            ),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ArtworkBox(
                imageUri = item.imageUri,
                fallbackIcon = Icons.Rounded.VolumeUp,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RecordButton(
    recordingState: RecordingUiState,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
    ) {
        Icon(Icons.Rounded.Mic, contentDescription = null)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = if (recordingState.isRecording) "Stop" else "Enregistrer",
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun PlayerBar(
    playerState: PlayerUiState,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onRepeat: () -> Unit,
    onShuffle: () -> Unit,
    onQueue: () -> Unit,
) {
    Surface(
        shadowElevation = 18.dp,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1F3A2C), Color(0xFF18271F))))
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            val currentItem = playerState.currentItem
            Row(verticalAlignment = Alignment.CenterVertically) {
                ArtworkBox(
                    imageUri = currentItem?.imageUri,
                    fallbackIcon = Icons.Rounded.MusicNote,
                    modifier = Modifier.size(68.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentItem?.title ?: "Aucune musique",
                        style = MaterialTheme.typography.titleLarge,
                        color = CardCream,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (currentItem == null) {
                            "Choisis une piste dans la page Musique"
                        } else {
                            "${formatDuration(playerState.positionMs)} / ${formatDuration(playerState.durationMs)}"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = CardCream.copy(alpha = 0.8f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = playerState.positionMs.toFloat().coerceAtMost(playerState.durationMs.toFloat().coerceAtLeast(1f)),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..playerState.durationMs.toFloat().coerceAtLeast(1f),
                enabled = currentItem != null,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SmallControlButton(
                    icon = Icons.Rounded.Shuffle,
                    tint = if (playerState.shuffleEnabled) Butter else CardCream,
                    onClick = onShuffle,
                )
                SmallControlButton(
                    icon = Icons.Rounded.SkipPrevious,
                    tint = CardCream,
                    onClick = onPrevious,
                )
                IconBadgeButton(
                    icon = if (playerState.isPlaying) Icons.Rounded.PauseCircleFilled else Icons.Rounded.PlayCircleFilled,
                    background = Butter,
                    onClick = onPlayPause,
                )
                SmallControlButton(
                    icon = Icons.Rounded.SkipNext,
                    tint = CardCream,
                    onClick = onNext,
                )
                SmallControlButton(
                    icon = when (playerState.repeatSetting) {
                        RepeatSetting.ONE -> Icons.Rounded.RepeatOne
                        RepeatSetting.ALL, RepeatSetting.OFF -> Icons.Rounded.Repeat
                    },
                    tint = if (playerState.repeatSetting == RepeatSetting.OFF) CardCream else Butter,
                    onClick = onRepeat,
                )
                SmallControlButton(
                    icon = Icons.Rounded.QueueMusic,
                    tint = CardCream,
                    onClick = onQueue,
                )
            }
        }
    }
}

@Composable
private fun SmallControlButton(
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun QueueSheet(
    playerState: PlayerUiState,
    favorites: Set<String>,
    onPlayIndex: (Int) -> Unit,
    onRemoveIndex: (Int) -> Unit,
    onFavoriteClick: (LibraryAudioItem) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(text = "File d'attente", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(playerState.queue.indices.toList()) { index ->
                val item = playerState.queue[index]
                QueueTrackRow(
                    item = item,
                    subtitle = if (index == playerState.currentIndex) "Lecture en cours" else item.categoryName,
                    isFavorite = favorites.contains(item.id),
                    onClick = { onPlayIndex(index) },
                    onFavoriteClick = { onFavoriteClick(item) },
                    onRemoveClick = { onRemoveIndex(index) },
                    canRemove = index != playerState.currentIndex,
                )
            }
        }
    }
}

@Composable
private fun RecentSheet(
    tracks: List<LibraryAudioItem>,
    favorites: Set<String>,
    onTrackClick: (LibraryAudioItem) -> Unit,
    onFavoriteClick: (LibraryAudioItem) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(text = "Musiques recentes", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))
        if (tracks.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Rounded.History,
                title = "Aucun historique",
                body = "L'historique se remplira au fur et a mesure des ecoutes.",
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(tracks, key = { it.id }) { track ->
                    TrackRow(
                        item = track,
                        subtitle = track.categoryName,
                        isFavorite = favorites.contains(track.id),
                        onClick = { onTrackClick(track) },
                        onLongPress = { },
                        onFavoriteClick = { onFavoriteClick(track) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoritesSheet(
    tracks: List<LibraryAudioItem>,
    onTrackClick: (LibraryAudioItem) -> Unit,
    onFavoriteClick: (LibraryAudioItem) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(text = "Musiques favorites", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))
        if (tracks.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Rounded.Favorite,
                title = "Aucun favori",
                body = "Ajoute des musiques en favoris pour les retrouver plus vite.",
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(tracks, key = { it.id }) { track ->
                    TrackRow(
                        item = track,
                        subtitle = track.categoryName,
                        isFavorite = true,
                        onClick = { onTrackClick(track) },
                        onLongPress = { },
                        onFavoriteClick = { onFavoriteClick(track) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackActionSheet(
    item: LibraryAudioItem,
    onRename: () -> Unit,
    onMove: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = item.title, style = MaterialTheme.typography.headlineMedium)
        ActionButton(label = "Renommer", icon = Icons.Rounded.Edit, onClick = onRename)
        ActionButton(label = "Deplacer", icon = Icons.Rounded.FolderOpen, onClick = onMove)
    }
}

@Composable
private fun SoundPadActionSheet(
    item: LibraryAudioItem,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = item.title, style = MaterialTheme.typography.headlineMedium)
        ActionButton(label = "Modifier le nom", icon = Icons.Rounded.Edit, onClick = onRename)
        ActionButton(label = "Supprimer", icon = Icons.Rounded.Delete, onClick = onDelete)
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.width(10.dp))
        Text(label)
    }
}

@Composable
private fun QueueTrackRow(
    item: LibraryAudioItem,
    subtitle: String,
    isFavorite: Boolean,
    canRemove: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = CardCream),
        border = androidx.compose.foundation.BorderStroke(2.dp, CardStroke),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArtworkBox(
                imageUri = item.imageUri,
                fallbackIcon = Icons.Rounded.MusicNote,
                modifier = Modifier.size(76.dp),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = SoftInk,
                )
            }
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) SoundRed else SoftInk,
                )
            }
            if (canRemove) {
                IconButton(onClick = onRemoveClick) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Retirer de la file",
                        tint = SoftInk,
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
    }
}

@Composable
private fun MoveTrackDialog(
    track: LibraryAudioItem,
    destinations: List<CategoryEntry>,
    onDismiss: () -> Unit,
    onMove: (CategoryEntry) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        },
        title = { Text("Deplacer \"${track.title}\"") },
        text = {
            if (destinations.isEmpty()) {
                Text("Aucune autre categorie disponible.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    destinations.forEach { category ->
                        OutlinedButton(
                            onClick = { onMove(category) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(category.name)
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun PinDialog(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    onValidate: (String) -> Boolean,
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var showError by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(subtitle)
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it.filter(Char::isDigit)
                        showError = false
                    },
                    placeholder = { Text("1234") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                )
                if (showError) {
                    Text(
                        text = "Code incorrect.",
                        color = SoundRed,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    showError = !onValidate(pin)
                },
            ) {
                Text("Valider")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        },
    )
}

@Composable
private fun TextEntryDialog(
    title: String,
    initialValue: String,
    confirmLabel: String,
    placeholder: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                placeholder = { Text(placeholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(value) }) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        },
    )
}

@Composable
private fun ArtworkBox(
    imageUri: String?,
    fallbackIcon: ImageVector,
    modifier: Modifier,
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(Butter, Mint)))
            .border(2.dp, CardStroke, RoundedCornerShape(22.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUri.isNullOrBlank()) {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = null,
                tint = Ink.copy(alpha = 0.8f),
                modifier = Modifier.size(38.dp),
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUri)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun LibraryAudioItem.matchesQuery(query: String): Boolean {
    if (query.isBlank()) return true
    return title.contains(query, ignoreCase = true) || categoryName.contains(query, ignoreCase = true)
}

private fun formatDuration(durationMs: Long?): String {
    val safeDuration = durationMs ?: 0L
    val totalSeconds = safeDuration / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
