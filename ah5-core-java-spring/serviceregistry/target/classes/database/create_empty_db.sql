DROP DATABASE IF EXISTS `ah_serviceregistry`;
CREATE DATABASE `ah_serviceregistry`;
USE `ah_serviceregistry`;

-- create tables
source create_tables.sql

-- Set up privileges
CREATE USER IF NOT EXISTS 'serviceregistry'@'localhost' IDENTIFIED BY 'IsD3KDg8yfUblab';
CREATE USER IF NOT EXISTS 'serviceregistry'@'%' IDENTIFIED BY 'IsD3KDg8yfUblab';
source grant_privileges.sql

-- Default content
source default_inserts.sql