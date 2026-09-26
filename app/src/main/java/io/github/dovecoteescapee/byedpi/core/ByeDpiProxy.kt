package io.github.dovecoteescapee.byedpi.core

class ByeDpiProxy {
    companion object {
        @Volatile
        private var libraryLoaded: Boolean? = null

        fun isAvailable(): Boolean {
            libraryLoaded?.let { return it }
            return synchronized(this) {
                libraryLoaded ?: run {
                    val loaded = try {
                        val libName = System.mapLibraryName("byedpi")
                        System.loadLibrary("byedpi")
                        timber.log.Timber.i("Native library %s loaded successfully", libName)
                        true
                    } catch (t: Throwable) {
                        timber.log.Timber.w(t, "Native byedpi library could not be loaded")
                        false
                    }
                    libraryLoaded = loaded
                    loaded
                }
            }
        }
    }

    private val lock = Any()
    @Volatile
    private var fd = -1

    fun startProxy(preferences: ByeDpiProxyPreferences): Int {
        if (!isAvailable()) return -1
        val socketFd = createSocket(preferences)
        if (socketFd < 0) {
            return -1
        }
        return try {
            jniStartProxy(socketFd)
        } catch (_: Throwable) {
            -1
        }
    }

    fun stopProxy(): Int {
        if (!isAvailable()) return 0
        return synchronized(lock) {
            val currentFd = fd
            if (currentFd < 0) {
                return@synchronized 0
            }
            val result = try {
                jniStopProxy(currentFd)
            } catch (_: Throwable) {
                -1
            }
            if (result == 0) {
                fd = -1
            }
            result
        }
    }

    private fun createSocket(preferences: ByeDpiProxyPreferences): Int =
        synchronized(lock) {
            if (fd >= 0) {
                return@synchronized fd
            }
            val created = try {
                createSocketFromPreferences(preferences)
            } catch (_: Throwable) {
                -1
            }
            if (created < 0) {
                return@synchronized -1
            }
            fd = created
            created
        }

    private fun createSocketFromPreferences(preferences: ByeDpiProxyPreferences): Int =
        when (preferences) {
            is ByeDpiProxyCmdPreferences -> jniCreateSocketWithCommandLine(preferences.args)
            is ByeDpiProxyUIPreferences -> jniCreateSocket(
                ip = preferences.ip,
                port = preferences.port,
                maxConnections = preferences.maxConnections,
                bufferSize = preferences.bufferSize,
                defaultTtl = preferences.defaultTtl,
                customTtl = preferences.customTtl,
                noDomain = preferences.noDomain,
                desyncHttp = preferences.desyncHttp,
                desyncHttps = preferences.desyncHttps,
                desyncUdp = preferences.desyncUdp,
                desyncMethod = preferences.desyncMethod.ordinal,
                splitPosition = preferences.splitPosition,
                splitAtHost = preferences.splitAtHost,
                fakeTtl = preferences.fakeTtl,
                fakeSni = preferences.fakeSni,
                oobChar = preferences.oobChar,
                hostMixedCase = preferences.hostMixedCase,
                domainMixedCase = preferences.domainMixedCase,
                hostRemoveSpaces = preferences.hostRemoveSpaces,
                tlsRecordSplit = preferences.tlsRecordSplit,
                tlsRecordSplitPosition = preferences.tlsRecordSplitPosition,
                tlsRecordSplitAtSni = preferences.tlsRecordSplitAtSni,
                hostsMode = preferences.hostsMode.ordinal,
                hosts = preferences.hosts,
                tcpFastOpen = preferences.tcpFastOpen,
                udpFakeCount = preferences.udpFakeCount,
                dropSack = preferences.dropSack,
                fakeOffset = preferences.fakeOffset,
            )
        }

    private external fun jniCreateSocketWithCommandLine(args: Array<String>): Int

    private external fun jniCreateSocket(
        ip: String,
        port: Int,
        maxConnections: Int,
        bufferSize: Int,
        defaultTtl: Int,
        customTtl: Boolean,
        noDomain: Boolean,
        desyncHttp: Boolean,
        desyncHttps: Boolean,
        desyncUdp: Boolean,
        desyncMethod: Int,
        splitPosition: Int,
        splitAtHost: Boolean,
        fakeTtl: Int,
        fakeSni: String,
        oobChar: Byte,
        hostMixedCase: Boolean,
        domainMixedCase: Boolean,
        hostRemoveSpaces: Boolean,
        tlsRecordSplit: Boolean,
        tlsRecordSplitPosition: Int,
        tlsRecordSplitAtSni: Boolean,
        hostsMode: Int,
        hosts: String?,
        tcpFastOpen: Boolean,
        udpFakeCount: Int,
        dropSack: Boolean,
        fakeOffset: Int,
    ): Int

    private external fun jniStartProxy(fd: Int): Int

    private external fun jniStopProxy(fd: Int): Int
}
