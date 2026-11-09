DROP DATABASE IF EXISTS `ah_serviceorchestration_dynamic`;
CREATE DATABASE `ah_serviceorchestration_dynamic`;
USE `ah_serviceorchestration_dynamic`;

-- create tables
source create_tables.sql

-- Set up privileges
CREATE USER IF NOT EXISTS 'serviceorchestration-dynamic'@'localhost' IDENTIFIED BY 'ModWO7puR3gQx9';
CREATE USER IF NOT EXISTS 'serviceorchestration-dynamic'@'%' IDENTIFIED BY 'ModWO7puR3gQx9';
source grant_privileges.sql

-- Default content
source default_inserts.sql