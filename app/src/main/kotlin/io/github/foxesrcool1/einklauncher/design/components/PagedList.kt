package io.github.foxesrcool1.einklauncher.design.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.foxesrcool1.einklauncher.design.EinkColors
import io.github.foxesrcool1.einklauncher.design.EinkDimens
import io.github.foxesrcool1.einklauncher.design.EinkType

/** Keeps the page index of a [PagedList] across recomposition and rotation. */
@Stable
class PagedListState(initialPage: Int = 0) {
    var page by mutableIntStateOf(initialPage)
        internal set

    fun goTo(page: Int, itemCount: Int, pageSize: Int) {
        this.page = Pagination.clampPage(page, itemCount, pageSize)
    }

    fun next(itemCount: Int, pageSize: Int) = goTo(page + 1, itemCount, pageSize)

    fun previous(itemCount: Int, pageSize: Int) = goTo(page - 1, itemCount, pageSize)
}

@Composable
fun rememberPagedListState(initialPage: Int = 0): PagedListState =
    rememberSaveable(saver = PagedListStateSaver) { PagedListState(initialPage) }

private val PagedListStateSaver =
    androidx.compose.runtime.saveable.Saver<PagedListState, Int>(
        save = { it.page },
        restore = { PagedListState(it) },
    )

/**
 * One page of rows, with Previous and Next below.
 *
 * E-ink rule 2: paginate, do not scroll. A scroll repaints the whole panel on
 * every frame and leaves a grey smear. A page turn repaints once. A swipe left
 * or right turns exactly one page.
 */
@Composable
fun <T> PagedList(
    items: List<T>,
    pageSize: Int,
    modifier: Modifier = Modifier,
    state: PagedListState = rememberPagedListState(),
    emptyText: String = "Nothing here yet",
    previousText: String = "Previous",
    nextText: String = "Next",
    itemContent: @Composable (index: Int, item: T) -> Unit,
) {
    val page = Pagination.clampPage(state.page, items.size, pageSize)
    val pageCount = Pagination.pageCount(items.size, pageSize)
    val firstIndex = Pagination.firstIndexOnPage(page, items.size, pageSize)
    val visible = Pagination.pageItems(items, page, pageSize)

    // One page turn per swipe. The threshold keeps a small drag from counting.
    val swipeThresholdPx = with(LocalDensity.current) { 56.dp.toPx() }
    var dragTotal by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(items.size, pageSize) {
                detectHorizontalDragGestures(
                    onDragStart = { dragTotal = 0f },
                    onDragEnd = {
                        if (dragTotal <= -swipeThresholdPx) {
                            state.next(items.size, pageSize)
                        } else if (dragTotal >= swipeThresholdPx) {
                            state.previous(items.size, pageSize)
                        }
                        dragTotal = 0f
                    },
                    onDragCancel = { dragTotal = 0f },
                    onHorizontalDrag = { _, amount -> dragTotal += amount },
                )
            },
    ) {
        Column(modifier = Modifier.weight(1f, fill = true)) {
            if (visible.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CapsLabel(
                        text = emptyText,
                        style = EinkType.caps.copy(color = EinkColors.Faded),
                    )
                }
            } else {
                visible.forEachIndexed { offset, item ->
                    itemContent(firstIndex + offset, item)
                }
            }
        }

        HairlineDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = EinkDimens.targetGap),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InvertPressButton(
                text = previousText,
                onClick = { state.previous(items.size, pageSize) },
                enabled = Pagination.hasPrevious(page),
            )
            CapsLabel(
                text = "Page ${page + 1} of $pageCount",
                style = EinkType.capsSmall,
            )
            InvertPressButton(
                text = nextText,
                onClick = { state.next(items.size, pageSize) },
                enabled = Pagination.hasNext(page, items.size, pageSize),
            )
        }
    }
}
