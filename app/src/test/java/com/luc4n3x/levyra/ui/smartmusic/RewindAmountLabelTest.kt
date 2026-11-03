package com.luc4n3x.levyra.ui.smartmusic

import org.junit.Assert.assertEquals
import org.junit.Test

class RewindAmountLabelTest {
    @Test
    fun keepsTheLocalisedUnitNextToTheNumber() {
        assertEquals("30 s", rewindAmountLabel("back {n} s", "30"))
        assertEquals("30 с", rewindAmountLabel("назад {n} с", "30"))
        assertEquals("7 mp", rewindAmountLabel("{n} mp vissza", "7"))
        assertEquals("41 秒", rewindAmountLabel("回退 {n} 秒", "41"))
        assertEquals("48초", rewindAmountLabel("{n}초 뒤로", "48"))
        assertEquals("0 ث", rewindAmountLabel("رجوع {n} ث", "0"))
        assertEquals("12", rewindAmountLabel("{n}", "12"))
    }
}
