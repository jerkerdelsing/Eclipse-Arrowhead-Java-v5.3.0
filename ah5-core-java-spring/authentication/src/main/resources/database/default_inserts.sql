INSERT IGNORE INTO `system_` (`name`, `authentication_method`, `sysop`, `created_by`, `updated_by`) VALUES ('Sysop', 'PASSWORD', 1, 'Initialization script', 'Initialization script');

-- Default password: xFNHbh2mQyIp1gmimeTC (In a production environment this password should be changed after first login)
INSERT IGNORE INTO `password_authentication` (`system_id`, `password`) VALUES ((SELECT `id` FROM `system_` WHERE `name` = 'Sysop'), '$2a$10$PeLSXNYONS1gjdqWwNqgxuj2fAKRj3sl4hbUspWqLMr0XpBWZnwmK');

INSERT IGNORE INTO `system_` (`name`, `authentication_method`, `sysop`, `created_by`, `updated_by`) VALUES ('ServiceRegistry', 'PASSWORD', 1, 'Initialization script', 'Initialization script');

-- Default password: 123456 (In a production environment this password should be changed after first login)
INSERT IGNORE INTO `password_authentication` (`system_id`, `password`) VALUES ((SELECT `id` FROM `system_` WHERE `name` = 'ServiceRegistry'), '$2a$10$scTu/ZS2KFMAJhpcET5B3ugMPkYhwH8UQmEpPefP6bMuwvAotsW1O');