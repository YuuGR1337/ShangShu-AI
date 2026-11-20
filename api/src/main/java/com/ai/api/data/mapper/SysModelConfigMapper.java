package com.ai.api.data.mapper;

import com.ai.api.data.entity.SysModelConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 系统模型配置 Mapper
 *
 * <p>依据《企业Java后端开发规范.md》§6：所有 Mapper 继承 {@link BaseMapper}， 复杂查询写在 XML（见 {@code
 * resources/mapper/system/SysModelConfigMapper.xml}）。
 *
 * <p>单表 CRUD 由 {@link BaseMapper} 提供，自定义查询通过 XML 实现，接口仅声明方法签名。
 *
 * @author bootstrap-project
 */
@Mapper
public interface SysModelConfigMapper extends BaseMapper<SysModelConfig> {

  /**
   * 按 provider 查询未逻辑删除的模型配置列表。
   *
   * @param provider 供应商标识（deepseek/qwen/ollama）
   * @return 模型配置列表，按 id 倒序
   */
  List<SysModelConfig> selectByProvider(@Param("provider") String provider);

  /**
   * 按 modelId 精确查询未逻辑删除的模型配置。
   *
   * @param modelId 模型标识
   * @return 模型配置，未找到返回 null
   */
  SysModelConfig selectByModelId(@Param("modelId") String modelId);

  /**
   * 查询所有启用的模型配置（enabled=1 且未逻辑删除）。
   *
   * @return 启用的模型配置列表，按 provider、modelId 升序
   */
  List<SysModelConfig> selectAllEnabled();
}
