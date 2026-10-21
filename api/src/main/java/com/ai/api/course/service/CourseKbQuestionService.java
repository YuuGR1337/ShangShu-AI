package com.ai.api.course.service;

import com.ai.api.common.exception.BusinessException;
import com.ai.api.common.exception.ErrorCode;
import com.ai.api.course.config.CourseProperties;
import com.ai.api.cs.intent.IntentClassifierSkill;
import com.ai.api.knowledge.service.RagRetrievalService;
import com.ai.api.model.config.ChatClientFactory;
import com.ai.api.model.route.service.ModelRouteService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 课程知识库问答编排 Service（add-course-kb）
 *
 * <p>对应实施方案「阶段 2.2 课程图谱问答编排」：意图分类 → 四路召回（RRF）→ 课程 prompt → LLM 生成。
 *
 * <ul>
 *   <li><b>知识点/习题意图</b>：走 {@link RagRetrievalService#retrieveWithRrf(Long, String, int)}
 *       四路召回（semantic/fulltext/parent/knowledge_graph，图谱路已含 BFS 多跳），基于检索资料组织答案；
 *       习题类问题追加「先答案后解析、注明考察知识点」的格式约束。
 *   <li><b>闲聊兜底</b>：非课程意图直接走模型（跳过检索），保证对话不中断。
 * </ul>
 *
 * @author add-course-kb
 */
@Slf4j
@Service
public class CourseKbQuestionService {

  private final CourseProperties properties;
  private final IntentClassifierSkill intentClassifierSkill;
  private final RagRetrievalService ragRetrievalService;
  private final ChatClientFactory chatClientFactory;
  private final ModelRouteService modelRouteService;

  public CourseKbQuestionService(
      CourseProperties properties,
      IntentClassifierSkill intentClassifierSkill,
      RagRetrievalService ragRetrievalService,
      ChatClientFactory chatClientFactory,
      ModelRouteService modelRouteService) {
    this.properties = properties;
    this.intentClassifierSkill = intentClassifierSkill;
    this.ragRetrievalService = ragRetrievalService;
    this.chatClientFactory = chatClientFactory;
    this.modelRouteService = modelRouteService;
  }

  /**
   * 课程问答编排入口。
   *
   * @param query 用户问题
   * @param history 多轮历史对话（[{role, content}]，可空）
   * @param modelId 模型 ID（可空，缺省用 {@code ai.course.model-id} 配置）
   * @return 回答文本
   */
  public String answer(String query, List<Map<String, String>> history, String modelId) {
    if (!StringUtils.hasText(query)) {
      throw new BusinessException(ErrorCode.COMMON_BAD_PARAM, "问题内容不能为空");
    }

    // 1. 意图分类（关键词规则优先，LLM 兜底；规则来自 cs_intent 表，可热更新）
    Map<String, Object> intentResult =
        intentClassifierSkill.classify(
            Map.of("query", query, "history", history == null ? List.of() : history));
    String intent = String.valueOf(intentResult.get("intent"));
    boolean isKnowledge = properties.getKnowledgeIntent().equals(intent);
    boolean isExercise = properties.getExerciseIntent().equals(intent);

    // 2. 闲聊兜底：非课程意图直接走模型（跳过检索）
    if (!isKnowledge && !isExercise) {
      log.debug("[CourseKbQuestion][非课程意图({})，直接闲聊]", intent);
      return callModelWithFallback(modelId, buildChatPrompt(query, history));
    }

    // 3. 课程意图 → RRF 四路召回（语义/关键词/父文档/知识图谱）；检索失败降级为普通对话
    List<Document> docs;
    try {
      docs = ragRetrievalService.retrieveWithRrf(properties.getKbId(), query, properties.getTopK());
    } catch (Exception e) {
      log.warn("[CourseKbQuestion][检索失败，降级普通对话][query({}) error({})]", query, e.getMessage());
      docs = List.of();
    }

    // 4. 拼课程 prompt（习题类追加「答案+解析」格式约束）并调用模型
    String prompt = buildCoursePrompt(intent, query, history, docs);
    return callModelWithFallback(modelId, prompt);
  }

  /** 课程资料检索 + 回答约束 prompt（知识点/习题差异化格式）。 */
  private String buildCoursePrompt(
      String intent, String query, List<Map<String, String>> history, List<Document> docs) {
    StringBuilder sb = new StringBuilder();
    sb.append("你是计算机网络课程学习助手，请基于【课程资料】回答问题。").append("资料来自课程知识库与知识图谱，如资料不足以回答请如实说明。\n");
    if (properties.getExerciseIntent().equals(intent)) {
      sb.append("用户问的是【习题类】问题：请先给出答案，再给出解题思路/解析；涉及考察的知识点请注明。\n");
    } else {
      sb.append("用户问的是【知识点类】问题：请条理清晰地解释概念/原理，如存在前置、包含、考察的知识点关系请一并说明。\n");
    }
    if (history != null && !history.isEmpty()) {
      sb.append("\n[历史对话]\n");
      for (Map<String, String> m : history) {
        sb.append(m.getOrDefault("role", "user"))
            .append(": ")
            .append(m.getOrDefault("content", ""))
            .append("\n");
      }
    }
    sb.append("\n[课程资料]\n");
    if (docs == null || docs.isEmpty()) {
      sb.append("（未检索到相关课程资料）\n");
    } else {
      for (int i = 0; i < docs.size(); i++) {
        String text = docs.get(i).getText();
        if (text == null) {
          text = "";
        }
        sb.append(i + 1).append(". ").append(text).append('\n');
      }
    }
    sb.append("\n[用户问题]\n").append(query);
    return sb.toString();
  }

  /** 闲聊 prompt：注入历史对话保持多轮连贯。 */
  private String buildChatPrompt(String query, List<Map<String, String>> history) {
    StringBuilder sb = new StringBuilder();
    if (history != null && !history.isEmpty()) {
      sb.append("[历史对话]\n");
      for (Map<String, String> m : history) {
        sb.append(m.getOrDefault("role", "user"))
            .append(": ")
            .append(m.getOrDefault("content", ""))
            .append("\n");
      }
      sb.append('\n');
    }
    sb.append("[用户问题]\n").append(query);
    return sb.toString();
  }

  /**
   * 按候选模型链调用，主模型调用失败时自动降级到下一个（最后兜底本地 ollama）。
   *
   * <p>候选顺序：显式入参 modelId → 配置 {@code ai.course.model-id} → 路由策略表 {@code sys_model_route_policy}
   * 的降级链 → 内置 deepseek/ollama。同一模型去重。
   */
  private String callModelWithFallback(String modelId, String prompt) {
    List<String> candidates = buildCandidateChain(modelId);
    if (candidates.isEmpty()) {
      throw new BusinessException(ErrorCode.MODEL_NOT_ENABLED);
    }
    BusinessException lastError = null;
    for (String candidate : candidates) {
      ChatClient chatClient = chatClientFactory.getChatClient(candidate);
      if (chatClient == null) {
        log.warn("[CourseKbQuestion][模型({}) 无可用 ChatClient，跳过]", candidate);
        continue;
      }
      try {
        return chatClient.prompt().user(prompt).call().content();
      } catch (Exception e) {
        log.warn("[CourseKbQuestion][模型({}) 调用失败，尝试降级：{}]", candidate, e.getMessage());
        lastError = new BusinessException(ErrorCode.MODEL_PING_FAILED);
      }
    }
    throw lastError != null ? lastError : new BusinessException(ErrorCode.MODEL_NOT_ENABLED);
  }

  /** 构建候选模型链：显式 modelId → 配置 → 路由策略降级链 → 内置 deepseek/ollama（去重、保序）。 */
  private List<String> buildCandidateChain(String modelId) {
    LinkedHashSet<String> chain = new LinkedHashSet<>();
    if (StringUtils.hasText(modelId)) {
      chain.add(modelId);
    }
    if (StringUtils.hasText(properties.getModelId())) {
      chain.add(properties.getModelId());
    }
    // 路由策略表配置的降级链（sys_model_route_policy，运营可热更新）
    if (StringUtils.hasText(properties.getModelId())) {
      chain.addAll(modelRouteService.resolveFallbackChain(properties.getModelId()));
    }
    if (StringUtils.hasText(modelId)) {
      chain.addAll(modelRouteService.resolveFallbackChain(modelId));
    }
    // 内置兜底：deepseek → ollama（本地模型最后兜底，保证服务不中断）
    chain.add("deepseek");
    chain.add("ollama");
    return new ArrayList<>(chain);
  }
}
