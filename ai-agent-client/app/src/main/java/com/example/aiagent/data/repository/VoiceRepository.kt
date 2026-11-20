package com.example.aiagent.data.repository

import com.example.aiagent.core.network.NetworkModule
import com.example.aiagent.core.network.safeApiCall
import com.example.aiagent.data.remote.VoiceApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class VoiceRepository {

    private val api = NetworkModule.api<VoiceApi>()

    suspend fun speechToText(audioBytes: ByteArray, filename: String = "audio.webm"): Map<String, String> {
        val requestBody = audioBytes.toRequestBody("audio/webm".toMediaType())
        val part = MultipartBody.Part.createFormData("file", filename, requestBody)
        return safeApiCall { api.speechToText(part) }
    }

    suspend fun textToSpeech(text: String, voice: String = "alloy"): ByteArray {
        val body = api.textToSpeech(mapOf("text" to text, "voice" to voice))
        return body.bytes()
    }
}
