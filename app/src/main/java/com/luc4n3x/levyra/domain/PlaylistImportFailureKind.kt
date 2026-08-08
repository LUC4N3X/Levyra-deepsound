package com.luc4n3x.levyra.domain

enum class PlaylistImportFailureKind {
    INVALID_INPUT,
    NOT_AVAILABLE,
    TOO_LARGE,
    NO_MATCHES,
    NETWORK,
    PROVIDER_CHANGED,
    STORAGE
}
