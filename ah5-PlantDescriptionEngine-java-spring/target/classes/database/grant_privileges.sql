CREATE USER IF NOT EXISTS 'plantdescriptionengine'@'%' IDENTIFIED BY 'change-me';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_plantdescriptionengine`.* TO 'plantdescriptionengine'@'%';
FLUSH PRIVILEGES;
