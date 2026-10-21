package com.ai.api.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * 课程问答请求 DTO（add-course-kb）
 *
 * @author add-course-kb
 */
@Data
public class CourseQuestionRequestDTO {

  /** 用户问题 */
  @NotBlank(message = "问题内容不能为空")
  @Size(max = 32768, message = "问题内容过长")
  private String content;

  /** 多轮历史对话（[{role, content}]，可空） */
  private List<Map<String, String>> history;

  /** 模型 ID（可空，缺省用 ai.course.model-id 配置） */
  private String modelId;
}
