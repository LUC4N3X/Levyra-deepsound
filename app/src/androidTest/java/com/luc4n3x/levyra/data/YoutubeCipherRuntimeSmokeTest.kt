package com.luc4n3x.levyra.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.reflect.InvocationTargetException
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

@RunWith(AndroidJUnit4::class)
class YoutubeCipherRuntimeSmokeTest {
    @Test
    fun existingRuntimeExecutesEs6AndMathPowForSignatureAndN() = runBlocking<Unit> {
        val source = """
            var _yt_player={};(function(p){
              const S=(a)=>a.split("").reverse().join("")+Math.pow(2,3);
              const N=(a)=>a.slice(1)+a[0]+Math.pow(3,2);
            })(_yt_player);
        """.trimIndent()
        val config = YoutubePlayerCipherConfig("abcdef12", "S(INPUT)", null, 123,
            nExpressionOverride = "N(INPUT)")
        val playerClass = Class.forName("com.luc4n3x.levyra.data.YoutubePlayerScript")
        val constructor = playerClass.declaredConstructors.first { it.parameterCount == 5 }
            .apply { isAccessible = true }
        val player = constructor.newInstance("abcdef12", "abcdef12", source, 123, emptyList<Any>())
        val runtimeClass = Class.forName("com.luc4n3x.levyra.data.YoutubeCipherWebRuntime")
        val companion = runtimeClass.getDeclaredField("Companion").apply { isAccessible = true }.get(null)
        val runtime = invokeSuspending(companion, "create",
            InstrumentationRegistry.getInstrumentation().targetContext, player, config)!!
        try {
            assertEquals("fedcba8", invokeSuspending(runtime, "decodeSignature", "abcdef"))
            coroutineScope {
                (0 until 8).map { index ->
                    async {
                        val input = "abcdef$index"
                        assertEquals(input.drop(1) + input.first() + "9",
                            invokeSuspending(runtime, "transformN", input))
                    }
                }.awaitAll()
            }
        } finally {
            invokeSuspending(runtime, "close")
        }
    }

    private suspend fun invokeSuspending(target: Any, name: String, vararg arguments: Any?): Any? =
        suspendCoroutineUninterceptedOrReturn { continuation ->
            val method = target.javaClass.declaredMethods.single {
                it.name == name && it.parameterCount == arguments.size + 1
            }.apply { isAccessible = true }
            try {
                method.invoke(target, *arguments, continuation)
            } catch (error: InvocationTargetException) {
                throw error.targetException
            }
        }
}
