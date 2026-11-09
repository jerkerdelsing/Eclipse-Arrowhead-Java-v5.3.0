CREATE USER IF NOT EXISTS 'eventhandler'@'%' IDENTIFIED BY 'change-me';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_eventhandler`.* TO 'eventhandler'@'%';
FLUSH PRIVILEGES;
