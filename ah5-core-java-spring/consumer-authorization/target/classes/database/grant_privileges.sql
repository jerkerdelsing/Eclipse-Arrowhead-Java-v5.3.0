USE `ah_consumer_authorization`;

REVOKE ALL, GRANT OPTION FROM 'consumerauthorization'@'localhost';

GRANT ALL PRIVILEGES ON `ah_consumer_authorization`.`logs` TO 'consumerauthorization'@'localhost';
GRANT ALL PRIVILEGES ON `ah_consumer_authorization`.`auth_mgmt_policy_header` TO 'consumerauthorization'@'localhost';
GRANT ALL PRIVILEGES ON `ah_consumer_authorization`.`auth_provider_policy_header` TO 'consumerauthorization'@'localhost';
GRANT ALL PRIVILEGES ON `ah_consumer_authorization`.`auth_policy` TO 'consumerauthorization'@'localhost';
GRANT ALL PRIVILEGES ON `ah_consumer_authorization`.`cryptographer_auxiliary` TO 'consumerauthorization'@'localhost';
GRANT ALL PRIVILEGES ON `ah_consumer_authorization`.`encryption_key` TO 'consumerauthorization'@'localhost';
GRANT ALL PRIVILEGES ON `ah_consumer_authorization`.`token_header` TO 'consumerauthorization'@'localhost';
GRANT ALL PRIVILEGES ON `ah_consumer_authorization`.`time_limited_token` TO 'consumerauthorization'@'localhost';
GRANT ALL PRIVILEGES ON `ah_consumer_authorization`.`usage_limited_token` TO 'consumerauthorization'@'localhost';
GRANT ALL PRIVILEGES ON `ah_consumer_authorization`.`self_contained_token` TO 'consumerauthorization'@'localhost';

REVOKE ALL, GRANT OPTION FROM 'consumerauthorization'@'%';

GRANT ALL PRIVILEGES ON `ah_consumer_authorization`.`logs` TO 'consumerauthorization'@'%';
GRANT ALL PRIVILEGES ON `ah_consumer_authorization`.`auth_mgmt_policy_header` TO 'consumerauthorization'@'%';
GRANT ALL PRIVILEGES ON `ah_consumer_authorization`.`auth_provider_policy_header` TO 'consumerauthorization'@'%';
GRANT ALL PRIVILEGES ON `ah_consumer_authorization`.`auth_policy` TO 'consumerauthorization'@'%';
GRANT ALL PRIVILEGES ON `ah_consumer_authorization`.`cryptographer_auxiliary` TO 'consumerauthorization'@'%';
GRANT ALL PRIVILEGES ON `ah_consumer_authorization`.`encryption_key` TO 'consumerauthorization'@'%';
GRANT ALL PRIVILEGES ON `ah_consumer_authorization`.`token_header` TO 'consumerauthorization'@'%';
GRANT ALL PRIVILEGES ON `ah_consumer_authorization`.`time_limited_token` TO 'consumerauthorization'@'%';
GRANT ALL PRIVILEGES ON `ah_consumer_authorization`.`usage_limited_token` TO 'consumerauthorization'@'%';
GRANT ALL PRIVILEGES ON `ah_consumer_authorization`.`self_contained_token` TO 'consumerauthorization'@'%';

FLUSH PRIVILEGES;