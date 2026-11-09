USE `ah_translation_manager`;

REVOKE ALL, GRANT OPTION FROM 'translationmanager'@'localhost';

GRANT ALL PRIVILEGES ON `ah_translation_manager`.`logs` TO 'translationmanager'@'localhost';
GRANT ALL PRIVILEGES ON `ah_translation_manager`.`bridge_header` TO 'translationmanager'@'localhost';
GRANT ALL PRIVILEGES ON `ah_translation_manager`.`bridge_discovery` TO 'translationmanager'@'localhost';
GRANT ALL PRIVILEGES ON `ah_translation_manager`.`bridge_details` TO 'translationmanager'@'localhost';

REVOKE ALL, GRANT OPTION FROM 'translationmanager'@'%';

GRANT ALL PRIVILEGES ON `ah_translation_manager`.`logs` TO 'translationmanager'@'%';
GRANT ALL PRIVILEGES ON `ah_translation_manager`.`bridge_header` TO 'translationmanager'@'%';
GRANT ALL PRIVILEGES ON `ah_translation_manager`.`bridge_discovery` TO 'translationmanager'@'%';
GRANT ALL PRIVILEGES ON `ah_translation_manager`.`bridge_details` TO 'translationmanager'@'%';

FLUSH PRIVILEGES;