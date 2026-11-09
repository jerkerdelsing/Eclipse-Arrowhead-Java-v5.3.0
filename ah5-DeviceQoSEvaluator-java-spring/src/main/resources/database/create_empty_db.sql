DROP DATABASE IF EXISTS `ah_device_qos_evaluator`;
CREATE DATABASE `ah_device_qos_evaluator`;
USE `ah_device_qos_evaluator`;

-- create tables
source create_tables.sql

-- Set up privileges
CREATE USER IF NOT EXISTS 'deviceqosevaluator'@'localhost' IDENTIFIED BY '5k2x9g8EU5tz1ksN';
CREATE USER IF NOT EXISTS 'deviceqosevaluator'@'%' IDENTIFIED BY '5k2x9g8EU5tz1ksN';
source grant_privileges.sql

-- Default content
source default_inserts.sql