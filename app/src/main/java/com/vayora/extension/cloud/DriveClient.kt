package com.vayora.extension.cloud

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import org.json.JSONArray
import org.json.JSONObject

class DriveException(val code: Int, body: String) :
    IOException("HTTP $code: ${body.take(160)}")

data class CloudPhoto(
    val id: String,
    val name: String,
    val time: Long,
    val category: String,
    val eventId: Long?,
    val thumbnailLink: String?
)

class DriveClient(private val token: String) {

    fun aboutEmail(): String {
        val text = call("GET", "$API/about?fields=user(emailAddress)")
        return JSONObject(text).getJSONObject("user").getString("emailAddress")
    }

    fun ensureFolder(existingId: String?): String {
        if (existingId != null) {
            try {
                val text = call("GET", "$API/files/$existingId?fields=id,trashed,name")
                val json = JSONObject(text)
                if (!json.optBoolean("trashed", false)) {
                    if (json.optString("name", "") != FOLDER_NAME) renameFolder(existingId)
                    return json.getString("id")
                }
            } catch (e: DriveException) {
                if (e.code != 404) throw e
            }
        }
        val body = JSONObject()
            .put("name", FOLDER_NAME)
            .put("mimeType", "application/vnd.google-apps.folder")
        val text = call(
            "POST",
            "$API/files?fields=id",
            "application/json; charset=UTF-8",
            body.toString().toByteArray(Charsets.UTF_8)
        )
        return JSONObject(text).getString("id")
    }

    fun upload(
        name: String,
        mime: String,
        bytes: ByteArray,
        parentId: String,
        props: Map<String, String>
    ): String {
        val boundary = "vayora" + System.currentTimeMillis()
        val meta = JSONObject()
            .put("name", name)
            .put("parents", JSONArray().put(parentId))
            .put("appProperties", JSONObject(props))
        val out = ByteArrayOutputStream()
        out.write(
            "--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n"
                .toByteArray(Charsets.UTF_8)
        )
        out.write(meta.toString().toByteArray(Charsets.UTF_8))
        out.write("\r\n--$boundary\r\nContent-Type: $mime\r\n\r\n".toByteArray(Charsets.UTF_8))
        out.write(bytes)
        out.write("\r\n--$boundary--".toByteArray(Charsets.UTF_8))
        val text = call(
            "POST",
            "$UPLOAD_API/files?uploadType=multipart&fields=id",
            "multipart/related; boundary=$boundary",
            out.toByteArray()
        )
        return JSONObject(text).getString("id")
    }

    fun listPhotos(folderId: String): List<CloudPhoto> {
        val query = "'$folderId' in parents and trashed = false and " +
            "appProperties has { key='kind' and value='photo' }"
        val fields = "nextPageToken,files(id,name,createdTime,thumbnailLink,appProperties)"
        val result = mutableListOf<CloudPhoto>()
        var pageToken: String? = null
        var pages = 0
        do {
            var url = "$API/files?q=${URLEncoder.encode(query, "UTF-8")}" +
                "&fields=${URLEncoder.encode(fields, "UTF-8")}" +
                "&orderBy=createdTime%20desc&pageSize=100"
            if (pageToken != null) {
                url += "&pageToken=" + URLEncoder.encode(pageToken, "UTF-8")
            }
            val json = JSONObject(call("GET", url))
            val files = json.optJSONArray("files")
            if (files != null) {
                for (i in 0 until files.length()) {
                    val f = files.getJSONObject(i)
                    val props = f.optJSONObject("appProperties")
                    val captured = props?.optString("capturedAt", "")?.toLongOrNull()
                    val time = captured ?: parseDriveTime(f.optString("createdTime", ""))
                    result.add(
                        CloudPhoto(
                            id = f.getString("id"),
                            name = f.optString("name", ""),
                            time = time,
                            category = props?.optString("category", "Unknown") ?: "Unknown",
                            eventId = props?.optString("eventId", "")?.toLongOrNull(),
                            thumbnailLink = f.optString("thumbnailLink", "").ifEmpty { null }
                        )
                    )
                }
            }
            pageToken = json.optString("nextPageToken", "").ifEmpty { null }
            pages++
        } while (pageToken != null && pages < 10)
        return result
    }

    fun downloadMedia(fileId: String): ByteArray =
        callBytes("GET", "$API/files/$fileId?alt=media")

    fun fetchBytes(url: String): ByteArray = callBytes("GET", url)

    fun deleteFile(fileId: String) {
        try {
            callBytes("DELETE", "$API/files/$fileId")
        } catch (e: DriveException) {
            if (e.code != 404) throw e
        }
    }

    private fun renameFolder(id: String) {
        try {
            val body = JSONObject().put("name", FOLDER_NAME)
            callBytes(
                "POST",
                "$API/files/$id?fields=id",
                "application/json; charset=UTF-8",
                body.toString().toByteArray(Charsets.UTF_8),
                mapOf("X-HTTP-Method-Override" to "PATCH")
            )
        } catch (e: IOException) {
            // keep the old folder name; renaming is cosmetic
        }
    }

    private fun call(
        method: String,
        url: String,
        contentType: String? = null,
        body: ByteArray? = null
    ): String = String(callBytes(method, url, contentType, body), Charsets.UTF_8)

    private fun callBytes(
        method: String,
        url: String,
        contentType: String? = null,
        body: ByteArray? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): ByteArray {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 20_000
            conn.readTimeout = 60_000
            conn.setRequestProperty("Authorization", "Bearer $token")
            for ((name, value) in extraHeaders) {
                conn.setRequestProperty(name, value)
            }
            if (body != null) {
                conn.doOutput = true
                if (contentType != null) {
                    conn.setRequestProperty("Content-Type", contentType)
                }
                conn.setFixedLengthStreamingMode(body.size)
                conn.outputStream.use { it.write(body) }
            }
            val code = conn.responseCode
            if (code in 200..299) {
                val stream = conn.inputStream
                return stream?.use { it.readBytes() } ?: ByteArray(0)
            }
            val errorText = conn.errorStream?.use { String(it.readBytes(), Charsets.UTF_8) } ?: ""
            throw DriveException(code, errorText)
        } finally {
            conn.disconnect()
        }
    }

    private fun parseDriveTime(text: String): Long {
        if (text.isEmpty()) return 0L
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(text)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    companion object {
        const val FOLDER_NAME = "Sentinel Evidence"
        private const val API = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD_API = "https://www.googleapis.com/upload/drive/v3"
    }
}
