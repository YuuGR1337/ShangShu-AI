package com.example.aiagent.data.model

import kotlinx.serialization.Serializable

/**
 * 模型配置响应
 * 对应后端 ModelConfig 实体
 */
@Serializable
data class ModelConfigResp(
    val id: String = "",
    val name: String = "",
    val provider: String = "",
    val model: String = "",
    val enabled: Boolean = true,
    val isDefault: Boolean = false,
    val maxTokens: Int = 0,
    val description: String = "",
)

/**
 * 简化的模型选项（用于选择器）
 */
@Serializable
data class ModelOption(
    val id: String = "",
    val displayName: String = "",
    val provider: String = "",
)
