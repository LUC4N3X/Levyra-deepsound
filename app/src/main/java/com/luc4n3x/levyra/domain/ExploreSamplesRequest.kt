package com.luc4n3x.levyra.domain

const val EXPLORE_SAMPLES_REQUEST = "samples:"

fun exploreSamplesRequestIndex(request: String): Int? =
    request.takeIf { it.startsWith(EXPLORE_SAMPLES_REQUEST) }?.removePrefix(EXPLORE_SAMPLES_REQUEST)?.toIntOrNull()
