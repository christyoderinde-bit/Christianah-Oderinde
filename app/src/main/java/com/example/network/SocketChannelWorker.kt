package com.example.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter

private const val TAG = "SocketChannelWorker"

class SocketChannelWorker(
    private val inputStream: InputStream,
    private val outputStream: OutputStream,
    private val underlyingCloseable: Closeable?,
    private val scope: CoroutineScope,
    private val onMessage: (WireMessage) -> Unit,
    private val onDisconnected: (String) -> Unit
) {
    private val writer = BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8))
    private var readJob: Job? = null
    @Volatile
    private var isRunning = true

    fun start() {
        isRunning = true
        readJob = scope.launch(Dispatchers.IO) {
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            try {
                while (isRunning) {
                    val line = reader.readLine() ?: break
                    if (line.isNotBlank()) {
                        val msg = WireMessage.fromJson(line)
                        if (msg != null) {
                            withContext(Dispatchers.Main) {
                                onMessage(msg)
                            }
                        }
                    }
                }
                if (isRunning) {
                    withContext(Dispatchers.Main) {
                        onDisconnected("Remote peer closed the connection")
                    }
                }
            } catch (e: Exception) {
                if (isRunning) {
                    Log.e(TAG, "Read error: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        onDisconnected(e.message ?: "Connection dropped")
                    }
                }
            } finally {
                close()
            }
        }
    }

    suspend fun sendMessage(message: WireMessage): Boolean = withContext(Dispatchers.IO) {
        if (!isRunning) return@withContext false
        try {
            val serialized = message.toJson() + "\n"
            synchronized(writer) {
                writer.write(serialized)
                writer.flush()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Write error: ${e.message}", e)
            withContext(Dispatchers.Main) {
                onDisconnected("Failed to send message: ${e.message}")
            }
            false
        }
    }

    fun close() {
        isRunning = false
        readJob?.cancel()
        try {
            writer.close()
        } catch (_: Exception) {}
        try {
            inputStream.close()
        } catch (_: Exception) {}
        try {
            outputStream.close()
        } catch (_: Exception) {}
        try {
            underlyingCloseable?.close()
        } catch (_: Exception) {}
    }
}
