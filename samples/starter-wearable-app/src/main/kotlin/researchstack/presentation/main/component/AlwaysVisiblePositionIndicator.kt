package researchstack.presentation.main.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.ScalingLazyListItemInfo
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.PositionIndicatorState
import androidx.wear.compose.material.PositionIndicatorVisibility
import kotlinx.coroutines.delay

/**
 * A wrapper around [PositionIndicator] that keeps the indicator visible when the screen first
 * appears so users immediately understand that the list can scroll.
 */
@Composable
fun AlwaysVisiblePositionIndicator(
    scalingLazyListState: ScalingLazyListState,
    modifier: Modifier = Modifier,
    initialVisibilityMillis: Long = 2000L,
) {
    var forceShow by remember { mutableStateOf(true) }

    LaunchedEffect(initialVisibilityMillis) {
        if (initialVisibilityMillis > 0) {
            delay(initialVisibilityMillis)
        }
        forceShow = false
    }

    LaunchedEffect(scalingLazyListState.isScrollInProgress) {
        if (scalingLazyListState.isScrollInProgress) {
            forceShow = false
        }
    }

    val indicatorState = remember(scalingLazyListState) {
        AlwaysVisibleScalingLazyColumnStateAdapter(scalingLazyListState) { forceShow }
    }

    PositionIndicator(
        state = indicatorState,
        modifier = modifier
    )
}

private class AlwaysVisibleScalingLazyColumnStateAdapter(
    private val state: ScalingLazyListState,
    private val forceShow: () -> Boolean,
) : PositionIndicatorState {

    override val positionFraction: Float
        get() {
            if (noVisibleItems()) {
                return 0f
            }
            val decimalFirstItemIndex = decimalFirstItemIndex()
            val decimalLastItemIndex = decimalLastItemIndex()
            val distanceFromEnd = totalItemsCount() - decimalLastItemIndex

            return if (decimalFirstItemIndex + distanceFromEnd == 0f) {
                0f
            } else {
                decimalFirstItemIndex /
                    (decimalFirstItemIndex + distanceFromEnd)
            }
        }

    override fun sizeFraction(scrollableContainerSizePx: Float): Float {
        return if (totalItemsCount() == 0) {
            1f
        } else {
            val decimalFirstItemIndex = decimalFirstItemIndex()
            val decimalLastItemIndex = decimalLastItemIndex()

            (decimalLastItemIndex - decimalFirstItemIndex) /
                totalItemsCount().toFloat()
        }
    }

    override fun visibility(scrollableContainerSizePx: Float): PositionIndicatorVisibility {
        val canScroll = !noVisibleItems() && canScrollBackwardsOrForwards()
        return if (canScroll) {
            if (forceShow() || isScrollInProgress()) {
                PositionIndicatorVisibility.Show
            } else {
                PositionIndicatorVisibility.AutoHide
            }
        } else {
            PositionIndicatorVisibility.Hide
        }
    }

    private fun noVisibleItems(): Boolean = state.layoutInfo.visibleItemsInfo.isEmpty()

    private fun totalItemsCount(): Int = state.layoutInfo.totalItemsCount

    private fun isScrollInProgress(): Boolean = state.isScrollInProgress

    private fun canScrollBackwardsOrForwards(): Boolean =
        state.canScrollBackward || state.canScrollForward

    private fun decimalLastItemIndex(): Float {
        val visibleItems = state.layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) return 0f
        val lastItem = visibleItems.last()
        val lastItemEndOffset = lastItem.startOffset(state.layoutInfo.anchorType) + lastItem.size
        val viewportEndOffset = state.layoutInfo.viewportSize.height / 2f
        val lastItemVisibleFraction = (
            1f - ((lastItemEndOffset - viewportEndOffset) /
                lastItem.size.coerceAtLeast(1))
            ).coerceAtMost(1f)

        return lastItem.index.toFloat() + lastItemVisibleFraction
    }

    private fun decimalFirstItemIndex(): Float {
        val visibleItems = state.layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) return 0f
        val firstItem = visibleItems.first()
        val firstItemStartOffset = firstItem.startOffset(state.layoutInfo.anchorType)
        val viewportStartOffset = -(state.layoutInfo.viewportSize.height / 2f)
        val firstItemInvisibleFraction = (
            (viewportStartOffset - firstItemStartOffset) /
                firstItem.size.coerceAtLeast(1)
            ).coerceAtLeast(0f)

        return firstItem.index.toFloat() + firstItemInvisibleFraction
    }

    override fun equals(other: Any?): Boolean {
        return other is AlwaysVisibleScalingLazyColumnStateAdapter && other.state == state
    }

    override fun hashCode(): Int = state.hashCode()
}

private fun ScalingLazyListItemInfo.startOffset(anchorType: ScalingLazyListAnchorType): Float =
    offset - if (anchorType == ScalingLazyListAnchorType.ItemCenter) {
        size / 2f
    } else {
        0f
    }
