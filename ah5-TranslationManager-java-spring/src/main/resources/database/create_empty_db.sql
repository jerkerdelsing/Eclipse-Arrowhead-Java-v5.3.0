DROP DATABASE IF EXISTS `ah_translation_manager`;
CREATE DATABASE `ah_translation_manager`;
USE `ah_translation_manager`;

-- create tables
source create_tables.sql

-- Set up privileges
CREATE USER IF NOT EXISTS 'translationmanager'@'localhost' IDENTIFIED BY '9KfoNbrr1yMK77O';
CREATE USER IF NOT EXISTS 'translationmanager'@'%' IDENTIFIED BY '9KfoNbrr1yMK77O';
source grant_privileges.sql

-- Default content
source default_inserts.sql