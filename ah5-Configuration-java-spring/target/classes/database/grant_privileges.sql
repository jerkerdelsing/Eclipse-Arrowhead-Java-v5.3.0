CREATE USER IF NOT EXISTS 'configuration'@'%' IDENTIFIED BY 'change-me';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_configuration`.* TO 'configuration'@'%';
FLUSH PRIVILEGES;
