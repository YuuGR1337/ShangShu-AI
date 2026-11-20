package com.example.aiagent.data.remote

import com.example.aiagent.core.network.ApiResult
import com.example.aiagent.data.model.MemoryCreateReq
import com.example.aiagent.data.model.MemoryResp
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 用户长期记忆 API
 * 对应后端 UserMemoryController
 */
interface MemoryApi {

    @POST("memory")
    suspend fun createMemory(@Body req: MemoryCreateReq): ApiResult<MemoryResp>

    @GET("memory")
    suspend fun listMemories(
        @Query("category") category: String? = null,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20,
    ): ApiResult<List<MemoryResp>>

    @GET("memory/search")
    suspend fun searchMemories(
        @Query("keyword") keyword: String,
        @Query("size") size: Int = 20,
    ): ApiResult<List<MemoryResp>>

    @DELETE("memory/{id}")
    suspend fun deleteMemory(@Path("id") id: Long): ApiResult<Unit>

    @GET("memory/context")
    suspend fun getContextMemories(
        @Query("limit") limit: Int = 10,
    ): ApiResult<List<MemoryResp>>
}
