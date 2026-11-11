USE `ah_solocalcloudconfigurator`;

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

-- System of LocalClouds designs: the design document as JSON text (members with their pinned LC design snapshots,
-- exchanges, relay and package options) and an optimistic revision

CREATE TABLE IF NOT EXISTS `solc_design` (
  `id` varchar(36) NOT NULL,
  `name` varchar(64) NOT NULL,
  `design` mediumtext NOT NULL,
  `revision` bigint NOT NULL,
  `updated_by` varchar(64) NOT NULL,
  `updated_at` varchar(40) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `solc_design_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Built deployment packages (zip, without source or image archives; the host assembler adds those)

CREATE TABLE IF NOT EXISTS `solc_package` (
  `id` varchar(36) NOT NULL,
  `design_id` varchar(36) NOT NULL,
  `design_name` varchar(64) NOT NULL,
  `design_revision` bigint NOT NULL,
  `image_mode` varchar(16) NOT NULL,
  `architectures` varchar(64) NOT NULL,
  `file_name` varchar(160) NOT NULL,
  `sha256` char(64) NOT NULL,
  `size_bytes` bigint NOT NULL,
  `content` longblob NOT NULL,
  `created_by` varchar(64) NOT NULL,
  `created_at` varchar(40) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
