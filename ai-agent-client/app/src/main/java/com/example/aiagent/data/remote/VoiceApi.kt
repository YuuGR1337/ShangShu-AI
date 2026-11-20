package com.example.aiagent.data.remote

import com.example.aiagent.core.network.ApiResult
import okhttp3.MultipartBody
import retrofit2.http.*

interface VoiceApi {

    @Multipart
    @POST("voice/stt")
    suspend fun speechToText(@Part file: MultipartBody.Part): ApiResult<Map<String, String>>

    @POST("voice/tts")
    suspend fun textToSpeech(@Body request: Map<String, String>): okhttp3.ResponseBody
}
