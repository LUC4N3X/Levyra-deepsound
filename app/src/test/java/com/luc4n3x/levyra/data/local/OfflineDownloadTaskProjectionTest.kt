package com.luc4n3x.levyra.data.local

import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class OfflineDownloadTaskProjectionTest {
    @Test
    fun activeTaskProjectionContainsOnlyUiFields() {
        val fields = OfflineDownloadTaskSummaryRow::class.java.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }
            .map { it.name }
            .toSet()

        assertEquals(
            setOf("taskKey", "trackId", "title", "artist", "state", "progress", "error"),
            fields
        )
        assertFalse("payload" in fields)
    }
}
