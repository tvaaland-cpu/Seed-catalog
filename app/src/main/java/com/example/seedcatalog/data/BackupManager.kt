package com.example.seedcatalog.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.example.seedcatalog.data.local.AppDatabase
import com.example.seedcatalog.data.local.Note
import com.example.seedcatalog.data.local.PacketLot
import com.example.seedcatalog.data.local.Photo
import com.example.seedcatalog.data.local.Plant
import com.example.seedcatalog.data.local.SourceAttribution
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManager(
    private val context: Context,
    private val db: AppDatabase
) {
    data class OperationResult(
        val warningMessage: String? = null
    )

    suspend fun exportToZip(destinationUri: Uri): OperationResult {
        val plants = db.plantDao().getAll()
        val packetLots = db.packetLotDao().getAll()
        val notes = db.noteDao().getAll()
        val sourceAttributions = db.sourceAttributionDao().getAll()
        val photos = db.photoDao().getAll()

        var imagesCopied = 0
        val photoPayload = JSONArray()

        context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
            ZipOutputStream(outputStream).use { zip ->
                zip.putNextEntry(ZipEntry("plants.json"))
                zip.write(plants.toJsonArray().toString(2).toByteArray())
                zip.closeEntry()

                zip.putNextEntry(ZipEntry("packet_lots.json"))
                zip.write(packetLots.toJsonArray().toString(2).toByteArray())
                zip.closeEntry()

                zip.putNextEntry(ZipEntry("notes.json"))
                zip.write(notes.toJsonArray().toString(2).toByteArray())
                zip.closeEntry()

                zip.putNextEntry(ZipEntry("source_attributions.json"))
                zip.write(sourceAttributions.toJsonArray().toString(2).toByteArray())
                zip.closeEntry()

                photos.forEach { photo ->
                    val imageEntry = tryCopyImageToZip(zip, context.contentResolver, photo)
                    if (imageEntry != null) imagesCopied += 1
                    photoPayload.put(photo.toJson(imageEntry))
                }

                zip.putNextEntry(ZipEntry("photos.json"))
                zip.write(photoPayload.toString(2).toByteArray())
                zip.closeEntry()
            }
        }

        val warning = if (imagesCopied < photos.size) {
            "Export finished with warnings: copied $imagesCopied/${photos.size} image files. " +
                "Remaining photos keep their original URI references."
        } else {
            null
        }
        return OperationResult(warning)
    }

    suspend fun restoreFromZip(sourceUri: Uri): OperationResult {
        val zipEntries = readZipEntries(sourceUri)
        val plants = zipEntries.readJsonArray("plants.json").toPlants()
        val packetLots = zipEntries.readJsonArray("packet_lots.json").toPacketLots()
        val notes = zipEntries.readJsonArray("notes.json").toNotes()
        val sources = zipEntries.readJsonArray("source_attributions.json").toSourceAttributions()
        val photoBackups = zipEntries.readJsonArray("photos.json").toPhotoBackups()

        var copiedImageCount = 0
        val restoredPhotos = photoBackups.map { backup ->
            val restoredUri = backup.imagePath?.let { path ->
                zipEntries[path]?.let { imageBytes ->
                    copiedImageCount += 1
                    persistPhotoBytes(imageBytes, backup.id)
                }
            } ?: backup.uri

            Photo(
                id = backup.id,
                packetLotId = backup.packetLotId,
                uri = restoredUri,
                type = backup.type
            )
        }

        db.withTransaction {
            db.clearAllTables()
            db.plantDao().insertAll(plants)
            db.packetLotDao().insertAll(packetLots)
            db.noteDao().insertAll(notes)
            db.sourceAttributionDao().insertAll(sources)
            db.photoDao().insertAll(restoredPhotos)
        }

        val warning = if (copiedImageCount < photoBackups.size) {
            "Restore finished with warnings: restored embedded files for $copiedImageCount/${photoBackups.size} photos. " +
                "Other photos keep backup URI references."
        } else {
            null
        }
        return OperationResult(warning)
    }

    private fun readZipEntries(sourceUri: Uri): Map<String, ByteArray> {
        val entries = mutableMapOf<String, ByteArray>()
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val bytes = ByteArrayOutputStream()
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = zip.read(buffer)
                            if (read <= 0) break
                            bytes.write(buffer, 0, read)
                        }
                        entries[entry.name] = bytes.toByteArray()
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        return entries
    }

    private fun Map<String, ByteArray>.readJsonArray(fileName: String): JSONArray {
        val bytes = getValue(fileName)
        return JSONArray(String(bytes))
    }

    private fun tryCopyImageToZip(
        zip: ZipOutputStream,
        resolver: ContentResolver,
        photo: Photo
    ): String? {
        val sourceUri = photo.uri.toUri()
        val extension = resolver.getType(sourceUri)
            ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            ?.ifBlank { null }
            ?: "jpg"
        val entryName = "images/photo_${photo.id}.$extension"

        val bytes = resolver.openInputStream(sourceUri)?.use { it.readBytes() } ?: return null
        zip.putNextEntry(ZipEntry(entryName))
        zip.write(bytes)
        zip.closeEntry()
        return entryName
    }

    private fun persistPhotoBytes(bytes: ByteArray, photoId: Int): String {
        val saved = context.openFileOutput("restored_photo_$photoId.jpg", Context.MODE_PRIVATE).use {
            it.write(bytes)
        }
        return Uri.fromFile(context.getFileStreamPath("restored_photo_$photoId.jpg")).toString()
    }

    private data class PhotoBackup(
        val id: Int,
        val packetLotId: Int,
        val uri: String,
        val type: String,
        val imagePath: String?
    )

    private fun Plant.toJson() = JSONObject()
        .put("id", id)
        .put("botanicalName", botanicalName)
        .put("commonName", commonName)
        .put("variety", variety)
        .put("plantType", plantType)
        .put("lightRequirement", lightRequirement)
        .put("indoorOutdoor", indoorOutdoor)
        .put("description", description)
        .put("medicinalUses", medicinalUses)
        .put("culinaryUses", culinaryUses)
        .put("growingInstructions", growingInstructions)
        .put("notes", notes)

    private fun PacketLot.toJson() = JSONObject()
        .put("id", id)
        .put("plantId", plantId)
        .put("lotCode", lotCode)
        .put("quantity", quantity)
        .put("notes", notes)

    private fun Note.toJson() = JSONObject()
        .put("id", id)
        .put("plantId", plantId)
        .put("packetLotId", packetLotId)
        .put("content", content)
        .put("createdAtEpochMs", createdAtEpochMs)

    private fun SourceAttribution.toJson() = JSONObject()
        .put("id", id)
        .put("plantId", plantId)
        .put("sourceName", sourceName)
        .put("url", url)

    private fun Photo.toJson(imagePath: String?) = JSONObject()
        .put("id", id)
        .put("packetLotId", packetLotId)
        .put("uri", uri)
        .put("type", type)
        .put("imagePath", imagePath)

    private fun List<Plant>.toJsonArray() = JSONArray().apply { forEach { put(it.toJson()) } }
    private fun List<PacketLot>.toJsonArray() = JSONArray().apply { forEach { put(it.toJson()) } }
    private fun List<Note>.toJsonArray() = JSONArray().apply { forEach { put(it.toJson()) } }
    private fun List<SourceAttribution>.toJsonArray() = JSONArray().apply { forEach { put(it.toJson()) } }

    private fun JSONArray.toPlants(): List<Plant> = List(length()) { index ->
        val item = getJSONObject(index)
        Plant(
            id = item.getInt("id"),
            botanicalName = item.getString("botanicalName"),
            commonName = item.getString("commonName"),
            variety = item.getString("variety"),
            plantType = item.getString("plantType"),
            lightRequirement = item.getString("lightRequirement"),
            indoorOutdoor = item.getString("indoorOutdoor"),
            description = item.getString("description"),
            medicinalUses = item.getString("medicinalUses"),
            culinaryUses = item.getString("culinaryUses"),
            growingInstructions = item.getString("growingInstructions"),
            notes = item.getString("notes")
        )
    }

    private fun JSONArray.toPacketLots(): List<PacketLot> = List(length()) { index ->
        val item = getJSONObject(index)
        PacketLot(
            id = item.getInt("id"),
            plantId = item.getInt("plantId"),
            lotCode = item.getString("lotCode"),
            quantity = item.getInt("quantity"),
            notes = item.getString("notes")
        )
    }

    private fun JSONArray.toNotes(): List<Note> = List(length()) { index ->
        val item = getJSONObject(index)
        Note(
            id = item.getInt("id"),
            plantId = item.optInt("plantId").takeIf { !item.isNull("plantId") },
            packetLotId = item.optInt("packetLotId").takeIf { !item.isNull("packetLotId") },
            content = item.getString("content"),
            createdAtEpochMs = item.getLong("createdAtEpochMs")
        )
    }

    private fun JSONArray.toSourceAttributions(): List<SourceAttribution> = List(length()) { index ->
        val item = getJSONObject(index)
        SourceAttribution(
            id = item.getInt("id"),
            plantId = item.getInt("plantId"),
            sourceName = item.getString("sourceName"),
            url = item.getString("url")
        )
    }

    private fun JSONArray.toPhotoBackups(): List<PhotoBackup> = List(length()) { index ->
        val item = getJSONObject(index)
        PhotoBackup(
            id = item.getInt("id"),
            packetLotId = item.getInt("packetLotId"),
            uri = item.getString("uri"),
            type = item.getString("type"),
            imagePath = item.optString("imagePath").ifBlank { null }
        )
    }
}
