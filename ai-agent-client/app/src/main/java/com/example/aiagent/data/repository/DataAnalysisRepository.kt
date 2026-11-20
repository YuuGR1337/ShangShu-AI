package com.example.aiagent.data.repository

import com.example.aiagent.core.network.NetworkModule
import com.example.aiagent.core.network.safeApiCall
import com.example.aiagent.data.model.DataAskReq
import com.example.aiagent.data.model.DataAskResp
import com.example.aiagent.data.model.DataReportReq
import com.example.aiagent.data.model.DataSchemaResp
import com.example.aiagent.data.remote.DataAnalysisApi

class DataAnalysisRepository(
    private val api: DataAnalysisApi = NetworkModule.api(),
) {

    suspend fun fetchSchema(): DataSchemaResp = safeApiCall { api.fetchSchema() }

    suspend fun ask(question: String): DataAskResp = safeApiCall { api.ask(DataAskReq(question)) }

    suspend fun report(req: DataReportReq): String = safeApiCall { api.report(req) }
}
