USE `ah_gatekeeper`;

-- Grant privileges: data manipulation only (JPA ddl-auto=validate, no migrations)
REVOKE ALL, GRANT OPTION FROM 'gatekeeper'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_gatekeeper`.`cloud` TO 'gatekeeper'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_gatekeeper`.`relay` TO 'gatekeeper'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_gatekeeper`.`logs` TO 'gatekeeper'@'localhost';

REVOKE ALL, GRANT OPTION FROM 'gatekeeper'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_gatekeeper`.`cloud` TO 'gatekeeper'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_gatekeeper`.`relay` TO 'gatekeeper'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_gatekeeper`.`logs` TO 'gatekeeper'@'%';

FLUSH PRIVILEGES;
