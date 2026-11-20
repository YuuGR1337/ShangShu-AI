package com.ai.api.data.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * RabbitMQ 队列声明
 *
 * <p>依据 design.md D6：声明 {@code ai.llm.log} exchange（direct）与 {@code q.llm.log} queue。
 *
 * <p>add-chat-module change：新增 {@code chat.message.log} 队列与死信队列。
 *
 * <p>注意：Bean 名称与方法名相同，避免重复声明冲突。
 *
 * @author bootstrap-project
 */
@Configuration
public class RabbitMqConfig {

  public static final String LLM_LOG_EXCHANGE = "ai.llm.log";
  public static final String LLM_LOG_QUEUE = "q.llm.log";
  public static final String LLM_LOG_ROUTING_KEY = "llm.log";

  // ===== add-chat-module: 聊天消息日志队列 =====
  public static final String CHAT_LOG_EXCHANGE = "chat.exchange";
  public static final String CHAT_LOG_QUEUE = "chat.message.log";
  public static final String CHAT_LOG_DLX = "chat.message.log.dlx";
  public static final String CHAT_LOG_ROUTING_KEY = "chat.message.log";

  // ===== add-p2-cache-search-rewrite: 对话历史 ES 索引任务队列 =====
  public static final String CHAT_SEARCH_INDEX_EXCHANGE = "chat.search";
  public static final String CHAT_SEARCH_INDEX_QUEUE = "chat.search.index";
  public static final String CHAT_SEARCH_INDEX_DLX = "chat.search.index.dlq";
  public static final String CHAT_SEARCH_INDEX_ROUTING_KEY = "chat.search.index";

  // ===== add-p3-rag-feedback-ops: 文档批量操作异步任务队列 =====
  public static final String DOCUMENT_BATCH_EXCHANGE = "document.batch";
  public static final String DOCUMENT_BATCH_QUEUE = "document.batch.task";
  public static final String DOCUMENT_BATCH_DLX = "document.batch.task.dlq";
  public static final String DOCUMENT_BATCH_ROUTING_KEY = "document.batch.task";

  /**
   * 全局消息转换器：JSON 序列化（Spring AMQP 4.x / Jackson 3）。
   *
   * <p>替代默认 {@code SimpleMessageConverter}（JDK 原生序列化），从根本上解决反序列化安全白名单拦截问题 （{@code
   * SecurityException: Attempt to deserialize unauthorized class java.util.HashMap}）。
   *
   * <p>Spring Boot 会自动将唯一的 {@link MessageConverter} Bean 应用到 {@code RabbitTemplate} 与所有
   * {@code @RabbitListener} 监听容器工厂，全局生效。复用 Spring Boot 自动配置的 {@link JsonMapper}（含 {@code
   * JacksonConfig} 中的日期格式 / Long 转字符串等定制），保证 MQ 消息与 HTTP API 序列化行为一致。
   */
  @Bean
  public MessageConverter jacksonJsonMessageConverter(JsonMapper jsonMapper) {
    return new JacksonJsonMessageConverter(jsonMapper);
  }

  /** LLM 日志 direct exchange */
  @Bean
  public DirectExchange aiLlmLogExchange() {
    return new DirectExchange(LLM_LOG_EXCHANGE, true, false);
  }

  /** LLM 日志 queue（durable，非 exclusive，不自动删除） */
  @Bean
  public Queue qLlmLog() {
    return QueueBuilder.durable(LLM_LOG_QUEUE).build();
  }

  /** 绑定 queue → exchange with routing key */
  @Bean
  public Binding bindingLlmLog() {
    return BindingBuilder.bind(qLlmLog()).to(aiLlmLogExchange()).with(LLM_LOG_ROUTING_KEY);
  }

  // ===== add-chat-module: 聊天消息日志 =====

  /** 聊天消息日志 dead letter queue */
  @Bean
  public Queue chatMessageLogDlq() {
    return QueueBuilder.durable(CHAT_LOG_DLX).build();
  }

  /** 聊天消息日志 queue（绑定死信队列） */
  @Bean
  public Queue chatMessageLogQueue() {
    return QueueBuilder.durable(CHAT_LOG_QUEUE)
        .withArgument("x-dead-letter-exchange", "")
        .withArgument("x-dead-letter-routing-key", CHAT_LOG_DLX)
        .build();
  }

  /** 聊天消息日志 topic exchange */
  @Bean
  public org.springframework.amqp.core.TopicExchange chatExchange() {
    return new org.springframework.amqp.core.TopicExchange(CHAT_LOG_EXCHANGE, true, false);
  }

  /** 绑定 chat.message.log queue → chat.exchange */
  @Bean
  public Binding bindingChatLog() {
    return BindingBuilder.bind(chatMessageLogQueue()).to(chatExchange()).with(CHAT_LOG_ROUTING_KEY);
  }

  // ===== add-p2-cache-search-rewrite: 对话历史 ES 索引任务 =====

  /** 对话历史 ES 索引死信队列 */
  @Bean
  public Queue chatSearchIndexDlq() {
    return QueueBuilder.durable(CHAT_SEARCH_INDEX_DLX).build();
  }

  /** 对话历史 ES 索引任务 queue（绑定死信队列） */
  @Bean
  public Queue chatSearchIndexQueue() {
    return QueueBuilder.durable(CHAT_SEARCH_INDEX_QUEUE)
        .withArgument("x-dead-letter-exchange", "")
        .withArgument("x-dead-letter-routing-key", CHAT_SEARCH_INDEX_DLX)
        .build();
  }

  /** 对话历史 ES 索引 topic exchange */
  @Bean
  public org.springframework.amqp.core.TopicExchange chatSearchExchange() {
    return new org.springframework.amqp.core.TopicExchange(CHAT_SEARCH_INDEX_EXCHANGE, true, false);
  }

  /** 绑定 chat.search.index queue → chat.search exchange */
  @Bean
  public Binding bindingChatSearchIndex() {
    return BindingBuilder.bind(chatSearchIndexQueue())
        .to(chatSearchExchange())
        .with(CHAT_SEARCH_INDEX_ROUTING_KEY);
  }

  // ===== add-p3-rag-feedback-ops: 文档批量操作 =====

  /** 文档批量操作死信队列 */
  @Bean
  public Queue documentBatchDlq() {
    return QueueBuilder.durable(DOCUMENT_BATCH_DLX).build();
  }

  /** 文档批量操作 queue（绑定死信队列） */
  @Bean
  public Queue documentBatchQueue() {
    return QueueBuilder.durable(DOCUMENT_BATCH_QUEUE)
        .withArgument("x-dead-letter-exchange", "")
        .withArgument("x-dead-letter-routing-key", DOCUMENT_BATCH_DLX)
        .build();
  }

  /** 文档批量操作 topic exchange */
  @Bean
  public org.springframework.amqp.core.TopicExchange documentBatchExchange() {
    return new org.springframework.amqp.core.TopicExchange(DOCUMENT_BATCH_EXCHANGE, true, false);
  }

  /** 绑定 document.batch.task queue → document.batch exchange */
  @Bean
  public Binding bindingDocumentBatch() {
    return BindingBuilder.bind(documentBatchQueue())
        .to(documentBatchExchange())
        .with(DOCUMENT_BATCH_ROUTING_KEY);
  }
}
