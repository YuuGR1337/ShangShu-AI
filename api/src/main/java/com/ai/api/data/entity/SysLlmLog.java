package com.ai.api.data.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * LLM 调用日志 DO
 *
 * <p>对应表 {@code sys_llm_log}，依据 design.md D6。
 *
 * <p>用于记录每次大模型调用的可观测性数据：traceId、prompt、response、Token 用量、 延迟、状态。通过 RabbitMQ 异步写入，避免阻塞主流程。
 *
 * @author bootstrap-project
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_llm_log")
public class SysLlmLog extends BaseDO {

  private static final long serialVersionUID = 1L;

  /** 链路追踪 ID */
  private String traceId;

  /** 模型标识 */
  private String modelId;

  /** 用户提示词（超长截断） */
  private String prompt;

  /** 模型响应内容（超长截断） */
  private String response;

  /** 输入 Token 数 */
  private Integer promptTokens;

  /** 输出 Token 数 */
  private Integer completionTokens;

  /** 总 Token 数 */
  private Integer totalTokens;

  /** 调用耗时（毫秒） */
  private Integer latencyMs;

  /** 状态：success/failed */
  private String status;

  /** 错误信息（失败时） */
  private String errorMsg;
}
