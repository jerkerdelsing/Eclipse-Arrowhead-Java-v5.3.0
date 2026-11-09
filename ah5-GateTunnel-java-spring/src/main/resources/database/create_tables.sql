USE `ah_gatetunnel`;

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

-- Session: control sessions of the 4.x-compatible gateTunnelConnection (inter-cloud relay sessions are kept in memory)

CREATE TABLE IF NOT EXISTS `session` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `session_id` varchar(63) NOT NULL,
  `queue_id` varchar(255) NOT NULL,
  `peer_name` varchar(255) NOT NULL,
  `service_definition` varchar(255) NOT NULL,
  `tunnel_port` int(11) NOT NULL,
  `state` varchar(14) NOT NULL,
  `consumer_json` mediumtext NOT NULL,
  `provider_json` mediumtext NOT NULL,
  `consumer_cloud_json` mediumtext NOT NULL,
  `provider_cloud_json` mediumtext NOT NULL,
  `relay_json` mediumtext NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_session_id` (`session_id`),
  UNIQUE KEY `uk_session_queue_id` (`queue_id`),
  UNIQUE KEY `uk_session_tunnel_port` (`tunnel_port`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
