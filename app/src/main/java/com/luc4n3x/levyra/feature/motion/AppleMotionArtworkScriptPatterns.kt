package com.luc4n3x.levyra.feature.motion

internal val SCRIPT_SRC_REGEX = Regex(
    """(?i)<script\b[^>]*\bsrc\s*=\s*[\"']([^\"']+\.js(?:\?[^\"']*)?)[\"']"""
)

internal val LEGACY_SCRIPT_REGEX = Regex(
    "[\\\"']([^\\\"']*/assets/index[^\\\"']*\\.js)[\\\"']"
)
