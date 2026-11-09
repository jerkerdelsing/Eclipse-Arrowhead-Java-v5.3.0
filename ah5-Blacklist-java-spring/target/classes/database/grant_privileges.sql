USE `ah_blacklist`;

-- Grant privileges
REVOKE ALL, GRANT OPTION FROM 'blacklist'@'localhost';
GRANT ALL PRIVILEGES ON `ah_blacklist`.`entry` TO 'blacklist'@'localhost';
GRANT ALL PRIVILEGES ON `ah_blacklist`.`logs` TO 'blacklist'@'localhost';

REVOKE ALL, GRANT OPTION FROM 'blacklist'@'%';
GRANT ALL PRIVILEGES ON `ah_blacklist`.`entry` TO 'blacklist'@'%';
GRANT ALL PRIVILEGES ON `ah_blacklist`.`logs` TO 'blacklist'@'%';

FLUSH PRIVILEGES;