package com.vince.localmp3player.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.Settings
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
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
import androidx.compose.material.icons.rounded.Remove
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
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.vince.localmp3player.BlindTestPhase
import com.vince.localmp3player.MainUiState
import com.vince.localmp3player.MainViewModel
import com.vince.localmp3player.data.AppAccessMode
import com.vince.localmp3player.data.CategoryEntry
import com.vince.localmp3player.data.ColoringPage
import com.vince.localmp3player.data.DrawingMode
import com.vince.localmp3player.data.DrawingProject
import com.vince.localmp3player.data.DrawingStorage
import com.vince.localmp3player.data.LibraryAudioItem
import com.vince.localmp3player.data.LibrarySection
import com.vince.localmp3player.data.SavedDrawingPreview
import com.vince.localmp3player.data.StoredDrawingPoint
import com.vince.localmp3player.data.StoredDrawingStroke
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
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private enum class AppRoute(val route: String) {
    HOME("home"),
    MUSIC("music"),
    SOUND("sound"),
    ORCHESTRA("orchestra"),
    DRAWING("drawing"),
    PIANO("orchestra/piano"),
    DRUMS("orchestra/drums"),
    XYLOPHONE("orchestra/xylophone"),
    BLIND_TEST("blind_test"),
}

private const val pianoKeyCount = 8
private val pianoNoteLabels = listOf("Do", "Ré", "Mi", "Fa", "Sol", "La", "Si", "Do")
private val moonlightTutorialSequence = listOf(0, 0, 0, 1, 2, 1, 0, 2, 1, 1, 0, 0, 1, 2, 2, 1, 1, 0)
private val drawingColors = listOf(
    Color(0xFF1F1F1F),
    Color(0xFFE74C3C),
    Color(0xFFF39C12),
    Color(0xFFF1C40F),
    Color(0xFF2ECC71),
    Color(0xFF3498DB),
    Color(0xFF9B59B6),
    Color(0xFFFFFFFF),
)
private val LocalUiScale = compositionLocalOf { 1f }
private val LocalIsLandscape = compositionLocalOf { false }
private enum class DrumPadKind {
    KICK,
    SNARE,
    HAT,
    TOM,
    CLAP,
    RIDE,
}

private data class DrumPadSpec(
    val label: String,
    val color: Color,
    val kind: DrumPadKind,
)

private data class DrawingStroke(
    val color: Color,
    val width: Float,
    val isEraser: Boolean,
    val points: List<Offset>,
)

private val drumPadSpecs = listOf(
    DrumPadSpec("Grosse caisse", Color(0xFFFFD36E), DrumPadKind.KICK),
    DrumPadSpec("Caisse claire", Color(0xFFFFA15F), DrumPadKind.SNARE),
    DrumPadSpec("Charleston", Color(0xFFFF7F7F), DrumPadKind.HAT),
    DrumPadSpec("Tom", Color(0xFF8FD3FF), DrumPadKind.TOM),
    DrumPadSpec("Clap", Color(0xFFA1E887), DrumPadKind.CLAP),
    DrumPadSpec("Cymbale ride", Color(0xFFC6A2FF), DrumPadKind.RIDE),
)
private val xylophoneBarColors = listOf(
    Color(0xFFFF7D7D),
    Color(0xFFFFA85C),
    Color(0xFFFFD64D),
    Color(0xFF7DDB82),
    Color(0xFF59C7FF),
    Color(0xFF7E9CFF),
    Color(0xFF9B7BFF),
    Color(0xFFFF7ED2),
)

private enum class InstrumentKind {
    PIANO,
    DRUMS,
    XYLOPHONE,
}

private enum class ProtectedAction {
    PICK_ROOT_FOLDER,
    CHANGE_PARENT_PIN,
    TOGGLE_SOUND_RECORDING,
    DELETE_SELECTED_MUSIC_CATEGORY,
    DELETE_SELECTED_SOUND_CATEGORY,
    ENTER_ADULT_MODE,
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
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppRoute.HOME.route
    val snackbarHostState = remember { SnackbarHostState() }

    var protectedAction by remember { mutableStateOf<ProtectedAction?>(null) }
    var trackForActions by remember { mutableStateOf<LibraryAudioItem?>(null) }
    var trackForRename by remember { mutableStateOf<LibraryAudioItem?>(null) }
    var trackForMove by remember { mutableStateOf<LibraryAudioItem?>(null) }
    var createCategorySection by remember { mutableStateOf<LibrarySection?>(null) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showRecentSheet by remember { mutableStateOf(false) }
    var showFavoritesSheet by remember { mutableStateOf(false) }
    var showRequestSongDialog by remember { mutableStateOf(false) }
    var showRequestedSongsSheet by remember { mutableStateOf(false) }
    var showInterfaceScaleDialog by remember { mutableStateOf(false) }
    var showSectionVisibilitySheet by remember { mutableStateOf(false) }
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

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    val effectiveUiScale = (uiState.settings.interfaceScale * if (isLandscape) 0.84f else 1f)
        .coerceIn(0.72f, 1.15f)

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

                        ProtectedAction.ENTER_ADULT_MODE -> {
                            viewModel.setAccessMode(AppAccessMode.ADULT)
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
                "Nouvelle catégorie musique"
            } else {
                "Nouvelle catégorie sons"
            },
            initialValue = "",
            confirmLabel = "Créer",
            placeholder = "Nom de la catégorie",
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

    if (showRequestSongDialog) {
        TextEntryDialog(
            title = "Demander une musique",
            initialValue = "",
            confirmLabel = "Envoyer",
            placeholder = "Nom de la musique",
            onDismiss = { showRequestSongDialog = false },
            onConfirm = { requestedTitle ->
                viewModel.requestSong(requestedTitle)
                showRequestSongDialog = false
            },
        )
    }

    if (showInterfaceScaleDialog) {
        InterfaceScaleDialog(
            currentScale = uiState.settings.interfaceScale,
            onDismiss = { showInterfaceScaleDialog = false },
            onScaleChange = viewModel::setInterfaceScale,
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
                onEnqueueTrack = viewModel::enqueueTrack,
                onFavoriteClick = viewModel::toggleFavorite,
            )
        }
    }

    if (showRequestedSongsSheet) {
        ModalBottomSheet(onDismissRequest = { showRequestedSongsSheet = false }) {
            RequestedSongsSheet(
                requestedSongs = uiState.requestedSongTitles,
                onRemove = viewModel::removeRequestedSong,
            )
        }
    }

    if (showSectionVisibilitySheet) {
        ModalBottomSheet(onDismissRequest = { showSectionVisibilitySheet = false }) {
            SectionVisibilitySheet(
                uiState = uiState,
                onMusicVisibleChange = viewModel::setMusicSectionVisible,
                onBlindTestVisibleChange = viewModel::setBlindTestSectionVisible,
                onOrchestraVisibleChange = viewModel::setOrchestraSectionVisible,
                onDrawingVisibleChange = viewModel::setDrawingSectionVisible,
                onSoundboardVisibleChange = viewModel::setSoundboardSectionVisible,
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

    CompositionLocalProvider(
        LocalUiScale provides effectiveUiScale,
        LocalIsLandscape provides isLandscape,
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = HoneyBackground,
            bottomBar = {
                Column {
                    if (currentRoute != AppRoute.BLIND_TEST.route) {
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
                    }
                    SystemVolumeControls(
                        compact = currentRoute == AppRoute.BLIND_TEST.route,
                    )
                }
            },
        ) { innerPadding ->
            AppNavigator(
                navController = navController,
                currentRoute = currentRoute,
                uiState = uiState,
                recordingState = recordingState,
                contentPadding = innerPadding,
                onOpenMusic = { navController.navigate(AppRoute.MUSIC.route) },
                onOpenBlindTest = {
                    viewModel.startBlindTest()
                    navController.navigate(AppRoute.BLIND_TEST.route)
                },
                onOpenOrchestra = { navController.navigate(AppRoute.ORCHESTRA.route) },
                onOpenDrawing = { navController.navigate(AppRoute.DRAWING.route) },
                onOpenSoundboard = { navController.navigate(AppRoute.SOUND.route) },
                onOpenPiano = { navController.navigate(AppRoute.PIANO.route) },
                onOpenDrums = { navController.navigate(AppRoute.DRUMS.route) },
                onOpenXylophone = { navController.navigate(AppRoute.XYLOPHONE.route) },
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
                onRequestSong = { showRequestSongDialog = true },
                onOpenRequestedSongs = { showRequestedSongsSheet = true },
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
                onEnterAdultMode = {
                    if (uiState.settings.adultModeLocked) {
                        protectedAction = ProtectedAction.ENTER_ADULT_MODE
                    } else {
                        viewModel.setAccessMode(AppAccessMode.ADULT)
                    }
                },
                onSwitchToChildMode = { viewModel.setAccessMode(AppAccessMode.CHILD) },
                onSetAdultModeLocked = viewModel::setAdultModeLocked,
                onOpenSectionVisibility = { showSectionVisibilitySheet = true },
                onOpenInterfaceScale = { showInterfaceScaleDialog = true },
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
                onPlayPianoNote = viewModel::playPianoNote,
                onPlayDrumPad = viewModel::playDrumPad,
                onPlayXylophoneNote = viewModel::playXylophoneNote,
                onStartBlindTest = viewModel::startBlindTest,
                onSkipBlindTestPlayback = viewModel::skipBlindTestPlayback,
                onSubmitBlindTestAnswer = viewModel::submitBlindTestAnswer,
                onStopBlindTest = viewModel::stopBlindTest,
            )
        }
    }
}

@Composable
private fun AppNavigator(
    navController: NavHostController,
    currentRoute: String,
    uiState: MainUiState,
    recordingState: RecordingUiState,
    contentPadding: PaddingValues,
    onOpenMusic: () -> Unit,
    onOpenBlindTest: () -> Unit,
    onOpenOrchestra: () -> Unit,
    onOpenDrawing: () -> Unit,
    onOpenSoundboard: () -> Unit,
    onOpenPiano: () -> Unit,
    onOpenDrums: () -> Unit,
    onOpenXylophone: () -> Unit,
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
    onRequestSong: () -> Unit,
    onOpenRequestedSongs: () -> Unit,
    onPickRootFolder: () -> Unit,
    onCreateMusicCategory: () -> Unit,
    onDeleteMusicCategory: () -> Unit,
    onCreateSoundCategory: () -> Unit,
    onDeleteSoundCategory: () -> Unit,
    onChangePin: () -> Unit,
    onEnterAdultMode: () -> Unit,
    onSwitchToChildMode: () -> Unit,
    onSetAdultModeLocked: (Boolean) -> Unit,
    onOpenSectionVisibility: () -> Unit,
    onOpenInterfaceScale: () -> Unit,
    onSoundClick: (LibraryAudioItem) -> Unit,
    onSoundLongPress: (LibraryAudioItem) -> Unit,
    onToggleSoundRecording: (Boolean) -> Unit,
    onRecordClick: () -> Unit,
    onPlayPianoNote: (Int) -> Unit,
    onPlayDrumPad: (Int) -> Unit,
    onPlayXylophoneNote: (Int) -> Unit,
    onStartBlindTest: () -> Unit,
    onSkipBlindTestPlayback: () -> Unit,
    onSubmitBlindTestAnswer: (String) -> Unit,
    onStopBlindTest: () -> Unit,
) {
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
                onOpenBlindTest = onOpenBlindTest,
                onOpenOrchestra = onOpenOrchestra,
                onOpenDrawing = onOpenDrawing,
                onOpenSoundboard = onOpenSoundboard,
                onPickRootFolder = onPickRootFolder,
                onChangePin = onChangePin,
                onEnterAdultMode = onEnterAdultMode,
                onSwitchToChildMode = onSwitchToChildMode,
                onSetAdultModeLocked = onSetAdultModeLocked,
                onOpenSectionVisibility = onOpenSectionVisibility,
                onOpenInterfaceScale = onOpenInterfaceScale,
                onRequestSong = onRequestSong,
                onOpenRequestedSongs = onOpenRequestedSongs,
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
                onRequestSong = onRequestSong,
                onOpenRequestedSongs = onOpenRequestedSongs,
                onCreateCategory = onCreateMusicCategory,
                onDeleteCategory = onDeleteMusicCategory,
                isCurrentRoute = currentRoute == AppRoute.MUSIC.route,
                isAdultMode = uiState.isAdultMode,
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
                onRequestSong = onRequestSong,
                onOpenRequestedSongs = onOpenRequestedSongs,
                isAdultMode = uiState.isAdultMode,
            )
        }

        composable(AppRoute.DRAWING.route) {
            DrawingScreen(
                rootUri = uiState.settings.rootUri,
                onHome = onGoHome,
                onBack = { navController.popBackStack() },
            )
        }

        composable(AppRoute.ORCHESTRA.route) {
            OrchestraScreen(
                onHome = onGoHome,
                onOpenPiano = onOpenPiano,
                onOpenDrums = onOpenDrums,
                onOpenXylophone = onOpenXylophone,
            )
        }

        composable(AppRoute.PIANO.route) {
            PianoScreen(
                onBack = { navController.popBackStack() },
                onHome = onGoHome,
                onPlayNote = onPlayPianoNote,
            )
        }

        composable(AppRoute.DRUMS.route) {
            DrumScreen(
                onBack = { navController.popBackStack() },
                onHome = onGoHome,
                onPlayPad = onPlayDrumPad,
            )
        }

        composable(AppRoute.XYLOPHONE.route) {
            XylophoneScreen(
                onBack = { navController.popBackStack() },
                onHome = onGoHome,
                onPlayNote = onPlayXylophoneNote,
            )
        }

        composable(AppRoute.BLIND_TEST.route) {
            BlindTestScreen(
                uiState = uiState,
                onBack = {
                    onStopBlindTest()
                    onGoHome()
                },
                onReplay = onStartBlindTest,
                onSkipPlayback = onSkipBlindTestPlayback,
                onAnswer = onSubmitBlindTestAnswer,
                onQuit = {
                    onStopBlindTest()
                    onGoHome()
                },
            )
        }
    }
}

@Composable
private fun HomeScreen(
    uiState: MainUiState,
    onOpenMusic: () -> Unit,
    onOpenBlindTest: () -> Unit,
    onOpenOrchestra: () -> Unit,
    onOpenDrawing: () -> Unit,
    onOpenSoundboard: () -> Unit,
    onPickRootFolder: () -> Unit,
    onChangePin: () -> Unit,
    onEnterAdultMode: () -> Unit,
    onSwitchToChildMode: () -> Unit,
    onSetAdultModeLocked: (Boolean) -> Unit,
    onOpenSectionVisibility: () -> Unit,
    onOpenInterfaceScale: () -> Unit,
    onRequestSong: () -> Unit,
    onOpenRequestedSongs: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    ScreenContainer(
        brush = Brush.verticalGradient(listOf(Mint, HoneyBackground)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            HeaderCard(
                title = "Accueil",
                accent = MeadowGreen,
                accentDark = MeadowGreenDark,
                leading = { Spacer(modifier = Modifier.size(46.dp)) },
                trailing = {
                    Box {
                        IconBadgeButton(
                            icon = Icons.Rounded.ExpandMore,
                            background = CardCream,
                            onClick = { menuExpanded = true },
                        )
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            if (uiState.isAdultMode) {
                                DropdownMenuItem(
                                    text = { Text("Passer en mode enfant") },
                                    leadingIcon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onSwitchToChildMode()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Demander une musique") },
                                    leadingIcon = { Icon(Icons.Rounded.MusicNote, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onRequestSong()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Demandes de musiques") },
                                    leadingIcon = { Icon(Icons.Rounded.MenuBook, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onOpenRequestedSongs()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Verrouiller le mode adulte") },
                                    leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                                    trailingIcon = {
                                        Switch(
                                            checked = uiState.settings.adultModeLocked,
                                            onCheckedChange = { locked ->
                                                menuExpanded = false
                                                onSetAdultModeLocked(locked)
                                            },
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onSetAdultModeLocked(!uiState.settings.adultModeLocked)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Échelle de l'interface") },
                                    leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onOpenInterfaceScale()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Sections visibles") },
                                    leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onOpenSectionVisibility()
                                    },
                                )
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
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Demander une musique") },
                                    leadingIcon = { Icon(Icons.Rounded.MusicNote, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onRequestSong()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Passer en mode adulte") },
                                    leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onEnterAdultMode()
                                    },
                                )
                            }
                        }
                    }
                },
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (uiState.settings.rootUri.isNullOrBlank() && uiState.isAdultMode) {
                OutlinedButton(
                    onClick = onPickRootFolder,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choisir le dossier racine")
                }
                Spacer(modifier = Modifier.height(18.dp))
            } else if (uiState.settings.rootUri.isNullOrBlank()) {
                InfoBanner("Passe en mode adulte pour choisir le dossier racine.")
                Spacer(modifier = Modifier.height(18.dp))
            }

            if (uiState.settings.musicSectionVisible) {
                FeatureCard(
                    title = "Musiques",
                    subtitle = "Catégories et playlists",
                    icon = Icons.Rounded.MenuBook,
                    gradient = Brush.horizontalGradient(listOf(MeadowGreen, StoryBlue)),
                    onClick = onOpenMusic,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (uiState.settings.blindTestSectionVisible) {
                FeatureCard(
                    title = "Blind Test",
                    subtitle = "Écoute, devine et marque des points",
                    icon = Icons.Rounded.QueueMusic,
                    gradient = Brush.horizontalGradient(listOf(Color(0xFFFF9A3D), Color(0xFFE05A47))),
                    onClick = onOpenBlindTest,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (uiState.settings.orchestraSectionVisible) {
                FeatureCard(
                    title = "Orchestre",
                    subtitle = "Instruments tactiles et réactions instantanées",
                    icon = Icons.Rounded.MusicNote,
                    gradient = Brush.horizontalGradient(listOf(Color(0xFF8866D9), StoryBlueDark)),
                    onClick = onOpenOrchestra,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (uiState.settings.drawingSectionVisible) {
                FeatureCard(
                    title = "Dessin",
                    subtitle = "Couleurs, pinceaux et coloriage libre",
                    icon = Icons.Rounded.Edit,
                    gradient = Brush.horizontalGradient(listOf(Color(0xFF49B7A5), Color(0xFF1E8CA0))),
                    onClick = onOpenDrawing,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (uiState.settings.soundboardSectionVisible) {
                FeatureCard(
                    title = "Boîte à sons",
                    subtitle = "Petits boutons, bruitages et enregistrements",
                    icon = Icons.Rounded.Widgets,
                    gradient = Brush.horizontalGradient(listOf(SoundRed, SoundRedDark)),
                    onClick = onOpenSoundboard,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (!uiState.settings.musicSectionVisible &&
                !uiState.settings.blindTestSectionVisible &&
                !uiState.settings.orchestraSectionVisible &&
                !uiState.settings.drawingSectionVisible &&
                !uiState.settings.soundboardSectionVisible
            ) {
                InfoBanner("Aucune section n'est visible pour le moment.")
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OrchestraScreen(
    onHome: () -> Unit,
    onOpenPiano: () -> Unit,
    onOpenDrums: () -> Unit,
    onOpenXylophone: () -> Unit,
) {
    val isLandscape = LocalIsLandscape.current
    val scale = LocalUiScale.current
    ScreenContainer(
        brush = Brush.verticalGradient(listOf(Color(0xFFF5EEFF), HoneyBackground)),
    ) {
        HeaderCard(
            title = "Orchestre",
            accent = Color(0xFF7D63D2),
            accentDark = Color(0xFF5E46B7),
            leading = {
                IconBadgeButton(
                    icon = Icons.Rounded.Home,
                    background = CardCream,
                    onClick = onHome,
                )
            },
            trailing = { Spacer(modifier = Modifier.size(46.dp)) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Choisis un instrument",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = if (isLandscape) 116.dp * scale else 150.dp * scale),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f),
        ) {
            item {
                InstrumentTile(
                    title = "Piano",
                    subtitle = "",
                    kind = InstrumentKind.PIANO,
                    gradient = Brush.verticalGradient(listOf(Color(0xFFF2E5FF), Color(0xFFC9B1FF))),
                    enabled = true,
                    onClick = onOpenPiano,
                )
            }
            item {
                InstrumentTile(
                    title = "Batterie",
                    subtitle = "",
                    kind = InstrumentKind.DRUMS,
                    gradient = Brush.verticalGradient(listOf(Color(0xFFFFF0D7), Color(0xFFFFD38A))),
                    enabled = true,
                    onClick = onOpenDrums,
                )
            }
            item {
                InstrumentTile(
                    title = "Xylophone",
                    subtitle = "",
                    kind = InstrumentKind.XYLOPHONE,
                    gradient = Brush.verticalGradient(listOf(Color(0xFFE7FBFF), Color(0xFF9FE5F2))),
                    enabled = true,
                    onClick = onOpenXylophone,
                )
            }
        }
    }
}

@Composable
private fun PianoScreen(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onPlayNote: (Int) -> Unit,
) {
    var tutorialActive by remember { mutableStateOf(false) }
    var tutorialLength by remember { mutableStateOf(0) }
    var tutorialInputCount by remember { mutableStateOf(0) }
    var tutorialStatus by remember { mutableStateOf("Touchez ▶ pour commencer") }
    var tutorialWrong by remember { mutableStateOf(false) }

    fun startTutorial() {
        tutorialActive = true
        tutorialLength = 1
        tutorialInputCount = 0
        tutorialWrong = false
        tutorialStatus = "Joue la suite"
    }

    fun stopTutorial() {
        tutorialActive = false
        tutorialLength = 0
        tutorialInputCount = 0
        tutorialWrong = false
        tutorialStatus = "Tutoriel arrêté"
    }

    fun handleTutorialTap(noteIndex: Int) {
        onPlayNote(noteIndex)
        if (!tutorialActive || tutorialLength <= 0) return
        val expectedNote = moonlightTutorialSequence[tutorialInputCount]
        if (noteIndex == expectedNote) {
            tutorialWrong = false
            tutorialInputCount += 1
            tutorialStatus = "Continue"
            if (tutorialInputCount >= tutorialLength) {
                if (tutorialLength >= moonlightTutorialSequence.size) {
                    tutorialStatus = "Bravo !"
                    tutorialActive = false
                    tutorialLength = 0
                    tutorialInputCount = 0
                } else {
                    tutorialLength += 1
                    tutorialInputCount = 0
                    tutorialStatus = "Très bien, on ajoute une note"
                }
            }
        } else {
            tutorialWrong = true
            tutorialInputCount = 0
            tutorialStatus = "On recommence"
        }
    }

    ScreenContainer(
        brush = Brush.verticalGradient(listOf(Color(0xFFFCFBFF), Color(0xFFEAE2FF))),
    ) {
        HeaderCard(
            title = "Piano",
            accent = Color(0xFF7D63D2),
            accentDark = Color(0xFF5E46B7),
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
                IconBadgeButton(
                    icon = if (tutorialActive) Icons.Rounded.Close else Icons.Rounded.PlayCircleFilled,
                    background = if (tutorialActive) SoundRed else CardCream,
                    onClick = {
                        if (tutorialActive) stopTutorial() else startTutorial()
                    },
                )
            },
        )

        Spacer(modifier = Modifier.height(18.dp))

        TutorialSequenceCard(
            title = "Au clair de la lune",
            noteLabels = if (tutorialLength > 0) moonlightTutorialSequence.take(tutorialLength).map { pianoNoteLabels[it] } else emptyList(),
            completedCount = tutorialInputCount,
            showError = tutorialWrong,
            status = tutorialStatus,
            isActive = tutorialActive,
        )

        Spacer(modifier = Modifier.height(14.dp))

        PianoKeyboard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            onPlayNote = ::handleTutorialTap,
            highlightedKeys = if (tutorialActive && tutorialLength > 0 && tutorialInputCount < tutorialLength) {
                setOf(moonlightTutorialSequence[tutorialInputCount])
            } else {
                emptySet()
            },
        )
    }
}

@Composable
private fun DrumScreen(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onPlayPad: (Int) -> Unit,
) {
    val isLandscape = LocalIsLandscape.current
    val scale = LocalUiScale.current

    ScreenContainer(
        brush = Brush.verticalGradient(listOf(Color(0xFFFFF8EF), Color(0xFFFFE7C8))),
    ) {
        HeaderCard(
            title = "Batterie",
            accent = Color(0xFFF29B38),
            accentDark = Color(0xFFD77421),
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
            trailing = { Spacer(modifier = Modifier.size(46.dp)) },
        )

        Spacer(modifier = Modifier.height(18.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = if (isLandscape) 108.dp * scale else 136.dp * scale),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 18.dp),
        ) {
            items(drumPadSpecs.indices.toList()) { index ->
                val spec = drumPadSpecs[index]
                DrumPadButton(
                    label = spec.label,
                    color = spec.color,
                    kind = spec.kind,
                    highlighted = false,
                    onClick = { onPlayPad(index) },
                )
            }
        }
    }
}

@Composable
private fun XylophoneScreen(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onPlayNote: (Int) -> Unit,
) {
    var tutorialActive by remember { mutableStateOf(false) }
    var tutorialLength by remember { mutableStateOf(0) }
    var tutorialInputCount by remember { mutableStateOf(0) }
    var tutorialStatus by remember { mutableStateOf("Touchez ▶ pour commencer") }
    var tutorialWrong by remember { mutableStateOf(false) }

    fun startTutorial() {
        tutorialActive = true
        tutorialLength = 1
        tutorialInputCount = 0
        tutorialWrong = false
        tutorialStatus = "Joue la suite"
    }

    fun stopTutorial() {
        tutorialActive = false
        tutorialLength = 0
        tutorialInputCount = 0
        tutorialWrong = false
        tutorialStatus = "Tutoriel arrêté"
    }

    fun handleTutorialTap(noteIndex: Int) {
        onPlayNote(noteIndex)
        if (!tutorialActive || tutorialLength <= 0) return
        val expectedNote = moonlightTutorialSequence[tutorialInputCount]
        if (noteIndex == expectedNote) {
            tutorialWrong = false
            tutorialInputCount += 1
            tutorialStatus = "Continue"
            if (tutorialInputCount >= tutorialLength) {
                if (tutorialLength >= moonlightTutorialSequence.size) {
                    tutorialStatus = "Bravo !"
                    tutorialActive = false
                    tutorialLength = 0
                    tutorialInputCount = 0
                } else {
                    tutorialLength += 1
                    tutorialInputCount = 0
                    tutorialStatus = "Très bien, on ajoute une note"
                }
            }
        } else {
            tutorialWrong = true
            tutorialInputCount = 0
            tutorialStatus = "On recommence"
        }
    }

    ScreenContainer(
        brush = Brush.verticalGradient(listOf(Color(0xFFEFFAFF), Color(0xFFD7F4FF))),
    ) {
        HeaderCard(
            title = "Xylophone",
            accent = Color(0xFF37A9C7),
            accentDark = Color(0xFF1C7E9A),
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
                IconBadgeButton(
                    icon = if (tutorialActive) Icons.Rounded.Close else Icons.Rounded.PlayCircleFilled,
                    background = if (tutorialActive) SoundRed else CardCream,
                    onClick = {
                        if (tutorialActive) stopTutorial() else startTutorial()
                    },
                )
            },
        )

        Spacer(modifier = Modifier.height(18.dp))

        TutorialSequenceCard(
            title = "Au clair de la lune",
            noteLabels = if (tutorialLength > 0) moonlightTutorialSequence.take(tutorialLength).map { pianoNoteLabels[it] } else emptyList(),
            completedCount = tutorialInputCount,
            showError = tutorialWrong,
            status = tutorialStatus,
            isActive = tutorialActive,
        )

        Spacer(modifier = Modifier.height(14.dp))

        XylophoneBars(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            onPlayNote = ::handleTutorialTap,
            highlightedIndex = if (tutorialActive && tutorialLength > 0 && tutorialInputCount < tutorialLength) {
                moonlightTutorialSequence[tutorialInputCount]
            } else {
                null
            },
        )
    }
}

@Composable
private fun DrawingScreen(
    rootUri: String?,
    onHome: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val storage = remember(context) { DrawingStorage(context) }
    val strokes = remember { mutableStateListOf<DrawingStroke>() }
    var currentMode by rememberSaveable { mutableStateOf(DrawingMode.DRAWING.name) }
    var selectedColoringPage by remember { mutableStateOf<ColoringPage?>(null) }
    var selectedColor by remember { mutableStateOf(drawingColors.first()) }
    var brushSize by remember { mutableStateOf(18f) }
    var eraserEnabled by remember { mutableStateOf(false) }
    var savedProjects by remember { mutableStateOf<List<SavedDrawingPreview>>(emptyList()) }
    var coloringPages by remember { mutableStateOf<List<ColoringPage>>(emptyList()) }
    var saveStatus by remember { mutableStateOf<String?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    val scale = LocalUiScale.current
    val isLandscape = LocalIsLandscape.current

    fun currentProject(): DrawingProject {
        val mode = DrawingMode.valueOf(currentMode)
        return DrawingProject(
            mode = mode,
            backgroundImageUri = if (mode == DrawingMode.COLORING) selectedColoringPage?.imageUri else null,
            strokes = strokes.toStoredStrokes(),
            updatedAt = System.currentTimeMillis(),
        )
    }

    fun syncAutosave() {
        storage.saveAutosave(currentProject())
    }

    fun applyProject(project: DrawingProject) {
        currentMode = project.mode.name
        strokes.clear()
        strokes.addAll(project.strokes.toUiStrokes())
        selectedColoringPage = when {
            project.backgroundImageUri.isNullOrBlank() -> null
            else -> coloringPages.firstOrNull { it.imageUri == project.backgroundImageUri }
                ?: ColoringPage(
                    id = project.backgroundImageUri,
                    title = "Coloriage",
                    imageUri = project.backgroundImageUri,
                )
        }
    }

    fun updateLastStroke(newPoint: Offset) {
        val lastIndex = strokes.lastIndex
        if (lastIndex < 0) return
        val lastStroke = strokes[lastIndex]
        strokes[lastIndex] = lastStroke.copy(points = lastStroke.points + newPoint)
    }

    LaunchedEffect(rootUri) {
        coloringPages = storage.loadColoringPages(rootUri)
        savedProjects = storage.listSavedProjects()
        storage.loadAutosave()?.let(::applyProject)
    }

    if (showSaveDialog) {
        TextEntryDialog(
            title = "Enregistrer le dessin",
            initialValue = "",
            confirmLabel = "Enregistrer",
            placeholder = "Nom du dessin",
            onDismiss = { showSaveDialog = false },
            onConfirm = { drawingName ->
                val success = storage.saveProject(drawingName, currentProject())
                savedProjects = storage.listSavedProjects()
                saveStatus = if (success) "Dessin enregistré." else "L'enregistrement du dessin a échoué."
                showSaveDialog = false
            },
        )
    }

    ScreenContainer(
        brush = Brush.verticalGradient(listOf(Color(0xFFFFFBF2), Color(0xFFE5F6FF))),
    ) {
        HeaderCard(
            title = "Dessin",
            accent = Color(0xFF1E8CA0),
            accentDark = Color(0xFF11697A),
            leading = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconBadgeButton(
                        icon = Icons.Rounded.ArrowBack,
                        background = CardCream,
                        onClick = {
                            syncAutosave()
                            onBack()
                        },
                    )
                    IconBadgeButton(
                        icon = Icons.Rounded.Home,
                        background = CardCream,
                        onClick = {
                            syncAutosave()
                            onHome()
                        },
                    )
                }
            },
            trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconBadgeButton(
                        icon = if (eraserEnabled) Icons.Rounded.Remove else Icons.Rounded.Edit,
                        background = if (eraserEnabled) Butter else CardCream,
                        onClick = { eraserEnabled = !eraserEnabled },
                    )
                    IconBadgeButton(
                        icon = Icons.Rounded.Delete,
                        background = CardCream,
                        onClick = {
                            strokes.clear()
                            syncAutosave()
                        },
                    )
                }
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                FilterChip(
                    selected = currentMode == DrawingMode.DRAWING.name,
                    onClick = {
                        currentMode = DrawingMode.DRAWING.name
                        selectedColoringPage = null
                        syncAutosave()
                    },
                    label = { Text("Dessin") },
                )
            }
            item {
                FilterChip(
                    selected = currentMode == DrawingMode.COLORING.name,
                    onClick = {
                        currentMode = DrawingMode.COLORING.name
                        if (selectedColoringPage == null) {
                            selectedColoringPage = coloringPages.firstOrNull()
                        }
                        syncAutosave()
                    },
                    label = { Text("Coloriage") },
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        saveStatus?.let {
            InfoBanner(it)
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (currentMode == DrawingMode.DRAWING.name) {
            SavedDrawingsRow(
                projects = savedProjects,
                onSelect = { preview ->
                    storage.loadSavedProject(preview.id)?.let(::applyProject)
                    saveStatus = "Dessin chargé."
                },
            )
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            ColoringPagesRow(
                pages = coloringPages,
                selectedPageUri = selectedColoringPage?.imageUri,
                onSelect = { page ->
                    selectedColoringPage = page
                    syncAutosave()
                },
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = CardCream),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (currentMode == DrawingMode.COLORING.name && selectedColoringPage != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(selectedColoringPage?.imageUri).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color.White))
                }
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .pointerInput(selectedColor, brushSize, eraserEnabled, currentMode, selectedColoringPage?.imageUri) {
                            detectDragGestures(
                                onDragStart = { startOffset ->
                                    strokes.add(
                                        DrawingStroke(
                                            color = if (eraserEnabled) Color.White else selectedColor,
                                            width = brushSize,
                                            isEraser = eraserEnabled,
                                            points = listOf(startOffset),
                                        ),
                                    )
                                    syncAutosave()
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    updateLastStroke(change.position)
                                },
                                onDragEnd = { syncAutosave() },
                                onDragCancel = { syncAutosave() },
                            )
                        },
                ) {
                    strokes.forEach { stroke ->
                        if (stroke.points.size == 1) {
                            drawCircle(
                                color = stroke.color,
                                radius = stroke.width / 2f,
                                center = stroke.points.first(),
                                blendMode = if (stroke.isEraser) BlendMode.SrcOver else BlendMode.SrcOver,
                            )
                        } else {
                            val path = Path().apply {
                                moveTo(stroke.points.first().x, stroke.points.first().y)
                                stroke.points.drop(1).forEach { point ->
                                    lineTo(point.x, point.y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = stroke.color,
                                style = Stroke(width = stroke.width, cap = StrokeCap.Round),
                            )
                        }
                    }
                }
                if (currentMode == DrawingMode.COLORING.name && selectedColoringPage == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyStateCard(
                            icon = Icons.Rounded.MenuBook,
                            title = "Aucun coloriage",
                            body = "Ajoute des images dans le dossier \"Coloriage\" du dossier racine.",
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardCream.copy(alpha = 0.98f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp * scale),
                verticalArrangement = Arrangement.spacedBy(12.dp * scale),
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp * scale)) {
                    items(drawingColors) { color ->
                        Box(
                            modifier = Modifier
                                .size((if (isLandscape) 34.dp else 40.dp) * scale)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedColor == color) 3.dp * scale else 1.dp,
                                    color = if (selectedColor == color) Ink else CardStroke,
                                    shape = CircleShape,
                                )
                                .clickable { selectedColor = color },
                        )
                    }
                }
                Text(
                    text = "Taille du crayon : ${brushSize.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Slider(
                    value = brushSize,
                    onValueChange = { brushSize = it },
                    valueRange = 6f..42f,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            if (strokes.isNotEmpty()) {
                                strokes.removeLast()
                                syncAutosave()
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Annuler")
                    }
                    OutlinedButton(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Enregistrer")
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedDrawingsRow(
    projects: List<SavedDrawingPreview>,
    onSelect: (SavedDrawingPreview) -> Unit,
) {
    val scale = LocalUiScale.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp * scale)) {
        Text(
            text = "Mes dessins",
            style = MaterialTheme.typography.titleMedium,
        )
        if (projects.isEmpty()) {
            InfoBanner("Tes dessins enregistrés apparaîtront ici.")
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp * scale)) {
                items(projects, key = { it.id }) { preview ->
                    Card(
                        shape = RoundedCornerShape(22.dp * scale),
                        colors = CardDefaults.cardColors(containerColor = CardCream),
                        border = androidx.compose.foundation.BorderStroke(2.dp * scale, CardStroke),
                        modifier = Modifier
                            .width(130.dp * scale)
                            .clickable { onSelect(preview) },
                    ) {
                        Column(modifier = Modifier.padding(8.dp * scale)) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(preview.previewUri).build(),
                                contentDescription = preview.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(16.dp * scale)),
                                contentScale = ContentScale.Crop,
                            )
                            Spacer(modifier = Modifier.height(8.dp * scale))
                            Text(
                                text = preview.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColoringPagesRow(
    pages: List<ColoringPage>,
    selectedPageUri: String?,
    onSelect: (ColoringPage) -> Unit,
) {
    val scale = LocalUiScale.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp * scale)) {
        Text(
            text = "Coloriages",
            style = MaterialTheme.typography.titleMedium,
        )
        if (pages.isEmpty()) {
            InfoBanner("Ajoute des images en noir et blanc dans le dossier \"Coloriage\".")
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp * scale)) {
                items(pages, key = { it.id }) { page ->
                    val isSelected = page.imageUri == selectedPageUri
                    Card(
                        shape = RoundedCornerShape(22.dp * scale),
                        colors = CardDefaults.cardColors(containerColor = CardCream),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 2.dp * scale,
                            color = if (isSelected) StoryBlueDark else CardStroke,
                        ),
                        modifier = Modifier
                            .width(130.dp * scale)
                            .clickable { onSelect(page) },
                    ) {
                        Column(modifier = Modifier.padding(8.dp * scale)) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(page.imageUri).build(),
                                contentDescription = page.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(16.dp * scale)),
                                contentScale = ContentScale.Crop,
                            )
                            Spacer(modifier = Modifier.height(8.dp * scale))
                            Text(
                                text = page.title.ifBlank { "Coloriage" },
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TutorialSequenceCard(
    title: String,
    noteLabels: List<String>,
    completedCount: Int,
    showError: Boolean,
    status: String,
    isActive: Boolean,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardCream.copy(alpha = 0.98f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium)
                    Text(text = status, style = MaterialTheme.typography.bodyMedium, color = SoftInk)
                }
                Text(
                    text = if (isActive) "$completedCount/${noteLabels.size}" else "Arrêté",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (showError) SoundRed else Ink,
                )
            }
            if (noteLabels.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(noteLabels.indices.toList()) { index ->
                        val background = when {
                            showError && index == completedCount -> Color(0xFFFFD6D6)
                            index < completedCount -> Color(0xFFD8F6D8)
                            index == completedCount && isActive -> Color(0xFFFFF0B8)
                            else -> Color(0xFFEFE7FF)
                        }
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = background,
                        ) {
                            Text(
                                text = noteLabels[index],
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Appuie sur ▶ pour démarrer la suite.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftInk,
                )
            }
        }
    }
}

@Composable
private fun BlindTestScreen(
    uiState: MainUiState,
    onBack: () -> Unit,
    onReplay: () -> Unit,
    onSkipPlayback: () -> Unit,
    onAnswer: (String) -> Unit,
    onQuit: () -> Unit,
) {
    val blindTest = uiState.blindTest
    val currentTrack = remember(blindTest.currentTrackId, uiState.library.musicTracks) {
        uiState.library.musicTracks.firstOrNull { it.id == blindTest.currentTrackId }
    }
    var showQuitDialog by remember { mutableStateOf(false) }

    if (showQuitDialog) {
        AlertDialog(
            onDismissRequest = { showQuitDialog = false },
            title = { Text("Quitter le blind test ?") },
            text = { Text("La partie en cours sera arrêtée.") },
            confirmButton = {
                Button(
                    onClick = {
                        showQuitDialog = false
                        onQuit()
                    },
                ) {
                    Text("Quitter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuitDialog = false }) {
                    Text("Annuler")
                }
            },
        )
    }

    ScreenContainer(
        brush = Brush.verticalGradient(listOf(Color(0xFF101C2A), Color(0xFF1B2E44))),
    ) {
        HeaderCard(
            title = "Blind Test",
            accent = Color(0xFF4C8DFF),
            accentDark = Color(0xFF2B5FB3),
            leading = {
                IconBadgeButton(
                    icon = Icons.Rounded.Close,
                    background = CardCream,
                    onClick = { showQuitDialog = true },
                )
            },
            trailing = { Spacer(modifier = Modifier.size(46.dp)) },
        )

        Spacer(modifier = Modifier.height(18.dp))

        if (blindTest.phase != BlindTestPhase.FINISHED) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = CardCream.copy(alpha = 0.98f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = when (blindTest.phase) {
                            BlindTestPhase.NOT_ENOUGH_TRACKS -> "Impossible de démarrer"
                            else -> "Question ${blindTest.roundNumber.coerceAtLeast(1)}/${blindTest.totalRounds}"
                        },
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "Score : ${blindTest.score}/${blindTest.totalRounds}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = SoftInk,
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        when (blindTest.phase) {
            BlindTestPhase.NOT_ENOUGH_TRACKS -> {
                EmptyStateCard(
                    icon = Icons.Rounded.QueueMusic,
                    title = "Pas assez de musiques",
                    body = blindTest.feedbackText ?: "Il faut au moins 4 musiques pour lancer une partie.",
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Retour")
                }
            }

            BlindTestPhase.PLAYING -> {
                BlindTestHeroCard(
                    title = "Écoute attentivement",
                    subtitle = "",
                    emphasis = "${blindTest.secondsLeft}s",
                )
                Spacer(modifier = Modifier.height(18.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    IconBadgeButton(
                        icon = Icons.Rounded.SkipNext,
                        background = Color(0xFF3C6FFF),
                        tint = CardCream,
                        size = 74.dp,
                        iconSize = 38.dp,
                        onClick = onSkipPlayback,
                    )
                }
            }

            BlindTestPhase.ANSWERING,
            BlindTestPhase.REVEAL -> {
                BlindTestHeroCard(
                    title = if (blindTest.phase == BlindTestPhase.ANSWERING) "Quelle est la bonne musique ?" else (blindTest.feedbackText
                        ?: "Réponse"),
                    subtitle = if (blindTest.phase == BlindTestPhase.ANSWERING) {
                        ""
                    } else {
                        currentTrack?.title ?: "Réponse"
                    },
                    emphasis = if (blindTest.phase == BlindTestPhase.ANSWERING) "${blindTest.secondsLeft}s" else null,
                )

                Spacer(modifier = Modifier.height(18.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(blindTest.options, key = { it.id }) { option ->
                        BlindTestOptionCard(
                            item = option,
                            enabled = blindTest.phase == BlindTestPhase.ANSWERING && blindTest.selectedAnswerId == null,
                            isSelected = blindTest.selectedAnswerId == option.id,
                            isCorrect = blindTest.revealedCorrectTrackId == option.id,
                            isWrongSelection = blindTest.phase == BlindTestPhase.REVEAL &&
                                blindTest.selectedAnswerId == option.id &&
                                blindTest.revealedCorrectTrackId != option.id,
                            onClick = { onAnswer(option.id) },
                        )
                    }
                }
            }

            BlindTestPhase.FINISHED -> {
                FinalBlindTestCelebration(
                    score = blindTest.score,
                    totalRounds = blindTest.totalRounds,
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onReplay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Text("Rejouer", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Text("Retour", style = MaterialTheme.typography.titleLarge)
                }
            }

            BlindTestPhase.IDLE -> {
                LoadingCard(label = "Préparation de la partie...")
            }

            BlindTestPhase.COUNTDOWN -> Unit
        }
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
    onRequestSong: () -> Unit,
    onOpenRequestedSongs: () -> Unit,
    onCreateCategory: () -> Unit,
    onDeleteCategory: () -> Unit,
    isCurrentRoute: Boolean,
    isAdultMode: Boolean,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isLandscape = LocalIsLandscape.current
    val scale = LocalUiScale.current

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
                Box {
                    IconBadgeButton(
                        icon = Icons.Rounded.ExpandMore,
                        background = CardCream,
                        onClick = { menuExpanded = true },
                    )
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Musiques récentes") },
                            leadingIcon = { Icon(Icons.Rounded.History, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onOpenRecent()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Musiques favorites") },
                            leadingIcon = { Icon(Icons.Rounded.Favorite, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onOpenFavorites()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Demander une musique") },
                            leadingIcon = { Icon(Icons.Rounded.MusicNote, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onRequestSong()
                            },
                        )
                        if (isAdultMode) {
                            DropdownMenuItem(
                                text = { Text("Demandes de musiques") },
                                leadingIcon = { Icon(Icons.Rounded.MenuBook, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onOpenRequestedSongs()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Créer une catégorie") },
                                leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onCreateCategory()
                                },
                            )
                            if (selectedCategory != null) {
                                DropdownMenuItem(
                                    text = { Text("Supprimer cette catégorie") },
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
                LoadingCard(label = "Chargement de la bibliothèque...")
            }

            uiState.library.musicCategories.isEmpty() && uiState.library.musicTracks.isEmpty() -> {
                EmptyStateCard(
                    icon = Icons.Rounded.MenuBook,
                    title = "Aucune musique trouvée",
                    body = "Place des dossiers de catégories et des fichiers mp3/m4a dans ton dossier racine.",
                )
            }

            selectedCategory == null && uiState.musicSearch.isBlank() -> {
                Text(
                    text = "Catégories",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = if (isLandscape) 118.dp * scale else 150.dp * scale),
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
                    text = if (selectedCategory == null) "Résultats" else "Musiques",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (visibleTracks.isEmpty()) {
                    EmptyStateCard(
                        icon = Icons.Rounded.Search,
                        title = if (selectedCategory != null && uiState.musicSearch.isBlank()) {
                            "Catégorie vide"
                        } else {
                            "Aucun résultat"
                        },
                        body = if (selectedCategory != null && uiState.musicSearch.isBlank()) {
                            "Cette catégorie est vide pour le moment."
                        } else {
                            "Essaie un autre mot-clé ou retourne à la liste des catégories."
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
                                    onLongPress = if (isAdultMode) {
                                        { onTrackLongPress(track) }
                                    } else {
                                        { }
                                    },
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
    onRequestSong: () -> Unit,
    onOpenRequestedSongs: () -> Unit,
    isAdultMode: Boolean,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isLandscape = LocalIsLandscape.current
    val scale = LocalUiScale.current
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
            title = selectedCategory?.name ?: "Boîte à sons",
            accent = StoryBlue,
            accentDark = StoryBlueDark,
            leading = {
                IconBadgeButton(
                    icon = Icons.Rounded.Home,
                    background = CardCream,
                    onClick = onHome,
                )
            },
            trailing = {
                Box {
                    IconBadgeButton(
                        icon = Icons.Rounded.ExpandMore,
                        background = CardCream,
                        onClick = { menuExpanded = true },
                    )
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Demander une musique") },
                            leadingIcon = { Icon(Icons.Rounded.MusicNote, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onRequestSong()
                            },
                        )
                        if (isAdultMode) {
                            DropdownMenuItem(
                                text = { Text("Demandes de musiques") },
                                leadingIcon = { Icon(Icons.Rounded.MenuBook, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onOpenRequestedSongs()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Créer une catégorie") },
                                leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onCreateCategory()
                                },
                            )
                            if (selectedCategory != null) {
                                DropdownMenuItem(
                                    text = { Text("Supprimer cette catégorie") },
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
                        } else {
                            DropdownMenuItem(
                                text = { Text("Mode enfant actif") },
                                leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                                onClick = { menuExpanded = false },
                            )
                        }
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
                title = if (uiState.library.soundPads.isEmpty()) "Aucun son disponible" else "Aucun résultat",
                body = if (uiState.library.soundPads.isEmpty()) {
                    "Ajoute des sons courts dans un dossier de la boîte à sons. Chaque fichier peut avoir une image du même nom."
                } else {
                    "Essaie un autre mot-clé ou une autre catégorie."
                },
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = if (isLandscape) 72.dp * scale else 84.dp * scale),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 18.dp),
            ) {
                items(visiblePads, key = { it.id }) { pad ->
                    SoundPad(
                        item = pad,
                        onClick = { onPlaySound(pad) },
                        onLongPress = if (isAdultMode) {
                            { onLongPressSound(pad) }
                        } else {
                            { }
                        },
                    )
                }
            }
        }

        if (isAdultMode && uiState.settings.soundboardRecordingEnabled) {
            Spacer(modifier = Modifier.height(18.dp))
            RecordingStatusBanner(recordingState = recordingState)
            Spacer(modifier = Modifier.height(10.dp))
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
    val scale = LocalUiScale.current
    val isLandscape = LocalIsLandscape.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush)
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            .padding(
                horizontal = (if (isLandscape) 12.dp else 18.dp) * scale,
                vertical = 10.dp * scale,
            ),
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
    val scale = LocalUiScale.current
    Surface(
        shape = RoundedCornerShape(26.dp * scale),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 10.dp * scale,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(26.dp * scale))
                .background(Brush.horizontalGradient(listOf(accentDark, accent)))
                .padding(horizontal = 16.dp * scale, vertical = 14.dp * scale),
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
                    .padding(horizontal = 16.dp * scale),
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
    size: Dp = 46.dp,
    iconSize: Dp = 24.dp,
) {
    Box(
        modifier = Modifier
            .size(size * LocalUiScale.current)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(iconSize * LocalUiScale.current))
    }
}

@Composable
private fun IconBadgeButton(
    icon: ImageVector,
    background: Color,
    tint: Color = Ink,
    size: Dp = 46.dp,
    iconSize: Dp = 24.dp,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size * LocalUiScale.current)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize * LocalUiScale.current),
        )
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
    val scale = LocalUiScale.current
    val isLandscape = LocalIsLandscape.current
    Card(
        shape = RoundedCornerShape(30.dp * scale),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp * scale),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(if (isLandscape) 16.dp * scale else 22.dp * scale),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size((if (isLandscape) 72.dp else 90.dp) * scale)
                    .clip(RoundedCornerShape(26.dp * scale))
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CardCream,
                    modifier = Modifier.size((if (isLandscape) 38.dp else 50.dp) * scale),
                )
            }
            Spacer(modifier = Modifier.width(18.dp * scale))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = CardCream,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(6.dp * scale))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = CardCream.copy(alpha = 0.92f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun InstrumentTile(
    title: String,
    subtitle: String,
    kind: InstrumentKind,
    gradient: Brush,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val scale = LocalUiScale.current
    Card(
        shape = RoundedCornerShape(30.dp * scale),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .background(gradient)
                .padding(16.dp * scale),
            verticalArrangement = Arrangement.spacedBy(12.dp * scale),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(26.dp * scale))
                    .background(Color.White.copy(alpha = if (enabled) 0.2f else 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                InstrumentIllustration(
                    kind = kind,
                    modifier = Modifier.fillMaxSize(0.78f),
                    alpha = if (enabled) 1f else 0.58f,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = if (enabled) Ink else Ink.copy(alpha = 0.7f),
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = SoftInk,
                )
            }
        }
    }
}

private fun List<DrawingStroke>.toStoredStrokes(): List<StoredDrawingStroke> {
    return map { stroke ->
        StoredDrawingStroke(
            colorArgb = stroke.color.toArgb(),
            width = stroke.width,
            isEraser = stroke.isEraser,
            points = stroke.points.map { point ->
                StoredDrawingPoint(
                    x = point.x,
                    y = point.y,
                )
            },
        )
    }
}

private fun List<StoredDrawingStroke>.toUiStrokes(): List<DrawingStroke> {
    return map { stroke ->
        DrawingStroke(
            color = Color(stroke.colorArgb),
            width = stroke.width,
            isEraser = stroke.isEraser,
            points = stroke.points.map { point ->
                Offset(
                    x = point.x,
                    y = point.y,
                )
            },
        )
    }
}

@Composable
private fun InstrumentIllustration(
    kind: InstrumentKind,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
) {
    Canvas(modifier = modifier) {
        when (kind) {
            InstrumentKind.PIANO -> {
                val outer = Color(0xFF382E48).copy(alpha = alpha)
                val keyShadow = Color(0xFFD6C7FF).copy(alpha = alpha)
                drawRoundRect(
                    color = outer,
                    cornerRadius = CornerRadius(24f, 24f),
                    size = Size(size.width, size.height * 0.82f),
                    topLeft = Offset(0f, size.height * 0.08f),
                )
                val whiteKeyWidth = size.width / 6.2f
                repeat(5) { index ->
                    drawRoundRect(
                        color = Color.White.copy(alpha = alpha),
                        topLeft = Offset(size.width * 0.08f + index * whiteKeyWidth, size.height * 0.16f),
                        size = Size(whiteKeyWidth * 0.86f, size.height * 0.56f),
                        cornerRadius = CornerRadius(14f, 14f),
                    )
                }
                repeat(3) { index ->
                    drawRoundRect(
                        color = keyShadow,
                        topLeft = Offset(size.width * (0.23f + index * 0.18f), size.height * 0.16f),
                        size = Size(size.width * 0.11f, size.height * 0.32f),
                        cornerRadius = CornerRadius(12f, 12f),
                    )
                }
            }

            InstrumentKind.DRUMS -> {
                drawCircle(
                    color = Color(0xFFFFD15C).copy(alpha = alpha),
                    radius = size.minDimension * 0.18f,
                    center = Offset(size.width * 0.28f, size.height * 0.62f),
                )
                drawCircle(
                    color = Color(0xFFFFA84E).copy(alpha = alpha),
                    radius = size.minDimension * 0.2f,
                    center = Offset(size.width * 0.68f, size.height * 0.58f),
                )
                drawCircle(
                    color = Color(0xFFFFF0B8).copy(alpha = alpha),
                    radius = size.minDimension * 0.14f,
                    center = Offset(size.width * 0.62f, size.height * 0.24f),
                )
                drawLine(
                    color = Color(0xFF6B4A1A).copy(alpha = alpha),
                    start = Offset(size.width * 0.36f, size.height * 0.26f),
                    end = Offset(size.width * 0.56f, size.height * 0.46f),
                    strokeWidth = size.minDimension * 0.045f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = Color(0xFF6B4A1A).copy(alpha = alpha),
                    start = Offset(size.width * 0.48f, size.height * 0.18f),
                    end = Offset(size.width * 0.23f, size.height * 0.46f),
                    strokeWidth = size.minDimension * 0.045f,
                    cap = StrokeCap.Round,
                )
            }

            InstrumentKind.XYLOPHONE -> {
                val colors = listOf(
                    Color(0xFFFF7D7D),
                    Color(0xFFFFB55C),
                    Color(0xFFFFD94D),
                    Color(0xFF7AD88B),
                    Color(0xFF59C8FF),
                    Color(0xFF8093FF),
                )
                val barWidth = size.width / 7.5f
                repeat(6) { index ->
                    drawRoundRect(
                        color = colors[index].copy(alpha = alpha),
                        topLeft = Offset(size.width * 0.08f + index * barWidth, size.height * (0.22f + index * 0.04f)),
                        size = Size(barWidth * 0.72f, size.height * (0.6f - index * 0.045f)),
                        cornerRadius = CornerRadius(16f, 16f),
                    )
                }
                drawLine(
                    color = Color(0xFF7C5A2C).copy(alpha = alpha),
                    start = Offset(size.width * 0.16f, size.height * 0.3f),
                    end = Offset(size.width * 0.84f, size.height * 0.7f),
                    strokeWidth = size.minDimension * 0.04f,
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    color = Color(0xFFFFE08A).copy(alpha = alpha),
                    radius = size.minDimension * 0.08f,
                    center = Offset(size.width * 0.78f, size.height * 0.24f),
                )
            }
        }
    }
}

@Composable
private fun BlindTestHeroCard(
    title: String,
    subtitle: String,
    emphasis: String?,
) {
    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = CardCream.copy(alpha = 0.98f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = SoftInk,
                    textAlign = TextAlign.Center,
                )
            }
            if (!emphasis.isNullOrBlank()) {
                Text(
                    text = emphasis,
                    style = MaterialTheme.typography.displayMedium,
                    color = StoryBlueDark,
                )
            }
        }
    }
}

@Composable
private fun FinalBlindTestCelebration(
    score: Int,
    totalRounds: Int,
) {
    val transition = rememberInfiniteTransition(label = "fireworks")
    val pulse by transition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1500)),
        label = "fireworksPulse",
    )

    Card(
        shape = RoundedCornerShape(34.dp),
        colors = CardDefaults.cardColors(containerColor = CardCream.copy(alpha = 0.98f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
        ) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .padding(8.dp),
            ) {
                val bursts = listOf(
                    Triple(Offset(size.width * 0.18f, size.height * 0.28f), Color(0xFFFFA84E), 1f),
                    Triple(Offset(size.width * 0.82f, size.height * 0.22f), Color(0xFF59C8FF), 0.82f),
                    Triple(Offset(size.width * 0.26f, size.height * 0.78f), Color(0xFF8FD876), 0.74f),
                    Triple(Offset(size.width * 0.74f, size.height * 0.72f), Color(0xFFFF7ED2), 0.92f),
                )
                bursts.forEach { (center, color, factor) ->
                    val radius = size.minDimension * 0.11f * pulse * factor
                    repeat(8) { spoke ->
                        val angle = (PI * 2.0 / 8.0) * spoke
                        val dx = (cos(angle) * radius).toFloat()
                        val dy = (sin(angle) * radius).toFloat()
                        drawLine(
                            color = color.copy(alpha = 0.7f),
                            start = center,
                            end = Offset(center.x + dx, center.y + dy),
                            strokeWidth = size.minDimension * 0.012f,
                            cap = StrokeCap.Round,
                        )
                    }
                    drawCircle(
                        color = color.copy(alpha = 0.22f),
                        radius = radius * 0.55f,
                        center = center,
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Bravo !",
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Ton score final",
                    style = MaterialTheme.typography.titleLarge,
                    color = SoftInk,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "$score/$totalRounds",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color(0xFF2B5FB3),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun BlindTestOptionCard(
    item: LibraryAudioItem,
    enabled: Boolean,
    isSelected: Boolean,
    isCorrect: Boolean,
    isWrongSelection: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = when {
        isCorrect -> Color(0xFFDDF6DD)
        isWrongSelection -> Color(0xFFFFE0E0)
        isSelected -> Color(0xFFEAF2FF)
        else -> CardCream
    }
    val borderColor = when {
        isCorrect -> MeadowGreen
        isWrongSelection -> SoundRed
        isSelected -> StoryBlue
        else -> CardStroke
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ArtworkBox(
                imageUri = item.imageUri,
                fallbackIcon = Icons.Rounded.MusicNote,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SystemVolumeControls(
    compact: Boolean,
) {
    val context = LocalContext.current
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val scale = LocalUiScale.current
    val maxVolume = remember(audioManager) {
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    }
    var currentVolume by remember {
        mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    }

    DisposableEffect(audioManager, context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            }
        }
        context.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
        onDispose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }

    fun setSystemVolume(volume: Float) {
        val target = (volume * maxVolume).toInt().coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }

    Surface(
        shadowElevation = if (compact) 6.dp * scale else 12.dp * scale,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF1B2C67), Color(0xFF5D44C5), Color(0xFF2F9ECF)),
                    ),
                )
                .padding(horizontal = 16.dp * scale, vertical = (if (compact) 10.dp else 12.dp) * scale),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp * scale),
        ) {
            Icon(
                imageVector = Icons.Rounded.VolumeUp,
                contentDescription = null,
                tint = CardCream,
            )
            Slider(
                value = currentVolume / maxVolume.toFloat(),
                onValueChange = ::setSystemVolume,
                valueRange = 0f..1f,
                steps = (maxVolume - 1).coerceAtLeast(0),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFFFFD55A),
                    inactiveTrackColor = Color.White.copy(alpha = 0.28f),
                ),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${(currentVolume * 100 / maxVolume)}%",
                color = CardCream,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun DrumPadButton(
    label: String,
    color: Color,
    kind: DrumPadKind,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    val glowColor by animateColorAsState(
        targetValue = if (highlighted) Color.White.copy(alpha = 0.34f) else Color.Transparent,
        label = "drumPadGlow",
    )
    Card(
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .border(width = 3.dp, color = glowColor, shape = RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                        radius = 420f,
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DrumPadIllustration(
                    kind = kind,
                    modifier = Modifier.size(60.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DrumPadIllustration(
    kind: DrumPadKind,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        when (kind) {
            DrumPadKind.KICK -> {
                drawCircle(Color(0xFF6B4A1A), radius = size.minDimension * 0.42f)
                drawCircle(Color(0xFFFFF2C7), radius = size.minDimension * 0.28f)
                drawCircle(Color(0xFF6B4A1A), radius = size.minDimension * 0.08f)
            }

            DrumPadKind.SNARE -> {
                drawRoundRect(
                    color = Color(0xFF65707E),
                    topLeft = Offset(size.width * 0.14f, size.height * 0.3f),
                    size = Size(size.width * 0.72f, size.height * 0.34f),
                    cornerRadius = CornerRadius(18f, 18f),
                )
                drawLine(
                    color = Color.White,
                    start = Offset(size.width * 0.18f, size.height * 0.42f),
                    end = Offset(size.width * 0.82f, size.height * 0.42f),
                    strokeWidth = size.minDimension * 0.04f,
                )
                drawLine(
                    color = Color.White,
                    start = Offset(size.width * 0.18f, size.height * 0.52f),
                    end = Offset(size.width * 0.82f, size.height * 0.52f),
                    strokeWidth = size.minDimension * 0.04f,
                )
            }

            DrumPadKind.HAT -> {
                drawOval(
                    color = Color(0xFFFFE08A),
                    topLeft = Offset(size.width * 0.18f, size.height * 0.14f),
                    size = Size(size.width * 0.64f, size.height * 0.2f),
                )
                drawOval(
                    color = Color(0xFFFFC857),
                    topLeft = Offset(size.width * 0.12f, size.height * 0.42f),
                    size = Size(size.width * 0.76f, size.height * 0.22f),
                )
                drawLine(
                    color = Color(0xFF7C5A2C),
                    start = Offset(size.width * 0.5f, size.height * 0.24f),
                    end = Offset(size.width * 0.5f, size.height * 0.72f),
                    strokeWidth = size.minDimension * 0.05f,
                )
            }

            DrumPadKind.TOM -> {
                drawRoundRect(
                    color = Color(0xFF4E80C8),
                    topLeft = Offset(size.width * 0.14f, size.height * 0.2f),
                    size = Size(size.width * 0.72f, size.height * 0.5f),
                    cornerRadius = CornerRadius(24f, 24f),
                )
                drawOval(
                    color = Color(0xFFDDF4FF),
                    topLeft = Offset(size.width * 0.18f, size.height * 0.16f),
                    size = Size(size.width * 0.64f, size.height * 0.18f),
                )
            }

            DrumPadKind.CLAP -> {
                drawRoundRect(
                    color = Color(0xFF68B565),
                    topLeft = Offset(size.width * 0.18f, size.height * 0.34f),
                    size = Size(size.width * 0.25f, size.height * 0.3f),
                    cornerRadius = CornerRadius(18f, 18f),
                )
                drawRoundRect(
                    color = Color(0xFF85CC82),
                    topLeft = Offset(size.width * 0.46f, size.height * 0.24f),
                    size = Size(size.width * 0.25f, size.height * 0.3f),
                    cornerRadius = CornerRadius(18f, 18f),
                )
                repeat(3) { index ->
                    drawLine(
                        color = Color.White.copy(alpha = 0.75f),
                        start = Offset(size.width * (0.28f + index * 0.11f), size.height * 0.18f),
                        end = Offset(size.width * (0.22f + index * 0.11f), size.height * 0.06f),
                        strokeWidth = size.minDimension * 0.035f,
                        cap = StrokeCap.Round,
                    )
                }
            }

            DrumPadKind.RIDE -> {
                drawOval(
                    color = Color(0xFFD3B5FF),
                    topLeft = Offset(size.width * 0.12f, size.height * 0.22f),
                    size = Size(size.width * 0.76f, size.height * 0.26f),
                )
                drawLine(
                    color = Color(0xFF6D52A6),
                    start = Offset(size.width * 0.5f, size.height * 0.34f),
                    end = Offset(size.width * 0.5f, size.height * 0.76f),
                    strokeWidth = size.minDimension * 0.05f,
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.65f),
                    radius = size.minDimension * 0.06f,
                    center = Offset(size.width * 0.5f, size.height * 0.35f),
                )
            }
        }
    }
}

@Composable
private fun XylophoneBars(
    modifier: Modifier = Modifier,
    onPlayNote: (Int) -> Unit,
    highlightedIndex: Int? = null,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        xylophoneBarColors.forEachIndexed { index, color ->
            val heightFraction = 0.58f + (index * 0.05f)
            val highlighted = highlightedIndex == index
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (highlighted) Color.White.copy(alpha = 0.95f) else color,
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(heightFraction.coerceAtMost(0.96f))
                    .border(
                        width = if (highlighted) 3.dp else 0.dp,
                        color = if (highlighted) Ink.copy(alpha = 0.4f) else Color.Transparent,
                        shape = RoundedCornerShape(20.dp),
                    )
                    .clickable(onClick = { onPlayNote(index) }),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.24f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.08f),
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Text(
                        text = pianoNoteLabels[index],
                        modifier = Modifier.padding(top = 14.dp),
                        color = Ink.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PianoKeyboard(
    modifier: Modifier = Modifier,
    onPlayNote: (Int) -> Unit,
    highlightedKeys: Set<Int> = emptySet(),
) {
    var widthPx by remember { mutableStateOf(1) }
    var pressedKeys by remember { mutableStateOf(setOf<Int>()) }
    val pointerToKey = remember { mutableMapOf<Int, Int>() }

    fun keyIndexFor(x: Float): Int {
        val keyWidth = widthPx.toFloat() / pianoKeyCount.toFloat()
        return (x / keyWidth).toInt().coerceIn(0, pianoKeyCount - 1)
    }

    fun refreshPressedKeys() {
        pressedKeys = pointerToKey.values.toSet()
    }

    fun pressPointer(pointerId: Int, x: Float) {
        val nextKey = keyIndexFor(x)
        if (pointerToKey[pointerId] != nextKey) {
            pointerToKey[pointerId] = nextKey
            onPlayNote(nextKey)
            refreshPressedKeys()
        }
    }

    fun releasePointer(pointerId: Int) {
        if (pointerToKey.remove(pointerId) != null) {
            refreshPressedKeys()
        }
    }

    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .pointerInteropFilter { event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN,
                        MotionEvent.ACTION_POINTER_DOWN,
                        -> {
                            val pointerIndex = event.actionIndex
                            pressPointer(
                                pointerId = event.getPointerId(pointerIndex),
                                x = event.getX(pointerIndex),
                            )
                            true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            for (index in 0 until event.pointerCount) {
                                pressPointer(
                                    pointerId = event.getPointerId(index),
                                    x = event.getX(index),
                                )
                            }
                            true
                        }

                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_POINTER_UP,
                        -> {
                            releasePointer(event.getPointerId(event.actionIndex))
                            true
                        }

                        MotionEvent.ACTION_CANCEL -> {
                            pointerToKey.clear()
                            refreshPressedKeys()
                            true
                        }

                        else -> false
                    }
                }
                .onSizeChanged { widthPx = it.width },
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(pianoKeyCount) { index ->
                    val isPressed = pressedKeys.contains(index) || highlightedKeys.contains(index)
                    val keyColor by animateColorAsState(
                        targetValue = if (isPressed) Color(0xFFFFE08A) else Color(0xFFF8F5ED),
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "pianoKeyColor",
                    )
                    val keyElevation by animateDpAsState(
                        targetValue = if (isPressed) 4.dp else 12.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "pianoKeyElevation",
                    )

                    Surface(
                        shadowElevation = keyElevation,
                        shape = RoundedCornerShape(20.dp),
                        color = keyColor,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            keyColor,
                                            if (isPressed) Color(0xFFFFC857) else Color(0xFFE2DDD3),
                                        ),
                                    ),
                                ),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Text(
                                text = pianoNoteLabels[index],
                                color = Ink.copy(alpha = 0.75f),
                                modifier = Modifier.padding(bottom = 16.dp),
                            )
                        }
                    }
                }
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
                text = "Bibliothèque",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(10.dp))
            HomeStatLine("Catégories musique", uiState.library.musicCategories.size.toString())
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
    val scale = LocalUiScale.current
    Card(
        shape = RoundedCornerShape(28.dp * scale),
        colors = CardDefaults.cardColors(containerColor = CardCream),
        border = androidx.compose.foundation.BorderStroke(2.dp * scale, CardStroke),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(10.dp * scale)) {
            ArtworkBox(
                imageUri = category.coverUri,
                fallbackIcon = icon,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
            Spacer(modifier = Modifier.height(10.dp * scale))
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp * scale))
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
                        text = "Ajouter à la file",
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
    val scale = LocalUiScale.current
    Card(
        shape = RoundedCornerShape(26.dp * scale),
        colors = CardDefaults.cardColors(containerColor = CardCream),
        border = androidx.compose.foundation.BorderStroke(2.dp * scale, CardStroke),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongPress,
                )
                .padding(12.dp * scale),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArtworkBox(
                imageUri = item.imageUri,
                fallbackIcon = Icons.Rounded.MusicNote,
                modifier = Modifier.size(76.dp * scale),
            )
            Spacer(modifier = Modifier.width(14.dp * scale))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp * scale))
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
    val scale = LocalUiScale.current
    Card(
        shape = RoundedCornerShape(24.dp * scale),
        colors = CardDefaults.cardColors(containerColor = CardCream),
        border = androidx.compose.foundation.BorderStroke(2.dp * scale, CardStroke),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress,
            ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp * scale, vertical = 10.dp * scale),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp * scale),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp * scale),
                contentAlignment = Alignment.Center,
            ) {
                ArtworkBox(
                    imageUri = item.imageUri,
                    fallbackIcon = Icons.Rounded.VolumeUp,
                    modifier = Modifier
                        .fillMaxWidth(0.74f)
                        .aspectRatio(1f),
                )
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RecordButton(
    recordingState: RecordingUiState,
    onClick: () -> Unit,
) {
    val scale = LocalUiScale.current
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp * scale),
    ) {
        Icon(Icons.Rounded.Mic, contentDescription = null)
        Spacer(modifier = Modifier.width(10.dp * scale))
        Text(
            text = if (recordingState.isRecording) "Arrêter" else "Enregistrer",
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun RecordingStatusBanner(
    recordingState: RecordingUiState,
) {
    val scale = LocalUiScale.current
    Surface(
        shape = RoundedCornerShape(20.dp * scale),
        color = CardCream.copy(alpha = 0.92f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = if (recordingState.isRecording) {
                "Enregistrement en cours ${formatElapsed(recordingState.elapsedMs)}"
            } else {
                "Prêt à enregistrer"
            },
            modifier = Modifier.padding(horizontal = 16.dp * scale, vertical = 12.dp * scale),
            style = MaterialTheme.typography.bodyLarge,
            color = Ink,
            textAlign = TextAlign.Center,
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
                            "Choisis une musique dans la page Musique"
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
        Text(text = "Musiques récentes", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))
        if (tracks.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Rounded.History,
                title = "Aucun historique",
                body = "L'historique se remplira au fur et a mesure des écoutes.",
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
    onEnqueueTrack: (LibraryAudioItem) -> Unit,
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
                    TrackQueueSwipeRow(
                        onEnqueue = { onEnqueueTrack(track) },
                    ) {
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
}

@Composable
private fun RequestedSongsSheet(
    requestedSongs: List<String>,
    onRemove: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(text = "Demandes de musiques", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))
        if (requestedSongs.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Rounded.MusicNote,
                title = "Aucune demande",
                body = "Les demandes des enfants apparaîtront ici.",
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(requestedSongs, key = { it }) { requestedSong ->
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CardCream),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = requestedSong,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            IconBadgeButton(
                                icon = Icons.Rounded.Delete,
                                background = Color(0xFFFFE5E5),
                                size = 40.dp,
                                iconSize = 20.dp,
                                onClick = { onRemove(requestedSong) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionVisibilitySheet(
    uiState: MainUiState,
    onMusicVisibleChange: (Boolean) -> Unit,
    onBlindTestVisibleChange: (Boolean) -> Unit,
    onOrchestraVisibleChange: (Boolean) -> Unit,
    onDrawingVisibleChange: (Boolean) -> Unit,
    onSoundboardVisibleChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = "Sections visibles", style = MaterialTheme.typography.headlineMedium)
        SectionToggleRow("Musiques", uiState.settings.musicSectionVisible, onMusicVisibleChange)
        SectionToggleRow("Blind Test", uiState.settings.blindTestSectionVisible, onBlindTestVisibleChange)
        SectionToggleRow("Orchestre", uiState.settings.orchestraSectionVisible, onOrchestraVisibleChange)
        SectionToggleRow("Dessin", uiState.settings.drawingSectionVisible, onDrawingVisibleChange)
        SectionToggleRow("Boîte à sons", uiState.settings.soundboardSectionVisible, onSoundboardVisibleChange)
    }
}

@Composable
private fun SectionToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardCream),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun InterfaceScaleDialog(
    currentScale: Float,
    onDismiss: () -> Unit,
    onScaleChange: (Float) -> Unit,
) {
    var localScale by rememberSaveable { mutableStateOf(currentScale) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Échelle de l'interface") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Ajuste la taille générale de l'application.")
                Slider(
                    value = localScale,
                    onValueChange = { localScale = it },
                    valueRange = 0.75f..1.1f,
                )
                Text(
                    text = "${(localScale * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onScaleChange(localScale)
                    onDismiss()
                },
            ) {
                Text("Appliquer")
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
        ActionButton(label = "Déplacer", icon = Icons.Rounded.FolderOpen, onClick = onMove)
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
        title = { Text("Déplacer \"${track.title}\"") },
        text = {
            if (destinations.isEmpty()) {
                Text("Aucune autre catégorie disponible.")
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

private fun formatElapsed(elapsedMs: Int): String {
    val totalSeconds = elapsedMs.coerceAtLeast(0) / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
