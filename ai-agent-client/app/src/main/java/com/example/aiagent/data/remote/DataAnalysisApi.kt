package com.example.aiagent.data.remote

import com.example.aiagent.core.network.ApiResult
import com.example.aiagent.data.model.DataAskReq
import com.example.aiagent.data.model.DataAskResp
import com.example.aiagent.data.model.DataReportReq
import com.example.aiagent.data.model.DataSchemaResp
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** 数据分析（NL2SQL）API，对应后端 DataAnalysisController */
interface DataAnalysisApi {

    /** 获取可分析的数据表清单 */
    @GET("data-analysis/schema")
    suspend fun fetchSchema(): ApiResult<DataSchemaResp>

    /** 自然语言问数（NL2SQL） */
    @POST("data-analysis/ask")
    suspend fun ask(@Body req: DataAskReq): ApiResult<DataAskResp>

    /** 生成 Markdown 分析报告 */
    @POST("data-analysis/report")
    suspend fun report(@Body req: DataReportReq): ApiResult<String>
}
