-- 解梦微信小程序 数据库 DDL（M0 冻结草案，MySQL 8.x / utf8mb4）
-- 命名：下划线小写；主键 bigint 自增；统一含 created_at / updated_at

-- 用户表
CREATE TABLE `user` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `openid`     VARCHAR(64)  NOT NULL COMMENT '微信 openid',
  `nickname`   VARCHAR(64)  DEFAULT NULL,
  `avatar`     VARCHAR(512) DEFAULT NULL,
  `role`       VARCHAR(16)  NOT NULL DEFAULT 'user' COMMENT 'user/admin',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

-- 梦境记录（用户输入）
CREATE TABLE `dream_record` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT       NOT NULL,
  `dream_text` TEXT         NOT NULL COMMENT '梦境原文',
  `tags`       VARCHAR(255) DEFAULT NULL COMMENT '标签，逗号分隔',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_created` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='梦境记录';

-- 解梦结果（AI 输出，result_json 符合 dream-result.schema.json）
CREATE TABLE `dream_result` (
  `id`              BIGINT      NOT NULL AUTO_INCREMENT,
  `dream_record_id` BIGINT      NOT NULL,
  `school`          VARCHAR(32) DEFAULT NULL COMMENT '流派展示偏好，空=展示全部',
  `result_json`     JSON        NOT NULL COMMENT '结构化解梦结果',
  `provider`        VARCHAR(32) DEFAULT NULL COMMENT '实际使用渠道，如 deepseek',
  `model`           VARCHAR(64) DEFAULT NULL,
  `prompt_version`  VARCHAR(32) DEFAULT NULL,
  `token_in`        INT         DEFAULT 0,
  `token_out`       INT         DEFAULT 0,
  `status`          VARCHAR(16) NOT NULL DEFAULT 'success' COMMENT 'success/fallback/failed',
  `created_at`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_record` (`dream_record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='解梦结果';

-- 收藏
CREATE TABLE `favorite` (
  `id`              BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`         BIGINT   NOT NULL,
  `dream_result_id` BIGINT   NOT NULL,
  `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_result` (`user_id`, `dream_result_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏';

-- AI 渠道配置（运行时可配置 + 热加载）
CREATE TABLE `ai_provider_config` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `name`            VARCHAR(64)  NOT NULL COMMENT '渠道名',
  `provider`        VARCHAR(32)  NOT NULL COMMENT 'deepseek/qwen/zhipu/openai...',
  `base_url`        VARCHAR(255) NOT NULL,
  `api_key`         VARCHAR(512) NOT NULL COMMENT '加密存储',
  `model`           VARCHAR(64)  NOT NULL,
  `temperature`     DECIMAL(3,2) DEFAULT 0.70,
  `max_tokens`      INT          DEFAULT 1024,
  `top_p`           DECIMAL(3,2) DEFAULT 1.00,
  `timeout_ms`      INT          DEFAULT 30000,
  `response_format` VARCHAR(32)  DEFAULT 'json_object' COMMENT 'json_object/function_call/text',
  `enabled`         TINYINT(1)   NOT NULL DEFAULT 1,
  `priority`        INT          NOT NULL DEFAULT 100 COMMENT '越小越优先',
  `weight`          INT          NOT NULL DEFAULT 100 COMMENT '灰度权重',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_enabled_priority` (`enabled`, `priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 渠道配置';

-- 提示词模板（运行时可配置）
CREATE TABLE `prompt_template` (
  `id`                   BIGINT      NOT NULL AUTO_INCREMENT,
  `scene_code`           VARCHAR(32) NOT NULL COMMENT '场景：interpret/title...',
  `version`              VARCHAR(32) NOT NULL,
  `system_prompt`        TEXT        NOT NULL,
  `user_prompt_template` TEXT        NOT NULL COMMENT '含 {{dreamText}} {{school}} 占位',
  `schema_json`          JSON        DEFAULT NULL COMMENT '期望输出 schema',
  `enabled`              TINYINT(1)  NOT NULL DEFAULT 1,
  `remark`               VARCHAR(255) DEFAULT NULL,
  `created_at`           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_scene_enabled` (`scene_code`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提示词模板';

-- 敏感词
CREATE TABLE `sensitive_word` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT,
  `word`       VARCHAR(64) NOT NULL,
  `type`       VARCHAR(16) DEFAULT 'block' COMMENT 'block/review',
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_word` (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词';
