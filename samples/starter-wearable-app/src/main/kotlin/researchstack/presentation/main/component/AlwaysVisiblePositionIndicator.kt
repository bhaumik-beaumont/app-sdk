package researchstack.presentation.main.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.material.PositionIndicator
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
    var showIndicator by remember { mutableStateOf(true) }

    LaunchedEffect(initialVisibilityMillis) {
        if (initialVisibilityMillis > 0) {
            delay(initialVisibilityMillis)
            showIndicator = false
        }
    }

    LaunchedEffect(scalingLazyListState.isScrollInProgress) {
        if (scalingLazyListState.isScrollInProgress) {
            showIndicator = true
        }
    }

    if (showIndicator || scalingLazyListState.isScrollInProgress || scalingLazyListState.canScrollForward || scalingLazyListState.canScrollBackward) {
        PositionIndicator(
            scalingLazyListState = scalingLazyListState,
            modifier = modifier
        )
    }
}
