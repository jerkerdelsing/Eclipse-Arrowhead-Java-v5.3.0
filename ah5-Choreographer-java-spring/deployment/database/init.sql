-- Only the private database schema is initialized. User/password come from Compose secrets.
CREATE DATABASE IF NOT EXISTS `ah_choreographer`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `ah_choreographer`;

CREATE TABLE IF NOT EXISTS `logs` (
  `log_id` varchar(100) NOT NULL,
  `entry_date` timestamp(3) NULL DEFAULT NULL,
  `logger` varchar(100) DEFAULT NULL,
  `log_level` varchar(100) DEFAULT NULL,
  `message` mediumtext,
  `exception` mediumtext,
  PRIMARY KEY (`log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Plans: a plan is a chain of actions; an action is a graph of steps.

CREATE TABLE IF NOT EXISTS `plan` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(63) NOT NULL,
  `first_action_name` varchar(63) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `plan_action` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `plan_id` bigint NOT NULL,
  `name` varchar(63) NOT NULL,
  `first_action` bit(1) NOT NULL,
  `next_action_name` varchar(63) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_action_plan_name` (`plan_id`, `name`),
  CONSTRAINT `fk_action_plan` FOREIGN KEY (`plan_id`) REFERENCES `plan` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `plan_step` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `action_id` bigint NOT NULL,
  `name` varchar(63) NOT NULL,
  `first_step` bit(1) NOT NULL,
  `service_definition` varchar(63) NOT NULL,
  `min_version` int DEFAULT NULL,
  `max_version` int DEFAULT NULL,
  `service_requirement` text NOT NULL,
  `local_cloud_only` bit(1) NOT NULL,
  `static_parameters` text NOT NULL,
  `quantity` int NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_step_action_name` (`action_id`, `name`),
  CONSTRAINT `fk_step_action` FOREIGN KEY (`action_id`) REFERENCES `plan_action` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `plan_step_connection` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `from_id` bigint NOT NULL,
  `to_id` bigint NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_step_connection` (`from_id`, `to_id`),
  CONSTRAINT `fk_step_connection_from` FOREIGN KEY (`from_id`) REFERENCES `plan_step` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_step_connection_to` FOREIGN KEY (`to_id`) REFERENCES `plan_step` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Executors: registered systems that execute steps of given service definitions.

CREATE TABLE IF NOT EXISTS `executor` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(63) NOT NULL,
  `address` varchar(255) NOT NULL,
  `port` int NOT NULL,
  `base_path` varchar(255) NOT NULL,
  `locked` bit(1) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_executor_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `executor_service_definition` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `executor_id` bigint NOT NULL,
  `service_definition` varchar(63) NOT NULL,
  `min_version` int NOT NULL,
  `max_version` int NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_executor_service_definition` (`executor_id`, `service_definition`),
  CONSTRAINT `fk_executor_service_definition_executor` FOREIGN KEY (`executor_id`) REFERENCES `executor` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Sessions: executions of a plan, their steps and the worklog.

CREATE TABLE IF NOT EXISTS `plan_session` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `plan_id` bigint NOT NULL,
  `status` varchar(14) NOT NULL,
  `quantity_done` bigint NOT NULL,
  `quantity_goal` bigint NOT NULL,
  `execution_number` bigint NOT NULL,
  `owner` varchar(63) NOT NULL,
  `allow_inter_cloud` bit(1) NOT NULL,
  `choose_optimal_executor` bit(1) NOT NULL,
  `notify_uri` varchar(1024) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_session_status` (`status`),
  CONSTRAINT `fk_session_plan` FOREIGN KEY (`plan_id`) REFERENCES `plan` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `session_step` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL,
  `step_id` bigint NOT NULL,
  `executor_id` bigint DEFAULT NULL,
  `status` varchar(14) NOT NULL,
  `execution_number` bigint NOT NULL,
  `message` text NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_step` (`session_id`, `step_id`, `execution_number`),
  CONSTRAINT `fk_session_step_session` FOREIGN KEY (`session_id`) REFERENCES `plan_session` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_session_step_step` FOREIGN KEY (`step_id`) REFERENCES `plan_step` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_session_step_executor` FOREIGN KEY (`executor_id`) REFERENCES `executor` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Worklog keeps names instead of foreign keys so that it survives the removal of plans.
CREATE TABLE IF NOT EXISTS `worklog` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `plan_name` varchar(63) DEFAULT NULL,
  `action_name` varchar(63) DEFAULT NULL,
  `step_name` varchar(63) DEFAULT NULL,
  `session_id` bigint DEFAULT NULL,
  `execution_number` bigint DEFAULT NULL,
  `message` mediumtext NOT NULL,
  `exception` mediumtext,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_worklog_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
