package com.example.aiagent.data.model

import kotlinx.serialization.Serializable

/** 意图识别结果 */
@Serializable
data class IntentResult(
    val intent: String = "",
    val confidence: Double = 0.0,
    val slotsNeeded: List<String> = emptyList(),
)

/** 槽位填充结果 */
@Serializable
data class SlotFillResult(
    val filledSlots: Map<String, String> = emptyMap(),
    val missingSlots: List<String> = emptyList(),
    val isComplete: Boolean = false,
)

/** 客服编排响应 */
@Serializable
data class CustomerServiceResp(
    val intent: IntentResult = IntentResult(),
    val kbAnswer: String? = null,
    val slots: SlotFillResult = SlotFillResult(),
)

/** 客服会话扩展 */
@Serializable
data class CsSession(
    val id: Long = 0,
    val sessionId: Long = 0,
    val runtimeId: Long? = null,
    val status: String = "", // ai|human|done|failed
    val assignedTo: Long? = null,
    val takeoverTime: String? = null,
    val createTime: String = "",
    val updateTime: String = "",
)

/** 标注记录 */
@Serializable
data class CsLabel(
    val id: Long = 0,
    val sessionId: Long = 0,
    val labelType: String = "", // positive|negative|neutral
    val labelContent: String = "",
    val labelerId: Long = 0,
    val createTime: String = "",
)

/** KB 命中统计 */
@Serializable
data class KbHitStat(
    val date: String = "",
    val total: Long = 0,
    val hit: Long = 0,
    val hitRate: Double = 0.0,
)

/** KB 命中汇总 */
@Serializable
data class KbHitSummary(
    val total: Long = 0,
    val hit: Long = 0,
    val hitRate: Double = 0.0,
    val byDay: List<KbHitStat> = emptyList(),
)

/** 客服会话分页列表 */
@Serializable
data class CsSessionList(
    val list: List<CsSession> = emptyList(),
    val total: Long = 0,
)

/** 客服聊天请求 */
@Serializable
data class CsChatReq(
    val query: String,
)