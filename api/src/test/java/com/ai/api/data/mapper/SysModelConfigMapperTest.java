package com.ai.api.data.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.api.data.config.AuditMetaObjectHandler;
import com.ai.api.data.config.MybatisPlusConfig;
import com.ai.api.data.entity.SysModelConfig;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
 * {@link SysModelConfigMapper} 集成测试
 *
 * <p>依据 tasks.md 3.7：使用 Testcontainers MySQL 验证 CRUD 与分页。覆盖：
 *
 * <ul>
 *   <li>BaseMapper.insert / selectById / updateById / deleteById
 *   <li>自定义查询：selectByProvider / selectByModelId / selectAllEnabled
 *   <li>分页插件：selectPage
 *   <li>逻辑删除：deleteById 后 selectById 应返回 null
 * </ul>
 *
 * <p>通过 Testcontainers 提供真实 MySQL 8 环境，使用 Flyway 初始脚本建表（仅取 DDL 部分），
 * 不依赖任何外部中间件（Redis/ES/RabbitMQ），可在 CI 中直接运行。
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
@DisplayName("SysModelConfigMapper 集成测试")
class SysModelConfigMapperTest {

  /** Testcontainers MySQL 8 容器 */
  @Container
  static final MySQLContainer<?> MYSQL =
      new MySQLContainer<>("mysql:8.0")
          .withDatabaseName("ai_agent_test")
          .withUsername("test")
          .withPassword("test")
          .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci");

  /**
   * 动态覆盖数据源配置，使用 Testcontainers 提供的 JDBC URL（不走 p6spy 代理）。
   *
   * @param registry 注册表
   */
  @DynamicPropertySource
  static void overrideProps(DynamicPropertyRegistry registry) {
    // 直接使用 MySQL 驱动（不走 p6spy，避免 Testcontainers 下初始化复杂依赖）
    registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    // 禁用 Flyway（测试通过 init SQL 手动建表）
    registry.add("spring.flyway.enabled", () -> "false");
    // 禁用 p6spy
    registry.add("decorator.datasource.p6spy.enable-logging", () -> "false");
    // MyBatis-Plus 配置
    registry.add("mybatis-plus.mapper-locations", () -> "classpath*:mapper/**/*.xml");
    registry.add("mybatis-plus.type-aliases-package", () -> "com.ai.api.**.entity");
    registry.add("mybatis-plus.configuration.map-underscore-to-camel-case", () -> "true");
    registry.add("mybatis-plus.global-config.db-config.logic-delete-field", () -> "deleted");
    registry.add("mybatis-plus.global-config.db-config.logic-not-delete-value", () -> "0");
    registry.add("mybatis-plus.global-config.db-config.logic-delete-value", () -> "1");
  }

  @Autowired private SysModelConfigMapper sysModelConfigMapper;
  @Autowired private DataSource dataSource;

  /**
   * 测试前手动建表（避开 Flyway 对类路径迁移脚本位置约束，保证测试独立）。
   *
   * @throws Exception 建表异常
   */
  @BeforeEach
  void setUp() throws Exception {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.execute("DROP TABLE IF EXISTS `sys_model_config`");
    jdbcTemplate.execute("DROP TABLE IF EXISTS `sys_llm_log`");
    // 复用 Flyway 初始化脚本（仅 DDL 部分）
    ScriptUtils.executeSqlScript(
        dataSource.getConnection(), new ClassPathResource("db/migration/V20260818__init.sql"));
  }

  /** 验证 BaseMapper.insert + selectById。 */
  @Test
  @Order(1)
  @DisplayName("insert + selectById 应返回正确数据")
  void insert_andSelectById_shouldReturnData() {
    // given
    SysModelConfig entity = new SysModelConfig();
    entity.setProvider("deepseek");
    entity.setModelId("deepseek-chat");
    entity.setDisplayName("DeepSeek Chat");
    entity.setBaseUrl("https://api.deepseek.com");
    entity.setEnabled(1);

    // when
    int rows = sysModelConfigMapper.insert(entity);

    // then
    assertThat(rows).isEqualTo(1);
    assertThat(entity.getId()).isNotNull();

    SysModelConfig loaded = sysModelConfigMapper.selectById(entity.getId());
    assertThat(loaded).isNotNull();
    assertThat(loaded.getProvider()).isEqualTo("deepseek");
    assertThat(loaded.getModelId()).isEqualTo("deepseek-chat");
    assertThat(loaded.getDisplayName()).isEqualTo("DeepSeek Chat");
    assertThat(loaded.getEnabled()).isEqualTo(1);
    assertThat(loaded.getDeleted()).isEqualTo(0);
    assertThat(loaded.getCreator()).isEqualTo("system");
    assertThat(loaded.getCreateTime()).isNotNull();
  }

  /** 验证 BaseMapper.updateById。 */
  @Test
  @Order(2)
  @DisplayName("updateById 应正确更新字段")
  void updateById_shouldUpdateFields() {
    // given
    SysModelConfig entity = new SysModelConfig();
    entity.setProvider("qwen");
    entity.setModelId("qwen-plus");
    entity.setDisplayName("Qwen Plus Old");
    entity.setEnabled(1);
    sysModelConfigMapper.insert(entity);

    // when
    entity.setDisplayName("Qwen Plus New");
    entity.setEnabled(0);
    int rows = sysModelConfigMapper.updateById(entity);

    // then
    assertThat(rows).isEqualTo(1);
    SysModelConfig loaded = sysModelConfigMapper.selectById(entity.getId());
    assertThat(loaded.getDisplayName()).isEqualTo("Qwen Plus New");
    assertThat(loaded.getEnabled()).isEqualTo(0);
  }

  /** 验证逻辑删除：deleteById 后 selectById 应返回 null。 */
  @Test
  @Order(3)
  @DisplayName("deleteById（逻辑删除）后 selectById 应返回 null")
  void deleteById_shouldLogicallyDelete() {
    // given
    SysModelConfig entity = new SysModelConfig();
    entity.setProvider("ollama");
    entity.setModelId("qwen2.5:7b");
    entity.setDisplayName("Ollama Qwen2.5");
    entity.setEnabled(0);
    sysModelConfigMapper.insert(entity);
    Long id = entity.getId();

    // when
    int rows = sysModelConfigMapper.deleteById(id);

    // then
    assertThat(rows).isEqualTo(1);
    // 逻辑删除后 selectById 返回 null（MyBatis-Plus 自动追加 deleted=0 条件）
    assertThat(sysModelConfigMapper.selectById(id)).isNull();
  }

  /** 验证自定义查询 selectByProvider。 */
  @Test
  @Order(4)
  @DisplayName("selectByProvider 应按 provider 过滤")
  void selectByProvider_shouldFilterByProvider() {
    // given
    insertModelConfig("deepseek", "deepseek-chat", 1);
    insertModelConfig("deepseek", "deepseek-coder", 1);
    insertModelConfig("qwen", "qwen-plus", 1);

    // when
    List<SysModelConfig> deepseekList = sysModelConfigMapper.selectByProvider("deepseek");
    List<SysModelConfig> qwenList = sysModelConfigMapper.selectByProvider("qwen");
    List<SysModelConfig> ollamaList = sysModelConfigMapper.selectByProvider("ollama");

    // then
    assertThat(deepseekList).hasSize(2);
    assertThat(qwenList).hasSize(1);
    assertThat(ollamaList).isEmpty();
    assertThat(deepseekList).allSatisfy(m -> assertThat(m.getProvider()).isEqualTo("deepseek"));
  }

  /** 验证自定义查询 selectByModelId。 */
  @Test
  @Order(5)
  @DisplayName("selectByModelId 应按 modelId 精确查询")
  void selectByModelId_shouldReturnByModelId() {
    // given
    insertModelConfig("deepseek", "deepseek-chat", 1);

    // when
    SysModelConfig loaded = sysModelConfigMapper.selectByModelId("deepseek-chat");
    SysModelConfig notFound = sysModelConfigMapper.selectByModelId("non-existed");

    // then
    assertThat(loaded).isNotNull();
    assertThat(loaded.getModelId()).isEqualTo("deepseek-chat");
    assertThat(notFound).isNull();
  }

  /** 验证自定义查询 selectAllEnabled（仅返回 enabled=1 的记录）。 */
  @Test
  @Order(6)
  @DisplayName("selectAllEnabled 应仅返回启用记录")
  void selectAllEnabled_shouldReturnOnlyEnabled() {
    // given
    insertModelConfig("deepseek", "deepseek-chat", 1);
    insertModelConfig("qwen", "qwen-plus", 1);
    insertModelConfig("ollama", "qwen2.5:7b", 0);

    // when
    List<SysModelConfig> enabledList = sysModelConfigMapper.selectAllEnabled();

    // then
    assertThat(enabledList).hasSize(2);
    assertThat(enabledList).allSatisfy(m -> assertThat(m.getEnabled()).isEqualTo(1));
  }

  /** 验证分页插件 selectPage。 */
  @Test
  @Order(7)
  @DisplayName("selectPage 应正确分页")
  void selectPage_shouldReturnPagedResult() {
    // given
    for (int i = 0; i < 10; i++) {
      insertModelConfig("deepseek", "deepseek-" + i, 1);
    }

    // when
    Page<SysModelConfig> page = new Page<>(1, 5);
    Page<SysModelConfig> result =
        sysModelConfigMapper.selectPage(
            page, new QueryWrapper<SysModelConfig>().eq("provider", "deepseek"));

    // then
    assertThat(result.getRecords()).hasSize(5);
    assertThat(result.getTotal()).isEqualTo(10);
    assertThat(result.getPages()).isEqualTo(2);
    assertThat(result.getSize()).isEqualTo(5);
  }

  /**
   * 辅助方法：构造并插入一条 SysModelConfig。
   *
   * @param provider 供应商
   * @param modelId 模型标识
   * @param enabled 是否启用
   */
  private void insertModelConfig(String provider, String modelId, int enabled) {
    SysModelConfig entity = new SysModelConfig();
    entity.setProvider(provider);
    entity.setModelId(modelId);
    entity.setDisplayName(provider + "-" + modelId);
    entity.setEnabled(enabled);
    sysModelConfigMapper.insert(entity);
  }
}
