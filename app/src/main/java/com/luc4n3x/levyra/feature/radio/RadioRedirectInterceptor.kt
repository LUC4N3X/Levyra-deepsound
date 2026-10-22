package com.luc4n3x.levyra.feature.radio

import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response

internal class RadioRedirectInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        repeat(6) { hop ->
            if (!RadioUrlPolicy.isAllowed(request.url.toString())) throw IOException("Blocked unsafe radio URL")
            val response = chain.proceed(request)
            if (response.code !in redirectCodes) return response
            val location = response.header("Location")
            val target = location?.let(request.url::resolve)
            response.close()
            if (hop == 5 || target == null || !RadioUrlPolicy.isAllowed(target.toString())) {
                throw IOException("Invalid or unsafe radio redirect")
            }
            request = request.newBuilder().url(target).removeHeader("Authorization").removeHeader("Cookie").build()
        }
        throw IOException("Too many radio redirects")
    }

    private companion object {
        val redirectCodes = setOf(300, 301, 302, 303, 307, 308)
    }
}
