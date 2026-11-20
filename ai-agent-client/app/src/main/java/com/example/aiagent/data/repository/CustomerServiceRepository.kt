package com.example.aiagent.data.repository

import com.example.aiagent.core.network.NetworkModule
import com.example.aiagent.core.network.safeApiCall
import com.example.aiagent.data.model.CsLabel
import com.example.aiagent.data.model.CsSession
import com.example.aiagent.data.model.CsSessionList
import com.example.aiagent.data.model.CustomerServiceResp
import com.example.aiagent.data.model.KbHitStat
import com.example.aiagent.data.model.KbHitSummary
import com.example.aiagent.data.remote.CustomerServiceApi

class CustomerServiceRepository(
    private val api: CustomerServiceApi = NetworkModule.api(),
) {

    // ============ 客服编排 ============

    suspend fun chat(runtimeId: Long, query: String): CustomerServiceResp =
        safeApiCall { api.chat(runtimeId, com.example.aiagent.data.model.CsChatReq(query)) }

    // ============ 会话工作台 ============

    suspend fun listSessions(
        status: String? = null,
        assignedTo: Long? = null,
        page: Int = 1,
        size: Int = 20,
    ): CsSessionList = safeApiCall { api.listSessions(status, assignedTo, page, size) }

    suspend fun getSession(sessionId: Long): CsSession =
        safeApiCall { api.getSession(sessionId) }

    suspend fun takeoverSession(sessionId: Long) {
        safeApiCall<Unit> { api.takeoverSession(sessionId) }
    }

    // ============ 标注回流 ============

    suspend fun submitLabel(sessionId: Long, labelType: String, content: String) {
        safeApiCall<Unit> {
            api.submitLabel(sessionId, mapOf("labelType" to labelType, "content" to content))
        }
    }

    suspend fun listLabels(sessionId: Long): List<CsLabel> =
        safeApiCall { api.listLabels(sessionId) }

    // ============ KB 命中统计 ============

    suspend fun kbHitStats(days: Int = 7): List<KbHitStat> =
        safeApiCall { api.kbHitStats(days) }

    suspend fun kbHitSummary(days: Int = 7): KbHitSummary =
        safeApiCall { api.kbHitSummary(days) }
}