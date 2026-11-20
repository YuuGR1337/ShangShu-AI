package com.example.aiagent.data.model

import kotlinx.serialization.Serializable

/**
 * 用户长期记忆响应
 * 对应后端 UserMemory 实体
 */
@Serializable
data class MemoryResp(
    val id: Long = 0,
    val category: String = "general",
    val content: String = "",
    val source: String = "",
    val importance: Int = 0,
    val timesUsed: Int = 0,
    val createTime: String = "",
    val updateTime: String = "",
)

/**
 * 创建记忆请求
 */
@Serializable
data class MemoryCreateReq(
    val category: String = "general",
    val content: String,
    val source: String = "",
    val importance: Int = 0,
)
