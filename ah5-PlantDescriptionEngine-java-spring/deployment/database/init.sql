-- Only the private database schema is initialized. User/password come from Compose secrets.
CREATE DATABASE IF NOT EXISTS `ah_plantdescriptionengine`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE `ah_plantdescriptionengine`;

CREATE TABLE IF NOT EXISTS `logs` (
  `log_id` varchar(100) NOT NULL,
  `entry_date` timestamp(3) NULL DEFAULT NULL,
  `logger` varchar(100) DEFAULT NULL,
  `log_level` varchar(100) DEFAULT NULL,
  `message` mediumtext,
  `exception` mediumtext,
  PRIMARY KEY (`log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Plant descriptions; include, systems and connections are one JSON document

CREATE TABLE IF NOT EXISTS `plant_description` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT 0,
  `body` longtext NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_plant_description_active` (`active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Alarms raised by mismatches between the active plant description and the local cloud

CREATE TABLE IF NOT EXISTS `pde_alarm` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `system_id` varchar(255) NULL DEFAULT NULL,
  `system_name` varchar(63) NULL DEFAULT NULL,
  `cause` varchar(63) NOT NULL,
  `description` varchar(1024) NOT NULL,
  `acknowledged` tinyint(1) NOT NULL DEFAULT 0,
  `cleared_at` timestamp NULL DEFAULT NULL,
  `acknowledged_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pde_alarm_cleared` (`cleared_at`),
  KEY `idx_pde_alarm_system` (`system_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
