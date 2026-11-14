package com.jmtube.app.ui.screens.player.stage

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.jmtube.app.R
import com.jmtube.app.data.model.Video
import com.jmtube.app.ui.components.shared.ShimmerGridItem
import com.jmtube.app.ui.components.shared.card.MediaVideoCard
import com.jmtube.app.ui.screens.player.state.PlayerScreenState
import com.jmtube.app.ui.screens.player.state.PlayerSheet
import com.jmtube.app.ui.theme.PlayerScrimContent
import com.jmtube.app.ui.theme.PlayerScrimContentSecondary
import com.jmtube.app.ui.theme.PlayerScrimPanel
import kotlinx.coroutines.launch

private const val MORE_VIDEOS_CARD_FRACTION = 0.41f
private const val MORE_VIDEOS_KEEP_FRACTION = 0.6f
private const val MORE_VIDEOS_CLOSE_FLING_PX_PER_S = 1_000f
private const val MORE_VIDEOS_PLACEHOLDERS = 3
private val MoreVideosCardSpacing = 16.dp
private val MoreVideosSideInset = 24.dp

@Composable
internal fun BoxScope.FullscreenMoreVideos(
    screenState: PlayerScreenState,
    videos: List<Video>,
    onVideoClick: (Video) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val reveal = remember { Animatable(0f) }
    var panelHeightPx by remember { mutableFloatStateOf(0f) }
    val isOpen = screenState.activeSheet == PlayerSheet.MoreVideos

    fun shownFraction(): Float = if (panelHeightPx > 0f) (reveal.value / panelHeightPx).coerceIn(0f, 1f) else 0f

    fun close() {
        scope.launch {
            reveal.animateTo(0f)
            if (screenState.activeSheet == PlayerSheet.MoreVideos) screenState.closeSheet()
        }
    }
    BackHandler(enabled = isOpen, onBack = ::close)

    LaunchedEffect(isOpen, panelHeightPx) {
        if (isOpen && panelHeightPx > 0f) reveal.animateTo(panelHeightPx)
    }

    Box(
        modifier =
            Modifier
                .matchParentSize()
                .zIndex(9f)
                .graphicsLayer { alpha = shownFraction() }
                .background(PlayerScrimPanel)
                .then(if (isOpen) Modifier.pointerInput(Unit) { detectTapGestures { close() } } else Modifier),
    )

    Column(
        modifier =
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .zIndex(9f)
                .onSizeChanged { panelHeightPx = it.height.toFloat() }
                .graphicsLayer { translationY = size.height - reveal.value.coerceAtMost(size.height) }
                .draggable(
                    state =
                        rememberDraggableState { delta ->
                            scope.launch { reveal.snapTo((reveal.value - delta).coerceIn(0f, panelHeightPx)) }
                        },
                    orientation = Orientation.Vertical,
                    enabled = isOpen,
                    onDragStopped = { velocity ->
                        if (velocity > MORE_VIDEOS_CLOSE_FLING_PX_PER_S || reveal.value < panelHeightPx * MORE_VIDEOS_KEEP_FRACTION) {
                            close()
                        } else {
                            reveal.animateTo(panelHeightPx)
                        }
                    },
                ).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .padding(start = MoreVideosSideInset, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "More videos",
                style = MaterialTheme.typography.titleLarge,
                color = PlayerScrimContent,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = ::close) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = PlayerScrimContent,
                )
            }
        }
        MaterialTheme(
            colorScheme =
                MaterialTheme.colorScheme.copy(
                    onSurface = PlayerScrimContent,
                    onSurfaceVariant = PlayerScrimContentSecondary,
                ),
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = MoreVideosSideInset),
                horizontalArrangement = Arrangement.spacedBy(MoreVideosCardSpacing),
                userScrollEnabled = isOpen,
            ) {
                if (videos.isEmpty()) {
                    items(MORE_VIDEOS_PLACEHOLDERS) {
                        ShimmerGridItem(
                            modifier = Modifier.fillParentMaxWidth(MORE_VIDEOS_CARD_FRACTION),
                            thumbnailAspectRatio = 16f / 9f,
                        )
                    }
                } else {
                    items(videos, key = { it.id }) { video ->
                        MediaVideoCard(
                            video = video,
                            onClick = { onVideoClick(video) },
                            useInternalPadding = false,
                            modifier = Modifier.fillParentMaxWidth(MORE_VIDEOS_CARD_FRACTION),
                        )
                    }
                }
            }
        }
    }
}

internal fun moreVideosFrom(relatedVideos: List<Video>): List<Video> = relatedVideos.filterNot { it.isShort }.distinctBy { it.id }
