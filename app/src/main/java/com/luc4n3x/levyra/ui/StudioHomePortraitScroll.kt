package com.luc4n3x.levyra.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import com.luc4n3x.levyra.domain.HomeStyle
import kotlinx.coroutines.flow.distinctUntilChanged

internal class StudioHomePortraitScroll(val position: MutableFloatState, val connection: NestedScrollConnection)

@Composable
internal fun rememberStudioHomePortraitScroll(homeListState: LazyListState, homeStyle: HomeStyle): StudioHomePortraitScroll {
        val portraitScroll = rememberSaveable {
            mutableFloatStateOf(
                if (homeListState.firstVisibleItemIndex == 0) {
                    homeListState.firstVisibleItemScrollOffset.toFloat()
                } else {
                    10_000f
                }
            )
        }
        val portraitScrollConnection = remember(homeListState, homeStyle) {
            object : NestedScrollConnection {
                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    if (homeStyle == HomeStyle.ExpressiveMax && consumed.y != 0f) {
                        portraitScroll.floatValue = (portraitScroll.floatValue - consumed.y).coerceAtLeast(0f)
                    }
                    return Offset.Zero
                }
            }
        }
        LaunchedEffect(homeListState) {
            snapshotFlow {
                homeListState.firstVisibleItemIndex == 0 && homeListState.firstVisibleItemScrollOffset == 0
            }.distinctUntilChanged().collect { atTop ->
                if (atTop) portraitScroll.floatValue = 0f
            }
        }
    return remember(portraitScroll, portraitScrollConnection) { StudioHomePortraitScroll(portraitScroll, portraitScrollConnection) }
}
