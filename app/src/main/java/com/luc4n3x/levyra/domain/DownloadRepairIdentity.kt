package com.luc4n3x.levyra.domain

import org.json.JSONObject

private const val RepairPayloadKey = "levyraRepairDownloadId"

internal fun downloadRepairTaskKey(downloadId: Long): String = "health-repair-$downloadId"

internal fun withDownloadRepairIdentity(payload: String, downloadId: Long): String =
    if (downloadId > 0L) JSONObject(payload).put(RepairPayloadKey, downloadId).toString() else payload

internal fun downloadRepairIdentity(payload: String): Long =
    runCatching { JSONObject(payload).optLong(RepairPayloadKey, 0L).coerceAtLeast(0L) }.getOrDefault(0L)
