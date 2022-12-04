CREATE TABLE `auction_house` (
  `id` int(10) unsigned NOT NULL DEFAULT '0',
  `ownerId` int(10) unsigned NOT NULL DEFAULT '0',
  `itemId` int(10) unsigned NOT NULL DEFAULT '0',
  `itemCount` int(10) unsigned NOT NULL DEFAULT '0',
  `enchant` int(10) unsigned NOT NULL DEFAULT '0',
  `priceId` int(10) unsigned NOT NULL DEFAULT '0',
  `priceCount` int(10) unsigned NOT NULL DEFAULT '0',
  `timer` bigint(20) unsigned DEFAULT '0',
  PRIMARY KEY (`id`)
);