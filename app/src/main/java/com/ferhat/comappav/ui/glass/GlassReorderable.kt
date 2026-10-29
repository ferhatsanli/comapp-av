package com.ferhat.comappav.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch

/**
 * A vertical list whose items can be put in a new order. Outside reorder mode it is just a
 * Column. While [enabled]:
 *  - nothing inside the items can be used (buttons, text fields, chips...) - a transparent layer
 *    over each item takes the touches, and shows a small grabber at the top;
 *  - holding an item (long press) picks it up, dragging moves it, and the other items slide
 *    out of its way; letting go drops it. A quick swipe still scrolls the screen as usual.
 *
 * Each swap is reported through [onMove] (from index, to index); the caller owns the list.
 *
 * The drag is tracked on the Column itself, which doesn't move, rather than on the item being
 * dragged: an item that follows the finger would otherwise see the finger standing still
 * relative to itself.
 */
@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    itemKey: (T) -> Any,
    enabled: Boolean,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = 14.dp,
    accent: Color = GlassColors.Blue,
    itemContent: @Composable (T) -> Unit,
) {
    val spacingPx = with(LocalDensity.current) { spacing.toPx() }
    val scope = rememberCoroutineScope()
    // Measured layout of each item (in the Column, ignoring drag offsets) - plain maps: they are
    // only read inside gesture callbacks, never to decide what to draw.
    val tops = remember { HashMap<Any, Float>() }
    val heights = remember { HashMap<Any, Int>() }
    val displacements = remember { HashMap<Any, Animatable<Float, AnimationVector1D>>() }
    var draggingKey by remember { mutableStateOf<Any?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val latestItems by rememberUpdatedState(items)
    val latestItemKey by rememberUpdatedState(itemKey)
    val latestOnMove by rememberUpdatedState(onMove)

    // Leaving reorder mode mid-drag simply drops the item where it is.
    LaunchedEffect(enabled) {
        if (!enabled) {
            draggingKey = null
            dragOffset = 0f
        }
    }

    val dragModifier = if (!enabled) {
        Modifier
    } else {
        Modifier.pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { start ->
                    val keyOf = latestItemKey
                    draggingKey = latestItems.map(keyOf).firstOrNull { k ->
                        val top = tops[k] ?: return@firstOrNull false
                        start.y >= top && start.y <= top + (heights[k] ?: 0)
                    }
                    dragOffset = 0f
                },
                onDragEnd = {
                    draggingKey = null
                    dragOffset = 0f
                },
                onDragCancel = {
                    draggingKey = null
                    dragOffset = 0f
                },
            ) { change, amount ->
                val held = draggingKey ?: return@detectDragGesturesAfterLongPress
                change.consume()
                dragOffset += amount.y

                val list = latestItems
                val keyOf = latestItemKey
                val index = list.indexOfFirst { keyOf(it) == held }
                if (index < 0) return@detectDragGesturesAfterLongPress
                val heldHeight = heights[held] ?: 0

                // Swap with a neighbour once the held item has covered half of it; the offset is
                // then reduced by that neighbour's size, because the held item's own slot just
                // moved by exactly that much. The neighbour animates from its old place.
                if (dragOffset > 0f && index < list.lastIndex) {
                    val next = keyOf(list[index + 1])
                    val nextHeight = heights[next] ?: 0
                    if (dragOffset > nextHeight / 2f + spacingPx / 2f) {
                        latestOnMove(index, index + 1)
                        dragOffset -= nextHeight + spacingPx
                        displacements[next]?.let { slide ->
                            scope.launch {
                                slide.snapTo(heldHeight + spacingPx)
                                slide.animateTo(0f)
                            }
                        }
                    }
                } else if (dragOffset < 0f && index > 0) {
                    val previous = keyOf(list[index - 1])
                    val previousHeight = heights[previous] ?: 0
                    if (-dragOffset > previousHeight / 2f + spacingPx / 2f) {
                        latestOnMove(index, index - 1)
                        dragOffset += previousHeight + spacingPx
                        displacements[previous]?.let { slide ->
                            scope.launch {
                                slide.snapTo(-(heldHeight + spacingPx))
                                slide.animateTo(0f)
                            }
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier.then(dragModifier),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        items.forEach { item ->
            val k = itemKey(item)
            key(k) {
                val displacement = remember { Animatable(0f) }
                displacements[k] = displacement
                val held = enabled && draggingKey == k
                val lift by animateFloatAsState(if (held) 1.03f else 1f, label = "reorderLift")
                Box(
                    Modifier
                        .fillMaxWidth()
                        // Before graphicsLayer on purpose: the measured slot, not the dragged one.
                        .onPlaced { coordinates ->
                            tops[k] = coordinates.positionInParent().y
                            heights[k] = coordinates.size.height
                        }
                        .zIndex(if (held) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (held) dragOffset else displacement.value
                            scaleX = lift
                            scaleY = lift
                        },
                ) {
                    itemContent(item)
                    if (enabled) {
                        // Takes every touch on the item, so nothing inside it can be used. It
                        // consumes nothing: the touches still reach the Column (to pick the item
                        // up) and the scroll container (to scroll).
                        Box(
                            Modifier
                                .matchParentSize()
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) awaitPointerEvent()
                                    }
                                },
                        ) {
                            Box(
                                Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 7.dp)
                                    .size(width = 36.dp, height = 4.dp)
                                    .clip(CircleShape)
                                    .background(accent.copy(alpha = 0.85f), CircleShape),
                            )
                        }
                    }
                }
            }
        }
    }
}
