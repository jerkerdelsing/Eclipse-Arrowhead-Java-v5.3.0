USE `ah_sosconfigurator`;

-- Data manipulation only: schema changes are made by the initialisation scripts as root.
REVOKE ALL, GRANT OPTION FROM 'sosconfigurator'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_sosconfigurator`.* TO 'sosconfigurator'@'localhost';

REVOKE ALL, GRANT OPTION FROM 'sosconfigurator'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_sosconfigurator`.* TO 'sosconfigurator'@'%';

FLUSH PRIVILEGES;
