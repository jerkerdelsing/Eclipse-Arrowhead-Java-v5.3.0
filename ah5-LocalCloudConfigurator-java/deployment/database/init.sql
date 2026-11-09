-- Only the private database schema is initialized. User/password come from Compose secrets.
CREATE DATABASE IF NOT EXISTS `ah_localcloudconfigurator`;

USE `ah_localcloudconfigurator`;

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

-- Saved designs (selections and graphical layout); list and map fields as JSON text

CREATE TABLE IF NOT EXISTS `configuration` (
  `id` varchar(36) NOT NULL,
  `name` varchar(200) NOT NULL,
  `systems` mediumtext NOT NULL,
  `removals` mediumtext NOT NULL,
  `reapply` bit(1) NOT NULL,
  `smoke` bit(1) NOT NULL,
  `layout` mediumtext NOT NULL,
  `revision` bigint NOT NULL,
  `updated_at` varchar(40) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Submitted deployments: reviewed plan and request, last reported state and step results

CREATE TABLE IF NOT EXISTS `deployment` (
  `id` varchar(128) NOT NULL,
  `plan` mediumtext NOT NULL,
  `request` mediumtext NOT NULL,
  `state` varchar(32) NOT NULL,
  `result` mediumtext,
  `submitted_at` varchar(40) NOT NULL,
  `updated_at` varchar(40) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- The deployment that blocks new ones (at most one row, id 1)

CREATE TABLE IF NOT EXISTS `active_deployment` (
  `id` int NOT NULL,
  `deployment_id` varchar(128) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
