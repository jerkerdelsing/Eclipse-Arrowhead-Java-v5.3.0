USE `ah_datamanager`;

REVOKE ALL, GRANT OPTION FROM 'datamanager'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_datamanager`.`logs` TO 'datamanager'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_datamanager`.`historian_record` TO 'datamanager'@'localhost';

REVOKE ALL, GRANT OPTION FROM 'datamanager'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_datamanager`.`logs` TO 'datamanager'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_datamanager`.`historian_record` TO 'datamanager'@'%';

FLUSH PRIVILEGES;
