package com.luc4n3x.levyra.domain

enum class PlaylistImportFailureKind {
    INVALID_INPUT,
    NOT_AVAILABLE,
    TOO_LARGE,
    NO_MATCHES,
    NETWORK,
    PROVIDER_CHANGED,
    STORAGE,
    UNSUPPORTED_SOURCE,
    AUTH_REQUIRED,
    NOT_A_PLAYLIST,
    RATE_LIMITED,
    FILE_MALFORMED,
    NO_USABLE_TRACKS
}
