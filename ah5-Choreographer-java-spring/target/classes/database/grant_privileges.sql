CREATE USER IF NOT EXISTS 'choreographer'@'%' IDENTIFIED BY 'change-me';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_choreographer`.* TO 'choreographer'@'%';
FLUSH PRIVILEGES;
