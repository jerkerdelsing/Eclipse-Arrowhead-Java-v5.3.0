DROP DATABASE IF EXISTS `ah_sysmonitor`;
CREATE DATABASE `ah_sysmonitor` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `ah_sysmonitor`;

-- create tables
source create_tables.sql

-- Set up privileges
CREATE USER IF NOT EXISTS 'sysmonitor'@'localhost' IDENTIFIED BY 'change-me';
CREATE USER IF NOT EXISTS 'sysmonitor'@'%' IDENTIFIED BY 'change-me';
source grant_privileges.sql
