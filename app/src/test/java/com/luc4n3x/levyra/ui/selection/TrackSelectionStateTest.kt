package com.luc4n3x.levyra.ui.selection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackSelectionStateTest {
    @Test
    fun `long press start enters selection with first item`() {
        val state = TrackSelectionState()
        state.start("a")
        assertTrue(state.isActive)
        assertEquals(setOf("a"), state.selectedIds)
    }

    @Test
    fun `toggle adds and removes stable ids`() {
        val state = TrackSelectionState()
        state.start("a")
        state.toggle("b")
        assertEquals(setOf("a", "b"), state.selectedIds)
        state.toggle("a")
        assertEquals(setOf("b"), state.selectedIds)
    }

    @Test
    fun `deselecting final item exits selection mode`() {
        val state = TrackSelectionState()
        state.start("a")
        state.toggle("a")
        assertFalse(state.isActive)
        assertEquals(0, state.count)
    }

    @Test
    fun `select all uses only available distinct ids`() {
        val state = TrackSelectionState()
        state.start("old")
        state.selectAll(listOf("a", "b", "a", "", "  "))
        assertEquals(linkedSetOf("a", "b"), state.selectedIds)
    }

    @Test
    fun `toggle select all doubles as deselect all`() {
        val state = TrackSelectionState()
        state.start("a")
        state.toggleSelectAll(listOf("a", "b"))
        assertEquals(linkedSetOf("a", "b"), state.selectedIds)
        state.toggleSelectAll(listOf("a", "b"))
        assertFalse(state.isActive)
    }

    @Test
    fun `reorder does not change selection identity`() {
        val state = TrackSelectionState()
        state.start("track-2")
        val reordered = listOf("track-3", "track-2", "track-1")
        assertEquals(listOf("track-2"), state.resolveSelected(reordered) { it })
    }

    @Test
    fun `list update never resolves vanished ids to another item`() {
        val state = TrackSelectionState()
        state.start("track-2")
        val updated = listOf("track-1", "track-3", "track-4")
        assertTrue(state.resolveSelected(updated) { it }.isEmpty())
        assertEquals(setOf("track-2"), state.selectedIds)
    }

    @Test
    fun `retain available safely prunes vanished ids`() {
        val state = TrackSelectionState()
        state.start("a")
        state.toggle("b")
        state.retainAvailable(listOf("b", "c"))
        assertEquals(setOf("b"), state.selectedIds)
    }

    @Test
    fun `exit clears all state`() {
        val state = TrackSelectionState()
        state.start("a")
        state.toggle("b")
        state.exit()
        assertFalse(state.isActive)
        assertEquals(emptySet<String>(), state.selectedIds)
    }
}
