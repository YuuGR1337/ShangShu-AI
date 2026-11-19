package com.ai.api.workflow.engine;

import java.time.Instant;
import java.util.Map;

/**
 * 工作流引擎事件（SSE 推送到前端）。
 *
 * @param eventType 事件类型
 * @param runId 运行 ID
 * @param nodeId 节点 ID（节点级事件时有值）
 * @param nodeType 节点类型
 * @param title 节点标题
 * @param data 事件附加数据
 * @param timestamp 事件时间戳
 */
public record WorkflowEvent(
    String eventType,
    String runId,
    String nodeId,
    String nodeType,
    String title,
    Map<String, Object> data,
    long timestamp) {

  public static final String WORKFLOW_STARTED = "workflow_started";
  public static final String NODE_STARTED = "node_started";
  public static final String NODE_SUCCEEDED = "node_succeeded";
  public static final String NODE_FAILED = "node_failed";
  public static final String NODE_RETRIED = "node_retried";
  public static final String NODE_SKIPPED = "node_skipped";
  public static final String WORKFLOW_SUCCEEDED = "workflow_succeeded";
  public static final String WORKFLOW_FAILED = "workflow_failed";

  public static WorkflowEvent workflowStarted(String runId) {
    return new WorkflowEvent(
        WORKFLOW_STARTED, runId, null, null, null, Map.of(), Instant.now().toEpochMilli());
  }

  public static WorkflowEvent nodeStarted(
      String runId, String nodeId, String nodeType, String title) {
    return new WorkflowEvent(
        NODE_STARTED, runId, nodeId, nodeType, title, Map.of(), Instant.now().toEpochMilli());
  }

  public static WorkflowEvent nodeSucceeded(
      String runId, String nodeId, String nodeType, String title, Map<String, Object> data) {
    return new WorkflowEvent(
        NODE_SUCCEEDED, runId, nodeId, nodeType, title, data, Instant.now().toEpochMilli());
  }

  public static WorkflowEvent nodeFailed(
      String runId, String nodeId, String nodeType, String title, String error) {
    return new WorkflowEvent(
        NODE_FAILED,
        runId,
        nodeId,
        nodeType,
        title,
        Map.of("error", error),
        Instant.now().toEpochMilli());
  }

  /** 节点重试事件（携带已尝试次数与错误信息，前端可展示重试进度）。 */
  public static WorkflowEvent nodeRetried(
      String runId, String nodeId, String nodeType, String title, Map<String, Object> data) {
    return new WorkflowEvent(
        NODE_RETRIED, runId, nodeId, nodeType, title, data, Instant.now().toEpochMilli());
  }

  /** 分支不可达节点跳过事件（不进入执行，仅记录状态供前端与执行记录展示）。 */
  public static WorkflowEvent nodeSkipped(
      String runId, String nodeId, String nodeType, String title) {
    return new WorkflowEvent(
        NODE_SKIPPED, runId, nodeId, nodeType, title, Map.of(), Instant.now().toEpochMilli());
  }

  public static WorkflowEvent workflowSucceeded(String runId, Map<String, Object> outputs) {
    return new WorkflowEvent(
        WORKFLOW_SUCCEEDED, runId, null, null, null, outputs, Instant.now().toEpochMilli());
  }

  public static WorkflowEvent workflowFailed(String runId, String error) {
    return new WorkflowEvent(
        WORKFLOW_FAILED,
        runId,
        null,
        null,
        null,
        Map.of("error", error),
        Instant.now().toEpochMilli());
  }
}
