# 规范执行监督与检查机制

> 本文档建立《企业Java后端开发规范.md》与《Java开发手册(黄山版).pdf》在项目开发全流程中的
> 监督、检查与反馈机制，确保所有团队成员在每个开发环节严格遵循规范要求。

## 1. 目标与范围

### 1.1 目标

- **零违规**：所有提交到主分支的代码必须通过规范检查
- **可追溯**：每次违规记录可追溯到具体提交、责任人、违规条目
- **可量化**：每月输出规范执行度报告，违规趋势可视化
- **自动化**：80%+ 的规范条目由工具自动检查，减少人工成本

### 1.2 适用范围

- **后端**：`d:\ai\api\`（Spring Boot 4.1 + MyBatis-Plus）
- **前端**：`d:\ai\ai-agent-web\`（Vue 3 + Element Plus）
- **规范文件**：
  - `d:\ai\docs\企业Java后端开发规范.md`（强制）
  - `d:\ai\docs\Java开发手册(黄山版).pdf`（强制）

## 2. 组织与责任

### 2.1 角色定义

| 角色 | 责任 | 检查频率 |
|------|------|----------|
| 开发者 | 自查、修复工具报错、修复 Review 意见 | 每次提交 |
| Code Reviewer | 人工审查规范遵循情况 | 每个 PR |
| 规范监督员 | 抽查、定期报告、推动改进 | 每周 |
| 技术负责人 | 仲裁争议、规范演进 | 每月 |

### 2.2 责任链

```
开发者自查 → 工具自动检查 → Reviewer 人工审查 → 监督员抽查 → 合并
```

任何环节发现问题，回退到开发者修复，修复后重新进入流程。

## 3. 自动化检查机制

### 3.1 提交前（Pre-commit）

**触发**：`git commit` 时由 husky pre-commit hook 触发

**检查项**：

| 工具 | 范围 | 检查内容 | 失败行为 |
|------|------|----------|----------|
| lint-staged | 前端暂存文件 | ESLint + Prettier 格式化 | 阻止提交 |
| spotless（Maven） | 后端 Java | google-java-format 格式化 | 阻止提交 |

**配置位置**：

- 前端：`d:\ai\ai-agent-web\.husky\pre-commit` + `package.json#lint-staged`
- 后端：`d:\ai\api\pom.xml` 中 `spotless-maven-plugin`（绑定 compile 阶段）

### 3.2 提交消息（Commit-msg）

**触发**：`git commit` 时由 husky commit-msg hook 触发

**检查项**：

- 提交消息必须符合 Conventional Commits 格式
- type 必须是：feat/fix/docs/style/refactor/perf/test/build/ci/chore/revert
- subject 不能为空
- header 最大长度 100 字符

**配置位置**：`d:\ai\ai-agent-web\.husky\commit-msg` + `commitlint.config.js`

### 3.3 编译期（Compile）

**触发**：`mvn compile` / `mvn validate`

**检查项**：

| 工具 | 检查内容 | 失败行为 |
|------|----------|----------|
| Checkstyle | 命名规范、Import 顺序、代码长度、空块、布尔简化 | 警告（不阻断） |
| Spotless | 代码格式化（google-java-format） | 阻断编译 |

**配置位置**：`d:\ai\api\pom.xml` 中 `maven-checkstyle-plugin` + `spotless-maven-plugin`

### 3.4 测试期（Test）

**触发**：`mvn test`

**检查项**：

- 单元测试：所有 Service / Controller 必须有对应测试
- 集成测试：Testcontainers（MySQL / RabbitMQ）
- 覆盖率：JaCoCo（后续引入，目标 ≥ 70%）

### 3.5 持续集成（CI）

**触发**：Push / PR 到主分支

**Pipeline 阶段**：

```
Checkout → Setup JDK 21 → Setup Node 22 → Cache Deps →
Maven Validate → Maven Compile → Maven Test →
NPM Install → NPM Build → NPM Lint →
Report → Notify
```

**失败行为**：阻止 PR 合并，自动评论失败原因。

## 4. 人工检查清单

### 4.1 Code Review 必查项

**依据《企业Java后端开发规范.md》**，每个 PR 必须由至少 1 名 Reviewer 检查以下条目：

#### 后端

| 序号 | 检查项 | 规范依据 | 通过标准 |
|------|--------|----------|----------|
| B01 | 分层依赖方向 | §4.1 | Controller → Service → Mapper，禁止反向依赖 |
| B02 | 对象模型分离 | §3.3 | DO/SaveReqVO/PageReqVO/RespVO/ReqDTO/RespDTO 严格分离，Controller 禁止返回 DO |
| B03 | 统一响应 | §3.2 | 所有 Controller 返回 `Result<T>` 包装 |
| B04 | 统一异常 | §5.5 | Service 抛 `BusinessException(ErrorCode.XXX)`，禁止 RuntimeException |
| B05 | 错误码命名 | §5.5 | `BUSINESS_OBJECT_SCENARIO` 格式（如 MODEL_CONFIG_NOT_FOUND） |
| B06 | BaseDO 继承 | §3.3 | 所有 DO 继承 `BaseDO`，含 id/creator/createTime/updater/updateTime/deleted |
| B07 | 逻辑删除 | §3.3 | 使用 `@TableLogic`，禁止物理删除 |
| B08 | LambdaQueryWrapper | §6 | 使用 `likeIfPresent/eqIfPresent`，禁止字符串拼接 SQL |
| B09 | validateXxxExists | §5.5 | Service 写操作前调用 `validateXxxExists` 校验 |
| B10 | @Transactional | §5.3 | 写操作标注 `@Transactional(rollbackFor = Exception.class)` |
| B11 | 依赖注入 | §4.2 | 使用 `@Resource`，禁止 `@Autowired` 字段注入 |
| B12 | 日志格式 | §4.6 | `@Slf4j` + `[方法名][描述({})]` 格式，使用占位符 |
| B13 | TraceId | §4.4 | 跨线程调用使用 `TaskDecorator` 传递 MDC |
| B14 | Mapper XML | §6 | 复杂查询写在 XML，单表 CRUD 用 BaseMapper |
| B15 | Flyway 迁移 | §3.5 | DDL 变更必须提供 Flyway 脚本 |
| B16 | Swagger 注解 | §3.4 | Controller 添加 `@Tag` + `@Operation`，VO 添加 `@Schema` |

#### 前端

| 序号 | 检查项 | 规范依据 | 通过标准 |
|------|--------|----------|----------|
| F01 | 组件命名 | - | PascalCase 组件名 + kebab-case 文件名 |
| F02 | API 封装 | - | 所有后端调用通过 `src/api/*.ts` 封装，禁止散落在组件中 |
| F03 | TraceId 注入 | - | axios 拦截器自动注入 `X-Trace-Id` |
| F04 | Result 处理 | - | 响应拦截器统一处理 `Result<T>`，业务代码直接使用 `data` |
| F05 | 错误提示 | - | ElMessage 统一弹窗，禁止 alert/console.error 面向用户 |
| F06 | TypeScript | - | 所有 `.ts`/`.vue` 必须有类型注解，禁止 any 泛滥 |
| F07 | 路由懒加载 | - | 业务页面使用 `() => import()` 懒加载 |

### 4.2 月度抽查

规范监督员每月抽查 10% 的 PR，检查：

- Reviewer 是否认真执行检查清单
- 是否存在"走过场"现象
- 自动化工具是否漏报
- 规范本身是否需要演进

## 5. 违规处理

### 5.1 违规分级

| 级别 | 定义 | 处理 |
|------|------|------|
| L1 轻微 | 命名风格、格式化 | 工具自动修复，无需人工 |
| L2 中等 | 缺少 Swagger 注解、日志格式不规范 | 阻止合并，开发者修复后重审 |
| L3 严重 | 分层依赖反向、DO 泄露到 Controller、缺少事务 | 阻止合并，技术负责人复核 |
| L4 致命 | SQL 注入、API Key 硬编码、物理删除数据 | 立即回滚，全员复盘 |

### 5.2 处理流程

```
发现违规 → 定级 → 通知开发者 → 修复 → 复审 → 通过
                ↓（L3/L4）
            技术负责人仲裁 → 必要时回滚 → 复盘记录
```

## 6. 度量与报告

### 6.1 指标

| 指标 | 目标 | 度量频率 |
|------|------|----------|
| 规范执行度 | ≥ 95% | 月度 |
| 工具拦截率 | ≥ 80% | 月度 |
| L3/L4 违规数 | 0 | 月度 |
| Review 平均耗时 | ≤ 2 小时 | 周度 |
| 测试覆盖率 | ≥ 70% | 月度 |

### 6.2 报告输出

- **周报**：每周五输出，含违规列表、处理状态、趋势
- **月报**：每月 1 日输出，含指标对比、改进建议、规范演进提案
- **年报**：年度总结，含规范修订计划

报告存放位置：`d:\ai\docs\compliance-reports\`（按月分目录）

## 7. 工具清单速查

| 工具 | 位置 | 作用 | 触发时机 |
|------|------|------|----------|
| ESLint + oxlint + Prettier | `ai-agent-web/eslint.config.ts` | 前端代码风格 | pre-commit + 手动 |
| lint-staged | `ai-agent-web/package.json` | 暂存文件检查 | pre-commit |
| husky pre-commit | `ai-agent-web/.husky/pre-commit` | Git 提交前钩子 | git commit |
| husky commit-msg | `ai-agent-web/.husky/commit-msg` | 提交消息检查 | git commit |
| commitlint | `ai-agent-web/commitlint.config.js` | Conventional Commits | commit-msg hook |
| Spotless | `api/pom.xml` | Java 代码格式化 | mvn compile |
| Checkstyle | `api/checkstyle.xml` + `pom.xml` | Java 规范检查 | mvn validate |
| Flyway | `api/src/main/resources/db/migration/` | 数据库迁移版本控制 | 应用启动 |
| MyBatis-Plus | `api/pom.xml` + `MybatisPlusConfig.java` | DAL 强制（团队要求） | 编译期 |
| springdoc OpenAPI | `api/pom.xml` + Controller 注解 | API 文档自动生成 | 应用启动 |
| Actuator | `api/pom.xml` + `application.yml` | 健康检查与监控 | 应用启动 |

## 8. 演进机制

### 8.1 规范修订流程

1. **提案**：任何成员可在 `d:\ai\docs\` 提交规范修订提案
2. **讨论**：技术负责人组织讨论，每月 1 次评审会
3. **决议**：达成共识后更新规范文件，提交 PR
4. **工具更新**：相应地更新 Checkstyle/ESLint/commitlint 配置
5. **全员通知**：更新后通知所有成员，新规范立即生效

### 8.2 工具升级

- 每季度评估一次工具链（Spotless/Checkstyle/ESLint/husky）版本与最佳实践
- 升级前在 dev 分支验证，通过后合并到主分支

## 9. 附：当前已落实的监督点

### 9.1 已自动化（机器执行）

- [x] 前端代码格式：ESLint + oxlint + Prettier + lint-staged
- [x] 提交消息：commitlint + Conventional Commits
- [x] Java 代码格式：Spotless（google-java-format）
- [x] Java 规范检查：Checkstyle（命名/Import/长度/空块）
- [x] 数据库版本：Flyway
- [x] DAL 强制：MyBatis-Plus（团队要求）
- [x] API 文档：springdoc OpenAPI
- [x] 链路追踪：TraceIdFilter + MDC + TaskDecorator

### 9.2 仍需人工（Review 执行）

- [ ] 分层依赖方向（B01）
- [ ] 对象模型分离（B02）
- [ ] 错误码命名规范（B05）
- [ ] validateXxxExists 模式（B09）
- [ ] @Transactional 正确使用（B10）
- [ ] 日志占位符（B12）
- [ ] Mapper XML 复杂查询（B14）
- [ ] Swagger 注解完整性（B16）

后续会逐步通过自定义 ArchUnit 规则、自定义 Checkstyle 规则、SpotBugs/SONAR 等工具
将上述人工检查项自动化。
