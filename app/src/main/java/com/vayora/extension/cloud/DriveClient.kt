package com.vayora.extension.cloud

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

class DriveException(val code: Int, body: String) :
    IOException("HTTP $code: ${body.take(160)}")

class DriveClient(private val token: String) {

    fun aboutEmail(): String {
        val text = call("GET", "$API/about?fields=user(emailAddress)")
        return JSONObject(text).getJSONObject("user").getString("emailAddress")
    }

    fun ensureFolder(existingId: String?): String {
        if (existingId != null) {
            try {
                val text = call("GET", "$API/files/$existingId?fields=id,trashed")
                val json = JSONObject(text)
                if (!json.optBoolean("trashed", false)) {
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

    private fun call(
        method: String,
        url: String,
        contentType: String? = null,
        body: ByteArray? = null
    ): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 20_000
            conn.readTimeout = 60_000
            conn.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                conn.doOutput = true
                if (contentType != null) {
                    conn.setRequestProperty("Content-Type", contentType)
                }
                conn.setFixedLengthStreamingMode(body.size)
                conn.outputStream.use { it.write(body) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw DriveException(code, text)
            return text
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        const val FOLDER_NAME = "Vayora Extension Evidence"
        private const val API = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD_API = "https://www.googleapis.com/upload/drive/v3"
    }
}
