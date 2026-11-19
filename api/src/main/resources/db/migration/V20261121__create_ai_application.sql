-- AI 应用发布表：用户可创建 Chat/RAG/Agent/Workflow 四种类型应用并发布，
-- 发布后生成 API Key，外部通过 POST /api/v1/app/{appKey}/chat 统一调用
CREATE TABLE IF NOT EXISTS `ai_application` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `name`           VARCHAR(128) NOT NULL COMMENT '应用名称',
    `type`           VARCHAR(20)  NOT NULL COMMENT '应用类型: chat | rag | agent | workflow',
    `config_json`    JSON         DEFAULT NULL COMMENT '类型特定配置（模型ID、知识库ID、workflowId、system prompt 等）',
    `api_key_hash`   VARCHAR(64)  DEFAULT NULL COMMENT 'SHA256(appKey)，运行时以 hash 查库；未发布时为 NULL',
    `description`    VARCHAR(512) DEFAULT NULL COMMENT '应用描述',
    `enabled`        TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
    `creator`        VARCHAR(64)  DEFAULT NULL,
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater`        VARCHAR(64)  DEFAULT NULL,
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_api_key_hash` (`api_key_hash`),
    KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 应用发布（对外 OpenAI 兼容入口）';
