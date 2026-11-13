package com.luc4n3x.levyra.domain

const val EXPLORE_SAMPLES_REQUEST = "samples:"
const val EXPLORE_PODCASTS_REQUEST = "podcasts"

fun exploreSamplesRequestIndex(request: String): Int? =
    request.takeIf { it.startsWith(EXPLORE_SAMPLES_REQUEST) }?.removePrefix(EXPLORE_SAMPLES_REQUEST)?.toIntOrNull()
