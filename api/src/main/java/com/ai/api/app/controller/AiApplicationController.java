package com.ai.api.app.controller;

import com.ai.api.app.entity.AiApplication;
import com.ai.api.app.service.AiApplicationService;
import com.ai.api.audit.annotation.OperationLog;
import com.ai.api.common.result.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 应用发布 Controller（V20261121）。
 *
 * <p>两类端点：
 *
 * <ul>
 *   <li>内部管理 CRUD + 发布（需要 JWT 鉴权）
 *   <li>对外统一调用入口 {@code POST /api/v1/app/{appKey}/chat}（**不需要 JWT**，用 API Key 鉴权）
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class AiApplicationController {

  private final AiApplicationService service;

  // ============ 内部管理（需要 JWT） ============

  @GetMapping("/api/v1/apps")
  public Result<Page<AiApplication>> list(
      @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
    return Result.success(service.list(page, size));
  }

  @GetMapping("/api/v1/apps/{id}")
  public Result<AiApplication> get(@PathVariable Long id) {
    return Result.success(service.get(id));
  }

  @PostMapping("/api/v1/apps")
  @OperationLog(module = "ai-app", action = "create")
  public Result<AiApplication> create(
      @RequestBody AiApplication app, @AuthenticationPrincipal UserDetails user) {
    String username = user != null ? user.getUsername() : "system";
    return Result.success(service.create(app, username));
  }

  @PutMapping("/api/v1/apps/{id}")
  @OperationLog(module = "ai-app", action = "update")
  public Result<AiApplication> update(@PathVariable Long id, @RequestBody AiApplication app) {
    return Result.success(service.update(id, app));
  }

  @DeleteMapping("/api/v1/apps/{id}")
  @OperationLog(module = "ai-app", action = "delete")
  public Result<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return Result.success(null);
  }

  /**
   * 发布 AI 应用：生成 API Key（明文仅返回一次）。
   *
   * <p>⚠️ 密钥只在本次响应返回，请妥善保存；数据库只存 SHA256 哈希。
   */
  @PostMapping("/api/v1/apps/{id}/publish")
  @OperationLog(module = "ai-app", action = "publish")
  public Result<Map<String, String>> publish(@PathVariable Long id) {
    String apiKey = service.publish(id);
    Map<String, String> result = new HashMap<>();
    result.put("apiKey", apiKey);
    result.put("message", "密钥仅展示一次，请妥善保存");
    return Result.success(result);
  }

  // ============ 对外统一调用入口（**不需要 JWT**，SecurityConfig 需加白名单） ============

  @Data
  static class ChatReq {
    /** 用户消息（简单格式，与 messages 二选一） */
    String message;

    /** OpenAI 兼容格式：[{role, content}] 消息数组（最后一条作为本轮输入，其余为多轮历史） */
    java.util.List<Map<String, String>> messages;

    /** 可选：历史多轮消息列表（简单格式，与 messages 互斥） */
    java.util.List<Map<String, String>> history;
  }

  /**
   * OpenAI Chat Completions 兼容入口。
   *
   * <p>路径中的 {@code appKey} 即发布时返回的明文 API Key；内部以 SHA256 哈希查库鉴权。
   *
   * <p>返回结构对齐 OpenAI：{@code {"choices":[{"message":{"role":"assistant","content":"..."}}]}}
   */
  @PostMapping("/api/v1/app/{appKey}/chat")
  public Map<String, Object> chat(@PathVariable String appKey, @RequestBody ChatReq req) {
    // 1. API Key 鉴权
    AiApplication app = service.findByApiKey(appKey);
    if (app == null) {
      Map<String, Object> err = new HashMap<>();
      err.put("error", Map.of("message", "无效或已禁用的 API Key", "type", "invalid_api_key"));
      return err;
    }

    // 2. 归一化输入：messages（OpenAI 格式）优先，回退 message + history
    String userMessage = req.message;
    java.util.List<Map<String, String>> history = req.history;
    if (req.messages != null && !req.messages.isEmpty()) {
      Map<String, String> last = req.messages.get(req.messages.size() - 1);
      userMessage = last.getOrDefault("content", "");
      history = req.messages.subList(0, req.messages.size() - 1);
    }
    String reply = service.chat(app, userMessage, history);

    // 3. OpenAI 兼容响应
    Map<String, Object> message = new HashMap<>();
    message.put("role", "assistant");
    message.put("content", reply);

    Map<String, Object> choice = new HashMap<>();
    choice.put("index", 0);
    choice.put("message", message);
    choice.put("finish_reason", "stop");

    Map<String, Object> response = new HashMap<>();
    response.put("id", "chatcmpl-" + Instant.now().toEpochMilli());
    response.put("object", "chat.completion");
    response.put("created", Instant.now().toEpochMilli());
    response.put("model", app.getType() + "-app");
    response.put("choices", java.util.List.of(choice));
    response.put("usage", Map.of("prompt_tokens", 0, "completion_tokens", 0, "total_tokens", 0));
    return response;
  }
}
