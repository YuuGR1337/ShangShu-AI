package com.ai.api.data.config;

import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.RedisClient;

/**
 * Redis 配置（add-p2-cache-search-rewrite）。
 *
 * <p>统一配置 {@code RedisTemplate<String, Object>}，使用 Jackson 序列化 value， 供 ChatHistoryCacheService /
 * RagHitsCacheService 使用。不影响既有 {@link org.springframework.data.redis.core.StringRedisTemplate}
 * Bean。
 *
 * @author add-p2-cache-search-rewrite
 */
@Configuration
public class RedisConfig {

  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
    template.afterPropertiesSet();
    return template;
  }

  /**
   * Jedis {@link RedisClient}（Spring AI RedisChatMemoryRepository 专用）。
   *
   * <p>Spring AI 2.0 的 {@code RedisChatMemoryAutoConfiguration.jedisClient()} 只传 host/port
   * 不传密码，Redis 开启 requirepass 时 {@code initializeSchema()} 抛 {@code NOAUTH Authentication
   * required}。该方法标注了 {@code @ConditionalOnMissingBean}，此处自行定义带密码的客户端即可覆盖。 连接参数复用 {@code
   * spring.data.redis.*}（host/port/password/database）。
   */
  @Bean
  public RedisClient redisClient(DataRedisProperties redisProperties) {
    DefaultJedisClientConfig.Builder clientConfig =
        DefaultJedisClientConfig.builder().database(redisProperties.getDatabase());
    if (redisProperties.getTimeout() != null) {
      int timeoutMillis = (int) redisProperties.getTimeout().toMillis();
      clientConfig.connectionTimeoutMillis(timeoutMillis).socketTimeoutMillis(timeoutMillis);
    }
    if (redisProperties.getPassword() != null && !redisProperties.getPassword().isEmpty()) {
      clientConfig.password(redisProperties.getPassword());
    }
    return RedisClient.builder()
        .hostAndPort(redisProperties.getHost(), redisProperties.getPort())
        .clientConfig(clientConfig.build())
        .build();
  }
}
