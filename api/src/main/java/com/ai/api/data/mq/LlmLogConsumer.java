package com.ai.api.data.mq;

import com.ai.api.data.entity.SysLlmLog;
import com.ai.api.data.mapper.SysLlmLogMapper;
import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * LLM 调用日志消费者
 *
 * <p>依据 design.md D5：消费 {@code q.llm.log} 队列消息，调用 {@link SysLlmLogMapper#insert} 写入 {@code
 * sys_llm_log} 表。
 *
 * <p>幂等性：本 change 阶段 LLM 日志天然幂等（重复写入不影响业务），后续如需严格幂等 可基于 traceId + modelId + createTime 做唯一键。
 *
 * @author bootstrap-project
 */
@Slf4j
@Component
public class LlmLogConsumer {

  @Resource private SysLlmLogMapper sysLlmLogMapper;

  /**
   * 消费 LLM 日志消息。
   *
   * @param logEntry 日志实体（由全局 JacksonJsonMessageConverter 反序列化）
   * @param channel RabbitMQ Channel（用于手动 ack）
   * @param tag 投递标签
   * @throws IOException 手动 ack/nack 时可能抛出
   */
  @RabbitListener(queues = "q.llm.log")
  public void consume(
      @Payload SysLlmLog logEntry, @Header(AmqpHeaders.DELIVERY_TAG) long tag, Channel channel)
      throws IOException {
    try {
      int rows = sysLlmLogMapper.insert(logEntry);
      if (rows <= 0) {
        log.warn(
            "[consume][插入 sys_llm_log 失败 modelId({}) traceId({})]",
            logEntry.getModelId(),
            logEntry.getTraceId());
      }
      // 手动 ack
      channel.basicAck(tag, false);
    } catch (Exception ex) {
      log.error(
          "[consume][消费 LLM 日志异常 modelId({}) traceId({}) error({})]",
          logEntry.getModelId(),
          logEntry.getTraceId(),
          ex.getMessage(),
          ex);
      // 失败时重新入队（false=不批量，true=重新投递）
      channel.basicNack(tag, false, true);
    }
  }
}
