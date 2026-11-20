package com.ai.api.data.mapper;

import com.ai.api.data.entity.SysLlmLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * LLM 调用日志 Mapper
 *
 * <p>依据《企业Java后端开发规范.md》§6：所有 Mapper 继承 {@link BaseMapper}， 复杂查询写在 XML（见 {@code
 * resources/mapper/system/SysLlmLogMapper.xml}）。
 *
 * <p>单表 CRUD 由 {@link BaseMapper} 提供，自定义查询通过 XML 实现，接口仅声明方法签名。
 *
 * @author bootstrap-project
 */
@Mapper
public interface SysLlmLogMapper extends BaseMapper<SysLlmLog> {

  /**
   * 按 traceId 查询调用链路日志。
   *
   * @param traceId 链路追踪 ID
   * @return 日志列表，按 id 升序
   */
  List<SysLlmLog> selectByTraceId(@Param("traceId") String traceId);

  /**
   * 按模型与时间范围统计调用日志。
   *
   * @param modelId 模型标识
   * @param startTime 起始时间（包含）
   * @param endTime 结束时间（包含）
   * @param limit 最大返回条数
   * @return 日志列表，按 create_time 倒序
   */
  List<SysLlmLog> selectByModelIdAndTimeRange(
      @Param("modelId") String modelId,
      @Param("startTime") LocalDateTime startTime,
      @Param("endTime") LocalDateTime endTime,
      @Param("limit") int limit);
}
