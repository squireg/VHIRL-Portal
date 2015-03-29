ALTER TABLE `downloads` ADD COLUMN
  `parentUrl` varchar(255) DEFAULT NULL;
ALTER TABLE `downloads` ADD COLUMN
  `parentName` varchar(255) DEFAULT NULL;
ALTER TABLE `downloads` ADD COLUMN
  `owner` varchar(255) DEFAULT NULL;