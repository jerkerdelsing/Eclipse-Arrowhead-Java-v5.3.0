DROP DATABASE IF EXISTS `ah_solocalcloudconfigurator`;
CREATE DATABASE `ah_solocalcloudconfigurator`;
USE `ah_solocalcloudconfigurator`;

-- create tables
source create_tables.sql

-- Set up privileges
CREATE USER IF NOT EXISTS 'solocalcloudconfigurator'@'localhost' IDENTIFIED BY 'change-me';
CREATE USER IF NOT EXISTS 'solocalcloudconfigurator'@'%' IDENTIFIED BY 'change-me';
source grant_privileges.sql
