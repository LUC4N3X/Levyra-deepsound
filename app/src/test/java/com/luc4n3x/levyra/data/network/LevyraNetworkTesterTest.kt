package com.luc4n3x.levyra.data.network

import com.luc4n3x.levyra.domain.LevyraNetworkTestOutcome
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import org.junit.Assert.assertEquals
import org.junit.Test

class LevyraNetworkTesterTest {

    @Test
    fun classifiesDirectAndNestedNetworkFailures() {
        assertEquals(
            LevyraNetworkTestOutcome.DnsResolutionFailed,
            LevyraNetworkTester.classify(UnknownHostException("dns"))
        )
        assertEquals(
            LevyraNetworkTestOutcome.Timeout,
            LevyraNetworkTester.classify(IOException("outer", SocketTimeoutException("timeout")))
        )
        assertEquals(
            LevyraNetworkTestOutcome.Timeout,
            LevyraNetworkTester.classify(InterruptedIOException("interrupted"))
        )
        assertEquals(
            LevyraNetworkTestOutcome.ConnectionRefused,
            LevyraNetworkTester.classify(ConnectException("refused"))
        )
        assertEquals(
            LevyraNetworkTestOutcome.TlsFailure,
            LevyraNetworkTester.classify(SSLHandshakeException("tls"))
        )
        assertEquals(
            LevyraNetworkTestOutcome.ProxyAuthenticationFailed,
            LevyraNetworkTester.classify(IOException("Failed to authenticate with proxy"))
        )
        assertEquals(
            LevyraNetworkTestOutcome.UnknownError,
            LevyraNetworkTester.classify(IllegalStateException("other"))
        )
    }
}
