package com.luc4n3x.levyra.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract

internal object DownloadFolderAccess {
    fun persist(context: Context, uri: Uri): Boolean {
        val resolver = context.contentResolver
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        return runCatching {
            resolver.takePersistableUriPermission(uri, flags)
            canWrite(context, uri.toString())
        }.getOrDefault(false)
    }

    fun canWrite(context: Context, rawUri: String): Boolean {
        val treeUri = parseTreeUri(rawUri) ?: return false
        if (!hasPersistedWritePermission(context, treeUri)) return false
        val documentUri = documentUri(treeUri) ?: return false
        return runCatching {
            context.contentResolver.query(
                documentUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_FLAGS
                ),
                null,
                null,
                null
            )?.use { cursor ->
                cursor.moveToFirst() &&
                    cursor.getString(0) == DocumentsContract.Document.MIME_TYPE_DIR &&
                    cursor.getInt(1) and DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE != 0
            } == true
        }.getOrDefault(false)
    }

    fun displayName(context: Context, rawUri: String): String? {
        val treeUri = parseTreeUri(rawUri) ?: return null
        val documentUri = documentUri(treeUri) ?: return null
        return runCatching {
            context.contentResolver.query(
                documentUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0)?.trim()?.takeIf(String::isNotBlank) else null
            }
        }.getOrNull()
    }

    fun parseTreeUri(rawUri: String): Uri? {
        val value = rawUri.trim()
        if (value.isBlank()) return null
        return runCatching { Uri.parse(value) }
            .getOrNull()
            ?.takeIf { it.scheme.equals("content", ignoreCase = true) }
    }

    fun documentUri(treeUri: Uri): Uri? = runCatching {
        DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )
    }.getOrNull()

    private fun hasPersistedWritePermission(context: Context, treeUri: Uri): Boolean =
        context.contentResolver.persistedUriPermissions.any { permission ->
            permission.isReadPermission && permission.isWritePermission && permission.uri == treeUri
        }
}
