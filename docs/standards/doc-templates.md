# 文档标准与模板

> 定义项目各类文档的结构与写作规范，所有文档统一存放于 `d:\ai\docs\`。

---

## 1. 文档目录布局

```
d:\ai\docs\
├── development-plan.md         # 项目开发计划（总纲）
├── coding-standards.md         # 编码规范
├── architecture.md             # 架构设计
├── doc-templates.md            # 本文件
├── changes-roadmap.md          # OpenSpec changes 路线图
├── deployment.md                # 部署运维手册
├── training-plan.md            # 学习培训计划
├── requirements/               # 需求文档（按模块）
│   └── <module>.md
├── designs/                    # 详细设计文档（OpenSpec design.md 的补充）
│   └── <module>.md
├── api/                        # API 文档（自动生成 + 补充）
│   └── README.md
├── test-reports/               # 测试报告
│   └── <module>-test.md
└── runbooks/                   # 运维手册
    └── <scenario>.md
```

---

## 2. OpenSpec 工件模板

OpenSpec 工件（proposal/specs/design/tasks）由 `/opsx-propose` 命令根据 `config.yaml` 的 `rules` 自动生成结构，团队在此基础上填充。下面给出参考模板。

### 2.1 proposal.md 模板

```markdown
# Proposal: <change-name>

## Why
<1-3 段说明背景与动机。为什么要做这个 change？解决什么问题？>

## What Changes
<要点列表，描述本次变更的内容>
- 新增 ...
- 修改 ...
- 移除 ...

## Non-Goals
<明确不做的事情，避免范围蔓延>
- 不包含 ...
- 不涉及 ...

## Dependencies
<依赖的前序 changes 或外部条件>
- 依赖 change: <change-name>
- 依赖外部：<如 DeepSeek API 可用>

## Acceptance Criteria
<至少 3 条，可测试>
1. <验收点 1，可验证>
2. <验收点 2，可验证>
3. <验收点 3，可验证>

## Open Questions
<待确认问题（可选）>
- <问题>
```

### 2.2 specs/<capability>/spec.md 模板（delta）

```markdown
## ADDED Requirements

### Requirement: <需求名>
<系统 SHALL/MUST/SHOULD ...>

#### Scenario: <正常场景名>
- **WHEN** <前置条件>
- **THEN** <期望行为>
- **AND** <附加验证>

#### Scenario: <异常场景名>
- **WHEN** <异常条件，如 LLM 超时>
- **THEN** <降级行为>
- **AND** <错误码/日志>

## MODIFIED Requirements
### Requirement: <已存在的需求名>
<新版本描述，包含所有保留的 scenarios>

## REMOVED Requirements
### Requirement: <被移除的需求名>

## RENAMED Requirements
- FROM: `### Requirement: 旧名`
- TO: `### Requirement: 新名`
```

### 2.3 design.md 模板

```markdown
# Design: <change-name>

## Context
<背景、约束、关联模块>

## Architecture

```mermaid
flowchart LR
    A[输入] --> B[处理]
    B --> C[输出]
```

## Key Decisions
| 决策 | 选项 | 选择 | 理由 |
|------|------|------|------|
| <决策点> | A/B | A | <理由> |

## Module Dependencies
- 依赖 `ai-agent-common`
- 调用 `ai-agent-model.ModelRouter`

## Public Interfaces

### REST API
- `POST /api/v1/<module>/<resource>`
  - Request: `<DTO>`
  - Response: `Result<T>`

### Internal SPI
- `<InterfaceName>` 提供给 <模块> 调用

## Data Model

### MySQL Tables
```sql
CREATE TABLE <table> (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ...
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
);
```

### Redis Keys
- `<prefix>:<key>` → `<value>`，TTL=<seconds>

### Elasticsearch Index
```json
{ "mappings": { "properties": { ... } } }
```

### RabbitMQ
- Exchange: `<name>` (topic/direct)
- Queue: `<name>`
- Routing Key: `<pattern>`

## Integration Points
- 与 <模块> 集成：<接口/事件>

## Trade-offs / Risks
- <权衡点与缓解>

## Migration Plan
<如涉及存量数据迁移>
```

### 2.4 tasks.md 模板

```markdown
# Tasks: <change-name>

## 数据层
- [ ] [单测] 创建 Flyway 脚本 V2026xxxx__<desc>.sql
- [ ] [单测] 实现 <Mapper> 与 XML

## 能力层
- [ ] [单测] 实现 <Service>，覆盖 ≥ 80%
- [ ] [单测] 实现 <Tool> 注册

## 业务层
- [ ] [接口测] 实现 <Controller>，提供 Swagger

## 接入层
- [ ] [手测] 配置 Nginx 路由

## 前端
- [ ] [手测] 实现 <页面>，使用 SSE

## 文档与部署
- [ ] [手测] 更新模块 README
- [ ] [手测] 提供部署配置
```

---

## 3. 需求文档模板

文件：`d:\ai\docs\requirements\<module>.md`

```markdown
# 需求规格：&lt;模块名&gt;

## 1. 概述
### 1.1 业务背景
### 1.2 目标用户
### 1.3 核心价值

## 2. 功能需求
### 2.1 F1: <功能>
- **描述**：
- **用户故事**：作为<角色>，我希望<动作>，以便<价值>
- **验收标准**：
  1. ...
  2. ...

## 3. 非功能需求
### 3.1 性能
### 3.2 安全
### 3.3 可用性

## 4. 接口需求
### 4.1 对外 API
### 4.2 依赖 API

## 5. 数据需求
### 5.1 输入数据
### 5.2 输出数据
### 5.3 持久化数据

## 6. 验收
### 6.1 测试用例（链接）
### 6.2 验收标准
```

---

## 4. 详细设计文档模板

文件：`d:\ai\docs\designs\<module>.md`（OpenSpec design.md 的扩展版，可选）

```markdown
# 详细设计：&lt;模块名&gt;

## 1. 引言
### 1.1 编写目的
### 1.2 背景
### 1.3 术语

## 2. 总体设计
### 2.1 架构图
### 2.2 模块划分
### 2.3 依赖关系

## 3. 详细设计
### 3.1 <子模块 1>
#### 3.1.1 类设计
#### 3.1.2 关键流程（时序图）
#### 3.1.3 算法说明

## 4. 数据库设计
### 4.1 表结构
### 4.2 索引
### 4.3 缓存策略

## 5. 接口设计
### 5.1 REST
### 5.2 内部 SPI
### 5.3 事件

## 6. 异常与降级

## 7. 监控与日志

## 8. 测试策略

## 9. 部署影响

## 10. 附录
```

---

## 5. 接口定义规范

### 5.1 REST API 标准

- 路径：`/api/v1/<module>/<resource>` 复数
- 方法：GET/POST/PUT/PATCH/DELETE
- 版本：通过路径版本化（v1/v2）
- 编码：UTF-8 JSON
- 鉴权：Bearer Token（`Authorization: Bearer <jwt>`）

### 5.2 统一响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": { },
  "traceId": "uuid-xxx"
}
```

错误响应：
```json
{
  "code": 40001,
  "message": "会话不存在",
  "data": null,
  "traceId": "uuid-xxx"
}
```

### 5.3 分页响应

```json
{
  "code": 0,
  "data": {
    "list": [ ],
    "total": 100,
    "page": 1,
    "size": 20
  }
}
```

### 5.4 SSE 流式接口

- Content-Type: `text/event-stream`
- 事件类型：
  - `message`：内容块
  - `done`：结束
  - `error`：错误
  - `tool_call`：Agent 工具调用（可选）

```
event: message
data: {"content":"你","index":0}

event: message
data: {"content":"好","index":1}

event: done
data: {"totalTokens":25,"messageId":"xxx"}
```

### 5.5 错误码规范

格式：5 位数字，前 2 位为模块号

| 模块 | 模块号 | 范围 |
|------|--------|------|
| 通用 | 10 | 10000-10999 |
| 系统 | 11 | 11000-11999 |
| 对话 | 20 | 20000-20999 |
| RAG | 21 | 21000-21999 |
| Agent | 22 | 22000-22999 |
| 客服 | 23 | 23000-23999 |
| 数据分析 | 24 | 24000-24999 |

---

## 6. 测试文档模板

文件：`d:\ai\docs\test-reports\<module>-test.md`

```markdown
# 测试报告：&lt;模块名&gt;

## 1. 测试范围
### 1.1 测试目标
### 1.2 测试环境

## 2. 测试用例
| ID | 用例名 | 类型 | 前置 | 步骤 | 期望 | 结果 |
|----|--------|------|------|------|------|------|

## 3. 单元测试
### 3.1 覆盖率
### 3.2 关键路径

## 4. 集成测试

## 5. 性能测试
| 接口 | QPS | P95 | P99 | 错误率 |

## 6. 缺陷汇总
| ID | 描述 | 严重度 | 状态 |

## 7. 结论与建议
```

---

## 7. 运维手册模板

文件：`d:\ai\docs\runbooks\<scenario>.md`

```markdown
# Runbook: &lt;场景&gt;（如 LLM-API 不可用）

## 触发条件
<告警/现象>

## 影响范围
<受影响的功能与用户>

## 处置步骤
1. <步骤 1>
2. <步骤 2>

## 回滚方案

## 验证恢复
<如何确认问题解决>

## 联系人
- 主责：
- 升级：
```

---

## 8. 写作规范

- **语言**：中文为主，技术术语保留英文（如 `ModelRouter`、`SSE`、`ReAct`）
- **规范级别词**：MUST/SHALL/SHOULD/MAY（RFC 2119）
- **图表**：Mermaid 优先，复杂图用 ASCII
- **代码块**：标注语言，不缩进
- **链接**：使用相对路径（如 `[coding-standards.md](./coding-standards.md)`）
- **文件引用**：使用 `file:///` 协议（如 [eslint.config.ts](file:///d:/ai/ai-agent-web/eslint.config.ts)）

---

## 9. 文档评审

- OpenSpec 工件：proposal/specs/design/tasks 在 `/opsx-apply` 前团队评审
- 项目级文档（本目录）：架构师评审，PM 归档
- 变更：使用 `/opsx-update` 同步更新
