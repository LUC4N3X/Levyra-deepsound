package com.luc4n3x.levyra.viewmodel

internal const val USER_NAME_MAX_CODE_POINTS = 40

internal fun limitUserName(name: String): String {
    if (name.codePointCount(0, name.length) <= USER_NAME_MAX_CODE_POINTS) return name
    return name.substring(0, name.offsetByCodePoints(0, USER_NAME_MAX_CODE_POINTS))
}
