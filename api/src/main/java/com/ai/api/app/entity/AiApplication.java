package com.ai.api.app.entity;

import com.ai.api.data.entity.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 应用发布实体（V20261121）。
 *
 * <p>四种类型：chat（纯对话） / rag（知识库问答） / agent（任务 Agent） / workflow（工作流执行）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_application")
public class AiApplication extends BaseDO {

  private String name;

  /** chat | rag | agent | workflow */
  private String type;

  /** 类型特定配置 JSON：modelId, kbId, workflowId, systemPrompt 等 */
  private String configJson;

  /** SHA256(appKey)，运行时以 hash 查库；未发布时为 null */
  private String apiKeyHash;

  private String description;

  /** 是否启用 */
  private Boolean enabled;
}
