package com.luc4n3x.levyra.feature.motion

import java.io.IOException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response

internal suspend fun awaitMotionArtworkResponse(call: Call): Response =
    suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, error: IOException) {
                val token = continuation.tryResumeWithException(error)
                if (token != null) continuation.completeResume(token)
            }

            override fun onResponse(call: Call, response: Response) {
                val token = continuation.tryResume(response)
                if (token != null) {
                    continuation.completeResume(token)
                } else {
                    response.close()
                }
            }
        })
    }
