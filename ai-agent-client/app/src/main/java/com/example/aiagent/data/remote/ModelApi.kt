package com.example.aiagent.data.remote

import com.example.aiagent.core.network.ApiResult
import com.example.aiagent.data.model.ModelConfigResp
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * 模型配置 API
 * 对应后端 ModelConfigController
 */
interface ModelApi {

    /**
     * 获取所有可用模型列表
     */
    @GET("models")
    suspend fun listModels(): ApiResult<List<ModelConfigResp>>

    /**
     * 获取模型详情
     */
    @GET("models/{id}")
    suspend fun getModel(@Path("id") id: String): ApiResult<ModelConfigResp>
}
