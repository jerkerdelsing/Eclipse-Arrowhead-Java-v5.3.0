USE `ah_localcloudconfigurator`;

-- Data manipulation only: schema changes are made by the initialisation scripts as root.
REVOKE ALL, GRANT OPTION FROM 'localcloudconfigurator'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_localcloudconfigurator`.* TO 'localcloudconfigurator'@'localhost';

REVOKE ALL, GRANT OPTION FROM 'localcloudconfigurator'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_localcloudconfigurator`.* TO 'localcloudconfigurator'@'%';

FLUSH PRIVILEGES;
