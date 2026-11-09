USE `ah_authentication`;

REVOKE ALL, GRANT OPTION FROM 'authentication'@'localhost';

GRANT ALL PRIVILEGES ON `ah_authentication`.`logs` TO 'authentication'@'localhost';
GRANT ALL PRIVILEGES ON `ah_authentication`.`system_` TO 'authentication'@'localhost';
GRANT ALL PRIVILEGES ON `ah_authentication`.`password_authentication` TO 'authentication'@'localhost';
GRANT ALL PRIVILEGES ON `ah_authentication`.`active_session` TO 'authentication'@'localhost';

REVOKE ALL, GRANT OPTION FROM 'authentication'@'%';

GRANT ALL PRIVILEGES ON `ah_authentication`.`logs` TO 'authentication'@'%';
GRANT ALL PRIVILEGES ON `ah_authentication`.`system_` TO 'authentication'@'%';
GRANT ALL PRIVILEGES ON `ah_authentication`.`password_authentication` TO 'authentication'@'%';
GRANT ALL PRIVILEGES ON `ah_authentication`.`active_session` TO 'authentication'@'%';

FLUSH PRIVILEGES;