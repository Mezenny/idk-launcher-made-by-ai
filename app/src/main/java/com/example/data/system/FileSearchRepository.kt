package com.example.data.system

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.example.data.local.FileIndexDao
import com.example.data.local.IndexedFileEntity
import com.example.data.local.IndexedFolderEntity
import com.example.model.IndexedFileItem
import com.example.model.IndexedFolderSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Stack

class FileSearchRepository(
    private val context: Context,
    private val fileIndexDao: FileIndexDao
) {

    val indexedFolders: Flow<List<IndexedFolderSummary>> =
        fileIndexDao.observeFolders().map { list ->
            list.map {
                IndexedFolderSummary(
                    treeUriString = it.treeUriString,
                    displayName = it.displayName,
                    addedAt = it.addedAt,
                    fileCount = it.fileCount
                )
            }
        }

    val indexedFiles: Flow<List<IndexedFileItem>> =
        fileIndexDao.observeAllFiles().map { list ->
            list.map {
                IndexedFileItem(
                    uriString = it.uriString,
                    name = it.name,
                    mimeType = it.mimeType,
                    sizeBytes = it.sizeBytes,
                    lastModified = it.lastModified,
                    folderName = it.folderName
                )
            }
        }

    val indexedFileCount: Flow<Int> = fileIndexDao.observeFileCount()

    suspend fun indexSafFolder(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, flags)
        } catch (_: SecurityException) {
            // Some providers grant transient access or already persisted
        }

        val folderName = resolveTreeDisplayName(treeUri)
        val treeUriString = treeUri.toString()
        val collected = ArrayList<IndexedFileEntity>(128)
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)

        // Iterative DFS bounded by max files/depth to keep indexing fast & safe
        data class DirNode(val docId: String, val depth: Int)
        val stack = Stack<DirNode>()
        stack.push(DirNode(rootDocId, 0))

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        while (stack.isNotEmpty() && collected.size < 1500) {
            val node = stack.pop()
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, node.docId)
            var cursor: Cursor? = null
            try {
                cursor = context.contentResolver.query(childrenUri, projection, null, null, null)
                if (cursor != null) {
                    val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                    val modIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                    while (cursor.moveToNext() && collected.size < 1500) {
                        val docId = if (idIdx >= 0) cursor.getString(idIdx) else continue
                        val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "Untitled" else "Untitled"
                        val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx) ?: "*/*" else "*/*"
                        val size = if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) cursor.getLong(sizeIdx) else 0L
                        val mod = if (modIdx >= 0 && !cursor.isNull(modIdx)) cursor.getLong(modIdx) else 0L

                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            if (node.depth < 3 && !name.startsWith(".")) {
                                stack.push(DirNode(docId, node.depth + 1))
                            }
                        } else {
                            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            collected.add(
                                IndexedFileEntity(
                                    uriString = docUri.toString(),
                                    name = name,
                                    mimeType = mime,
                                    sizeBytes = size,
                                    lastModified = mod,
                                    folderUri = treeUriString,
                                    folderName = folderName
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                cursor?.close()
            }
        }

        fileIndexDao.deleteFilesForFolder(treeUriString)
        if (collected.isNotEmpty()) {
            fileIndexDao.insertFiles(collected)
        }
        fileIndexDao.upsertFolder(
            IndexedFolderEntity(
                treeUriString = treeUriString,
                displayName = folderName,
                addedAt = System.currentTimeMillis(),
                fileCount = collected.size
            )
        )
        collected.size
    }

    suspend fun indexSinglePickedFile(fileUri: Uri) = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.takePersistableUriPermission(
                fileUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }

        var name = "Document"
        var size = 0L
        val mime = context.contentResolver.getType(fileUri) ?: "*/*"

        context.contentResolver.query(fileUri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameCol = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameCol >= 0) name = cursor.getString(nameCol) ?: name
                if (sizeCol >= 0 && !cursor.isNull(sizeCol)) size = cursor.getLong(sizeCol)
            }
        }

        fileIndexDao.insertFiles(
            listOf(
                IndexedFileEntity(
                    uriString = fileUri.toString(),
                    name = name,
                    mimeType = mime,
                    sizeBytes = size,
                    lastModified = System.currentTimeMillis(),
                    folderUri = "pinned_files",
                    folderName = "Pinned Files"
                )
            )
        )
    }

    suspend fun removeIndexedFolder(treeUriString: String) = withContext(Dispatchers.IO) {
        fileIndexDao.deleteFilesForFolder(treeUriString)
        fileIndexDao.deleteFolder(treeUriString)
    }

    suspend fun rescanFolder(treeUriString: String): Int {
        return indexSafFolder(Uri.parse(treeUriString))
    }

    fun openFile(file: IndexedFileItem): Boolean {
        return try {
            val uri = Uri.parse(file.uriString)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, file.mimeType.takeIf { it.isNotBlank() } ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open ${file.name}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun resolveTreeDisplayName(treeUri: Uri): String {
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        context.contentResolver.query(
            docUri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                if (idx >= 0) {
                    val name = cursor.getString(idx)
                    if (!name.isNullOrBlank()) return name
                }
            }
        }
        return docId.substringAfterLast(':').ifBlank { "Folder" }
    }

    companion object {
        fun evaluateQuickMath(rawQuery: String): String? {
            val expr = rawQuery.trim()
            if (expr.length < 3 || !expr.any { it in "+-*/%" }) return null
            if (!expr.all { it.isDigit() || it in "+-*/().% " }) return null
            return try {
                val result = MathParser(expr).parse()
                if (result.isNaN() || result.isInfinite()) return null
                if (result % 1.0 == 0.0 && result in -1e12..1e12) {
                    result.toLong().toString()
                } else {
                    String.format(java.util.Locale.US, "%.4f", result)
                        .trimEnd('0')
                        .trimEnd('.')
                }
            } catch (_: Exception) {
                null
            }
        }

        fun formatFileSize(bytes: Long): String {
            if (bytes <= 0L) return "—"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(java.util.Locale.US, "%.1f GB", gb)
                mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
                kb >= 1.0 -> String.format(java.util.Locale.US, "%.0f KB", kb)
                else -> "$bytes B"
            }
        }
    }

    private class MathParser(private val input: String) {
        private var pos = -1
        private var ch = 0

        private fun nextChar() {
            ch = if (++pos < input.length) input[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < input.length) throw IllegalArgumentException("Unexpected character")
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> {
                        val d = parseFactor()
                        if (d == 0.0) throw ArithmeticException("Division by zero")
                        x /= d
                    }
                    eat('%'.code) -> x %= parseFactor()
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()
            val startPos = pos
            val x: Double
            if (eat('('.code)) {
                x = parseExpression()
                if (!eat(')'.code)) throw IllegalArgumentException("Missing ')'")
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                x = input.substring(startPos, pos).toDouble()
            } else {
                throw IllegalArgumentException("Unexpected factor")
            }
            return x
        }
    }
}
