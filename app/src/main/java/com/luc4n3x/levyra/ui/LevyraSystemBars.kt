package com.luc4n3x.levyra.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.luc4n3x.levyra.domain.ImmersiveBars
import java.util.WeakHashMap

@Composable
internal fun HideSystemBars(bars: ImmersiveBars) {
    val view = LocalView.current
    DisposableEffect(view, bars) {
        val window = view.context.findActivity()?.window
        if (!bars.any || window == null) return@DisposableEffect onDispose {}
        val requests = SystemBarsRequests.forWindow(window, view)
        val owner = Any()
        requests.set(owner, bars.insetTypes())
        onDispose { requests.set(owner, 0) }
    }
}

internal fun ImmersiveBars.insetTypes(): Int {
    var types = 0
    if (status) types = types or WindowInsetsCompat.Type.statusBars()
    if (navigation) types = types or WindowInsetsCompat.Type.navigationBars()
    return types
}

private class SystemBarsRequests(private val window: Window, private val controller: WindowInsetsControllerCompat) {
    private val requests = LinkedHashMap<Any, Int>()
    private val defaultBehavior = controller.systemBarsBehavior

    fun set(owner: Any, types: Int) {
        if (types == 0) requests.remove(owner) else requests[owner] = types
        val hidden = requests.values.fold(0) { combined, requested -> combined or requested }
        val shown = WindowInsetsCompat.Type.systemBars() and hidden.inv()
        controller.systemBarsBehavior = if (hidden == 0) {
            defaultBehavior
        } else {
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        if (shown != 0) controller.show(shown)
        if (hidden != 0) controller.hide(hidden)
        if (requests.isEmpty()) byWindow.remove(window)
    }

    companion object {
        private val byWindow = WeakHashMap<Window, SystemBarsRequests>()

        fun forWindow(window: Window, view: View): SystemBarsRequests =
            byWindow.getOrPut(window) { SystemBarsRequests(window, WindowCompat.getInsetsController(window, view)) }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
