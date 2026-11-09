USE `ah_datamanager`;

REVOKE ALL, GRANT OPTION FROM 'datamanager'@'localhost';
GRANT ALL PRIVILEGES ON `ah_datamanager`.`logs` TO 'datamanager'@'localhost';
GRANT ALL PRIVILEGES ON `ah_datamanager`.`historian_record` TO 'datamanager'@'localhost';

REVOKE ALL, GRANT OPTION FROM 'datamanager'@'%';
GRANT ALL PRIVILEGES ON `ah_datamanager`.`logs` TO 'datamanager'@'%';
GRANT ALL PRIVILEGES ON `ah_datamanager`.`historian_record` TO 'datamanager'@'%';

FLUSH PRIVILEGES;
