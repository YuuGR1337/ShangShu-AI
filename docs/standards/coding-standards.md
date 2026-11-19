# 编码规范 - 智能助手 AI 开放平台

> 本规范适用于 `d:\ai\ai-agent-web`（前端）与 `d:\ai\ai-agent-server`（后端）。

---

## 1. 通用原则

- **简洁优先**：不过度设计，三行重复代码优于过早抽象
- **可读性**：代码是写给人看的，命名清晰胜过注释
- **单一职责**：函数/类/模块只做一件事
- **变更最小化**：只改必要的代码，不顺带重构

---

## 2. 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| Java 类 | PascalCase | `ChatSessionService` |
| Java 方法/变量 | camelCase | `createSession` |
| Java 常量 | UPPER_SNAKE | `MAX_TOKEN_COUNT` |
| Java 包 | 全小写 | `com.ai.api.chat.service`（保留现有根包 `com.ai.api`） |
| TypeScript 类型/接口 | PascalCase | `ChatMessage` |
| TS 变量/函数 | camelCase | `sendMessage` |
| Vue 组件名 | PascalCase | `ChatWindow` |
| Vue 文件 | PascalCase.vue | `ChatWindow.vue` |
| CSS 类 | kebab-case | `.chat-window` |
| 数据库表 | snake_case + 模块前缀 | `chat_session`、`kb_document` |
| 数据库字段 | snake_case | `created_at` |
| REST 路径 | kebab-case 复数 | `/api/v1/chat/sessions` |
| 配置 key | kebab-case | `spring.ai.deep-seek.api-key` |
| OpenSpec change | kebab-case 动词短语 | `add-chat-module` |

---

## 3. 目录结构

### 3.1 后端 Maven 多模块（演进策略）

**现状**：`d:\ai\api` 为单模块 Spring Boot 4.1 + Spring AI 2.0 项目，根包 `com.ai.api`，已引入核心依赖。

**演进策略**：M0 阶段先以单模块推进，按子包组织代码；待模块边界稳定后再拆分为多模块父工程。建议的多模块目标结构如下（M2 后视情况落地）：

```
api/                                    # 父工程（保留 d:\ai\api 路径）
├── pom.xml                              # 父 POM
├── api-common/                          # 通用工具、DTO、异常、常量
├── api-data/                            # 数据层：entity/mapper/redis/es/mq
├── api-model/                           # 模型能力层：Spring AI 封装
├── api-chat/                            # 对话模块
├── api-rag/                             # 知识库 RAG
├── api-agent/                           # Agent 模块（ReAct、工具）
├── api-customer-service/                # 智能客服
├── api-data-analysis/                   # 数据分析助手
├── api-system/                          # 系统管理（用户、权限、配置）
└── api-bootstrap/                       # 启动模块：主类 + Controller 聚合
```

**M0 单模块阶段**的包内布局（保留现有 `com.ai.api` 根包）：

```
api/src/main/java/com/ai/api/
├── ApiApplication.java                  # 已存在
├── common/                              # 通用：DTO/异常/常量/工具
├── data/                                # 数据层：entity/mapper/redis/es/mq
├── model/                               # 模型能力层：ModelRouter/Prompt 管理
├── chat/                                # 对话模块
│   ├── controller/
│   ├── service/
│   ├── domain/
│   └── dto/
├── rag/                                 # 知识库 RAG
├── agent/                               # Agent 模块
├── customerservice/                     # 智能客服（避免关键字）
├── dataanalysis/                        # 数据分析助手
└── system/                              # 系统管理
```

每个子包内部统一布局：

```
<module>/
├── controller/        # REST 入口
├── service/           # 业务逻辑
│   └── impl/
├── domain/            # 领域模型（非 entity）
├── dto/               # 请求/响应 DTO
├── config/            # 模块内配置
└── mapper/            # MyBatis Mapper（如需）
```

资源目录：

```
api/src/main/resources/
├── application.yml                     # 主配置（替换现有 application.properties）
├── application-dev.yml                 # 环境配置
├── application-prod.yml
├── mapper/                             # XML Mapper（按模块）
└── db/migration/                       # Flyway 脚本（按模块分目录）
    ├── chat/
    ├── rag/
    └── agent/
```

### 3.2 前端目录（基于现有 `ai-agent-web`）

```
ai-agent-web/src/
├── api/                  # 接口封装（按模块分文件）
│   ├── chat.ts
│   ├── rag.ts
│   └── agent.ts
├── assets/               # 静态资源
├── components/           # 公共组件
│   ├── common/
│   └── layout/
├── composables/          # 组合式函数（useXxx）
├── router/               # 路由
├── stores/               # Pinia stores（按模块）
├── types/                # TS 类型定义
├── utils/                # 工具函数
├── views/                # 页面（按模块）
│   ├── chat/
│   ├── rag/
│   ├── agent/
│   ├── customer-service/
│   └── system/
├── App.vue
└── main.ts
```

---

## 4. Java 编码规范

### 4.1 基础
- JDK 21
- Spring Boot 4.1.x + Spring AI 2.0.x（已在 [pom.xml](file:///d:/ai/api/pom.xml) 配置）
- 使用 Lombok（@Data/@Slf4j/@RequiredArgsConstructor，已在 pom 中引入）
- 构造器注入（不使用 @Autowired 字段注入）

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatSessionServiceImpl implements ChatSessionService {

    private final ChatSessionMapper sessionMapper;
    private final ModelRouter modelRouter;

    public ChatSession create(CreateSessionRequest request) {
        // ...
    }
}
```

### 4.2 异常处理
- 业务异常：`BusinessException(ErrorCode.CHAT_SESSION_NOT_FOUND)`
- 全局异常处理器：`@RestControllerAdvice`
- 错误码统一定义在 `ai-agent-common` 的 `ErrorCode` 枚举

### 4.3 接口规范
- RESTful：`/api/v1/<module>/<resource>` 复数
- 统一响应：`Result<T>`（code/message/data）
- 分页：`PageResult<T>`
- 入参校验：`@Valid` + JSR-303 注解

### 4.4 LLM 调用约定
- 所有模型调用通过 `ModelRouter`，禁止直接调用 Spring AI 的 ChatClient
- 必须传递 `traceId`，记录到 LLM 调用日志表
- 流式输出统一返回 `SseEmitter` 或 `Flux<ServerSentEvent>`

### 4.5 Agent 工具注册
```java
@Component
@ToolRegistry(description = "查询指定城市天气")
public class WeatherTool implements AgentTool {
    @Override
    public ToolDefinition getDefinition() { ... }

    @Override
    public String execute(Map<String, Object> params) { ... }
}
```

---

## 5. TypeScript / Vue 编码规范

### 5.1 已有配置（沿用）
- ESLint + oxlint + Prettier（见 [eslint.config.ts](file:///d:/ai/ai-agent-web/eslint.config.ts)）
- Prettier：`semi: false`、`singleQuote: true`、`printWidth: 100`
- 路径别名 `@` → `./src`

### 5.2 组件规范
- 使用 `<script setup lang="ts">`
- Props/Emits 使用类型定义
- 不使用 Options API
- 组件名必须是多单词（避免与 HTML 元素冲突）

```vue
<script setup lang="ts">
import { ref } from 'vue'

interface Props {
  sessionId: string
}
const props = defineProps<Props>()
const emit = defineEmits<{ 'message-sent': [content: string] }>()
</script>
```

### 5.3 状态管理
- 全局状态用 Pinia
- 模块化 store：`useChatStore`、`useRagStore`
- 组件内局部状态用 `ref`/`reactive`

### 5.4 API 调用
- 统一通过 `@/api/<module>.ts` 封装
- 使用 axios 拦截器处理 token、错误、loading
- SSE 使用 `@microsoft/fetch-event-source`

```typescript
// src/api/chat.ts
import { fetchEventSource } from '@microsoft/fetch-event-source'

export function streamChat(
  payload: ChatRequest,
  onMessage: (chunk: string) => void,
): AbortController {
  const ctrl = new AbortController()
  fetchEventSource('/api/v1/chat/messages/stream', {
    method: 'POST',
    body: JSON.stringify(payload),
    signal: ctrl.signal,
    onmessage: (ev) => onMessage(ev.data),
  })
  return ctrl
}
```

---

## 6. 数据库规范

- 引擎：InnoDB
- 字符集：`utf8mb4`、`utf8mb4_0900_ai_ci`
- 每表必有：`id`（BIGINT 自增）、`created_at`、`updated_at`、`deleted`（TINYINT 软删除）
- 主键索引：`PRIMARY KEY (id)`
- 业务唯一索引：`uk_<table>_<field>`
- 普通索引：`idx_<table>_<field>`
- 外键：不使用物理外键，仅逻辑外键
- 变更：Flyway 脚本，命名 `V<日期>__<描述>.sql`，如 `V20260818__create_chat_session.sql`

---

## 7. Git 规范

### 7.1 分支策略
- `main`：生产分支，只接受 PR 合入
- `develop`：集成分支
- `feature/<change-name>`：功能分支（对齐 OpenSpec change）
- `fix/<issue>`：修复分支
- `release/<version>`：发布分支

### 7.2 提交规范（Conventional Commits）

```
<type>(<scope>): <subject>

<body>

<footer>
```

| type | 说明 |
|------|------|
| feat | 新功能 |
| fix | Bug 修复 |
| docs | 文档 |
| style | 格式（不影响代码） |
| refactor | 重构 |
| test | 测试 |
| chore | 构建/工具 |
| perf | 性能 |
| ci | CI 配置 |

**示例**：
```
feat(chat): 实现 SSE 流式输出接口

- 新增 /api/v1/chat/messages/stream
- 使用 SseEmitter 支持断线重连
- 集成 ModelRouter 调用多模型

Closes #123
```

### 7.3 PR 规范
- 标题：`[<模块>] <type>: <subject>`
- 关联 OpenSpec change（在 PR 描述中链接）
- 至少 1 人 approve
- CI 必须通过
- 不直接推 main

---

## 8. 测试规范

### 8.1 后端
- 框架：JUnit 5 + Mockito + AssertJ
- 命名：`<ClassName>Test`，方法 `should_<expected>_when_<condition>`
- 覆盖率：Service/Util ≥ 80%，Controller ≥ 60%
- 集成测试：Testcontainers（MySQL/Redis/ES/RabbitMQ）

### 8.2 前端
- 框架：Vitest + @vue/test-utils
- 组件测试：覆盖 props/emits/slots
- E2E（可选）：Playwright

### 8.3 验证标记
tasks.md 中每个任务必须标注验证方式：
- `[单测]` 单元测试
- `[接口测]` 接口测试（Swagger/Postman）
- `[集成测]` 集成测试
- `[手测]` 手工验证

---

## 9. OpenSpec 工件规范

详见 `d:\ai\openspec\config.yaml` 的 `rules` 与 `operations` 段。简要：

- **proposal.md**：包含"非目标"、依赖 changes、≥3 条可测试验收标准
- **specs/.../spec.md**：每需求 ≥ 2 个 Scenario（正常 + 异常），WHEN/THEN/AND
- **design.md**：Mermaid 依赖图 + 对外接口 + 集成点 + 数据结构
- **tasks.md**：任务 ≤ 4 小时，标注验证方式

---

## 10. 工具与配置

| 工具 | 用途 | 配置位置 |
|------|------|----------|
| checkstyle | Java 代码风格 | `ai-agent-server/checkstyle.xml` |
| spotless | Java 格式化 | 父 POM |
| eslint + oxlint | 前端 lint | [eslint.config.ts](file:///d:/ai/ai-agent-web/eslint.config.ts) |
| prettier | 前端格式化 | [.prettierrc.json](file:///d:/ai/ai-agent-web/.prettierrc.json) |
| husky + lint-staged | 提交前钩子 | 待添加（M0） |
| commitlint | 提交信息校验 | 待添加（M0） |
