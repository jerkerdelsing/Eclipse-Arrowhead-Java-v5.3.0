USE `ah_serviceorchestration_simple`;

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

-- Orchestration store

CREATE TABLE IF NOT EXISTS `orchestration_store` (
  `id` binary(16) NOT NULL,
  `consumer` varchar(63) NOT NULL,
  `service_definition` varchar(63) NOT NULL,
  `service_instance_id` varchar(255) NOT NULL,
  `priority` INT NOT NULL,
  `created_by` varchar(63) NOT NULL,
  `updated_by` varchar(63) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `subscription_uk` (`consumer`, `service_definition`, `priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Subscription

CREATE TABLE IF NOT EXISTS `subscription` (
  `id` binary(16) NOT NULL,
  `owner_system` varchar(63) NOT NULL,
  `target_system` varchar(63) NOT NULL,
  `service_definition` varchar(63),
  `expires_at` timestamp,
  `notify_protocol` varchar(14) NOT NULL,
  `notify_properties` mediumtext NOT NULL,
  `orchestration_request` mediumtext,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 

-- Orchestration job

CREATE TABLE IF NOT EXISTS `orchestration_job` (
  `id` binary(16) NOT NULL,
  `status` varchar(14) NOT NULL,
  `type` varchar(14) NOT NULL,
  `requester_system` varchar(63) NOT NULL,
  `target_system` varchar(63) NOT NULL,
  `service_definition` varchar(63),
  `subscription_id` varchar(63),
  `message` mediumtext,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `started_at` timestamp,
  `finished_at` timestamp,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;