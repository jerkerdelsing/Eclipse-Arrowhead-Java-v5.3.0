DROP DATABASE IF EXISTS `ah_datamanager`;
CREATE DATABASE `ah_datamanager`;
USE `ah_datamanager`;

source create_tables.sql

CREATE USER IF NOT EXISTS 'datamanager'@'localhost' IDENTIFIED BY 'gRLjXbqu0YwYhfK';
CREATE USER IF NOT EXISTS 'datamanager'@'%' IDENTIFIED BY 'gRLjXbqu0YwYhfK';
source grant_privileges.sql
