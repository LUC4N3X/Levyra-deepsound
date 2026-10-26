package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.LevyraTab
import org.junit.Assert.assertEquals
import org.junit.Test

class TabBackStackTest {
    private val history = TabBackStack()

    @Test
    fun `back from empty search after collapsing player returns to the previous tab`() {
        history.rememberTransition(LevyraTab.Home, LevyraTab.Search)
        history.rememberTransition(LevyraTab.Search, LevyraTab.Player)
        history.rememberTransition(LevyraTab.Player, LevyraTab.Search)

        assertEquals(LevyraTab.Home, history.pop(LevyraTab.Search))
    }

    @Test
    fun `collapsing player preserves the earlier navigation order`() {
        history.rememberTransition(LevyraTab.Home, LevyraTab.Explore)
        history.rememberTransition(LevyraTab.Explore, LevyraTab.Search)
        history.rememberTransition(LevyraTab.Search, LevyraTab.Player)
        history.rememberTransition(LevyraTab.Player, LevyraTab.Search)

        assertEquals(LevyraTab.Explore, history.pop(LevyraTab.Search))
        assertEquals(LevyraTab.Home, history.pop(LevyraTab.Explore))
    }

    @Test
    fun `repeated player expansions never reinsert the player on back`() {
        history.rememberTransition(LevyraTab.Home, LevyraTab.Search)
        repeat(20) {
            history.rememberTransition(LevyraTab.Search, LevyraTab.Player)
            history.rememberTransition(LevyraTab.Player, LevyraTab.Search)
        }

        assertEquals(LevyraTab.Home, history.pop(LevyraTab.Search))
        assertEquals(LevyraTab.Home, history.pop(LevyraTab.Home))
    }

    @Test
    fun `switching from player to another tab returns to its original tab`() {
        history.rememberTransition(LevyraTab.Home, LevyraTab.Search)
        history.rememberTransition(LevyraTab.Search, LevyraTab.Player)
        history.rememberTransition(LevyraTab.Player, LevyraTab.Library)

        assertEquals(LevyraTab.Search, history.pop(LevyraTab.Library))
        assertEquals(LevyraTab.Home, history.pop(LevyraTab.Search))
    }

    @Test
    fun `system back from player still returns to the tab underneath`() {
        history.rememberTransition(LevyraTab.Home, LevyraTab.Search)
        history.rememberTransition(LevyraTab.Search, LevyraTab.Player)

        assertEquals(LevyraTab.Search, history.pop(LevyraTab.Player))
        assertEquals(LevyraTab.Home, history.pop(LevyraTab.Search))
    }

    @Test
    fun `reselecting current tab preserves back history`() {
        history.rememberTransition(LevyraTab.Home, LevyraTab.Search)
        history.rememberTransition(LevyraTab.Search, LevyraTab.Search)

        assertEquals(LevyraTab.Home, history.pop(LevyraTab.Search))
    }

    @Test
    fun `back from search results returns to search start before leaving the tab`() {
        history.rememberTransition(LevyraTab.Home, LevyraTab.Search)
        history.rememberTransition(LevyraTab.Search, LevyraTab.Player)
        history.rememberTransition(LevyraTab.Player, LevyraTab.Search)

        assertEquals(TabBackAction.ClearSearch, history.back(LevyraTab.Search, "da dio"))
        assertEquals(TabBackAction.ShowTab(LevyraTab.Home), history.back(LevyraTab.Search, ""))
    }

    @Test
    fun `returning to search start preserves the previous browsing screen`() {
        history.rememberTransition(LevyraTab.Home, LevyraTab.Explore)
        history.rememberTransition(LevyraTab.Explore, LevyraTab.Search)

        assertEquals(TabBackAction.ClearSearch, history.back(LevyraTab.Search, "da dio"))
        assertEquals(TabBackAction.ShowTab(LevyraTab.Explore), history.back(LevyraTab.Search, ""))
    }

    @Test
    fun `back from player preserves the search results underneath`() {
        history.rememberTransition(LevyraTab.Home, LevyraTab.Search)
        history.rememberTransition(LevyraTab.Search, LevyraTab.Player)

        assertEquals(TabBackAction.ShowTab(LevyraTab.Search), history.back(LevyraTab.Player, "da dio"))
        assertEquals(TabBackAction.ClearSearch, history.back(LevyraTab.Search, "da dio"))
        assertEquals(TabBackAction.ShowTab(LevyraTab.Home), history.back(LevyraTab.Search, ""))
    }

    @Test
    fun `saved search query does not intercept back from another tab`() {
        history.rememberTransition(LevyraTab.Home, LevyraTab.Search)
        history.rememberTransition(LevyraTab.Search, LevyraTab.Library)

        assertEquals(TabBackAction.ShowTab(LevyraTab.Search), history.back(LevyraTab.Library, "da dio"))
    }

    @Test
    fun `blank search returns to the previous tab without another clear step`() {
        history.rememberTransition(LevyraTab.Home, LevyraTab.Explore)
        history.rememberTransition(LevyraTab.Explore, LevyraTab.Search)

        assertEquals(TabBackAction.ShowTab(LevyraTab.Explore), history.back(LevyraTab.Search, "  "))
    }

    @Test
    fun `ordinary tab navigation keeps the most recent distinct destinations`() {
        history.rememberTransition(LevyraTab.Home, LevyraTab.Search)
        history.rememberTransition(LevyraTab.Search, LevyraTab.Explore)
        history.rememberTransition(LevyraTab.Explore, LevyraTab.Search)

        assertEquals(LevyraTab.Explore, history.pop(LevyraTab.Search))
        assertEquals(LevyraTab.Home, history.pop(LevyraTab.Explore))
    }
}
