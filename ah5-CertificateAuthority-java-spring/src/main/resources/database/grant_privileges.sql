USE `ah_certificate_authority`;

-- Data manipulation only: schema changes are made by the initialisation scripts as root.
REVOKE ALL, GRANT OPTION FROM 'certificateauthority'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_certificate_authority`.* TO 'certificateauthority'@'localhost';

REVOKE ALL, GRANT OPTION FROM 'certificateauthority'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_certificate_authority`.* TO 'certificateauthority'@'%';

FLUSH PRIVILEGES;
