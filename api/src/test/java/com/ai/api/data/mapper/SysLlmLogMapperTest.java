package com.ai.api.data.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.api.data.config.AuditMetaObjectHandler;
import com.ai.api.data.config.MybatisPlusConfig;
import com.ai.api.data.entity.SysLlmLog;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import java.time.LocalDateTime;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * {@link SysLlmLogMapper} 集成测试
 *
 * <p>依据 tasks.md 3.7：使用 Testcontainers MySQL 验证 CRUD 与自定义查询。覆盖：
 *
 * <ul>
 *   <li>BaseMapper.insert / selectById / deleteById
 *   <li>自定义查询：selectByTraceId / selectByModelIdAndTimeRange
 *   <li>逻辑删除：deleteById 后 selectById 应返回 null
 *   <li>MyBatis-Plus 自动填充审计字段（creator/createTime/updater/updateTime/deleted）
 * </ul>
 *
 * @author bootstrap-project
 */
@Testcontainers
@SpringBootTest(
    classes = {
      SysModelConfigMapper.class,
      SysLlmLogMapper.class,
      MybatisPlusConfig.class,
      AuditMetaObjectHandler.class,
      // 引入 Spring Boot 数据源自动配置的最小子集
      DataSourceAutoConfiguration.class,
      MybatisPlusAutoConfiguration.class
    })
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIfEnvironmentVariable(named = "DOCKER_HOST", matches = ".*")
@DisplayName("SysLlmLogMapper 集成测试")
class SysLlmLogMapperTest {

  /** Testcontainers MySQL 8 容器 */
  @Container
  static final MySQLContainer<?> MYSQL =
      new MySQLContainer<>("mysql:8.0")
          .withDatabaseName("ai_agent_test")
          .withUsername("test")
          .withPassword("test")
          .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci");

  /**
   * 动态覆盖数据源配置。
   *
   * @param registry 注册表
   */
  @DynamicPropertySource
  static void overrideProps(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    registry.add("spring.flyway.enabled", () -> "false");
    registry.add("decorator.datasource.p6spy.enable-logging", () -> "false");
    // MyBatis-Plus 配置
    registry.add("mybatis-plus.mapper-locations", () -> "classpath*:mapper/**/*.xml");
    registry.add("mybatis-plus.type-aliases-package", () -> "com.ai.api.**.entity");
    registry.add("mybatis-plus.configuration.map-underscore-to-camel-case", () -> "true");
    registry.add("mybatis-plus.global-config.db-config.logic-delete-field", () -> "deleted");
    registry.add("mybatis-plus.global-config.db-config.logic-not-delete-value", () -> "0");
    registry.add("mybatis-plus.global-config.db-config.logic-delete-value", () -> "1");
  }

  @Autowired private SysLlmLogMapper sysLlmLogMapper;
  @Autowired private DataSource dataSource;

  /**
   * 测试前手动建表。
   *
   * @throws Exception 建表异常
   */
  @BeforeEach
  void setUp() throws Exception {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.execute("DROP TABLE IF EXISTS `sys_model_config`");
    jdbcTemplate.execute("DROP TABLE IF EXISTS `sys_llm_log`");
    ScriptUtils.executeSqlScript(
        dataSource.getConnection(), new ClassPathResource("db/migration/V20260818__init.sql"));
  }

  /** 验证 BaseMapper.insert + selectById。 */
  @Test
  @Order(1)
  @DisplayName("insert + selectById 应返回正确数据")
  void insert_andSelectById_shouldReturnData() {
    // given
    SysLlmLog entity = buildLlmLog("trace-001", "deepseek-chat", "success");

    // when
    int rows = sysLlmLogMapper.insert(entity);

    // then
    assertThat(rows).isEqualTo(1);
    assertThat(entity.getId()).isNotNull();

    SysLlmLog loaded = sysLlmLogMapper.selectById(entity.getId());
    assertThat(loaded).isNotNull();
    assertThat(loaded.getTraceId()).isEqualTo("trace-001");
    assertThat(loaded.getModelId()).isEqualTo("deepseek-chat");
    assertThat(loaded.getStatus()).isEqualTo("success");
    assertThat(loaded.getTotalTokens()).isEqualTo(60);
    assertThat(loaded.getDeleted()).isEqualTo(0);
    assertThat(loaded.getCreator()).isEqualTo("system");
    assertThat(loaded.getCreateTime()).isNotNull();
  }

  /** 验证逻辑删除。 */
  @Test
  @Order(2)
  @DisplayName("deleteById（逻辑删除）后 selectById 应返回 null")
  void deleteById_shouldLogicallyDelete() {
    // given
    SysLlmLog entity = buildLlmLog("trace-002", "qwen-plus", "success");
    sysLlmLogMapper.insert(entity);
    Long id = entity.getId();

    // when
    int rows = sysLlmLogMapper.deleteById(id);

    // then
    assertThat(rows).isEqualTo(1);
    assertThat(sysLlmLogMapper.selectById(id)).isNull();
  }

  /** 验证 selectByTraceId。 */
  @Test
  @Order(3)
  @DisplayName("selectByTraceId 应按 traceId 查询日志链路")
  void selectByTraceId_shouldReturnByTraceId() {
    // given
    sysLlmLogMapper.insert(buildLlmLog("trace-chain-1", "deepseek-chat", "success"));
    sysLlmLogMapper.insert(buildLlmLog("trace-chain-1", "deepseek-chat", "success"));
    sysLlmLogMapper.insert(buildLlmLog("trace-other", "qwen-plus", "success"));

    // when
    List<SysLlmLog> chain = sysLlmLogMapper.selectByTraceId("trace-chain-1");
    List<SysLlmLog> notFound = sysLlmLogMapper.selectByTraceId("non-existed");

    // then
    assertThat(chain).hasSize(2);
    assertThat(notFound).isEmpty();
    assertThat(chain).allSatisfy(log -> assertThat(log.getTraceId()).isEqualTo("trace-chain-1"));
  }

  /** 验证 selectByModelIdAndTimeRange。 */
  @Test
  @Order(4)
  @DisplayName("selectByModelIdAndTimeRange 应按模型与时间范围查询")
  void selectByModelIdAndTimeRange_shouldFilterByModelAndTime() throws Exception {
    // given
    // 注意：测试表中 create_time 由 DB 默认值填充（CURRENT_TIMESTAMP），
    // 为避免时间边界问题，先插入两条 deepseek-chat 日志，等待 1.1s 后再插入一条 qwen-plus 日志
    sysLlmLogMapper.insert(buildLlmLog("trace-1", "deepseek-chat", "success"));
    sysLlmLogMapper.insert(buildLlmLog("trace-2", "deepseek-chat", "success"));
    Thread.sleep(1100);
    LocalDateTime boundary = LocalDateTime.now();
    Thread.sleep(1100);
    sysLlmLogMapper.insert(buildLlmLog("trace-3", "qwen-plus", "success"));

    // when：查询 deepseek-chat 在 [boundary-1s, +∞] 范围的日志
    List<SysLlmLog> deepseekLogs =
        sysLlmLogMapper.selectByModelIdAndTimeRange(
            "deepseek-chat", boundary.minusSeconds(1), LocalDateTime.now(), 10);
    List<SysLlmLog> qwenLogs =
        sysLlmLogMapper.selectByModelIdAndTimeRange(
            "qwen-plus", boundary.minusSeconds(1), LocalDateTime.now(), 10);

    // then
    assertThat(deepseekLogs).hasSizeGreaterThanOrEqualTo(1);
    assertThat(deepseekLogs)
        .allSatisfy(log -> assertThat(log.getModelId()).isEqualTo("deepseek-chat"));
    assertThat(qwenLogs).hasSize(1);
  }

  /** 验证 selectByModelIdAndTimeRange 的 limit 参数。 */
  @Test
  @Order(5)
  @DisplayName("selectByModelIdAndTimeRange 应受 limit 参数限制")
  void selectByModelIdAndTimeRange_shouldRespectLimit() {
    // given
    for (int i = 0; i < 5; i++) {
      sysLlmLogMapper.insert(buildLlmLog("trace-" + i, "deepseek-chat", "success"));
    }

    // when：limit=2
    List<SysLlmLog> limited =
        sysLlmLogMapper.selectByModelIdAndTimeRange(
            "deepseek-chat",
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(1),
            2);

    // then
    assertThat(limited).hasSize(2);
  }

  /**
   * 构造一条 SysLlmLog 实例（未设置 id/审计字段，由 MyBatis-Plus 自动填充）。
   *
   * @param traceId 链路追踪 ID
   * @param modelId 模型标识
   * @param status 状态
   * @return SysLlmLog 实例
   */
  private SysLlmLog buildLlmLog(String traceId, String modelId, String status) {
    SysLlmLog entity = new SysLlmLog();
    entity.setTraceId(traceId);
    entity.setModelId(modelId);
    entity.setPrompt("测试提示词");
    entity.setResponse("测试响应内容");
    entity.setPromptTokens(10);
    entity.setCompletionTokens(50);
    entity.setTotalTokens(60);
    entity.setLatencyMs(123);
    entity.setStatus(status);
    return entity;
  }
}
