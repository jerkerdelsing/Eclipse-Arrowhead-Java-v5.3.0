USE `ah_device_qos_evaluator`;

REVOKE ALL, GRANT OPTION FROM 'deviceqosevaluator'@'localhost';

GRANT ALL PRIVILEGES ON `ah_device_qos_evaluator`.`logs` TO 'deviceqosevaluator'@'localhost';
GRANT ALL PRIVILEGES ON `ah_device_qos_evaluator`.`device` TO 'deviceqosevaluator'@'localhost';
GRANT ALL PRIVILEGES ON `ah_device_qos_evaluator`.`system_` TO 'deviceqosevaluator'@'localhost';
GRANT ALL PRIVILEGES ON `ah_device_qos_evaluator`.`stat_round_trip_time` TO 'deviceqosevaluator'@'localhost';
GRANT ALL PRIVILEGES ON `ah_device_qos_evaluator`.`stat_cpu_total_load` TO 'deviceqosevaluator'@'localhost';
GRANT ALL PRIVILEGES ON `ah_device_qos_evaluator`.`stat_memory_used` TO 'deviceqosevaluator'@'localhost';
GRANT ALL PRIVILEGES ON `ah_device_qos_evaluator`.`stat_net_egress_load` TO 'deviceqosevaluator'@'localhost';
GRANT ALL PRIVILEGES ON `ah_device_qos_evaluator`.`stat_net_ingress_load` TO 'deviceqosevaluator'@'localhost';

REVOKE ALL, GRANT OPTION FROM 'deviceqosevaluator'@'%';

GRANT ALL PRIVILEGES ON `ah_device_qos_evaluator`.`logs` TO 'deviceqosevaluator'@'%';
GRANT ALL PRIVILEGES ON `ah_device_qos_evaluator`.`device` TO 'deviceqosevaluator'@'%';
GRANT ALL PRIVILEGES ON `ah_device_qos_evaluator`.`system_` TO 'deviceqosevaluator'@'%';
GRANT ALL PRIVILEGES ON `ah_device_qos_evaluator`.`stat_round_trip_time` TO 'deviceqosevaluator'@'%';
GRANT ALL PRIVILEGES ON `ah_device_qos_evaluator`.`stat_cpu_total_load` TO 'deviceqosevaluator'@'%';
GRANT ALL PRIVILEGES ON `ah_device_qos_evaluator`.`stat_memory_used` TO 'deviceqosevaluator'@'%';
GRANT ALL PRIVILEGES ON `ah_device_qos_evaluator`.`stat_net_egress_load` TO 'deviceqosevaluator'@'%';
GRANT ALL PRIVILEGES ON `ah_device_qos_evaluator`.`stat_net_ingress_load` TO 'deviceqosevaluator'@'%';

FLUSH PRIVILEGES;