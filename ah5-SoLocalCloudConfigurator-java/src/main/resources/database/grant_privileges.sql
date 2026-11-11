USE `ah_solocalcloudconfigurator`;

-- Data manipulation only: schema changes are made by the initialisation scripts as root.
REVOKE ALL, GRANT OPTION FROM 'solocalcloudconfigurator'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_solocalcloudconfigurator`.* TO 'solocalcloudconfigurator'@'localhost';

REVOKE ALL, GRANT OPTION FROM 'solocalcloudconfigurator'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_solocalcloudconfigurator`.* TO 'solocalcloudconfigurator'@'%';

FLUSH PRIVILEGES;
