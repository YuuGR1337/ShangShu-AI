package com.example.aiagent.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CharacterCardResp(
    val id: Long = 0,
    val userId: Long = 0,
    val name: String = "",
    val avatar: String = "",
    val description: String = "",
    val systemPrompt: String = "",
    val openingStatement: String = "",
    val tags: String = "",
    val isPublic: Boolean = false,
    val isDefault: Boolean = false,
    val useCount: Int = 0,
    val likeCount: Int = 0,
    val createTime: String = "",
)

@Serializable
data class CharacterCardCreateReq(
    val name: String,
    val avatar: String = "",
    val description: String = "",
    val systemPrompt: String,
    val openingStatement: String = "",
    val tags: String = "",
    val isPublic: Boolean = false,
)

@Serializable
data class CharacterCardUpdateReq(
    val name: String? = null,
    val avatar: String? = null,
    val description: String? = null,
    val systemPrompt: String? = null,
    val openingStatement: String? = null,
    val tags: String? = null,
    val isPublic: Boolean? = null,
)
