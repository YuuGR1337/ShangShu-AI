package com.ai.api.data.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统模型配置 DO
 *
 * <p>对应表 {@code sys_model_config}，依据 design.md D6。
 *
 * <p>本表仅用于演示 MyBatis 通路，完整模型注册功能在 {@code add-model-capability} 中实现。
 *
 * @author bootstrap-project
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_model_config")
public class SysModelConfig extends BaseDO {

  private static final long serialVersionUID = 1L;

  /** 供应商：deepseek/qwen/ollama */
  private String provider;

  /** 模型标识，如 deepseek-chat */
  private String modelId;

  /** 展示名称 */
  private String displayName;

  /** API Key（实际从环境变量读取，本字段通常留空） */
  private String apiKey;

  /** 基础 URL */
  private String baseUrl;

  /** 是否启用：1=是，0=否 */
  private Integer enabled;

  /** 模型参数 JSON（temperature/max_tokens/top_p 等） */
  private String parameters;

  /** Jasypt 加密后的 API Key */
  private String apiKeyEncrypted;

  /** 排序权重 */
  private Integer sortOrder;
}
