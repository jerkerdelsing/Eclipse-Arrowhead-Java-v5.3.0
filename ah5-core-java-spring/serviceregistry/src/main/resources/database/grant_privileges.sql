USE `ah_serviceregistry`;

REVOKE ALL, GRANT OPTION FROM 'serviceregistry'@'localhost';

GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceregistry`.`logs` TO 'serviceregistry'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceregistry`.`device` TO 'serviceregistry'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceregistry`.`device_address` TO 'serviceregistry'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceregistry`.`device_system_connector` TO 'serviceregistry'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceregistry`.`service_definition` TO 'serviceregistry'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceregistry`.`service_instance` TO 'serviceregistry'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceregistry`.`service_instance_interface` TO 'serviceregistry'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceregistry`.`service_interface_template` TO 'serviceregistry'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceregistry`.`service_interface_template_property` TO 'serviceregistry'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceregistry`.`system_` TO 'serviceregistry'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceregistry`.`system_address` TO 'serviceregistry'@'localhost';

REVOKE ALL, GRANT OPTION FROM 'serviceregistry'@'%';

GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceregistry`.`logs` TO 'serviceregistry'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceregistry`.`device` TO 'serviceregistry'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceregistry`.`device_address` TO 'serviceregistry'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceregistry`.`device_system_connector` TO 'serviceregistry'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceregistry`.`service_definition` TO 'serviceregistry'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceregistry`.`service_instance` TO 'serviceregistry'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceregistry`.`service_instance_interface` TO 'serviceregistry'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceregistry`.`service_interface_template` TO 'serviceregistry'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceregistry`.`service_interface_template_property` TO 'serviceregistry'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceregistry`.`system_` TO 'serviceregistry'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_serviceregistry`.`system_address` TO 'serviceregistry'@'%';

FLUSH PRIVILEGES;