-- Only the private database schema is initialized. User/password come from Compose secrets.
CREATE DATABASE IF NOT EXISTS `ah_eventhandler`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `ah_eventhandler`;

CREATE TABLE IF NOT EXISTS `logs` (
  `log_id` varchar(100) NOT NULL,
  `entry_date` timestamp(3) NULL DEFAULT NULL,
  `logger` varchar(100) DEFAULT NULL,
  `log_level` varchar(100) DEFAULT NULL,
  `message` mediumtext,
  `exception` mediumtext,
  PRIMARY KEY (`log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `event_subscription` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `subscriber_system_name` varchar(63) NOT NULL,
  `event_type` varchar(63) NOT NULL,
  `notify_uri` varchar(1024) NOT NULL,
  `match_metadata` bit(1) NOT NULL,
  `metadata_filter` text,
  `starts_at` timestamp NULL DEFAULT NULL,
  `expires_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_event_subscription_type` (`event_type`),
  KEY `idx_event_subscription_subscriber` (`subscriber_system_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
