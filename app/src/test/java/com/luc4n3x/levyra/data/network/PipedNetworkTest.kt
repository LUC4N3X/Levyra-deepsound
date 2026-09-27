package com.luc4n3x.levyra.data.network

import java.net.InetAddress
import java.net.UnknownHostException
import okhttp3.Dns
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PipedNetworkTest {
    @Test
    fun acceptsHttpsProxyUrlAndPreservesSignedQueryShape() {
        val signed = "https://proxy.example/videoplayback?n=a%2Bb%3D&range=0-"

        assertTrue(PipedStreamUrlPolicy.isValidProxyStreamUrl(signed, "https://proxy.example"))
        assertTrue(signed.contains("n=a%2Bb%3D"))
    }

    @Test
    fun rejectsCleartextCredentialsAndCrossHostProxyUrls() {
        assertFalse(
            PipedStreamUrlPolicy.isValidProxyStreamUrl(
                "http://proxy.example/videoplayback?n=signed",
                "https://proxy.example"
            )
        )
        assertFalse(
            PipedStreamUrlPolicy.isValidProxyStreamUrl(
                "https://user:pass@proxy.example/videoplayback",
                "https://proxy.example"
            )
        )
        assertFalse(
            PipedStreamUrlPolicy.isValidProxyStreamUrl(
                "https://other.example/videoplayback",
                "https://proxy.example"
            )
        )
    }

    @Test
    fun publicDnsFiltersPrivateAnswers() {
        val public = InetAddress.getByName("93.184.216.34")
        val private = InetAddress.getByName("127.0.0.1")
        val dns = PipedPublicDns(Dns { listOf(private, public) })

        assertEquals(listOf(public), dns.lookup("proxy.example"))
    }

    @Test(expected = UnknownHostException::class)
    fun publicDnsRejectsPrivateOnlyAnswers() {
        val dns = PipedPublicDns(Dns { listOf(InetAddress.getByName("192.168.1.10")) })

        dns.lookup("proxy.example")
    }
}
