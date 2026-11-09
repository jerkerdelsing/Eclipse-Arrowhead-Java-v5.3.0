DROP DATABASE IF EXISTS `ah_serviceorchestration_simple`;
CREATE DATABASE `ah_serviceorchestration_simple`;
USE `ah_serviceorchestration_simple`;

-- create tables
source create_tables.sql

-- Set up privileges
CREATE USER IF NOT EXISTS 'serviceorchestration-simple'@'localhost' IDENTIFIED BY 'le36iWI1TCzH2k';
CREATE USER IF NOT EXISTS 'serviceorchestration-simple'@'%' IDENTIFIED BY 'le36iWI1TCzH2k';
source grant_privileges.sql

-- Default content
source default_inserts.sql