CREATE USER IF NOT EXISTS 'eventhandler'@'%' IDENTIFIED BY 'change-me';
GRANT ALL PRIVILEGES ON `ah_eventhandler`.* TO 'eventhandler'@'%';
FLUSH PRIVILEGES;
