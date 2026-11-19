-- Prompt 模板版本历史表：每次 publish 时快照当前内容到此表，支持回滚
CREATE TABLE IF NOT EXISTS `prompt_template_version` (
    `id`           BIGINT AUTO_INCREMENT PRIMARY KEY,
    `template_id`  BIGINT       NOT NULL COMMENT '关联 prompt_template.id',
    `version`      INT          NOT NULL COMMENT '版本号（与 prompt_template.version 对应）',
    `content`      TEXT         NOT NULL COMMENT '该版本的模板内容快照',
    `variables`    JSON         DEFAULT NULL COMMENT '该版本的变量列表快照',
    `model_id`     VARCHAR(128) DEFAULT NULL COMMENT '该版本关联的模型 ID',
    `change_log`   VARCHAR(512) DEFAULT NULL COMMENT '本次版本变更说明',
    `created_by`   VARCHAR(64)  DEFAULT NULL COMMENT '发布人',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    UNIQUE KEY `uk_template_version` (`template_id`, `version`),
    KEY `idx_template_id` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Prompt 模板版本历史（快照）';
