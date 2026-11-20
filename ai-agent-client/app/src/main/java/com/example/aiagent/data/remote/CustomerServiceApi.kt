package com.example.aiagent.data.remote

import com.example.aiagent.core.network.ApiResult
import com.example.aiagent.data.model.CsChatReq
import com.example.aiagent.data.model.CsLabel
import com.example.aiagent.data.model.CsSession
import com.example.aiagent.data.model.CsSessionList
import com.example.aiagent.data.model.CustomerServiceResp
import com.example.aiagent.data.model.KbHitStat
import com.example.aiagent.data.model.KbHitSummary
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CustomerServiceApi {

    // ============ 客服编排 ============

    /** 客服入口：编排意图识别 + KB 回退 + 槽位填充 */
    @POST("cs/chat")
    suspend fun chat(
        @Query("runtimeId") runtimeId: Long,
        @Body req: CsChatReq,
    ): ApiResult<CustomerServiceResp>

    // ============ 会话工作台 ============

    /** 分页查询会话列表 */
    @GET("cs/sessions")
    suspend fun listSessions(
        @Query("status") status: String? = null,
        @Query("assignedTo") assignedTo: Long? = null,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20,
    ): ApiResult<CsSessionList>

    /** 查询会话详情 */
    @GET("cs/sessions/{sessionId}")
    suspend fun getSession(@Path("sessionId") sessionId: Long): ApiResult<CsSession>

    /** 人工接管 */
    @POST("cs/sessions/{sessionId}/takeover")
    suspend fun takeoverSession(@Path("sessionId") sessionId: Long): ApiResult<Unit>

    // ============ 标注回流 ============

    /** 提交标注 */
    @POST("cs/labels/{sessionId}")
    suspend fun submitLabel(
        @Path("sessionId") sessionId: Long,
        @Body data: Map<String, @JvmSuppressWildcards String>,
    ): ApiResult<Unit>

    /** 查询会话所有标注 */
    @GET("cs/labels/{sessionId}")
    suspend fun listLabels(@Path("sessionId") sessionId: Long): ApiResult<List<CsLabel>>

    // ============ KB 命中统计 ============

    /** 按天统计 KB 命中率 */
    @GET("cs/stats/kb-hit")
    suspend fun kbHitStats(@Query("days") days: Int = 7): ApiResult<List<KbHitStat>>

    /** 汇总 KB 命中率 */
    @GET("cs/stats/kb-hit/summary")
    suspend fun kbHitSummary(@Query("days") days: Int = 7): ApiResult<KbHitSummary>
}