package com.ai.api.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 工作流发布版本快照实体。
 *
 * <p>发布时固化当前草稿 graph，快照内容永不修改：保证已发布版本可重复执行、历史执行可回溯， 草稿编辑不污染已发布内容。
 */
@Data
@TableName(value = "wf_workflow_version", autoResultMap = true)
public class WfWorkflowVersion {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String workflowId;
  private Integer version;

  /** 发布时的 DSL JSON 快照（不可变）。 */
  @TableField(typeHandler = JacksonTypeHandler.class)
  private String graph;

  private Long publishedBy;
  private String notes;
  private LocalDateTime publishedAt;
}
