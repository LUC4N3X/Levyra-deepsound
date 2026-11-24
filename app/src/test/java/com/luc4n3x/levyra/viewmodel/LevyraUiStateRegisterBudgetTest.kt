package com.luc4n3x.levyra.viewmodel

import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraUiStateRegisterBudgetTest {

    @Test
    fun everyConstructorFitsTheDalvikArgumentRegisterLimit() {
        LevyraUiState::class.java.declaredConstructors.forEach { constructor ->
            val registers = 1 + constructor.parameterTypes.sumOf { type ->
                if (type == java.lang.Long.TYPE || type == java.lang.Double.TYPE) 2L else 1L
            }
            assertTrue("$constructor needs $registers argument registers", registers <= 255)
        }
    }
}
