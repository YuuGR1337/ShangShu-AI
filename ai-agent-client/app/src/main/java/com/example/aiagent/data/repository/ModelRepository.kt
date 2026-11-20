package com.example.aiagent.data.repository

import com.example.aiagent.core.network.NetworkModule
import com.example.aiagent.core.network.safeApiCall
import com.example.aiagent.data.model.ModelConfigResp
import com.example.aiagent.data.remote.ModelApi

class ModelRepository(
    private val api: ModelApi = NetworkModule.api(),
) {

    /**
     * 获取所有可用模型列表
     */
    suspend fun listModels(): List<ModelConfigResp> =
        safeApiCall { api.listModels() }

    /**
     * 获取模型详情
     */
    suspend fun getModel(id: String): ModelConfigResp =
        safeApiCall { api.getModel(id) }
}
