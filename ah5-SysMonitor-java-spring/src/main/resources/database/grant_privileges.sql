USE `ah_sysmonitor`;

-- Data manipulation only: schema changes are made by the initialisation scripts as root.
REVOKE ALL, GRANT OPTION FROM 'sysmonitor'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_sysmonitor`.* TO 'sysmonitor'@'localhost';

REVOKE ALL, GRANT OPTION FROM 'sysmonitor'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_sysmonitor`.* TO 'sysmonitor'@'%';

FLUSH PRIVILEGES;
