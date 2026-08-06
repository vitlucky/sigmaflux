package com.sigmaflux.market.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlin.math.abs

/**
 * Поведение bottom bar: скрывается при свайпе вниз по контенту,
 * появляется при свайпе вверх. Применяется к LazyColumn контента.
 */
class BottomBarBehavior {

    var hidden by mutableStateOf(false)
        private set

    private var acc = 0f

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.Drag) return Offset.Zero
            acc += available.y
            if (!hidden && acc > 160f) {        // скролл вниз
                hidden = true
                acc = 0f
            } else if (hidden && acc < -40f) {  // скролл вверх
                hidden = false
                acc = 0f
            } else if (abs(acc) > 400f) {
                acc = 0f
            }
            return Offset.Zero
        }
    }

    fun Modifier.applyTo(modifier: Modifier): Modifier = modifier.nestedScroll(connection)

    /** Скрыть панель (например, на графике — immersive). */
    fun forceHide() { hidden = true }
    fun forceShow() { hidden = false }
}
