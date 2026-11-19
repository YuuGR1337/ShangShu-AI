# OpenSpec Change 归档审计（2026-11-19，Phase 3 梳理）

> 审计方式：逐一核对 change 的 tasks.md 与代码库实际实现（`com.ai.api.*` 包结构 + 前端页面）。
> 阶段性开发自 Phase 2 起改用 `.trae/specs/<phase>/` 流程管理，openspec/changes 仅保留进行中的 change。

## 归档清单（8 个）

| Change | 归档依据（代码证据） | 归档时遗留验证项（已知差距，非阻塞） |
|---|---|---|
| bootstrap-project | ApiApplication 正常启动；`/actuator/health` UP；Swagger UI 可用；traceId 与 sys_llm_log 链路在 Phase 1-3 启动验证中反复经过 | `docs/test-reports/bootstrap-test.md` 测试报告未编写 |
| add-user-auth | identity 包完整（注册/登录/JWT/登出/忘记密码）；SecurityConfig 全链路在所有 API 调用中使用 | OWASP ZAP 扫描与登录性能基线（<500ms）未正式执行 |
| add-rbac-management | identity 包 18 个类（用户/角色/权限/授权）；SUPER_ADMIN 全链路在开发环境日常使用 | 全链路人工手测清单未留存记录 |
| add-chat-module | chat 包 + ChatMessageLogConsumer + RabbitMQ 异步落库 + ES 双写，Phase 2 已验证消费链路 | 9.3/9.4 联调与性能验证项（随 Phase 2 启动验证部分覆盖） |
| add-customer-service | cs 包 9 个类（意图/会话/知识路由） | 无 |
| add-agent-runtime | agent 包完整落地：AgentProperties/AgentMqConfig/5 张表迁移/runtime+task+trace+skill 实体/Runner/Consumer/ContextManager/Eval/Trace/Pause | tasks.md 未随实现同步勾选（实现真实存在，见 com.ai.api.agent 包） |
| add-agent-runner-pipeline | runner 包（CotAgentRunner/FcAgentRunner/StrategyFactory/pipeline）已在 Agent 执行链路生产使用 | 无 |
| add-p2-cache-search-rewrite | RagRetrievalService/QueryRewriteService + p2 缓存与检索链路，Phase 2 检索测试端点已验证 | 无 |

## 保留清单（1 个）

| Change | 保留原因 |
|---|---|
| add-p3-rag-feedback-ops | 主体功能已完成（74/84），但存在 10 项真实遗留：4 项运行时验证（命中计数并发、低质 chunk 重处理、万级导入压测、备份调度复核）+ 6 项文档工作（docs/architecture.md 章节、OpenAPI 描述等，明确标注"后续迭代"） |

## 后续约定

- 新需求一律走 `.trae/specs/` 阶段流程，不再新建 openspec change；本目录仅维护进行中 change。
- 归档 change 的遗留验证项如需跟进，建议在 issue/backlog 中登记，不在 openspec 中复开。
