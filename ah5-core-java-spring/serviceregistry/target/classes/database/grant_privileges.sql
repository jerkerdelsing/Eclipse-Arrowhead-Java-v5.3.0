USE `ah_serviceregistry`;

REVOKE ALL, GRANT OPTION FROM 'serviceregistry'@'localhost';

GRANT ALL PRIVILEGES ON `ah_serviceregistry`.`logs` TO 'serviceregistry'@'localhost';
GRANT ALL PRIVILEGES ON `ah_serviceregistry`.`device` TO 'serviceregistry'@'localhost';
GRANT ALL PRIVILEGES ON `ah_serviceregistry`.`device_address` TO 'serviceregistry'@'localhost';
GRANT ALL PRIVILEGES ON `ah_serviceregistry`.`device_system_connector` TO 'serviceregistry'@'localhost';
GRANT ALL PRIVILEGES ON `ah_serviceregistry`.`service_definition` TO 'serviceregistry'@'localhost';
GRANT ALL PRIVILEGES ON `ah_serviceregistry`.`service_instance` TO 'serviceregistry'@'localhost';
GRANT ALL PRIVILEGES ON `ah_serviceregistry`.`service_instance_interface` TO 'serviceregistry'@'localhost';
GRANT ALL PRIVILEGES ON `ah_serviceregistry`.`service_interface_template` TO 'serviceregistry'@'localhost';
GRANT ALL PRIVILEGES ON `ah_serviceregistry`.`service_interface_template_property` TO 'serviceregistry'@'localhost';
GRANT ALL PRIVILEGES ON `ah_serviceregistry`.`system_` TO 'serviceregistry'@'localhost';
GRANT ALL PRIVILEGES ON `ah_serviceregistry`.`system_address` TO 'serviceregistry'@'localhost';

REVOKE ALL, GRANT OPTION FROM 'serviceregistry'@'%';

GRANT ALL PRIVILEGES ON `ah_serviceregistry`.`logs` TO 'serviceregistry'@'%';
GRANT ALL PRIVILEGES ON `ah_serviceregistry`.`device` TO 'serviceregistry'@'%';
GRANT ALL PRIVILEGES ON `ah_serviceregistry`.`device_address` TO 'serviceregistry'@'%';
GRANT ALL PRIVILEGES ON `ah_serviceregistry`.`device_system_connector` TO 'serviceregistry'@'%';
GRANT ALL PRIVILEGES ON `ah_serviceregistry`.`service_definition` TO 'serviceregistry'@'%';
GRANT ALL PRIVILEGES ON `ah_serviceregistry`.`service_instance` TO 'serviceregistry'@'%';
GRANT ALL PRIVILEGES ON `ah_serviceregistry`.`service_instance_interface` TO 'serviceregistry'@'%';
GRANT ALL PRIVILEGES ON `ah_serviceregistry`.`service_interface_template` TO 'serviceregistry'@'%';
GRANT ALL PRIVILEGES ON `ah_serviceregistry`.`service_interface_template_property` TO 'serviceregistry'@'%';
GRANT ALL PRIVILEGES ON `ah_serviceregistry`.`system_` TO 'serviceregistry'@'%';
GRANT ALL PRIVILEGES ON `ah_serviceregistry`.`system_address` TO 'serviceregistry'@'%';

FLUSH PRIVILEGES;