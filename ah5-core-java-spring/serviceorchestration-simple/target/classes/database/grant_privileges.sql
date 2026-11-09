USE `ah_serviceorchestration_simple`;

REVOKE ALL, GRANT OPTION FROM 'serviceorchestration-simple'@'localhost';

GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceorchestration_simple`.`logs` TO 'serviceorchestration-simple'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceorchestration_simple`.`subscription` TO 'serviceorchestration-simple'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceorchestration_simple`.`orchestration_job` TO 'serviceorchestration-simple'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceorchestration_simple`.`orchestration_store` TO 'serviceorchestration-simple'@'localhost';

REVOKE ALL, GRANT OPTION FROM 'serviceorchestration-simple'@'%';

GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceorchestration_simple`.`logs` TO 'serviceorchestration-simple'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceorchestration_simple`.`subscription` TO 'serviceorchestration-simple'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceorchestration_simple`.`orchestration_job` TO 'serviceorchestration-simple'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceorchestration_simple`.`orchestration_store` TO 'serviceorchestration-simple'@'%';

FLUSH PRIVILEGES;