USE `ah_serviceregistry`;

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

-- Devices

CREATE TABLE IF NOT EXISTS `device` (
	`id` bigint(20) NOT NULL AUTO_INCREMENT,
	`name` varchar(63) NOT NULL,
	`metadata` mediumtext,
	`created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY (`id`),
	UNIQUE KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `device_address` (
	`id` bigint(20) NOT NULL AUTO_INCREMENT,
	`device_id` bigint(20) NOT NULL,
	`address_type` varchar(30) NOT NULL,
	`address` VARCHAR(1024) NOT NULL,
	`created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY (`id`),
	CONSTRAINT `fk_device_id` FOREIGN KEY (`device_id`) REFERENCES `device` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Systems

CREATE TABLE IF NOT EXISTS `system_` (
	`id` bigint(20) NOT NULL AUTO_INCREMENT,
	`name` varchar(63) NOT NULL,
	`metadata` mediumtext,
	`version` varchar(14) NOT NULL DEFAULT '1.0.0',
	`created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY (`id`),
	UNIQUE KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `system_address` (
	`id` bigint(20) NOT NULL AUTO_INCREMENT,
	`system_id` bigint(20) NOT NULL,
	`address_type` varchar(30) NOT NULL,
	`address` VARCHAR(1024) NOT NULL,
	`created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY (`id`),
	CONSTRAINT `fk_system_id` FOREIGN KEY (`system_id`) REFERENCES `system_` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `device_system_connector` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `device_id` bigint(20) NOT NULL,
  `system_id` bigint(20) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `pair` (`device_id`,`system_id`),
  UNIQUE KEY `system` (`system_id`),
  CONSTRAINT `fk_device_id2` FOREIGN KEY (`device_id`) REFERENCES `device` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_system_id2` FOREIGN KEY (`system_id`) REFERENCES `system_` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Services

CREATE TABLE IF NOT EXISTS `service_definition` (
	`id` bigint(20) NOT NULL AUTO_INCREMENT,
	`name` varchar(63) NOT NULL,
	`created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY (`id`),
	UNIQUE KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `service_instance` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `service_instance_id` varchar(255) NOT NULL,
  `system_id` bigint(20) NOT NULL,
  `service_definition_id` bigint(20) NOT NULL,
  `version` varchar(14) NOT NULL DEFAULT '1.0.0',
  `expires_at` timestamp,
  `metadata` mediumtext,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_service_instance_id` (`service_instance_id`),
  CONSTRAINT `fk_system_id3` FOREIGN KEY (`system_id`) REFERENCES `system_` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_service_definition_id` FOREIGN KEY (`service_definition_id`) REFERENCES `service_definition` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE IF NOT EXISTS `service_interface_template` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(63) NOT NULL,
  `protocol` varchar(63) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `service_interface_template_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `service_interface_template_property` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `service_interface_template_id` bigint(20) NOT NULL,
  `property_name` varchar(63) NOT NULL,
  `mandatory` tinyint(1) NOT NULL DEFAULT 0,
  `validator` varchar(1024),
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `pair` (`service_interface_template_id`, `property_name`),
  CONSTRAINT `fk_service_interface_template_property_id` FOREIGN KEY (`service_interface_template_id`) REFERENCES `service_interface_template` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `service_instance_interface` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `service_instance_id` bigint(20) NOT NULL,
  `service_interface_template_id` bigint(20) NOT NULL,
  `properties` mediumtext NOT NULL,
  `policy` varchar(63) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_service_instance_id` FOREIGN KEY (`service_instance_id`) REFERENCES `service_instance` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_service_interface_template_id` FOREIGN KEY (`service_interface_template_id`) REFERENCES `service_interface_template` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
