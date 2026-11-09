-- Only the private database schema is initialized. User/password come from Compose secrets.
CREATE DATABASE IF NOT EXISTS `ah_gatekeeper`;
USE `ah_gatekeeper`;


-- Logs

CREATE TABLE IF NOT EXISTS `logs` (
  `log_id` varchar(100) NOT NULL,
  `entry_date` timestamp(3) NULL DEFAULT NULL,
  `logger` varchar(100) DEFAULT NULL,
  `log_level` varchar(100) DEFAULT NULL,
  `message` mediumtext,
  `exception` mediumtext,
  PRIMARY KEY (`log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Neighbour clouds: (name, operator) is unique

CREATE TABLE IF NOT EXISTS `cloud` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `operator` varchar(255) NOT NULL,
  `gatekeeper_address` varchar(255) DEFAULT NULL,
  `gatekeeper_port` int(11) DEFAULT NULL,
  `metadata` mediumtext NOT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cloud_name_operator` (`name`, `operator`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Relay brokers: the name is unique

CREATE TABLE IF NOT EXISTS `relay` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `address` varchar(255) NOT NULL,
  `port` int(11) NOT NULL,
  `type` varchar(63) NOT NULL,
  `secure` tinyint(1) NOT NULL DEFAULT 1,
  `metadata` mediumtext NOT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_relay_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
