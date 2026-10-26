package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.LevyraTab
import org.junit.Assert.assertEquals
import org.junit.Test

class TabBackStackTest {
    private val history = TabBackStack()

    @Test
    fun `back after collapsing search player returns home`() {
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
    fun `ordinary tab navigation keeps the most recent distinct destinations`() {
        history.rememberTransition(LevyraTab.Home, LevyraTab.Search)
        history.rememberTransition(LevyraTab.Search, LevyraTab.Explore)
        history.rememberTransition(LevyraTab.Explore, LevyraTab.Search)

        assertEquals(LevyraTab.Explore, history.pop(LevyraTab.Search))
        assertEquals(LevyraTab.Home, history.pop(LevyraTab.Explore))
    }
}
