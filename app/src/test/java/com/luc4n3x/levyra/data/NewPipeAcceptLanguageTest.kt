package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Test

class NewPipeAcceptLanguageTest {
    @Test
    fun acceptLanguageFollowsTheSelectedLanguage() {
        NewPipeRuntime.setLanguage("es")
        assertEquals("es-ES,es;q=0.9,en-US;q=0.8,en;q=0.7", NewPipeRuntime.acceptLanguageHeader())

        NewPipeRuntime.setLanguage("it")
        assertEquals("it-IT,it;q=0.9,en-US;q=0.8,en;q=0.7", NewPipeRuntime.acceptLanguageHeader())
    }

    @Test
    fun englishDoesNotRepeatItselfAsItsOwnFallback() {
        NewPipeRuntime.setLanguage("en")
        assertEquals("en-US,en;q=0.9", NewPipeRuntime.acceptLanguageHeader())
    }
}
