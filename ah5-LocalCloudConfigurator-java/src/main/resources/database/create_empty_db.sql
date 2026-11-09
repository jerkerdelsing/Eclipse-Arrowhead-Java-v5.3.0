DROP DATABASE IF EXISTS `ah_localcloudconfigurator`;
CREATE DATABASE `ah_localcloudconfigurator`;
USE `ah_localcloudconfigurator`;

-- create tables
source create_tables.sql

-- Set up privileges
CREATE USER IF NOT EXISTS 'localcloudconfigurator'@'localhost' IDENTIFIED BY 'change-me';
CREATE USER IF NOT EXISTS 'localcloudconfigurator'@'%' IDENTIFIED BY 'change-me';
source grant_privileges.sql
