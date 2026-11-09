CREATE USER IF NOT EXISTS 'timemanager'@'%' IDENTIFIED BY 'change-me';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_timemanager`.* TO 'timemanager'@'%';
FLUSH PRIVILEGES;
