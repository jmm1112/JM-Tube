package com.jmtube.app.ui.screens.search

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.jmtube.app.R
import com.jmtube.app.data.local.ContentType
import com.jmtube.app.data.local.HomeFeedColumns
import com.jmtube.app.data.local.SearchFilter
import com.jmtube.app.data.local.WatchState
import com.jmtube.app.data.model.Channel
import com.jmtube.app.data.model.Playlist
import com.jmtube.app.data.model.Video
import com.jmtube.app.data.paging.SearchResultItem
import com.jmtube.app.data.paging.supportsVideoFilters
import com.jmtube.app.data.shorts.queue.ShortsQueueSource
import com.jmtube.app.ui.components.FEED_MAX_AUTO_COLUMNS
import com.jmtube.app.ui.components.FeedCardMinWidth
import com.jmtube.app.ui.components.layout.navigation.LocalMediaNavigator
import com.jmtube.app.ui.components.rememberFeedGridLayout
import com.jmtube.app.ui.components.search.SearchFilterBar
import com.jmtube.app.ui.components.search.SearchResultActions
import com.jmtube.app.ui.components.search.SearchResults
import com.jmtube.app.ui.components.search.SearchShortsGrid
import com.jmtube.app.ui.components.shared.FeedGridSkeleton
import com.jmtube.app.ui.components.shared.FlowEmptyState
import com.jmtube.app.ui.components.shared.FlowErrorState
import com.jmtube.app.ui.components.shared.SubscriptionBellViewModel
import com.jmtube.app.ui.components.shared.quickactions.QuickActionsViewModel
import com.jmtube.app.ui.components.shared.sharedSubscriptionBellViewModel
import com.jmtube.app.utils.YouTubeLink
import com.jmtube.app.utils.parseYouTubeLink

private val FilterBarVerticalPadding = 4.dp

/**
 * Runs typed text as a search with the given filters, or opens it when it is a YouTube link, the way
 * every search field in the app treats a pasted link.
 */
@Composable
internal fun rememberSearchSubmitter(
    viewModel: SearchViewModel,
    state: SearchScreenState,
    quickActions: QuickActionsViewModel,
): (String, SearchFilter) -> Unit {
    val mediaNavigator = LocalMediaNavigator.current
    return remember(viewModel, state, quickActions, mediaNavigator) {
        { raw, filters ->
            val text = raw.trim()
            val link = parseYouTubeLink(text)
            val search: (String) -> Unit = { query ->
                state.onSubmit(query)
                viewModel.submit(query, filters)
            }
            when {
                text.isEmpty() -> Unit
                link == null -> search(text)
                link is YouTubeLink.Search -> search(link.query)
                !mediaNavigator.openLink(link) -> quickActions.announce(R.string.link_not_supported)
            }
        }
    }
}

/**
 * A finished search: the filter bar over the results, through their loading, error and empty states.
 * [maxAutoColumns] and [minCellWidth] size the grid the way the caller's canvas needs.
 */
@Composable
internal fun ColumnScope.SearchResultsBody(
    viewModel: SearchViewModel,
    filters: SearchFilter,
    shortsEnabled: Boolean,
    isGridMode: Boolean,
    feedColumns: HomeFeedColumns,
    pagingItems: LazyPagingItems<SearchResultItem>,
    gridState: LazyGridState,
    quickActions: QuickActionsViewModel,
    onVideoClick: (Video) -> Unit,
    onChannelClick: (Channel) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onShortsQueue: (ShortsQueueSource) -> Unit,
    dismissKeyboard: () -> Unit,
    maxAutoColumns: Int = FEED_MAX_AUTO_COLUMNS,
    minCellWidth: Dp = FeedCardMinWidth,
    bell: SubscriptionBellViewModel = sharedSubscriptionBellViewModel(),
) {
    val subscribedIds by quickActions.subscribedChannelIds.collectAsStateWithLifecycle()
    val notifyingIds by bell.notifyingChannelIds.collectAsStateWithLifecycle()
    val currentOnVideoClick by rememberUpdatedState(onVideoClick)

    SearchFilterBar(
        filter = filters,
        shortsEnabled = shortsEnabled,
        onFilterChange = viewModel::updateFilters,
        modifier = Modifier.padding(vertical = FilterBarVerticalPadding),
    )

    val refreshState = pagingItems.loadState.refresh
    BoxWithConstraints(modifier = Modifier.weight(1f)) {
        val feedLayout = rememberFeedGridLayout(maxWidth, feedColumns, maxAutoColumns, minCellWidth)
        val actions =
            remember(feedLayout, subscribedIds, notifyingIds) {
                SearchResultActions(
                    onVideoClick = { currentOnVideoClick(it) },
                    onShortsClick = { shelf, tapped ->
                        onShortsQueue(viewModel.shortsShelfSource(shelf, tapped))
                    },
                    onChannelClick = onChannelClick,
                    onPlaylistClick = onPlaylistClick,
                    dismissKeyboard = dismissKeyboard,
                    isSubscribed = { it in subscribedIds },
                    onSubscribeToggle = { channel ->
                        quickActions.toggleSubscription(channel.id, channel.name, channel.thumbnailUrl)
                    },
                    isNotifying = { it in notifyingIds },
                    onNotificationChange = { channel, enabled -> bell.setNotifications(channel.id, enabled) },
                )
            }

        when {
            refreshState is LoadState.Loading -> {
                FeedGridSkeleton(layout = feedLayout, listMode = isGridMode)
            }

            refreshState is LoadState.Error && pagingItems.itemCount == 0 -> {
                FlowErrorState(
                    error = refreshState.error.localizedMessage ?: stringResource(R.string.search_failed),
                    onRetry = pagingItems::retry,
                )
            }

            pagingItems.itemCount == 0 -> {
                FlowEmptyState(
                    title = stringResource(filters.emptyResultsRes()),
                    icon = Icons.Rounded.Search,
                )
            }

            filters.contentType == ContentType.SHORTS -> {
                SearchShortsGrid(pagingItems, gridState, feedLayout, actions)
            }

            else -> {
                SearchResults(pagingItems, gridState, feedLayout, isGridMode, actions)
            }
        }
    }
}

private fun SearchFilter.emptyResultsRes(): Int =
    when {
        !contentType.supportsVideoFilters() -> R.string.no_results_found
        watchState == WatchState.WATCHED -> R.string.search_watch_empty_watched
        watchState == WatchState.UNWATCHED -> R.string.search_watch_empty_unwatched
        else -> R.string.no_results_found
    }
