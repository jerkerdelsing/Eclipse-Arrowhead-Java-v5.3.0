DROP DATABASE IF EXISTS `ah_gatekeeper`;
CREATE DATABASE `ah_gatekeeper`;
USE `ah_gatekeeper`;

-- create tables
source create_tables.sql

-- Set up privileges (replace the password before use)
CREATE USER IF NOT EXISTS 'gatekeeper'@'localhost' IDENTIFIED BY 'change-me';
CREATE USER IF NOT EXISTS 'gatekeeper'@'%' IDENTIFIED BY 'change-me';
source grant_privileges.sql
