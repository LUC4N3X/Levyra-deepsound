package com.luc4n3x.levyra.desktop.core.localmusic

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalMusicIdentityTest {

    @Test
    fun containmentRequiresAPathBoundary() {
        assertTrue(LocalMusicIdentity.isWithin("C:/Music", "C:/Music"))
        assertTrue(LocalMusicIdentity.isWithin("C:/Music/Albums/song.flac", "C:/Music"))
        assertFalse(LocalMusicIdentity.isWithin("C:/Music2/song.flac", "C:/Music"))
        assertFalse(LocalMusicIdentity.isWithin("C:/Musical/song.flac", "C:/Music"))
    }

    @Test
    fun containmentNormalizesWindowsSeparatorsAndCase() {
        assertTrue(LocalMusicIdentity.isWithin("c:\\MUSIC\\Albums\\song.flac", "C:/Music/"))
        assertTrue(LocalMusicIdentity.isWithin("D:/song.flac", "d:/"))
        assertFalse(LocalMusicIdentity.isWithin("E:/song.flac", "D:/"))
    }
}
