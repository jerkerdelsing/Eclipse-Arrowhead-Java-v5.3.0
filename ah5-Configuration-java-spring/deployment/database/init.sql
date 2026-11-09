-- Only the private database schema is initialized. User/password come from Compose secrets.
CREATE DATABASE IF NOT EXISTS `ah_configuration`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE `ah_configuration`;

CREATE TABLE IF NOT EXISTS `logs` (
  `log_id` varchar(100) NOT NULL,
  `entry_date` timestamp(3) NULL DEFAULT NULL,
  `logger` varchar(100) DEFAULT NULL,
  `log_level` varchar(100) DEFAULT NULL,
  `message` mediumtext,
  `exception` mediumtext,
  PRIMARY KEY (`log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- One configuration file per system; data holds the raw (decoded) bytes.
CREATE TABLE IF NOT EXISTS `configuration_data` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `system_name` varchar(63) NOT NULL,
  `file_name` varchar(255) NOT NULL,
  `content_type` varchar(255) NOT NULL,
  `data` longblob NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_configuration_data_system_name` (`system_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
