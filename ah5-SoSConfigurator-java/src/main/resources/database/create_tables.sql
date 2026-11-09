-- SoSConfigurator schema (ah_sosconfigurator)

CREATE TABLE IF NOT EXISTS `sos_deployment` (
  `id` varchar(64) NOT NULL,
  `sos_name` varchar(255) NOT NULL,
  `submitted_by` varchar(127) NOT NULL,
  `submitted_at` datetime(6) NOT NULL,
  `request_json` mediumtext NOT NULL,
  `plan_json` mediumtext NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_sos_deployment_submitted_at` (`submitted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `logs` (
  `log_id` varchar(100) NOT NULL,
  `entry_date` timestamp(3) NULL DEFAULT NULL,
  `logger` varchar(100) DEFAULT NULL,
  `log_level` varchar(100) DEFAULT NULL,
  `message` mediumtext,
  `exception` mediumtext,
  PRIMARY KEY (`log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
