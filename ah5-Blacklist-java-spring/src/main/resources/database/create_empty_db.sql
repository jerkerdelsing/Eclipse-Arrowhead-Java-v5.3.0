DROP DATABASE IF EXISTS `ah_blacklist`;
CREATE DATABASE `ah_blacklist`;
USE `ah_blacklist`;

-- create tables
source create_tables.sql

-- Set up privileges
CREATE USER IF NOT EXISTS 'blacklist'@'localhost' IDENTIFIED BY '7w1AZwfnubFLei0';
CREATE USER IF NOT EXISTS 'blacklist'@'%' IDENTIFIED BY '7w1AZwfnubFLei0';
source grant_privileges.sql