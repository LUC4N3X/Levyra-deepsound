package com.luc4n3x.levyra.data

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import com.luc4n3x.levyra.R
import java.io.File
import java.io.FileNotFoundException

class AutomaticBackupDocumentsProvider : DocumentsProvider() {
    override fun onCreate(): Boolean = true

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val cursor = MatrixCursor(rootColumns(projection))
        cursor.newRow()
            .add(Root.COLUMN_ROOT_ID, ROOT_ID)
            .add(Root.COLUMN_DOCUMENT_ID, ROOT_DOCUMENT_ID)
            .add(Root.COLUMN_TITLE, "Levyra")
            .add(Root.COLUMN_SUMMARY, "Backup automatici")
            .add(Root.COLUMN_FLAGS, Root.FLAG_LOCAL_ONLY)
            .add(Root.COLUMN_MIME_TYPES, BACKUP_MIME_TYPE)
            .add(Root.COLUMN_ICON, R.mipmap.ic_launcher)
        return cursor
    }

    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        val cursor = MatrixCursor(documentColumns(projection))
        if (documentId == ROOT_DOCUMENT_ID) {
            includeRootDocument(cursor)
        } else {
            includeBackupDocument(cursor, resolveBackupFile(documentId))
        }
        return cursor
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        if (parentDocumentId != ROOT_DOCUMENT_ID) throw FileNotFoundException("Cartella backup non valida")
        val cursor = MatrixCursor(documentColumns(projection))
        automaticBackupFiles().forEach { includeBackupDocument(cursor, it) }
        return cursor
    }

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        if (mode.any { it == 'w' || it == 'a' || it == '+' }) {
            throw FileNotFoundException("I backup automatici sono di sola lettura")
        }
        return ParcelFileDescriptor.open(resolveBackupFile(documentId), ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getDocumentType(documentId: String): String =
        if (documentId == ROOT_DOCUMENT_ID) Document.MIME_TYPE_DIR else BACKUP_MIME_TYPE

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
        if (parentDocumentId != ROOT_DOCUMENT_ID || documentId == ROOT_DOCUMENT_ID) return false
        return runCatching { resolveBackupFile(documentId) }.isSuccess
    }

    private fun includeRootDocument(cursor: MatrixCursor) {
        cursor.newRow()
            .add(Document.COLUMN_DOCUMENT_ID, ROOT_DOCUMENT_ID)
            .add(Document.COLUMN_DISPLAY_NAME, "Backup automatici")
            .add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR)
            .add(Document.COLUMN_FLAGS, 0)
    }

    private fun includeBackupDocument(cursor: MatrixCursor, file: File) {
        cursor.newRow()
            .add(Document.COLUMN_DOCUMENT_ID, FILE_DOCUMENT_PREFIX + file.name)
            .add(Document.COLUMN_DISPLAY_NAME, file.name)
            .add(Document.COLUMN_MIME_TYPE, BACKUP_MIME_TYPE)
            .add(Document.COLUMN_FLAGS, 0)
            .add(Document.COLUMN_LAST_MODIFIED, file.lastModified())
            .add(Document.COLUMN_SIZE, file.length())
    }

    private fun automaticBackupFiles(): List<File> {
        val directory = backupDirectory()
        return directory.listFiles().orEmpty()
            .filter { isSafeAutomaticBackup(directory, it) }
            .sortedByDescending(File::lastModified)
    }

    private fun resolveBackupFile(documentId: String): File {
        if (!documentId.startsWith(FILE_DOCUMENT_PREFIX)) throw FileNotFoundException("Backup non valido")
        val name = documentId.removePrefix(FILE_DOCUMENT_PREFIX)
        if (!isAutomaticBackupName(name)) throw FileNotFoundException("Backup non valido")
        val directory = backupDirectory()
        val file = File(directory, name).canonicalFile
        if (file.parentFile != directory || !file.isFile) throw FileNotFoundException("Backup non disponibile")
        return file
    }

    private fun backupDirectory(): File {
        val appContext = context ?: throw FileNotFoundException("Provider non disponibile")
        return File(appContext.filesDir, AUTOMATIC_BACKUP_DIRECTORY).canonicalFile
    }

    private fun isSafeAutomaticBackup(directory: File, file: File): Boolean {
        if (!isAutomaticBackupName(file.name) || !file.isFile) return false
        return runCatching { file.canonicalFile.parentFile == directory }.getOrDefault(false)
    }

    private fun isAutomaticBackupName(name: String): Boolean =
        name.startsWith(AUTOMATIC_BACKUP_PREFIX) &&
            name.endsWith(".zip") &&
            name.substring(AUTOMATIC_BACKUP_PREFIX.length, name.length - 4).all(Char::isDigit)

    private fun rootColumns(projection: Array<out String>?): Array<String> =
        projection?.map { it }?.toTypedArray() ?: DEFAULT_ROOT_PROJECTION

    private fun documentColumns(projection: Array<out String>?): Array<String> =
        projection?.map { it }?.toTypedArray() ?: DEFAULT_DOCUMENT_PROJECTION

    private companion object {
        const val ROOT_ID = "levyra-automatic-backups"
        const val ROOT_DOCUMENT_ID = "automatic-backups-root"
        const val FILE_DOCUMENT_PREFIX = "automatic-backup:"
        const val BACKUP_MIME_TYPE = "application/zip"
        const val AUTOMATIC_BACKUP_DIRECTORY = "backups"
        const val AUTOMATIC_BACKUP_PREFIX = "levyra-auto-backup-"

        val DEFAULT_ROOT_PROJECTION = arrayOf(
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_TITLE,
            Root.COLUMN_SUMMARY,
            Root.COLUMN_FLAGS,
            Root.COLUMN_MIME_TYPES,
            Root.COLUMN_ICON
        )

        val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_FLAGS,
            Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_SIZE
        )
    }
}
