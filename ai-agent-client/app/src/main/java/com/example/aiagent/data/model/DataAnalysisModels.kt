package com.example.aiagent.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** 可分析数据表的列元信息 */
@Serializable
data class DataColumnMeta(
    val name: String = "",
    val comment: String = "",
    val type: String = "", // int/string/datetime/decimal/boolean
)

/** 可分析数据表元信息 */
@Serializable
data class DataTableMeta(
    val name: String = "",
    val comment: String = "",
    val columns: List<DataColumnMeta> = emptyList(),
)

/** schema 响应 */
@Serializable
data class DataSchemaResp(
    val tables: List<DataTableMeta> = emptyList(),
)

/** 问数请求 */
@Serializable
data class DataAskReq(
    val question: String,
)

/** 问数响应：结果集 + 图表规格 */
@Serializable
data class DataAskResp(
    val question: String = "",
    val sql: String = "",
    val columns: List<String> = emptyList(),
    val rows: List<Map<String, JsonElement>> = emptyList(),
    val chartType: String = "table", // bar/line/pie/table
    val chartTitle: String = "",
    val message: String? = null,
    val latencyMs: Long = 0,
)

/** 生成报告请求 */
@Serializable
data class DataReportReq(
    val question: String,
    val sql: String,
    val columns: List<String>,
    val rows: List<Map<String, JsonElement>>,
)
