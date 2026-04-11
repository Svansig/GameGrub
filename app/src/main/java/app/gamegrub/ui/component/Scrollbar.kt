package app.gamegrub.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class ScrollbarMetrics(
    val totalItemsCount: Int,
    val visibleItemsCount: Int,
    val viewportHeight: Int,
    val columnsCount: Int,
    val totalRows: Int,
    val scrollProgress: Float,
    val thumbHeightRatio: Float,
)

/**
 * Reads frequently changing [LazyGridState] values once and exposes stable metrics for UI rendering.
 *
 * Rationale: direct reads like `layoutInfo`, `firstVisibleItemScrollOffset`, and scroll progress in
 * composable bodies can trigger avoidable recompositions and Compose lint warnings. Centralizing the
 * calculation keeps behavior the same while reducing composition churn.
 */
private fun calculateScrollbarMetrics(listState: LazyGridState): ScrollbarMetrics {
    val layoutInfo = listState.layoutInfo
    val totalItemsCount = layoutInfo.totalItemsCount
    val visibleItemsInfo = layoutInfo.visibleItemsInfo
    val visibleItemsCount = visibleItemsInfo.size
    val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset

    if (totalItemsCount == 0 || visibleItemsInfo.isEmpty()) {
        return ScrollbarMetrics(
            totalItemsCount = totalItemsCount,
            visibleItemsCount = visibleItemsCount,
            viewportHeight = viewportHeight,
            columnsCount = 1,
            totalRows = 1,
            scrollProgress = 0f,
            thumbHeightRatio = 1f,
        )
    }

    val firstRowY = visibleItemsInfo.first().offset.y
    val columnsCount = visibleItemsInfo.count { it.offset.y == firstRowY }.coerceAtLeast(1)
    val avgItemHeight = visibleItemsInfo.sumOf { it.size.height } / visibleItemsInfo.size.toFloat()
    val totalRows = (totalItemsCount + columnsCount - 1) / columnsCount

    val estimatedTotalHeight = totalRows * avgItemHeight
    val estimatedScrollableHeight = (estimatedTotalHeight - viewportHeight).coerceAtLeast(1f)
    val currentRow = listState.firstVisibleItemIndex / columnsCount
    val currentScrollOffset = currentRow * avgItemHeight + listState.firstVisibleItemScrollOffset
    val scrollProgress = (currentScrollOffset / estimatedScrollableHeight).coerceIn(0f, 1f)

    val thumbHeightRatio = if (viewportHeight <= 0 || estimatedTotalHeight <= 0f) {
        1f
    } else {
        (viewportHeight.toFloat() / estimatedTotalHeight).coerceIn(0.05f, 1f)
    }

    return ScrollbarMetrics(
        totalItemsCount = totalItemsCount,
        visibleItemsCount = visibleItemsCount,
        viewportHeight = viewportHeight,
        columnsCount = columnsCount,
        totalRows = totalRows,
        scrollProgress = scrollProgress,
        thumbHeightRatio = thumbHeightRatio,
    )
}

/**
 * Draggable scrollbar for LazyVerticalGrid.
 *
 * Design notes for future refactors:
 * - Rendering consumes derived metrics instead of directly reading volatile grid state.
 * - Gesture interactions cache grid geometry at drag start to keep drag behavior stable.
 * - Visibility and touch-scrolling are observed via flows in side effects, not composition-time keys.
 */
@Composable
fun Scrollbar(
    listState: LazyGridState,
    modifier: Modifier = Modifier,
    thumbColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
    trackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
    thumbWidthCollapsed: Dp = 4.dp,
    thumbWidthExpanded: Dp = 10.dp,
    thumbMinHeightDp: Dp = 48.dp,
    hideDelay: Long = 1500L,
    content: @Composable BoxScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()

    // Track visibility and interaction state
    val visibilityState = remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var isTouchScrolling by remember { mutableStateOf(false) }
    var containerHeight by remember { mutableFloatStateOf(0f) }

    // Drag state - when dragging, thumb follows gesture directly instead of list state
    var dragProgress by remember { mutableFloatStateOf(0f) }
    // Cache grid parameters at drag start to prevent recalculation during drag
    var dragColumnsCount by remember { mutableIntStateOf(1) }
    var dragTotalRows by remember { mutableIntStateOf(1) }
    var dragTotalItems by remember { mutableIntStateOf(0) }

    // Keep volatile grid reads behind derived state so UI code only consumes stable primitive values.
    val metrics by remember(listState) { derivedStateOf { calculateScrollbarMetrics(listState) } }
    val totalItemsCount = metrics.totalItemsCount
    val visibleItemsCount = metrics.visibleItemsCount
    val scrollProgress = metrics.scrollProgress
    val thumbHeightRatio = metrics.thumbHeightRatio
    val columnsCount = metrics.columnsCount
    val totalRows = metrics.totalRows

    val isExpanded = isDragging || isTouchScrolling

    // Animate width
    val thumbWidth by animateDpAsState(
        targetValue = if (isExpanded) thumbWidthExpanded else thumbWidthCollapsed,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "thumbWidth",
    )

    // Animate visibility
    val alpha by animateFloatAsState(
        targetValue = if (visibilityState.value || isDragging) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "scrollbarAlpha",
    )

    // Animate grab handle opacity
    val grabHandleAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "grabHandleAlpha",
    )

    // Observe scroll-in-progress outside composition to avoid direct volatile reads in composable scope.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collectLatest { isScrollInProgress ->
                if (isScrollInProgress && !isDragging) {
                    isTouchScrolling = true
                } else if (!isScrollInProgress) {
                    delay(300)
                    isTouchScrolling = false
                }
            }
    }

    // React to scroll position/layout updates in a side effect and drive auto-hide visibility from there.
    LaunchedEffect(listState, hideDelay) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            listState.firstVisibleItemIndex to
                    (listState.firstVisibleItemScrollOffset to (layoutInfo.totalItemsCount > layoutInfo.visibleItemsInfo.size))
        }.collectLatest { (_, scrollInfo) ->
            if (scrollInfo.second) {
                visibilityState.value = true
                delay(hideDelay)
                if (!isDragging && !isTouchScrolling) {
                    visibilityState.value = false
                }
            }
        }
    }

    val showScrollbar = totalItemsCount > visibleItemsCount

    Box(modifier = modifier.fillMaxSize()) {
        content()

        if (showScrollbar && alpha > 0f) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val thumbMinHeightPx = with(density) { thumbMinHeightDp.toPx() }
            val thumbHeightPx = (containerHeight * thumbHeightRatio).coerceAtLeast(thumbMinHeightPx)
            val maxOffset = (containerHeight - thumbHeightPx).coerceAtLeast(0f)
            val thumbHeightDp = with(density) { thumbHeightPx.toDp() }

            // When dragging, thumb follows gesture directly; otherwise follows list state
            val effectiveProgress = if (isDragging) dragProgress else scrollProgress
            val thumbOffset = effectiveProgress * maxOffset

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(24.dp)
                    .padding(end = 4.dp)
                    .alpha(alpha)
                    .onSizeChanged { containerHeight = it.height.toFloat() }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val targetProgress = (offset.y / containerHeight).coerceIn(0f, 1f)
                            val targetRow = (targetProgress * (totalRows - 1)).roundToInt()
                            val targetIndex = (targetRow * columnsCount).coerceIn(0, totalItemsCount - 1)
                            scope.launch {
                                listState.animateScrollToItem(targetIndex)
                            }
                        }
                    }
                    .pointerInput(totalItemsCount, columnsCount, totalRows) {
                        detectDragGestures(
                            onDragStart = {
                                // Cache grid parameters at drag start
                                dragColumnsCount = columnsCount
                                dragTotalRows = totalRows
                                dragTotalItems = totalItemsCount
                                dragProgress = scrollProgress
                                isDragging = true
                                visibilityState.value = true
                            },
                            onDragEnd = {
                                isDragging = false
                                scope.launch {
                                    delay(hideDelay)
                                    if (!isTouchScrolling) {
                                        visibilityState.value = false
                                    }
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                                scope.launch {
                                    delay(hideDelay)
                                    if (!isTouchScrolling) {
                                        visibilityState.value = false
                                    }
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                // Update drag progress directly from gesture
                                val deltaProgress = dragAmount.y / maxOffset.coerceAtLeast(1f)
                                dragProgress = (dragProgress + deltaProgress).coerceIn(0f, 1f)

                                // Use cached grid parameters for stable scroll calculations
                                val maxRow = (dragTotalRows - 1).coerceAtLeast(0)
                                val targetRow = (dragProgress * maxRow).roundToInt()
                                val targetIndex = (targetRow * dragColumnsCount).coerceIn(0, dragTotalItems - 1)

                                // Scroll synchronously to avoid race conditions
                                scope.launch {
                                    listState.scrollToItem(targetIndex.coerceAtLeast(0))
                                }
                            },
                        )
                    },
            ) {
                // Track
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(thumbWidth)
                        .clip(RoundedCornerShape(50))
                        .background(trackColor),
                )

                // Scrollbar thumb
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset { IntOffset(0, thumbOffset.roundToInt()) }
                        .width(thumbWidth)
                        .height(thumbHeightDp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    thumbColor,
                                    thumbColor.copy(alpha = thumbColor.alpha * 0.8f),
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    // Grab handle lines (only visible when expanded)
                    if (grabHandleAlpha > 0f) {
                        Column(
                            modifier = Modifier.alpha(grabHandleAlpha),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            repeat(3) {
                                Box(
                                    modifier = Modifier
                                        .width(6.dp)
                                        .height(1.5.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
