package com.ai.api.course.controller;

import com.ai.api.common.result.Result;
import com.ai.api.course.dto.CourseQuestionRequestDTO;
import com.ai.api.course.service.CourseKbQuestionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 课程知识库问答 Controller（add-course-kb）
 *
 * <p>路径前缀 {@code /api/v1/course}。课程问答采用 JSON 同步返回（非 SSE），Web 端与小程序端均可直接调用。
 *
 * @author add-course-kb
 */
@RestController
@RequestMapping("/api/v1/course")
public class CourseQuestionController {

  private final CourseKbQuestionService courseKbQuestionService;

  public CourseQuestionController(CourseKbQuestionService courseKbQuestionService) {
    this.courseKbQuestionService = courseKbQuestionService;
  }

  /** 课程智能问答：意图分类 → 四路召回 → 课程 prompt → 模型回答。 */
  @PostMapping("/question")
  public Result<String> question(@Valid @RequestBody CourseQuestionRequestDTO req) {
    return Result.success(
        courseKbQuestionService.answer(req.getContent(), req.getHistory(), req.getModelId()));
  }
}
