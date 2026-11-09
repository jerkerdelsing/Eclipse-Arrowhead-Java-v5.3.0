-- SysMonitor schema (ah_sysmonitor). The history is retention-bounded; times are ISO-8601 UTC text, epochs are seconds.

USE `ah_sysmonitor`;

CREATE TABLE IF NOT EXISTS `subscription` (
  `provider_system` varchar(63) NOT NULL,
  `observation_types` varchar(2048) NOT NULL,
  `enabled` tinyint(1) NOT NULL,
  `revision` int NOT NULL,
  `updated_at` varchar(40) NOT NULL,
  `updated_by` varchar(63) NULL DEFAULT NULL,
  PRIMARY KEY (`provider_system`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `amqp_binding` (
  `binding_key` varchar(255) NOT NULL,
  PRIMARY KEY (`binding_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `outbox` (
  `observation_id` varchar(255) NOT NULL,
  `payload` mediumtext NOT NULL,
  `mqtt_published` tinyint(1) NOT NULL DEFAULT 0,
  `attempts` int NOT NULL DEFAULT 0,
  `next_attempt` double NOT NULL DEFAULT 0,
  `last_error` varchar(255) NULL DEFAULT NULL,
  `created_at` varchar(40) NOT NULL,
  PRIMARY KEY (`observation_id`),
  KEY `idx_outbox_pending` (`mqtt_published`, `next_attempt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `observation` (
  `observation_id` varchar(255) NOT NULL,
  `provider_system` varchar(255) NOT NULL,
  `service_instance` varchar(255) NOT NULL,
  `observation_type` varchar(255) NOT NULL,
  `source_time` varchar(40) NOT NULL,
  `source_epoch` double NOT NULL,
  `received_time` varchar(40) NOT NULL,
  `received_epoch` double NOT NULL,
  `quality` varchar(63) NULL DEFAULT NULL,
  `unit` varchar(63) NULL DEFAULT NULL,
  `payload` mediumtext NOT NULL,
  PRIMARY KEY (`observation_id`),
  KEY `idx_observation_by_system` (`provider_system`, `observation_type`, `source_epoch`),
  KEY `idx_observation_by_age` (`received_epoch`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `audit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `at` varchar(40) NOT NULL,
  `actor` varchar(63) NOT NULL,
  `operation` varchar(63) NOT NULL,
  `target` varchar(1024) NOT NULL,
  `outcome` varchar(1024) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `logs` (
  `log_id` varchar(100) NOT NULL,
  `entry_date` timestamp(3) NULL DEFAULT NULL,
  `logger` varchar(100) DEFAULT NULL,
  `log_level` varchar(100) DEFAULT NULL,
  `message` mediumtext,
  `exception` mediumtext,
  PRIMARY KEY (`log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
