package com.ai.api.prompt.controller;

import com.ai.api.audit.annotation.OperationLog;
import com.ai.api.common.result.PageResult;
import com.ai.api.common.result.Result;
import com.ai.api.prompt.dto.PromptTemplateReqDTO;
import com.ai.api.prompt.dto.PromptTemplateRespDTO;
import com.ai.api.prompt.entity.PromptTemplateVersion;
import com.ai.api.prompt.service.PromptTemplateService;
import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/prompt-templates")
@RequiredArgsConstructor
public class PromptTemplateController {

  private final PromptTemplateService service;

  @GetMapping
  public Result<PageResult<PromptTemplateRespDTO>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String category,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size) {
    return Result.success(service.list(keyword, category, page, size));
  }

  @GetMapping("/{id}")
  public Result<PromptTemplateRespDTO> get(@PathVariable Long id) {
    return Result.success(service.get(id));
  }

  @PostMapping
  @OperationLog(module = "prompt-template", action = "create")
  public Result<PromptTemplateRespDTO> create(
      @Valid @RequestBody PromptTemplateReqDTO req, @AuthenticationPrincipal UserDetails user) {
    String username = user != null ? user.getUsername() : "system";
    return Result.success(service.create(req, username));
  }

  @PutMapping("/{id}")
  @OperationLog(module = "prompt-template", action = "update")
  public Result<PromptTemplateRespDTO> update(
      @PathVariable Long id, @Valid @RequestBody PromptTemplateReqDTO req) {
    return Result.success(service.update(id, req));
  }

  @PostMapping("/{id}/new-version")
  @OperationLog(module = "prompt-template", action = "new-version")
  public Result<PromptTemplateRespDTO> createNewVersion(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails user) {
    String username = user != null ? user.getUsername() : "system";
    return Result.success(service.createNewVersion(id, username));
  }

  @DeleteMapping("/{id}")
  @OperationLog(module = "prompt-template", action = "delete")
  public Result<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return Result.success(null);
  }

  // ========== 版本历史 V20261120 ==========

  /** 查询版本历史列表。 */
  @GetMapping("/{id}/versions")
  public Result<List<PromptTemplateVersion>> listVersions(@PathVariable Long id) {
    return Result.success(service.listVersions(id));
  }

  @Data
  static class PublishReq {
    /** 本次变更说明（可空） */
    String changeLog;
  }

  /** 发布当前模板：snapshot 到版本历史表，主表 version++。 */
  @PostMapping("/{id}/publish")
  @OperationLog(module = "prompt-template", action = "publish")
  public Result<PromptTemplateRespDTO> publish(
      @PathVariable Long id,
      @RequestBody(required = false) PublishReq req,
      @AuthenticationPrincipal UserDetails user) {
    String username = user != null ? user.getUsername() : "system";
    String changeLog = req != null ? req.changeLog : null;
    return Result.success(service.publish(id, changeLog, username));
  }

  @Data
  static class RollbackReq {
    /** 要回滚到的历史版本号 */
    Integer version;
  }

  /** 回滚到指定历史版本。 */
  @PostMapping("/{id}/rollback")
  @OperationLog(module = "prompt-template", action = "rollback")
  public Result<PromptTemplateRespDTO> rollback(
      @PathVariable Long id, @RequestBody RollbackReq req) {
    return Result.success(service.rollback(id, req.version));
  }
}
