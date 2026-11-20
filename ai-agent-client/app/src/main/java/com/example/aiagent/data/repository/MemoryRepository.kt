package com.example.aiagent.data.repository

import com.example.aiagent.core.network.NetworkModule
import com.example.aiagent.core.network.safeApiCall
import com.example.aiagent.data.model.MemoryCreateReq
import com.example.aiagent.data.model.MemoryResp
import com.example.aiagent.data.remote.MemoryApi

class MemoryRepository(
    private val api: MemoryApi = NetworkModule.api(),
) {

    suspend fun createMemory(req: MemoryCreateReq): MemoryResp =
        safeApiCall { api.createMemory(req) }

    suspend fun listMemories(category: String? = null, page: Int = 1, size: Int = 20): List<MemoryResp> =
        safeApiCall { api.listMemories(category, page, size) }

    suspend fun searchMemories(keyword: String, size: Int = 20): List<MemoryResp> =
        safeApiCall { api.searchMemories(keyword, size) }

    suspend fun deleteMemory(id: Long): Unit =
        safeApiCall { api.deleteMemory(id) }

    suspend fun getContextMemories(limit: Int = 10): List<MemoryResp> =
        safeApiCall { api.getContextMemories(limit) }
}
