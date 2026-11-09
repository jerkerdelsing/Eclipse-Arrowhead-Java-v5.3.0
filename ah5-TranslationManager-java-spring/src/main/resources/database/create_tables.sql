USE `ah_translation_manager`;

-- Logs

CREATE TABLE IF NOT EXISTS `logs` (
  `log_id` varchar(100) NOT NULL,
  `entry_date` timestamp(3) NULL DEFAULT NULL,
  `logger` varchar(100) DEFAULT NULL,
  `log_level` varchar(100) DEFAULT NULL,
  `message` mediumtext,
  `exception` mediumtext,
  PRIMARY KEY (`log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;  

-- BridgeHeader

CREATE TABLE IF NOT EXISTS `bridge_header` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(63) NOT NULL,
  `created_by` varchar(63) NOT NULL,
  `status` varchar(14) NOT NULL DEFAULT 'NEW',
  `message` mediumtext,
  `usage_report_count` integer NOT NULL DEFAULT 0,
  `alives_at` timestamp,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- BridgeDiscovery

CREATE TABLE IF NOT EXISTS `bridge_discovery` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `header_id` bigint(20) NOT NULL,
  `data` mediumtext NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_bridge_header_discovery` FOREIGN KEY (`header_id`) REFERENCES `bridge_header` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- BridgeDetails

CREATE TABLE IF NOT EXISTS `bridge_details` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `header_id` bigint(20) NOT NULL,
  `consumer` varchar(63) NOT NULL, 
  `provider` varchar(63) NOT NULL, 
  `service_definition` varchar(63) NOT NULL, 
  `operation` varchar(63) NOT NULL, 
  `interface_translator` varchar(63) NOT NULL,
  `interface_translator_data` mediumtext NOT NULL,
  `input_dm_translator` varchar(63),  
  `input_dm_translator_data` mediumtext,
  `result_dm_translator` varchar(63),  
  `result_dm_translator_data` mediumtext,
  PRIMARY KEY (`id`),
  UNIQUE KEY (`header_id`),
  CONSTRAINT `fk_bridge_header_details` FOREIGN KEY (`header_id`) REFERENCES `bridge_header` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;