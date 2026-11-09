USE `ah_serviceorchestration_dynamic`;

CREATE TABLE IF NOT EXISTS `qos_eval_result` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `orchestration_job_id` binary(16) NOT NULL,
  `evaluation_type` varchar(255) NOT NULL,
  `operation` varchar(63) NOT NULL,
  `result` mediumtext NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_job_qos_id` FOREIGN KEY (`orchestration_job_id`) REFERENCES `orchestration_job` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

GRANT ALL PRIVILEGES ON `ah_serviceorchestration_dynamic`.`qos_eval_result` TO 'serviceorchestration-dynamic'@'localhost';
GRANT ALL PRIVILEGES ON `ah_serviceorchestration_dynamic`.`qos_eval_result` TO 'serviceorchestration-dynamic'@'%';