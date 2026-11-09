USE `ah_serviceorchestration_dynamic`;

REVOKE ALL, GRANT OPTION FROM 'serviceorchestration-dynamic'@'localhost';

GRANT ALL PRIVILEGES ON `ah_serviceorchestration_dynamic`.`logs` TO 'serviceorchestration-dynamic'@'localhost';
GRANT ALL PRIVILEGES ON `ah_serviceorchestration_dynamic`.`subscription` TO 'serviceorchestration-dynamic'@'localhost';
GRANT ALL PRIVILEGES ON `ah_serviceorchestration_dynamic`.`orchestration_job` TO 'serviceorchestration-dynamic'@'localhost';
GRANT ALL PRIVILEGES ON `ah_serviceorchestration_dynamic`.`orchestration_lock` TO 'serviceorchestration-dynamic'@'localhost';
GRANT ALL PRIVILEGES ON `ah_serviceorchestration_dynamic`.`qos_eval_result` TO 'serviceorchestration-dynamic'@'localhost';

REVOKE ALL, GRANT OPTION FROM 'serviceorchestration-dynamic'@'%';

GRANT ALL PRIVILEGES ON `ah_serviceorchestration_dynamic`.`logs` TO 'serviceorchestration-dynamic'@'%';
GRANT ALL PRIVILEGES ON `ah_serviceorchestration_dynamic`.`subscription` TO 'serviceorchestration-dynamic'@'%';
GRANT ALL PRIVILEGES ON `ah_serviceorchestration_dynamic`.`orchestration_job` TO 'serviceorchestration-dynamic'@'%';
GRANT ALL PRIVILEGES ON `ah_serviceorchestration_dynamic`.`orchestration_lock` TO 'serviceorchestration-dynamic'@'%';
GRANT ALL PRIVILEGES ON `ah_serviceorchestration_dynamic`.`qos_eval_result` TO 'serviceorchestration-dynamic'@'%';

FLUSH PRIVILEGES;
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceorchestration_dynamic`.`orchestration_plant_rules` TO 'serviceorchestration-dynamic'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceorchestration_dynamic`.`orchestration_plant_rules` TO 'serviceorchestration-dynamic'@'%';
