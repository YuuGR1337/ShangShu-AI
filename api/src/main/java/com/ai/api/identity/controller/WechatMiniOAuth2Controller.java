package com.ai.api.identity.controller;

import com.ai.api.common.result.Result;
import com.ai.api.identity.dto.LoginResponseDTO;
import com.ai.api.identity.dto.WechatMiniLoginRequestDTO;
import com.ai.api.identity.service.WechatMiniOAuth2Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信小程序登录 Controller
 *
 * <p>路径前缀 {@code /api/v1/auth/wechat-mini}。小程序端无法处理浏览器 302 重定向，因此登录接口以 JSON 方式返回令牌（而非 Gitee 的
 * redirect 模式）。
 *
 * @author add-wechat-mini-login
 */
@RestController
@RequestMapping("/api/v1/auth/wechat-mini")
public class WechatMiniOAuth2Controller {

  private final WechatMiniOAuth2Service wechatMiniOAuth2Service;

  public WechatMiniOAuth2Controller(WechatMiniOAuth2Service wechatMiniOAuth2Service) {
    this.wechatMiniOAuth2Service = wechatMiniOAuth2Service;
  }

  /** 小程序一键登录：code 换 openid → 建档绑定 → 签发 JWT（JSON 返回）。 */
  @PostMapping("/code")
  public Result<LoginResponseDTO> loginByCode(@RequestBody WechatMiniLoginRequestDTO request) {
    return Result.success(wechatMiniOAuth2Service.loginWithCode(request.getCode()));
  }
}
