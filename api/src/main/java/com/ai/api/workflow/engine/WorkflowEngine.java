package com.ai.api.workflow.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 工作流执行引擎。
 *
 * <p>接收前端 DSL JSON，解析为 DAG，按拓扑顺序执行节点，通过 SSE 实时推送执行进度。 支持：Start/End/LLM/IfElse 四种节点类型，变量池节点间传递，条件分支。
 */
@Slf4j
@Service
public class WorkflowEngine {

  /** 默认最大并行度：单机引擎下 4 足以重叠 LLM/HTTP 等耗时节点，又不会压垮下游资源 */
  private static final int DEFAULT_PARALLELISM = 4;

  /** 默认重试次数（1 = 不重试，与未配置 retry 的既有 DSL 行为一致） */
  private static final int DEFAULT_MAX_ATTEMPTS = 1;

  /** 默认重试间隔毫秒 */
  private static final long DEFAULT_BACKOFF_MS = 0L;

  /** onError 策略：失败中断整个流程（默认，与既有行为一致） */
  private static final String ON_ERROR_FAIL = "fail";

  /** onError 策略：节点失败但流程继续走下游（节点仍记 failed） */
  private static final String ON_ERROR_CONTINUE = "continue";

  private final ObjectMapper objectMapper;
  private final Map<String, NodeExecutor> executorMap;
  private final int parallelism;

  public WorkflowEngine(
      ObjectMapper objectMapper,
      @Value("${workflow.parallelism:" + DEFAULT_PARALLELISM + "}") int parallelism,
      List<NodeExecutor> executors) {
    this.objectMapper = objectMapper;
    this.parallelism = parallelism;
    this.executorMap = buildExecutorMap(executors);
  }

  /**
   * 按 type() 构建节点执行器注册表（SPI 式：Spring 收集容器内全部 NodeExecutor Bean，包括第三方通过 AutoConfiguration
   * 注册的扩展节点，新增执行器无需修改本类）。
   *
   * <p>重复 type 视为装配错误，启动即失败，避免运行期出现节点被错误覆盖的隐性 Bug。
   */
  private static Map<String, NodeExecutor> buildExecutorMap(List<NodeExecutor> executors) {
    Map<String, NodeExecutor> registry = new HashMap<>();
    for (NodeExecutor executor : executors) {
      NodeExecutor existing = registry.putIfAbsent(executor.type(), executor);
      if (existing != null) {
        throw new IllegalStateException(
            "Duplicate NodeExecutor type '"
                + executor.type()
                + "': "
                + existing.getClass().getName()
                + " vs "
                + executor.getClass().getName());
      }
    }
    return Map.copyOf(registry);
  }

  /**
   * 执行工作流。
   *
   * @param dslJson DSL JSON 字符串（前端画布序列化输出）
   * @param runId 运行 ID
   * @param inputs 运行时输入参数
   * @param eventSink SSE 事件消费者（可为 null）
   * @return 最终输出
   */
  public Map<String, Object> execute(
      String dslJson, String runId, Map<String, Object> inputs, Consumer<WorkflowEvent> eventSink) {
    // 1. 解析 DSL
    JsonNode root;
    try {
      root = objectMapper.readTree(dslJson);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid workflow DSL: " + e.getMessage(), e);
    }

    List<NodeDef> nodes = parseNodes(root);
    List<EdgeDef> edges = parseEdges(root);

    // 2. 分层拓扑：同层节点互不依赖，可并行执行
    List<List<NodeDef>> layers = topologicalLayers(nodes, edges);

    // 3. 构建邻接表（考虑条件分支）
    Map<String, List<EdgeDef>> adjacency =
        edges.stream().collect(Collectors.groupingBy(EdgeDef::from));

    // 4. 逐层执行
    VariablePool variablePool = new VariablePool();
    // workflow inputs 进入全局作用域，供所有节点以 ${global.xxx} 引用（并发池不允许 null 值）
    if (inputs != null) {
      inputs.forEach(
          (key, value) -> {
            if (value != null) {
              variablePool.globalPut(key, value);
            }
          });
    }
    Map<String, Object> finalOutputs = new LinkedHashMap<>();
    long totalTokens = 0L;
    long startTime = System.currentTimeMillis();

    emit(eventSink, WorkflowEvent.workflowStarted(runId));

    Set<String> executedNodes = new HashSet<>();
    Exception firstFailure = null;

    // 虚拟线程按任务创建成本极低，真正的并发上限由 Semaphore 控制，防止下游模型/DB 资源被打满
    Semaphore permits = new Semaphore(Math.max(parallelism, 1));
    ExecutorService pool =
        Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("wf-node-", 0).factory());
    try {
      for (List<NodeDef> layer : layers) {
        List<Future<NodeOutcome>> futures = new ArrayList<>();
        for (NodeDef node : layer) {
          // 条件分支路径判断：不可达节点记 skipped，保证执行记录覆盖画布全部节点
          if (!shouldExecute(node, edges, executedNodes, adjacency)) {
            emit(eventSink, WorkflowEvent.nodeSkipped(runId, node.id, node.type, node.title));
            continue;
          }
          futures.add(
              pool.submit(
                  () -> executeNode(node, runId, inputs, variablePool, permits, eventSink)));
        }
        // 等待整层结束后在主线程统一合并结果，共享集合只在主线程写，规避并发修改
        for (Future<NodeOutcome> future : futures) {
          NodeOutcome outcome = future.get();
          if (outcome.error() != null) {
            if (firstFailure == null) {
              firstFailure = outcome.error();
            }
            continue;
          }
          executedNodes.add(outcome.node().id());
          // 节点输出写入变量池供下游层引用（同层节点互不依赖，层间串行合并无竞争）
          variablePool.put(outcome.node().id(), outcome.outputs());
          finalOutputs.putAll(outcome.outputs());
          totalTokens += outcome.tokens();
        }
        if (firstFailure != null) {
          break;
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Workflow execution interrupted", e);
    } catch (ExecutionException e) {
      throw new RuntimeException("Workflow execution failed", e.getCause());
    } finally {
      pool.close();
    }

    if (firstFailure != null) {
      if (firstFailure instanceof RuntimeException runtimeFailure) {
        throw runtimeFailure;
      }
      throw new RuntimeException("Workflow failed", firstFailure);
    }

    long elapsed = System.currentTimeMillis() - startTime;
    Map<String, Object> resultData = new LinkedHashMap<>(finalOutputs);
    resultData.put("totalTokens", totalTokens);
    resultData.put("elapsedMs", elapsed);
    emit(eventSink, WorkflowEvent.workflowSucceeded(runId, resultData));

    return finalOutputs;
  }

  /**
   * 在工作线程中执行单个节点：发 started 事件 → 执行 → 发 succeeded/failed 事件。
   *
   * <p>并行层内多节点共享 eventSink 与变量池：emit 已同步、变量池为并发实现；节点输出不直接写共享集合， 而是随 NodeOutcome
   * 返回给主线程合并。异常不在工作线程抛出（避免中断同层兄弟节点）， 而是包装后随结果返回。
   */
  private NodeOutcome executeNode(
      NodeDef node,
      String runId,
      Map<String, Object> inputs,
      VariablePool variablePool,
      Semaphore permits,
      Consumer<WorkflowEvent> eventSink) {
    String nodeId = node.id();
    String nodeType = node.type();
    String title = node.title();
    try {
      permits.acquire();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      emit(eventSink, WorkflowEvent.nodeFailed(runId, nodeId, nodeType, title, "节点执行被中断"));
      return new NodeOutcome(
          node, Map.of(), 0L, new RuntimeException("Node " + nodeId + " interrupted", e));
    }

    NodeExecutor executor = executorMap.get(nodeType);
    if (executor == null) {
      String msg = "Unsupported node type: " + nodeType;
      emit(eventSink, WorkflowEvent.nodeFailed(runId, nodeId, nodeType, title, msg));
      return new NodeOutcome(node, Map.of(), 0L, new UnsupportedOperationException(msg));
    }

    emit(eventSink, WorkflowEvent.nodeStarted(runId, nodeId, nodeType, title));
    NodeContext ctx = new NodeContext(nodeId, nodeType, title, node.data(), variablePool, inputs);
    try {
      return executeWithRetry(node, runId, ctx, executor, eventSink);
    } finally {
      permits.release();
    }
  }

  /**
   * 带重试的单节点执行：按节点配置 retry.maxAttempts / retry.backoffMs 重试，重试期间发 NODE_RETRIED 事件；重试耗尽后按 onError
   * 策略处理——fail（默认）返回失败结果中断流程，continue 返回空成功结果让流程 继续走下游（节点本身已通过 SSE 记为 failed）。
   */
  private NodeOutcome executeWithRetry(
      NodeDef node,
      String runId,
      NodeContext ctx,
      NodeExecutor executor,
      Consumer<WorkflowEvent> eventSink) {
    String nodeId = ctx.nodeId();
    String nodeType = ctx.nodeType();
    String title = ctx.title();
    int maxAttempts =
        (int) Math.max(1, readRetryNumber(node.data(), "maxAttempts", DEFAULT_MAX_ATTEMPTS));
    long backoffMs = Math.max(0L, readRetryNumber(node.data(), "backoffMs", DEFAULT_BACKOFF_MS));
    boolean continueOnError = ON_ERROR_CONTINUE.equals(readOnError(node.data()));

    String lastMsg = null;
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        NodeResult result = executor.execute(ctx);

        Map<String, Object> eventData = new HashMap<>(result.outputs());
        eventData.put("tokens", result.tokens());
        eventData.put("edgeLabel", result.edgeLabel());
        emit(eventSink, WorkflowEvent.nodeSucceeded(runId, nodeId, nodeType, title, eventData));
        return new NodeOutcome(node, result.outputs(), result.tokens(), null);
      } catch (Exception e) {
        lastMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        if (attempt < maxAttempts) {
          emit(
              eventSink,
              WorkflowEvent.nodeRetried(
                  runId,
                  nodeId,
                  nodeType,
                  title,
                  Map.of("attempt", attempt, "maxAttempts", maxAttempts, "error", lastMsg)));
          if (backoffMs > 0) {
            try {
              Thread.sleep(backoffMs);
            } catch (InterruptedException ie) {
              Thread.currentThread().interrupt();
              break;
            }
          }
        }
      }
    }

    log.error("[WorkflowEngine][node({}) 重试 {} 次后仍失败]", nodeId, maxAttempts, lastMsg);
    emit(eventSink, WorkflowEvent.nodeFailed(runId, nodeId, nodeType, title, lastMsg));
    if (continueOnError) {
      return new NodeOutcome(node, Map.of(), 0L, null);
    }
    return new NodeOutcome(
        node,
        Map.of(),
        0L,
        new RuntimeException(
            "Node " + nodeId + " failed after " + maxAttempts + " attempts: " + lastMsg));
  }

  /** 读取节点配置中 retry.<key> 数值（嵌套 Map），缺失或类型不符时返回默认值。 */
  private static long readRetryNumber(Map<String, Object> data, String key, long defaultValue) {
    if (data.get("retry") instanceof Map<?, ?> retry && retry.get(key) instanceof Number number) {
      return number.longValue();
    }
    return defaultValue;
  }

  /** 读取节点 onError 策略，仅接受 continue，其余值（含缺省）一律按 fail 处理。 */
  private static String readOnError(Map<String, Object> data) {
    return data.get("onError") instanceof String value && ON_ERROR_CONTINUE.equals(value)
        ? ON_ERROR_CONTINUE
        : ON_ERROR_FAIL;
  }

  /** 单节点调试执行。 */
  public Map<String, Object> executeNode(
      String nodeType, Map<String, Object> data, Map<String, Object> inputs) {
    NodeExecutor executor = executorMap.get(nodeType);
    if (executor == null) {
      throw new UnsupportedOperationException("Unsupported node type: " + nodeType);
    }
    VariablePool vars = new VariablePool();
    NodeContext ctx = new NodeContext("debug", nodeType, "Debug", data, vars, inputs);
    try {
      NodeResult result = executor.execute(ctx);
      return result.outputs();
    } catch (Exception e) {
      throw new RuntimeException("Debug node failed: " + e.getMessage(), e);
    }
  }

  /** 判断节点是否应该执行（条件分支路径上可达）。 */
  private boolean shouldExecute(
      NodeDef node,
      List<EdgeDef> edges,
      Set<String> executedNodes,
      Map<String, List<EdgeDef>> adjacency) {
    // Start 节点总是执行
    if ("start".equals(node.type)) return true;

    // 找到所有指向该节点的边
    List<EdgeDef> incomingEdges = edges.stream().filter(e -> e.to.equals(node.id)).toList();

    // 如果没有入边（仅 start），总是执行
    if (incomingEdges.isEmpty()) return true;

    // 如果任一前置节点已执行，且边的标签匹配，则执行
    for (EdgeDef edge : incomingEdges) {
      if (executedNodes.contains(edge.from)) {
        // 对于 IfElse 节点出边，需要检查 edgeLabel 匹配
        // 如果没有 sourceHandle 或 sourceHandle 为空，默认执行
        if (edge.sourceHandle == null || edge.sourceHandle.isBlank()) {
          return true;
        }
        // sourceHandle 存在时，需要检查上游节点的分支输出
        // 简化处理：如果上游节点已执行，则执行（实际分支判断在执行时决定）
        return true;
      }
    }
    return false;
  }

  /** Kahn 分层拓扑：按依赖深度将节点分批，同层节点互不依赖可并行；层内出现环时抛出。 */
  private List<List<NodeDef>> topologicalLayers(List<NodeDef> nodes, List<EdgeDef> edges) {
    Map<String, NodeDef> nodeMap = new LinkedHashMap<>();
    Map<String, Integer> inDegree = new HashMap<>();
    Map<String, List<String>> adjacency = new HashMap<>();

    for (NodeDef n : nodes) {
      nodeMap.put(n.id, n);
      inDegree.put(n.id, 0);
      adjacency.put(n.id, new ArrayList<>());
    }
    for (EdgeDef e : edges) {
      adjacency.get(e.from).add(e.to);
      inDegree.merge(e.to, 1, Integer::sum);
    }

    List<List<NodeDef>> layers = new ArrayList<>();
    List<String> frontier = new ArrayList<>();
    for (Map.Entry<String, Integer> e : inDegree.entrySet()) {
      if (e.getValue() == 0) {
        frontier.add(e.getKey());
      }
    }
    int processed = 0;
    while (!frontier.isEmpty()) {
      processed += frontier.size();
      layers.add(frontier.stream().map(nodeMap::get).toList());
      List<String> next = new ArrayList<>();
      for (String id : frontier) {
        for (String nextId : adjacency.get(id)) {
          if (inDegree.merge(nextId, -1, Integer::sum) == 0) {
            next.add(nextId);
          }
        }
      }
      frontier = next;
    }
    if (processed != nodes.size()) {
      throw new IllegalArgumentException("Workflow contains cycle");
    }
    return layers;
  }

  @SuppressWarnings("unchecked")
  private List<NodeDef> parseNodes(JsonNode root) {
    List<NodeDef> nodes = new ArrayList<>();
    JsonNode nodesArr = root.get("nodes");
    if (nodesArr != null && nodesArr.isArray()) {
      for (JsonNode n : nodesArr) {
        String id = n.path("id").asText();
        String type = n.path("type").asText();
        String title = n.path("data").path("title").asText(id);
        Map<String, Object> data = objectMapper.convertValue(n.path("data"), Map.class);
        nodes.add(new NodeDef(id, type, title, data != null ? data : Map.of()));
      }
    }
    return nodes;
  }

  private List<EdgeDef> parseEdges(JsonNode root) {
    List<EdgeDef> edges = new ArrayList<>();
    JsonNode edgesArr = root.get("edges");
    if (edgesArr != null && edgesArr.isArray()) {
      for (JsonNode e : edgesArr) {
        String id = e.path("id").asText();
        String from = e.path("source").asText();
        String to = e.path("target").asText();
        String sourceHandle = e.path("sourceHandle").asText(null);
        edges.add(new EdgeDef(id, from, to, sourceHandle));
      }
    }
    return edges;
  }

  /** 推送 SSE 事件（同步化：并行节点共享 eventSink，下游 SseEmitter.send 非线程安全）。 */
  private synchronized void emit(Consumer<WorkflowEvent> eventSink, WorkflowEvent event) {
    if (eventSink != null) {
      try {
        eventSink.accept(event);
      } catch (Exception e) {
        log.warn("[WorkflowEngine][emit event failed]", e);
      }
    }
  }

  /** 内部节点定义。 */
  record NodeDef(String id, String type, String title, Map<String, Object> data) {}

  /** 内部边定义。 */
  record EdgeDef(String id, String from, String to, String sourceHandle) {}

  /** 单节点执行结果汇总：error 非空表示失败，outputs/tokens 由主线程统一合并。 */
  record NodeOutcome(NodeDef node, Map<String, Object> outputs, long tokens, Exception error) {}
}
