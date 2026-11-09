DROP DATABASE IF EXISTS `ah_consumer_authorization`;
CREATE DATABASE `ah_consumer_authorization`;
USE `ah_consumer_authorization`;

-- create tables
source create_tables.sql

-- Set up privileges
CREATE USER IF NOT EXISTS 'consumerauthorization'@'localhost' IDENTIFIED BY '0BlG016nhDwmL47';
CREATE USER IF NOT EXISTS 'consumerauthorization'@'%' IDENTIFIED BY '0BlG016nhDwmL47';
source grant_privileges.sql

-- Default content
source default_inserts.sql