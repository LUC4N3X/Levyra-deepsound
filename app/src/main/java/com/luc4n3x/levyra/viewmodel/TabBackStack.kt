package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.LevyraTab

internal sealed interface TabBackAction {
    data object ClearSearch : TabBackAction
    data class ShowTab(val tab: LevyraTab) : TabBackAction
}

internal class TabBackStack {
    private val tabs = ArrayDeque<LevyraTab>()

    fun rememberTransition(current: LevyraTab, target: LevyraTab) {
        if (current == target) return
        tabs.remove(target)
        tabs.remove(current)
        if (current != LevyraTab.Player) tabs.addLast(current)
        while (tabs.size > 8) tabs.removeFirst()
    }

    fun back(current: LevyraTab, query: String): TabBackAction {
        if (current == LevyraTab.Search && query.isNotBlank()) return TabBackAction.ClearSearch
        return TabBackAction.ShowTab(pop(current))
    }

    fun pop(current: LevyraTab): LevyraTab {
        while (tabs.isNotEmpty()) {
            val candidate = tabs.removeLast()
            if (candidate != current) return candidate
        }
        return LevyraTab.Home
    }
}
