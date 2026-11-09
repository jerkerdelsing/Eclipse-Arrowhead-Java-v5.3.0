USE `ah_consumer_authorization`;

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

-- Management Level Policy Headers

CREATE TABLE IF NOT EXISTS `auth_mgmt_policy_header` (
	`id` bigint(20) NOT NULL AUTO_INCREMENT,
	`instance_id` varchar(1024) NOT NULL,
	`target_type` varchar(30) NOT NULL,
	`cloud` varchar(255) NOT NULL DEFAULT 'LOCAL',
	`provider` varchar(63) NOT NULL,
	`target` varchar(63) NOT NULL,
	`description` varchar(1024) NULL,
	`created_by` varchar(63) NOT NULL,
	`created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`id`),
	UNIQUE KEY `unique_mgmt_instance_id` (`instance_id`),
	UNIQUE KEY unique_mgmt_type_cloud_provider_target (`target_type`, `cloud`, `provider`, `target`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Provider Level Policy Headers

CREATE TABLE IF NOT EXISTS `auth_provider_policy_header` (
	`id` bigint(20) NOT NULL AUTO_INCREMENT,
	`instance_id` varchar(1024) NOT NULL,
	`target_type` varchar(30) NOT NULL,
	`cloud` varchar(255) NOT NULL DEFAULT 'LOCAL',
	`provider` varchar(63) NOT NULL,
	`target` varchar(63) NOT NULL,
	`description` varchar(1024) NULL,
	`created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`id`),
	UNIQUE KEY `unique_pr_instance_id` (`instance_id`),
	UNIQUE KEY unique_pr_type_cloud_provider_target (`target_type`, `cloud`, `provider`, `target`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Policy

CREATE TABLE IF NOT EXISTS `auth_policy` (
	`id` bigint(20) NOT NULL AUTO_INCREMENT,
	`level` varchar(30) NOT NULL,
	`header_id` bigint(20) NOT NULL,
	`scope` varchar(63) NOT NULL DEFAULT '*',
	`policy_type` varchar(30) NOT NULL,
	`policy` mediumtext,
	PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Cryptographer Auxiliary

CREATE TABLE IF NOT EXISTS `cryptographer_auxiliary` (
	`id` bigint(20) NOT NULL AUTO_INCREMENT,	
	`value` mediumtext NOT NULL,
	`created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Encryption Key

CREATE TABLE IF NOT EXISTS `encryption_key` (
	`id` bigint(20) NOT NULL AUTO_INCREMENT,	
	`system_name` varchar(63) NOT NULL,
	`encrypted_key` mediumtext NOT NULL,
	`algorithm` varchar(63) NOT NULL,
	`internal_auxiliary_id` bigint(20) NOT NULL,
	`external_auxiliary_id` bigint(20),
	`created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`id`),
	UNIQUE KEY `unique_ek_system_name` (`system_name`),
	CONSTRAINT `fk_ek_ica_id` FOREIGN KEY (`internal_auxiliary_id`) REFERENCES `cryptographer_auxiliary` (`id`) ON DELETE CASCADE,
	CONSTRAINT `fk_ek_eca_id` FOREIGN KEY (`external_auxiliary_id`) REFERENCES `cryptographer_auxiliary` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Token Headers

CREATE TABLE IF NOT EXISTS `token_header` (
	`id` bigint(20) NOT NULL AUTO_INCREMENT,
	`token_type` varchar(30) NOT NULL,
	`token_hash` mediumtext NOT NULL,
	`consumer_cloud` varchar(255) NOT NULL DEFAULT 'LOCAL',
	`consumer` varchar(63) NOT NULL,
	`provider` varchar(63) NOT NULL,
	`target_type` varchar(30) NOT NULL,
	`target` varchar(63) NOT NULL,
	`scope` varchar(63) NULL,
	`requester` varchar(63) NOT NULL,
	`created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Time Limited Tokens

CREATE TABLE IF NOT EXISTS `time_limited_token` (
	`id` bigint(20) NOT NULL AUTO_INCREMENT,
	`header_id` bigint(20) NOT NULL,
	`expires_at` timestamp NOT NULL,
	PRIMARY KEY (`id`),
	CONSTRAINT `fk_token_header_id1` FOREIGN KEY (`header_id`) REFERENCES `token_header` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Usage Limited Tokens

CREATE TABLE IF NOT EXISTS `usage_limited_token` (
	`id` bigint(20) NOT NULL AUTO_INCREMENT,
	`header_id` bigint(20) NOT NULL,
	`usage_limit` mediumint NOT NULL,
	`usage_left` mediumint NOT NULL,
	PRIMARY KEY (`id`),
	CONSTRAINT `fk_token_header_id2` FOREIGN KEY (`header_id`) REFERENCES `token_header` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Self Contained Tokens

CREATE TABLE IF NOT EXISTS `self_contained_token` (
	`id` bigint(20) NOT NULL AUTO_INCREMENT,
	`header_id` bigint(20) NOT NULL,
	`variant` varchar(63) NOT NULL,
	`expires_at` timestamp NOT NULL,
	PRIMARY KEY (`id`),
	CONSTRAINT `fk_token_header_id3` FOREIGN KEY (`header_id`) REFERENCES `token_header` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;