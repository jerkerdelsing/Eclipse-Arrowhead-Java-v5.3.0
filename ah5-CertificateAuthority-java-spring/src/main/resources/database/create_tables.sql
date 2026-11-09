-- CertificateAuthority schema (ah_certificate_authority)

CREATE TABLE IF NOT EXISTS `issued_certificate` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `serial_number` varchar(64) NOT NULL,
  `common_name` varchar(255) NOT NULL,
  `created_by` varchar(127) NOT NULL,
  `valid_from` datetime(6) NOT NULL,
  `valid_until` datetime(6) NOT NULL,
  `revoked_at` datetime(6) NULL DEFAULT NULL,
  `certificate_pem` text NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_issued_certificate_serial` (`serial_number`),
  KEY `idx_issued_certificate_common_name` (`common_name`),
  KEY `idx_issued_certificate_revoked` (`revoked_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `trusted_key` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `public_key_pem` text NOT NULL,
  `public_key_hash` varchar(64) NOT NULL,
  `description` varchar(1024) NOT NULL DEFAULT '',
  `valid_after` datetime(6) NOT NULL,
  `valid_before` datetime(6) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trusted_key_hash` (`public_key_hash`)
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
