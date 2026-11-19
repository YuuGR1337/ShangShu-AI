package com.ai.api.workflow.engine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流变量池。
 *
 * <p>每个节点执行后，将输出写入 VariablePool；下游节点通过 {@code ${nodeId.outputKey}} 引用上游节点输出。
 * 支持：整串变量引用（保留原始类型）、字符串内嵌变量（拼接为字符串）、global 作用域（{@code ${global.key}}）。 并行分支执行时多节点并发读写，底层使用并发安全容器。
 */
public class VariablePool {

  /** nodeId → outputs */
  private final Map<String, Map<String, Object>> pool = new ConcurrentHashMap<>();

  /** 全局作用域：workflow inputs 与节点显式写入的跨节点共享变量，表达式 ${global.key} 引用 */
  private final Map<String, Object> globalScope = new ConcurrentHashMap<>();

  /** 写入节点输出。 */
  public void put(String nodeId, Map<String, Object> outputs) {
    pool.put(nodeId, outputs);
  }

  /** 读取节点完整输出。 */
  public Map<String, Object> get(String nodeId) {
    return pool.getOrDefault(nodeId, Map.of());
  }

  /** 读取节点单个输出字段。 */
  public Object get(String nodeId, String key) {
    Map<String, Object> outputs = pool.get(nodeId);
    return outputs != null ? outputs.get(key) : null;
  }

  /**
   * 变量插值：将字符串中 {@code ${nodeId.key}} 替换为实际值。
   *
   * <p>如果整个字符串恰好是一个变量引用，则保留原始类型（如 List、Number）； 否则做字符串拼接。
   */
  public Object resolve(String template) {
    if (template == null || !template.contains("${")) {
      return template;
    }

    // 整串是单个变量引用 → 保留原始类型
    if (template.startsWith("${")
        && template.endsWith("}")
        && template.indexOf("${") == 0
        && template.lastIndexOf("}") == template.length() - 1) {
      String path = template.substring(2, template.length() - 1);
      return resolvePath(path);
    }

    // 包含多个变量或带其他文本 → 字符串拼接
    StringBuilder sb = new StringBuilder();
    int i = 0;
    while (i < template.length()) {
      int start = template.indexOf("${", i);
      if (start < 0) {
        sb.append(template, i, template.length());
        break;
      }
      int end = template.indexOf("}", start);
      if (end < 0) {
        sb.append(template, i, template.length());
        break;
      }
      sb.append(template, i, start);
      String path = template.substring(start + 2, end);
      Object value = resolvePath(path);
      sb.append(value != null ? value : "");
      i = end + 1;
    }
    return sb.toString();
  }

  /** 写入全局作用域变量（跨节点共享，如 workflow inputs）。 */
  public void globalPut(String key, Object value) {
    globalScope.put(key, value);
  }

  /** 读取全局作用域变量。 */
  public Object globalGet(String key) {
    return globalScope.get(key);
  }

  /**
   * 类型化变量读取：在 {@link #resolve} 基础上校验取值类型。
   *
   * <p>路径缺失抛 {@link VariableNotFoundException}，类型不符抛 {@link VariableTypeMismatchException}，
   * 让节点配置错误在执行早期以明确信息暴露，而不是下游隐式空值。
   */
  public <T> T resolveTyped(String template, Class<T> expectedType) {
    Object value = resolve(template);
    if (value == null) {
      throw new VariableNotFoundException(template);
    }
    if (!expectedType.isInstance(value)) {
      throw new VariableTypeMismatchException(template, value.getClass(), expectedType);
    }
    return expectedType.cast(value);
  }

  /** 解析变量路径 nodeId.key。 */
  private Object resolvePath(String path) {
    if (path.equals("global") || path.startsWith("global.")) {
      // global 是保留作用域前缀，优先于 nodeId 匹配，避免与同名节点冲突
      int dot = path.indexOf('.');
      return dot < 0 ? globalScope : resolveGlobalPath(path.substring(dot + 1));
    }
    int dot = path.indexOf('.');
    if (dot < 0) {
      return pool.get(path);
    }
    String nodeId = path.substring(0, dot);
    String key = path.substring(dot + 1);
    return get(nodeId, key);
  }

  /** 按点分路径逐层下钻全局作用域（值必须是 Map），支持 ${global.user.name} 形式。 */
  private Object resolveGlobalPath(String path) {
    Object current = globalScope;
    for (String segment : path.split("\\.")) {
      if (!(current instanceof Map<?, ?> map)) {
        return null;
      }
      current = map.get(segment);
    }
    return current;
  }

  /** 清空变量池。 */
  public void clear() {
    pool.clear();
    globalScope.clear();
  }
}
