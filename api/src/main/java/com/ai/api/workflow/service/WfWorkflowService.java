package com.ai.api.workflow.service;

import com.ai.api.common.exception.BusinessException;
import com.ai.api.common.exception.ErrorCode;
import com.ai.api.common.result.PageResult;
import com.ai.api.workflow.engine.WorkflowEngine;
import com.ai.api.workflow.engine.WorkflowEvent;
import com.ai.api.workflow.entity.WfNodeRun;
import com.ai.api.workflow.entity.WfWorkflow;
import com.ai.api.workflow.entity.WfWorkflowRun;
import com.ai.api.workflow.entity.WfWorkflowVersion;
import com.ai.api.workflow.mapper.WfNodeRunMapper;
import com.ai.api.workflow.mapper.WfWorkflowMapper;
import com.ai.api.workflow.mapper.WfWorkflowRunMapper;
import com.ai.api.workflow.mapper.WfWorkflowVersionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.ObjectMapper;

/** 工作流业务层：CRUD + 执行 + SSE 推送。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfWorkflowService {

  private final WfWorkflowMapper workflowMapper;
  private final WfWorkflowRunMapper runMapper;
  private final WfNodeRunMapper nodeRunMapper;
  private final WfWorkflowVersionMapper versionMapper;
  private final WorkflowEngine workflowEngine;
  private final ObjectMapper objectMapper;

  // ============ CRUD ============

  /** 创建工作流。 */
  @Transactional
  public WfWorkflow create(String name, String description, String graph, Long ownerId) {
    WfWorkflow wf = new WfWorkflow();
    wf.setWorkflowId(UUID.randomUUID().toString().replace("-", ""));
    wf.setName(name != null ? name : "未命名工作流");
    wf.setDescription(description != null ? description : "");
    wf.setGraph(
        graph != null
            ? graph
            : "{\"version\":\"1.0\",\"viewport\":{\"x\":0,\"y\":0,\"zoom\":1},\"nodes\":[],\"edges\":[]}");
    wf.setVersion(1);
    wf.setPublished(0);
    wf.setOwnerId(ownerId);
    wf.setDeleted(0);
    workflowMapper.insert(wf);
    return wf;
  }

  /** 更新工作流草稿。 */
  @Transactional
  public WfWorkflow updateDraft(String workflowId, String name, String description, String graph) {
    WfWorkflow wf = getByWorkflowId(workflowId);
    if (name != null) wf.setName(name);
    if (description != null) wf.setDescription(description);
    if (graph != null) wf.setGraph(graph);
    workflowMapper.updateById(wf);
    return wf;
  }

  /**
   * 发布工作流：当前草稿 graph 固化为不可变版本快照（version+1）。
   *
   * <p>快照写入 wf_workflow_version 后永不修改；后续编辑只更新主表草稿，历史版本保持可重复执行。
   */
  @Transactional
  public WfWorkflow publish(String workflowId, Long publishedBy) {
    WfWorkflow wf = getByWorkflowId(workflowId);
    int newVersion = wf.getVersion() + 1;
    WfWorkflowVersion snapshot = new WfWorkflowVersion();
    snapshot.setWorkflowId(workflowId);
    snapshot.setVersion(newVersion);
    snapshot.setGraph(wf.getGraph());
    snapshot.setPublishedBy(publishedBy);
    snapshot.setPublishedAt(LocalDateTime.now());
    versionMapper.insert(snapshot);
    wf.setVersion(newVersion);
    wf.setPublished(1);
    workflowMapper.updateById(wf);
    return wf;
  }

  /** 获取工作流详情。 */
  public WfWorkflow getByWorkflowId(String workflowId) {
    LambdaQueryWrapper<WfWorkflow> qw =
        new LambdaQueryWrapper<WfWorkflow>()
            .eq(WfWorkflow::getWorkflowId, workflowId)
            .eq(WfWorkflow::getDeleted, 0);
    WfWorkflow wf = workflowMapper.selectOne(qw);
    if (wf == null) {
      throw new BusinessException(ErrorCode.WORKFLOW_NOT_FOUND, "工作流不存在: " + workflowId);
    }
    return wf;
  }

  /** 工作流列表。 */
  public List<WfWorkflow> list(Long ownerId) {
    LambdaQueryWrapper<WfWorkflow> qw =
        new LambdaQueryWrapper<WfWorkflow>()
            .eq(WfWorkflow::getDeleted, 0)
            .eq(ownerId != null, WfWorkflow::getOwnerId, ownerId)
            .orderByDesc(WfWorkflow::getUpdatedAt);
    return workflowMapper.selectList(qw);
  }

  /** 删除工作流（逻辑删除）。 */
  @Transactional
  public void delete(String workflowId) {
    WfWorkflow wf = getByWorkflowId(workflowId);
    wf.setDeleted(1);
    workflowMapper.updateById(wf);
  }

  /** 复制工作流。 */
  @Transactional
  public WfWorkflow duplicate(String workflowId) {
    WfWorkflow source = getByWorkflowId(workflowId);
    WfWorkflow copy = new WfWorkflow();
    copy.setWorkflowId(UUID.randomUUID().toString().replace("-", ""));
    copy.setName(source.getName() + " (副本)");
    copy.setDescription(source.getDescription());
    copy.setGraph(source.getGraph());
    copy.setVersion(1);
    copy.setPublished(0);
    copy.setOwnerId(source.getOwnerId());
    copy.setDeleted(0);
    workflowMapper.insert(copy);
    return copy;
  }

  // ============ 执行 ============

  /** 运行工作流（同步返回结果；sseEmitter 非空时推送事件，为 null 时仅落执行记录——AI 应用入口无 SSE 场景）。 */
  public WfWorkflowRun run(String workflowId, Map<String, Object> inputs, SseEmitter sseEmitter) {
    WfWorkflow wf = getByWorkflowId(workflowId);
    // 优先执行最新发布快照：已发布内容不被进行中的草稿编辑污染；从未发布过则执行草稿
    WfWorkflowVersion snapshot = latestPublishedVersion(workflowId);
    String graph = snapshot != null ? snapshot.getGraph() : wf.getGraph();
    int runVersion = snapshot != null ? snapshot.getVersion() : wf.getVersion();
    String runId = UUID.randomUUID().toString().replace("-", "");
    long runStart = System.currentTimeMillis();

    // 创建运行记录
    WfWorkflowRun runRecord = new WfWorkflowRun();
    runRecord.setRunId(runId);
    runRecord.setWorkflowId(workflowId);
    runRecord.setVersion(runVersion);
    runRecord.setStatus("running");
    runRecord.setInputs(toJson(inputs));
    runRecord.setTriggeredBy("manual");
    runMapper.insert(runRecord);

    // SSE 事件消费者：同时记录节点执行（并行分支时多线程回调，需并发安全容器）
    Map<String, Long> nodeStartTimes = new ConcurrentHashMap<>();
    Consumer<WorkflowEvent> eventSink =
        event -> {
          try {
            String json = objectMapper.writeValueAsString(event);
            // sseEmitter 为 null 表示无 SSE 的同步调用（AI 应用入口），仅保留执行记录
            if (sseEmitter != null) {
              sseEmitter.send(SseEmitter.event().name("workflow").data(json));
            }
            String nodeId = event.nodeId();
            long now = System.currentTimeMillis();
            switch (event.eventType()) {
              case WorkflowEvent.NODE_STARTED -> nodeStartTimes.put(nodeId, now);
              case WorkflowEvent.NODE_SKIPPED -> saveSkippedNodeRun(runId, event);
              case WorkflowEvent.NODE_SUCCEEDED, WorkflowEvent.NODE_FAILED -> {
                Long start = nodeStartTimes.remove(nodeId);
                long elapsed = (start != null) ? (now - start) : 0L;
                saveNodeRun(runId, event, elapsed, null);
              }
              default -> {}
            }
          } catch (Exception e) {
            log.warn("[WfWorkflowService][SSE send failed]", e);
          }
        };

    try {
      Map<String, Object> outputs = workflowEngine.execute(graph, runId, inputs, eventSink);
      long elapsedMs = System.currentTimeMillis() - runStart;
      // 更新运行记录
      runRecord.setStatus("succeeded");
      runRecord.setOutputs(toJson(outputs));
      runRecord.setElapsedMs(elapsedMs);
      runRecord.setTotalTokens(extractTotalTokens(outputs));
      runRecord.setFinishedAt(LocalDateTime.now());
      runMapper.updateById(runRecord);
    } catch (Exception e) {
      long elapsedMs = System.currentTimeMillis() - runStart;
      runRecord.setStatus("failed");
      runRecord.setErrorMessage(e.getMessage());
      runRecord.setElapsedMs(elapsedMs);
      runRecord.setFinishedAt(LocalDateTime.now());
      runMapper.updateById(runRecord);
      throw e;
    } finally {
      if (sseEmitter != null) {
        try {
          sseEmitter.complete();
        } catch (Exception ignored) {
        }
      }
    }

    return runRecord;
  }

  /** 单节点调试。 */
  public Map<String, Object> debugNode(
      String nodeType, Map<String, Object> data, Map<String, Object> inputs) {
    return workflowEngine.executeNode(nodeType, data, inputs);
  }

  // ============ 运行历史 ============

  /** 查询工作流运行历史。 */
  public List<WfWorkflowRun> listRuns(String workflowId) {
    LambdaQueryWrapper<WfWorkflowRun> qw =
        new LambdaQueryWrapper<WfWorkflowRun>()
            .eq(WfWorkflowRun::getWorkflowId, workflowId)
            .orderByDesc(WfWorkflowRun::getCreatedAt);
    return runMapper.selectList(qw);
  }

  // ============ 版本快照 ============

  /** 最新发布版本快照（按版本号倒序取一），从未发布返回 null。 */
  public WfWorkflowVersion latestPublishedVersion(String workflowId) {
    return versionMapper.selectOne(
        new LambdaQueryWrapper<WfWorkflowVersion>()
            .eq(WfWorkflowVersion::getWorkflowId, workflowId)
            .orderByDesc(WfWorkflowVersion::getVersion)
            .last("LIMIT 1"));
  }

  /** 版本快照列表（按版本号倒序）。 */
  public List<WfWorkflowVersion> listVersions(String workflowId) {
    return versionMapper.selectList(
        new LambdaQueryWrapper<WfWorkflowVersion>()
            .eq(WfWorkflowVersion::getWorkflowId, workflowId)
            .orderByDesc(WfWorkflowVersion::getVersion));
  }

  /** 版本快照详情（发布时的不可变 graph）。 */
  public WfWorkflowVersion getVersion(String workflowId, int version) {
    WfWorkflowVersion snapshot =
        versionMapper.selectOne(
            new LambdaQueryWrapper<WfWorkflowVersion>()
                .eq(WfWorkflowVersion::getWorkflowId, workflowId)
                .eq(WfWorkflowVersion::getVersion, version));
    if (snapshot == null) {
      throw new BusinessException(
          ErrorCode.WORKFLOW_NOT_FOUND, "版本快照不存在: " + workflowId + " v" + version);
    }
    return snapshot;
  }

  // ============ 运行历史查询 ============

  /** 全局运行记录分页列表（可选按状态过滤）。 */
  public PageResult<WfWorkflowRun> listAllRuns(int pageNum, int pageSize, String status) {
    LambdaQueryWrapper<WfWorkflowRun> qw =
        new LambdaQueryWrapper<WfWorkflowRun>()
            .eq(status != null && !status.isBlank(), WfWorkflowRun::getStatus, status)
            .orderByDesc(WfWorkflowRun::getCreatedAt);
    Page<WfWorkflowRun> page = runMapper.selectPage(Page.of(pageNum, pageSize), qw);
    return PageResult.of(page.getRecords(), page.getTotal());
  }

  /** 运行记录详情（含所有节点执行记录）。 */
  public Map<String, Object> getRunDetail(String runId) {
    WfWorkflowRun run =
        runMapper.selectOne(
            new LambdaQueryWrapper<WfWorkflowRun>().eq(WfWorkflowRun::getRunId, runId));
    if (run == null) {
      throw new BusinessException(ErrorCode.WORKFLOW_NOT_FOUND, "运行记录不存在: " + runId);
    }
    List<WfNodeRun> nodes =
        nodeRunMapper.selectList(
            new LambdaQueryWrapper<WfNodeRun>()
                .eq(WfNodeRun::getRunId, runId)
                .orderByAsc(WfNodeRun::getCreatedAt));
    Map<String, Object> result = new HashMap<>();
    result.put("run", run);
    result.put("nodes", nodes);
    return result;
  }

  // ============ 内部方法 ============

  /** 记录单个节点执行结果（含耗时、Token、错误信息）。 */
  private void saveNodeRun(
      String runId, WorkflowEvent event, long elapsedMs, Map<String, Object> inputs) {
    WfNodeRun nodeRun = new WfNodeRun();
    nodeRun.setRunId(runId);
    nodeRun.setNodeId(event.nodeId());
    nodeRun.setNodeType(event.nodeType());
    nodeRun.setTitle(event.title());
    nodeRun.setStatus(
        WorkflowEvent.NODE_SUCCEEDED.equals(event.eventType()) ? "succeeded" : "failed");
    nodeRun.setElapsedMs(elapsedMs);
    if (inputs != null) {
      nodeRun.setInputs(toJson(inputs));
    }
    if (event.data() != null) {
      nodeRun.setOutputs(toJson(event.data()));
      if (event.data().containsKey("tokens")) {
        nodeRun.setTokens(((Number) event.data().get("tokens")).longValue());
      }
      if (event.data().containsKey("error")) {
        nodeRun.setErrorMessage(String.valueOf(event.data().get("error")));
      }
    }
    nodeRun.setFinishedAt(LocalDateTime.now());
    nodeRunMapper.insert(nodeRun);
  }

  /** 记录被跳过的节点（条件分支不可达），保证执行记录覆盖画布全部节点。 */
  private void saveSkippedNodeRun(String runId, WorkflowEvent event) {
    WfNodeRun nodeRun = new WfNodeRun();
    nodeRun.setRunId(runId);
    nodeRun.setNodeId(event.nodeId());
    nodeRun.setNodeType(event.nodeType());
    nodeRun.setTitle(event.title());
    nodeRun.setStatus("skipped");
    nodeRun.setFinishedAt(LocalDateTime.now());
    nodeRunMapper.insert(nodeRun);
  }

  /** 从最终 outputs 中提取 totalTokens。 */
  private Long extractTotalTokens(Map<String, Object> outputs) {
    Object tokens = outputs.get("totalTokens");
    if (tokens instanceof Number n) return n.longValue();
    return 0L;
  }

  private String toJson(Object obj) {
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (Exception e) {
      return "{}";
    }
  }
}
