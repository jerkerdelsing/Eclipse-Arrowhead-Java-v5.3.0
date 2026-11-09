USE `ah_device_qos_evaluator`;

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

-- Device

CREATE TABLE IF NOT EXISTS `device` (
  `id` binary(16) NOT NULL,
  `address` VARCHAR(1024) NOT NULL,
  `rtt_port` int(11) DEFAULT NULL,
  `augmented` int(1) NOT NULL DEFAULT 0,
  `inactive` int(1) NOT NULL DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `device_address_uk` (`address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- System

CREATE TABLE IF NOT EXISTS `system_` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(63) NOT NULL,
  `device_id` binary(16) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `sys_name_uk` (`name`),
  CONSTRAINT `fk_device_id` FOREIGN KEY (`device_id`) REFERENCES `device` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Statistics | Round-Trip Time

CREATE TABLE IF NOT EXISTS `stat_round_trip_time` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` binary(16) NOT NUll,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `minimum` double NOT NULL,
  `maximum` double NOT NULL,
  `mean` double NOT NULL,
  `median` double NOT NULL,
  `current` double NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Statistics | CPU Total Load

CREATE TABLE IF NOT EXISTS `stat_cpu_total_load` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` binary(16) NOT NUll,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `minimum` double NOT NULL,
  `maximum` double NOT NULL,
  `mean` double NOT NULL,
  `median` double NOT NULL,
  `current` double NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Statistics | Memory Used

CREATE TABLE IF NOT EXISTS `stat_memory_used` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` binary(16) NOT NUll,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `minimum` double NOT NULL,
  `maximum` double NOT NULL,
  `mean` double NOT NULL,
  `median` double NOT NULL,
  `current` double NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Statistics | Network Egress Load

CREATE TABLE IF NOT EXISTS `stat_net_egress_load` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` binary(16) NOT NUll,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `minimum` double NOT NULL,
  `maximum` double NOT NULL,
  `mean` double NOT NULL,
  `median` double NOT NULL,
  `current` double NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Statistics | Network Ingress Load

CREATE TABLE IF NOT EXISTS `stat_net_ingress_load` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uuid` binary(16) NOT NUll,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `minimum` double NOT NULL,
  `maximum` double NOT NULL,
  `mean` double NOT NULL,
  `median` double NOT NULL,
  `current` double NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;