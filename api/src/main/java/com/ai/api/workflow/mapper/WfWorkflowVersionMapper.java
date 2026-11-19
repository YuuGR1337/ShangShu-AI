package com.ai.api.workflow.mapper;

import com.ai.api.workflow.entity.WfWorkflowVersion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 工作流发布版本快照 Mapper。 */
@Mapper
public interface WfWorkflowVersionMapper extends BaseMapper<WfWorkflowVersion> {}
