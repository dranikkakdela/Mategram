package com.xxcactussell.presentation.chats.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.floatingToolbarVerticalNestedScroll
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xxcactussell.domain.FormattedText
import com.xxcactussell.mategram.presentation.R
import com.xxcactussell.presentation.chats.model.ChatListUiState
import com.xxcactussell.presentation.chats.model.ChatsEvent
import com.xxcactussell.presentation.localization.localizedString
import com.xxcactussell.presentation.tools.FormattedTextView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChatListContent(
    state: ChatListUiState,
    onEvent: (ChatsEvent) -> Unit,
    onChatClick: (Long, Long?, Long?) -> Unit,
    onFabClicked: () -> Unit,
    onAvatarClicked: () -> Unit,
    onStoriesClicked: () -> Unit = {},
    onContactsClicked: () -> Unit = {},
    onSavedMessagesClicked: () -> Unit = {},
    onCallsClicked: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val textFieldState = rememberTextFieldState()
    val searchBarState = rememberSearchBarState()
    var toolBarExpanded by rememberSaveable { mutableStateOf(true) }
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f,
        pageCount = { state.allTabs.size }
    )
    val foldersFiltersState = rememberLazyListState()

    LaunchedEffect(pagerState.currentPage, state.allTabs) {
        val currentPage = pagerState.currentPage
        if (currentPage < state.allTabs.size) {
            onEvent(ChatsEvent.OnFolderChanged(state.allTabs[currentPage].id))
            foldersFiltersState.animateScrollToItem(currentPage)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier
            .floatingToolbarVerticalNestedScroll(
                expanded = toolBarExpanded,
                onExpand = { toolBarExpanded = true },
                onCollapse = { toolBarExpanded = false },
            ),
        topBar = {
            Column {
                AppBarWithSearch(
                    state = searchBarState,
                    inputField = {
                        SearchBarDefaults.InputField(
                            modifier = Modifier,
                            searchBarState = searchBarState,
                            textFieldState = textFieldState,
                            onSearch = { scope.launch { searchBarState.animateToCollapsed() } },
                            placeholder = {
                                if (searchBarState.currentValue == SearchBarValue.Collapsed) {
                                    Text(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = localizedString("Search"),
                                        textAlign = TextAlign.Center,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1
                                    )
                                }
                            }
                        )
                    },
                    navigationIcon = {
                        // Кнопка Stories — теперь работает
                        IconButton(onClick = onStoriesClicked) {
                            Icon(painterResource(R.drawable.photo_camera_24px), "Stories")
                        }
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .padding(12.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onAvatarClicked)
                        ) {
                            ChatAvatar(
                                modifier = Modifier.fillMaxSize(),
                                state = state.me?.avatar,
                                isPinned = false,
                                isOnline = false
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = SearchBarDefaults.appBarWithSearchColors(
                        appBarContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        scrolledAppBarContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
                if (state.allTabs.size > 1) {
                    LazyRow(
                        modifier = Modifier,
                        state = foldersFiltersState,
                        contentPadding = PaddingValues(
                            start = WindowInsets.displayCutout.asPaddingValues().calculateStartPadding(LocalLayoutDirection.current) + 12.dp,
                            top = 0.dp,
                            end = WindowInsets.displayCutout.asPaddingValues().calculateEndPadding(LocalLayoutDirection.current) + 12.dp,
                            bottom = 8.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        itemsIndexed(
                            items = state.allTabs,
                            key = { _, tab -> "tab_id_${tab.id}" }
                        ) { index, tab ->
                            ToggleButton(
                                checked = state.selectedIndex == index,
                                onCheckedChange = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(index)
                                        foldersFiltersState.animateScrollToItem(index)
                                    }
                                    haptic.performHapticFeedback(
                                        if (it) HapticFeedbackType.ToggleOn
                                        else HapticFeedbackType.ToggleOff
                                    )
                                },
                                shapes = when (index) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    state.allTabs.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                },
                            ) {
                                FormattedTextView(
                                    text = tab.title ?: FormattedText(localizedString("FilterAllChats"), emptyList()),
                                    clickable = false
                                )
                                if (tab.unreadCount > 0) {
                                    Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                    Badge(
                                        containerColor = if (tab.isSelected)
                                            MaterialTheme.colorScheme.errorContainer
                                        else
                                            MaterialTheme.colorScheme.error
                                    ) {
                                        Text("${tab.unreadCount}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            HorizontalFloatingToolbar(
                expanded = toolBarExpanded,
                modifier = Modifier,
                colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                floatingActionButton = {
                    FloatingToolbarDefaults.VibrantFloatingActionButton(
                        onClick = onFabClicked,
                        modifier = Modifier
                    ) {
                        Icon(painterResource(R.drawable.ic_notification), "Create Chat")
                    }
                }
            ) {
                // Кнопки тулбара — теперь все работают
                IconButton(onClick = onContactsClicked) {
                    Icon(painterResource(R.drawable.contacts_24px), "Contacts")
                }
                IconButton(onClick = onSavedMessagesClicked) {
                    Icon(painterResource(R.drawable.bookmarks_24px), "Saved messages")
                }
                IconButton(onClick = onCallsClicked) {
                    Icon(painterResource(R.drawable.call_24px), "Phone")
                }
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 2,
            pageSpacing = 16.dp
        ) { page ->
            if (page < state.allTabs.size) {
                FolderScreen(
                    folderId = state.allTabs[page].id,
                    state = state,
                    onChatClicked = onChatClick,
                    paddingValues = paddingValues
                )
            }
        }
    }
}
