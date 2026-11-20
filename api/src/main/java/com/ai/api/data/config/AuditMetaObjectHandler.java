package com.ai.api.data.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/**
 * MyBatis-Plus 字段自动填充处理器
 *
 * <p>依据《企业Java后端开发规范.md》§3.3：BaseDO 的 {@code creator/createTime/updater/ updateTime/deleted}
 * 由框架自动填充，业务代码无需关心。
 *
 * <p>填充策略：
 *
 * <ul>
 *   <li>INSERT：creator/createTime/updater/updateTime/deleted
 *   <li>UPDATE：updater/updateTime
 * </ul>
 *
 * <p>当前简化版本：creator/updater 填充固定值 "system"，后续接入鉴权后从 SecurityContext 获取。
 *
 * @author bootstrap-project
 */
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

  /** 默认操作人（接入鉴权后从 SecurityContext 获取） */
  private static final String DEFAULT_OPERATOR = "system";

  @Override
  public void insertFill(MetaObject metaObject) {
    LocalDateTime now = LocalDateTime.now();
    String operator = getOperator();
    // creator
    this.strictInsertFill(metaObject, "creator", String.class, operator);
    // createTime
    this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
    // updater
    this.strictInsertFill(metaObject, "updater", String.class, operator);
    // updateTime
    this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
    // deleted
    this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
  }

  @Override
  public void updateFill(MetaObject metaObject) {
    // updater
    this.strictUpdateFill(metaObject, "updater", String.class, getOperator());
    // updateTime
    this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
  }

  /**
   * 获取当前操作人（接入鉴权后从 SecurityContext 获取，当前返回默认值）。
   *
   * @return String
   */
  private String getOperator() {
    // TODO: 接入 RBAC 后从 SecurityContextHolder 获取当前用户
    return DEFAULT_OPERATOR;
  }
}
