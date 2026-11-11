USE `ah_blacklist`;

-- Grant privileges
REVOKE ALL, GRANT OPTION FROM 'blacklist'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_blacklist`.`entry` TO 'blacklist'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_blacklist`.`logs` TO 'blacklist'@'localhost';

REVOKE ALL, GRANT OPTION FROM 'blacklist'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_blacklist`.`entry` TO 'blacklist'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_blacklist`.`logs` TO 'blacklist'@'%';

FLUSH PRIVILEGES;