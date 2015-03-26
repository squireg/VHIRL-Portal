CREATE TABLE IF NOT EXISTS `uploads` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
    `name` varchar(255) DEFAULT NULL,
    `fileName` varchar(255) DEFAULT NULL,
    `size` bigint(20) DEFAULT NULL,
    `directoryFlag` bit(1) DEFAULT NULL,
    `parentPath` varchar(255) DEFAULT NULL,
    `owner` varchar(255) DEFAULT NULL,
    `date` datetime DEFAULT NULL,
    `description` varchar(255) DEFAULT NULL,
    `copyright` varchar(255) DEFAULT NULL,
    `jobId` int(11) NOT NULL,
    PRIMARY KEY (`id`),
  KEY `jobId` (`jobId`)
);