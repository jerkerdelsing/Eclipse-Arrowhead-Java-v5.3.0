DROP DATABASE IF EXISTS `ah_authentication`;
CREATE DATABASE `ah_authentication`;
USE `ah_authentication`;

-- create tables
source create_tables.sql

-- Set up privileges
CREATE USER IF NOT EXISTS 'authentication'@'localhost' IDENTIFIED BY 'kleOW2G26URu8CT';
CREATE USER IF NOT EXISTS 'authentication'@'%' IDENTIFIED BY 'kleOW2G26URu8CT';
source grant_privileges.sql

-- Default content
source default_inserts.sql