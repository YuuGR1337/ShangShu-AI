package com.example.aiagent.data.remote

import com.example.aiagent.core.network.ApiResult
import com.example.aiagent.data.model.ChatSearchHit
import com.example.aiagent.data.model.ChatSearchResult
import com.example.aiagent.data.model.CreateSessionReq
import com.example.aiagent.data.model.CursorPage
import com.example.aiagent.data.model.MessageResp
import com.example.aiagent.data.model.SessionResp
import com.example.aiagent.data.model.TokenStatsResp
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApi {

    @POST("chat/sessions")
    suspend fun createSession(@Body req: CreateSessionReq): ApiResult<SessionResp>

    /**
     * 查询会话列表（游标分页）。
     *
     * 按 updateTime 倒序；首屏不传 lastId，下一页传上一页最后一条的 sessionId。
     */
    @GET("chat/sessions")
    suspend fun listSessions(
        @Query("lastId") lastId: Long? = null,
        @Query("limit") limit: Int = 20,
    ): ApiResult<CursorPage<SessionResp>>

    @DELETE("chat/sessions/{id}")
    suspend fun deleteSession(@Path("id") id: Long): ApiResult<Unit>

    /**
     * 查询会话消息历史（游标分页："先最新一屏，向上加载更早"）。
     *
     * 首屏不传 beforeId 返回最新 limit 条（时间正序）；向上滚动加载更早时，
     * 传当前已加载消息中最旧一条的 messageId。
     */
    @GET("chat/sessions/{id}/messages")
    suspend fun getMessages(
        @Path("id") id: Long,
        @Query("beforeId") beforeId: Long? = null,
        @Query("limit") limit: Int = 50,
    ): ApiResult<CursorPage<MessageResp>>

    /** Token 用量统计（对应 ChatTokenController） */
    @GET("chat/tokens/stats")
    suspend fun getTokenStats(
        @Query("period") period: String = "today",
    ): ApiResult<TokenStatsResp>

    /** 全局搜索对话历史（对应 ChatSearchController） */
    @GET("chat/search")
    suspend fun searchChatHistory(
        @Query("q") q: String,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20,
        @Query("role") role: String? = null,
        @Query("sessionId") sessionId: Long? = null,
        @Query("startTime") startTime: Long? = null,
        @Query("endTime") endTime: Long? = null,
    ): ApiResult<ChatSearchResult>
}


