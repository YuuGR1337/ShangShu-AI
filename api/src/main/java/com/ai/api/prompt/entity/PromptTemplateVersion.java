package com.ai.api.prompt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** Prompt 模板版本历史快照表（V20261120）。 */
@Data
@TableName("prompt_template_version")
public class PromptTemplateVersion {

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 关联 prompt_template.id */
  private Long templateId;

  /** 版本号，与 prompt_template.version 对应 */
  private Integer version;

  /** 该版本的模板内容快照 */
  private String content;

  /** 该版本的变量列表快照（JSON 字符串） */
  private String variables;

  /** 该版本关联的模型 ID */
  private String modelId;

  /** 本次版本变更说明 */
  private String changeLog;

  /** 发布人 */
  private String createdBy;

  /** 发布时间 */
  private LocalDateTime createdAt;
}
