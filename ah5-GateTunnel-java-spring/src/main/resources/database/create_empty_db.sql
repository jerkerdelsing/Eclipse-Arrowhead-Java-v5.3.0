DROP DATABASE IF EXISTS `ah_gatetunnel`;
CREATE DATABASE `ah_gatetunnel`;
USE `ah_gatetunnel`;

-- create tables
source create_tables.sql

-- Set up privileges (development example password; deployments use Compose secrets)
CREATE USER IF NOT EXISTS 'gatetunnel'@'localhost' IDENTIFIED BY 'gT4kQe9vLw2nXs7';
CREATE USER IF NOT EXISTS 'gatetunnel'@'%' IDENTIFIED BY 'gT4kQe9vLw2nXs7';
source grant_privileges.sql
