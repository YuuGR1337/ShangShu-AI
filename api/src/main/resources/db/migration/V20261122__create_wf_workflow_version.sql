-- 工作流发布版本快照表
-- publish 时将当前草稿 graph 固化为不可变快照；执行与执行详情按 (workflow_id, version) 回溯。
-- 快照永不修改（无 deleted 字段），编辑草稿只影响 wf_workflow.graph，不污染历史版本。
CREATE TABLE IF NOT EXISTS `wf_workflow_version` (
    `id`           BIGINT AUTO_INCREMENT PRIMARY KEY,
    `workflow_id`  VARCHAR(64)  NOT NULL COMMENT '工作流业务 ID（UUID）',
    `version`      INT          NOT NULL COMMENT '版本号（同 workflow_id 内唯一，发布递增）',
    `graph`        LONGTEXT     NOT NULL COMMENT '发布时的 DSL JSON 快照（不可变）',
    `published_by` BIGINT       NULL     COMMENT '发布人用户 ID',
    `notes`        VARCHAR(512) NULL     DEFAULT '' COMMENT '发布说明',
    `published_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    UNIQUE KEY `uk_workflow_version` (`workflow_id`, `version`),
    KEY `idx_workflow_published` (`workflow_id`, `published_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流发布版本快照';
