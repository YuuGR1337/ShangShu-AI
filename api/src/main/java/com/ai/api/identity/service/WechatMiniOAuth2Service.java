package com.ai.api.identity.service;

import com.ai.api.common.exception.BusinessException;
import com.ai.api.common.exception.ErrorCode;
import com.ai.api.identity.config.AuthProperties;
import com.ai.api.identity.dto.LoginResponseDTO;
import com.ai.api.identity.entity.SysUserOauth;
import com.ai.api.identity.entity.SystemUser;
import com.ai.api.identity.mapper.SysUserOauthMapper;
import com.ai.api.identity.mapper.SystemUserMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 微信小程序登录服务
 *
 * <p>与 Gitee OAuth2 的区别：小程序端无法走浏览器 302 授权跳转，而是由客户端调用 {@code uni.login()} 获取临时登录凭证 {@code
 * code}，后端直接调微信 {@code jscode2session} 接口换取 {@code openid}（平台用户唯一标识）。
 *
 * <p>流程：
 *
 * <ol>
 *   <li>小程序端 {@code uni.login()} 获取 code；
 *   <li>{@link #loginWithCode(String)}：code 换 openid/session_key → 按 (provider, openId) 查找或新建
 *       本地用户并绑定 → 复用 {@link AuthService} 签发 JWT。
 * </ol>
 *
 * <p>注意：微信 code 5 分钟有效且只能使用一次；本服务无 state 环节（无浏览器重定向，天然规避授权码劫持）。
 *
 * @author add-wechat-mini-login
 */
@Slf4j
@Service
public class WechatMiniOAuth2Service {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final String PROVIDER = "wechat_mini";
  private static final String PLACEHOLDER_EMAIL_DOMAIN = "oauth.local";

  private final AuthProperties authProperties;
  private final ObjectMapper objectMapper;
  private final SystemUserMapper systemUserMapper;
  private final SysUserOauthMapper sysUserOauthMapper;
  private final PasswordEncoder passwordEncoder;
  private final AuthService authService;
  private final RestClient restClient;

  public WechatMiniOAuth2Service(
      AuthProperties authProperties,
      ObjectMapper objectMapper,
      SystemUserMapper systemUserMapper,
      SysUserOauthMapper sysUserOauthMapper,
      PasswordEncoder passwordEncoder,
      AuthService authService) {
    this.authProperties = authProperties;
    this.objectMapper = objectMapper;
    this.systemUserMapper = systemUserMapper;
    this.sysUserOauthMapper = sysUserOauthMapper;
    this.passwordEncoder = passwordEncoder;
    this.authService = authService;
    this.restClient = RestClient.builder().build();
  }

  /**
   * 微信小程序一键登录：code 换 openid → 查找或新建本地用户并绑定 → 签发 JWT。
   *
   * @param code 小程序端 uni.login 返回的临时登录凭证
   * @return 登录响应（含 access/refresh token）
   */
  @Transactional(rollbackFor = Exception.class)
  public LoginResponseDTO loginWithCode(String code) {
    AuthProperties.WechatMini cfg = authProperties.getOauth().getWechatMini();
    ensureConfigured(cfg);

    if (!StringUtils.hasText(code)) {
      throw new BusinessException(ErrorCode.AUTH_OAUTH_FAILED, "缺少登录凭证 code");
    }

    JsonNode session = jscode2session(cfg, code);
    // 微信错误时返回 errcode != 0（如 40029 code 无效、40163 code 已被使用）
    int errcode = session.path("errcode").asInt(0);
    if (errcode != 0) {
      log.warn("[WechatMiniOAuth][jscode2session 返回错误：{}]", session);
      throw new BusinessException(ErrorCode.AUTH_OAUTH_FAILED);
    }
    String openId = session.path("openid").asText(null);
    if (!StringUtils.hasText(openId)) {
      log.warn("[WechatMiniOAuth][jscode2session 缺少 openid：{}]", session);
      throw new BusinessException(ErrorCode.AUTH_OAUTH_FAILED);
    }
    String unionId = session.path("unionid").asText(null);

    SystemUser user = findOrCreateUser(openId, unionId, session);
    return authService.issueTokenPairForOAuth(user);
  }

  /** 调用微信 jscode2session 接口换取 openid/session_key。 */
  private JsonNode jscode2session(AuthProperties.WechatMini cfg, String code) {
    String url =
        cfg.getJscode2sessionUrl()
            + "?appid="
            + urlEncode(cfg.getAppId())
            + "&secret="
            + urlEncode(cfg.getAppSecret())
            + "&js_code="
            + urlEncode(code)
            + "&grant_type=authorization_code";
    String body;
    try {
      body = restClient.get().uri(url).retrieve().body(String.class);
    } catch (Exception e) {
      log.warn("[WechatMiniOAuth][jscode2session 请求失败：{}]", e.getMessage());
      throw new BusinessException(ErrorCode.AUTH_OAUTH_FAILED);
    }
    return readJson(body);
  }

  /** 查找现有绑定；不存在则新建本地用户并写入绑定关系。 */
  private SystemUser findOrCreateUser(String openId, String unionId, JsonNode session) {
    SysUserOauth existed = sysUserOauthMapper.selectByProviderAndOpenId(PROVIDER, openId);
    if (existed != null) {
      SystemUser user = systemUserMapper.selectById(existed.getUserId());
      if (user == null) {
        throw new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND);
      }
      return user;
    }

    SystemUser user = new SystemUser();
    user.setUsername(generateUniqueUsername(openId));
    user.setEmail(generateEmail(openId));
    // 微信用户无密码登录：写入随机不可猜密码哈希以满足 NOT NULL，登录仅走第三方通道
    user.setPasswordHash(passwordEncoder.encode(generateRandomPassword()));
    user.setStatus(1);
    user.setFailCount(0);
    systemUserMapper.insert(user);

    SysUserOauth bind = new SysUserOauth();
    bind.setUserId(user.getId());
    bind.setProvider(PROVIDER);
    bind.setOpenId(openId);
    bind.setUnionId(unionId);
    bind.setNickname("微信用户_" + openId.substring(Math.min(openId.length(), 6)));
    bind.setRawProfile(session.toString());
    sysUserOauthMapper.insert(bind);

    log.info("[WechatMiniOAuth][新建并绑定用户：userId={}, openId={}]", user.getId(), openId);
    return user;
  }

  /** 生成唯一用户名：基于 openid 前缀清洗，冲突时附加序号。 */
  private String generateUniqueUsername(String openId) {
    String base = "wx_" + openId;
    base = base.replaceAll("[^a-zA-Z0-9_]", "_").replaceAll("_+", "_");
    if (base.length() > 32) {
      base = base.substring(0, 32);
    }
    if (base.length() < 3) {
      base = "wx_" + openId;
    }
    String candidate = base;
    int i = 1;
    while (systemUserMapper.selectByUsername(candidate) != null) {
      candidate = base + "_" + i;
      if (candidate.length() > 32) {
        candidate = candidate.substring(0, 32);
      }
      i++;
    }
    return candidate;
  }

  /** 生成邮箱：openid 全局唯一（同一小程序下），直接拼占位邮箱规避唯一索引冲突。 */
  private String generateEmail(String openId) {
    return "wechat_" + openId + "@" + PLACEHOLDER_EMAIL_DOMAIN;
  }

  private void ensureConfigured(AuthProperties.WechatMini cfg) {
    if (!cfg.isEnabled()
        || !StringUtils.hasText(cfg.getAppId())
        || !StringUtils.hasText(cfg.getAppSecret())) {
      throw new BusinessException(ErrorCode.AUTH_OAUTH_FAILED, "微信小程序登录未配置或已禁用");
    }
  }

  private String generateRandomPassword() {
    byte[] bytes = new byte[32];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private JsonNode readJson(String body) {
    try {
      return objectMapper.readTree(body);
    } catch (Exception e) {
      log.warn("[WechatMiniOAuth][JSON 解析失败：{}]", e.getMessage());
      throw new BusinessException(ErrorCode.AUTH_OAUTH_FAILED);
    }
  }

  private String urlEncode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
