package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.LevyraTab

internal class TabBackStack {
    private val tabs = ArrayDeque<LevyraTab>()

    fun rememberTransition(current: LevyraTab, target: LevyraTab) {
        if (current == target) return
        tabs.remove(target)
        tabs.remove(current)
        if (current != LevyraTab.Player) tabs.addLast(current)
        while (tabs.size > 8) tabs.removeFirst()
    }

    fun pop(current: LevyraTab): LevyraTab {
        while (tabs.isNotEmpty()) {
            val candidate = tabs.removeLast()
            if (candidate != current) return candidate
        }
        return LevyraTab.Home
    }
}
