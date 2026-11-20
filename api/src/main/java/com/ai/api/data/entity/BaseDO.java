package com.ai.api.data.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 基础 DO（Data Object）
 *
 * <p>依据《企业Java后端开发规范.md》§3.3 与 design.md D6：所有 DO 继承此类，统一包含：
 *
 * <ul>
 *   <li>{@code id}：主键，自增
 *   <li>{@code creator}：创建人
 *   <li>{@code createTime}：创建时间
 *   <li>{@code updater}：更新人
 *   <li>{@code updateTime}：更新时间
 *   <li>{@code deleted}：逻辑删除标识（0=未删除，1=已删除）
 * </ul>
 *
 * <p>注意：DO 严格禁止直接对外暴露（Controller 不可返回 DO），必须转换为 VO/DTO。
 *
 * @author bootstrap-project
 */
@Data
public abstract class BaseDO implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 主键 ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 创建人 */
  @TableField(fill = FieldFill.INSERT)
  private String creator;

  /** 创建时间 */
  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createTime;

  /** 更新人 */
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private String updater;

  /** 更新时间 */
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updateTime;

  /** 逻辑删除标识：0=未删除，1=已删除 */
  @TableLogic
  @TableField(fill = FieldFill.INSERT)
  private Integer deleted;
}
