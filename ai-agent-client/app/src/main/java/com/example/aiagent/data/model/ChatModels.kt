package com.example.aiagent.data.model

import kotlinx.serialization.Serializable

/** 移动端默认使用的模型 ID（与 Web 端新建会话默认值 `ollama` 一致）。 */
const val DEFAULT_MODEL_ID = "ollama"

@Serializable
data class SessionResp(
    val sessionId: Long = 0,
    val title: String = "",
    val modelId: String = "",
    val messageCount: Int = 0,
    val totalTokens: Long = 0,
    val createTime: String = "",
    val updateTime: String = "",
)

@Serializable
data class MessageResp(
    val messageId: Long = 0,
    val sessionId: Long = 0,
    val role: String = "",
    val content: String = "",
    val promptTokens: Long = 0,
    val completionTokens: Long = 0,
    val totalTokens: Long = 0,
    val latencyMs: Long = 0,
    val modelId: String = "",
    val createTime: String = "",
    /** 网络搜索引用来源（webSearch 增强时由 SSE search 事件填充，本地构造，可省略） */
    val sources: List<String> = emptyList(),
) {
    val isUser: Boolean get() = role == "user"
}

@Serializable
data class CreateSessionReq(
    val modelId: String,
    val title: String? = null,
)

/**
 * 游标分页响应（Dify 式 keyset 分页）。
 *
 * 对应后端 {@code CursorPage<T>}：不返回 total，用 hasMore 标记是否还有更多数据，
 * 客户端用返回数据首/末条 ID 作为下一页游标。
 */
@Serializable
data class CursorPage<T>(
    val data: List<T> = emptyList(),
    val limit: Int = 0,
    val hasMore: Boolean = false,
)

@Serializable
data class SendMessageReq(
    val content: String,
    /** 是否启用网络搜索增强（后端未配置搜索服务时自动忽略） */
    val webSearch: Boolean = false,
)

/** Token 统计响应，对应后端 ChatTokenController TokenStatsRespDTO。 */
@Serializable
data class TokenStatsResp(
    val period: String = "today",
    val promptTokens: Long = 0,
    val completionTokens: Long = 0,
    val totalTokens: Long = 0,
)

/** 全局检索命中记录，对应后端 ChatSearchController 返回。 */
@Serializable
data class ChatSearchHit(
    val messageId: String = "",
    val sessionId: Long = 0,
    val userId: Long? = null,
    val role: String = "",
    val content: String = "",
    /** 会话标题 */
    val title: String? = null,
    val modelId: String? = null,
    val createTime: String? = null,
    val score: Double? = null,
    /** 内容高亮片段（ES 原生高亮或服务端截取的关键词上下文） */
    val highlight: String? = null,
    /** 标题高亮片段 */
    val titleHighlight: String? = null,
)

/** 全局检索结果（含分页信息） */
@Serializable
data class ChatSearchResult(
    val total: Int = 0,
    val page: Int = 1,
    val size: Int = 20,
    val hasMore: Boolean = false,
    val hits: List<ChatSearchHit> = emptyList(),
)

/** 知识库简要信息，对应后端 KnowledgeController 返回。 */
@Serializable
data class KnowledgeBaseResp(
    val id: Long = 0,
    val name: String = "",
    val description: String = "",
    val ownerId: Long = 0,
    val embeddingModel: String = "",
    val chunkSize: Int = 0,
    val chunkOverlap: Int = 0,
    val similarityThreshold: Double = 0.0,
    val documentCount: Int = 0,
    val chunkCount: Int = 0,
    val status: String = "",
    val createTime: String = "",
    val updateTime: String = "",
)
