DROP DATABASE IF EXISTS `ah_certificate_authority`;
CREATE DATABASE `ah_certificate_authority` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `ah_certificate_authority`;

-- create tables
source create_tables.sql

-- Set up privileges
CREATE USER IF NOT EXISTS 'certificateauthority'@'localhost' IDENTIFIED BY 'change-me';
CREATE USER IF NOT EXISTS 'certificateauthority'@'%' IDENTIFIED BY 'change-me';
source grant_privileges.sql
