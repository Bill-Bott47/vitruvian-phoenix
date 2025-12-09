package com.devil.phoenixproject.util

import com.devil.phoenixproject.database.VitruvianDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import platform.Foundation.*

/**
 * iOS implementation of DataBackupManager.
 * Uses NSFileManager for file operations and Documents directory for storage.
 */
@OptIn(ExperimentalForeignApi::class)
class IosDataBackupManager(
    database: VitruvianDatabase
) : BaseDataBackupManager(database) {

    private val fileManager = NSFileManager.defaultManager

    private val documentsDirectory: String
        get() {
            val paths = NSSearchPathForDirectoriesInDomains(
                NSDocumentDirectory,
                NSUserDomainMask,
                true
            )
            return paths.firstOrNull() as? String ?: ""
        }

    private val backupDirectory: String
        get() {
            val dir = "$documentsDirectory/VitruvianBackups"
            val url = NSURL.fileURLWithPath(dir)
            if (!fileManager.fileExistsAtPath(dir)) {
                fileManager.createDirectoryAtURL(
                    url,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = null
                )
            }
            return dir
        }

    override suspend fun saveToFile(backup: BackupData): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonString = json.encodeToString(backup)
            val timestamp = KmpUtils.formatTimestamp(KmpUtils.currentTimeMillis(), "yyyy-MM-dd")
                .replace("-", "") + "_" +
                KmpUtils.formatTimestamp(KmpUtils.currentTimeMillis(), "HH:mm:ss")
                    .replace(":", "")
            val fileName = "vitruvian_backup_$timestamp.json"
            val filePath = "$backupDirectory/$fileName"

            val data = (jsonString as NSString).dataUsingEncoding(NSUTF8StringEncoding)
                ?: throw Exception("Failed to encode backup data")

            val success = data.writeToFile(filePath, atomically = true)
            if (!success) {
                throw Exception("Failed to write backup file")
            }

            Result.success(filePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importFromFile(filePath: String): Result<ImportResult> = withContext(Dispatchers.IO) {
        try {
            val data = NSData.dataWithContentsOfFile(filePath)
                ?: throw Exception("Cannot read file")

            val jsonString = NSString.create(data, NSUTF8StringEncoding) as? String
                ?: throw Exception("Cannot decode file contents")

            importFromJson(jsonString)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get list of available backup files
     */
    fun getAvailableBackups(): List<String> {
        return try {
            val contents = fileManager.contentsOfDirectoryAtPath(backupDirectory, null)
            (contents as? List<*>)
                ?.filterIsInstance<String>()
                ?.filter { it.endsWith(".json") }
                ?.map { "$backupDirectory/$it" }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
