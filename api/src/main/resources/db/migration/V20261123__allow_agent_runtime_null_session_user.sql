-- ============================================================
-- AI 应用触发 Agent 运行时的空值兼容（Phase 3 Task 6 验证发现）
-- 背景：POST /api/v1/app/{appKey}/chat 的 agent 类型走
--   AiApplicationService.runAgent → AgentRuntimeService.createRuntime(null, ...)，
--   此时无会话（sessionId=null）且调用方为 API Key 匿名用户（userId=null），
--   而 agent_runtime 两列均 NOT NULL 无默认值，INSERT 报
--   "Field 'session_id' doesn't have a default value" → 500。
-- 修复：两列改为可空；chat 场景（有登录用户/会话）行为不变。
-- ============================================================

ALTER TABLE agent_runtime
    MODIFY COLUMN session_id BIGINT NULL COMMENT '关联会话 ID（chat_session.id）；AI 应用触发的运行无会话，允许 NULL',
    MODIFY COLUMN user_id BIGINT NULL COMMENT '触发用户 ID；API Key 匿名调用为 NULL';
