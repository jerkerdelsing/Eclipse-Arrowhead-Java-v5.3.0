DROP DATABASE IF EXISTS `ah_sosconfigurator`;
CREATE DATABASE `ah_sosconfigurator` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `ah_sosconfigurator`;

-- create tables
source create_tables.sql

-- Set up privileges
CREATE USER IF NOT EXISTS 'sosconfigurator'@'localhost' IDENTIFIED BY 'change-me';
CREATE USER IF NOT EXISTS 'sosconfigurator'@'%' IDENTIFIED BY 'change-me';
source grant_privileges.sql
