package com.ai.api.identity.dto;

import lombok.Data;

/**
 * 微信小程序登录请求 DTO
 *
 * <p>携带小程序端 {@code uni.login()} 获取的临时登录凭证 {@code code}，后端用它换取 openid 并建档绑定。
 *
 * @author add-wechat-mini-login
 */
@Data
public class WechatMiniLoginRequestDTO {

  /** 小程序端 uni.login 返回的临时登录凭证（5 分钟有效，只能使用一次） */
  private String code;
}
