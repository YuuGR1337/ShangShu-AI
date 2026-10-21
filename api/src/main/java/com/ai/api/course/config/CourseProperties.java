package com.ai.api.course.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 课程知识库问答配置（add-course-kb）
 *
 * <p>对应 {@code application-dev.yml} 的 {@code ai.course.*} 配置块。
 *
 * @author add-course-kb
 */
@Setter
@Getter
@Configuration
@ConfigurationProperties("ai.course")
public class CourseProperties {

  /** 课程知识库 ID（问答检索范围；为空时检索全部知识库） */
  private Long kbId;

  /** 课程问答默认模型 ID（缺省走 DeepSeek） */
  private String modelId = "deepseek-chat";

  /** RRF 四路召回 topK */
  private int topK = 5;

  /** 知识点意图名（cs_intent 表，可随种子数据调整） */
  private String knowledgeIntent = "course_knowledge";

  /** 习题意图名（cs_intent 表，可随种子数据调整） */
  private String exerciseIntent = "course_exercise";
}
