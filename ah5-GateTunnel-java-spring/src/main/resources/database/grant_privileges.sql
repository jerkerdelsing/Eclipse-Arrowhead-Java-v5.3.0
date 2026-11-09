USE `ah_gatetunnel`;

-- Grant privileges: data manipulation only (no DDL; the schema is validated, never changed, at run time)
REVOKE ALL, GRANT OPTION FROM 'gatetunnel'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_gatetunnel`.`session` TO 'gatetunnel'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_gatetunnel`.`logs` TO 'gatetunnel'@'localhost';

REVOKE ALL, GRANT OPTION FROM 'gatetunnel'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_gatetunnel`.`session` TO 'gatetunnel'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_gatetunnel`.`logs` TO 'gatetunnel'@'%';

FLUSH PRIVILEGES;
