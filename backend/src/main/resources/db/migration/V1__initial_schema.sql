SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `board` (
  `active` bit(1) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `region_id` bigint DEFAULT NULL,
  `school_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `scope` enum('REGION','SCHOOL') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_board_school_title` (`school_id`,`title`),
  UNIQUE KEY `uq_board_region_title` (`region_id`,`title`),
  CONSTRAINT `FK7jmlc31ml9b7rnumwsb5ot7kn` FOREIGN KEY (`school_id`) REFERENCES `school` (`id`),
  CONSTRAINT `FKcau2b6wvevn6xl4ix9nsh3502` FOREIGN KEY (`region_id`) REFERENCES `region` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `chat_message` (
  `chat_room_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sender_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `content` text COLLATE utf8mb4_unicode_ci,
  `type` enum('IMAGE','TEXT') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_message_room_id` (`chat_room_id`,`id`),
  KEY `FKm92rh2bmfw19xcn7nj5vrixsi` (`sender_id`),
  CONSTRAINT `FKj52yap2xrm9u0721dct0tjor9` FOREIGN KEY (`chat_room_id`) REFERENCES `chat_room` (`id`),
  CONSTRAINT `FKm92rh2bmfw19xcn7nj5vrixsi` FOREIGN KEY (`sender_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `chat_room` (
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `last_message_at` datetime(6) DEFAULT NULL,
  `last_message_id` bigint DEFAULT NULL,
  `source_post_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user1_id` bigint NOT NULL,
  `user2_id` bigint NOT NULL,
  `last_message_preview` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `display_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_chat_room_dm_source` (`user1_id`,`user2_id`,`source_post_id`),
  KEY `idx_chat_room_last_message_at` (`last_message_at`),
  KEY `idx_room_empty_cutoff` (`last_message_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `chat_room_user` (
  `hidden` bit(1) NOT NULL,
  `blocked_at` datetime(6) DEFAULT NULL,
  `chat_room_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `last_read_message_id` bigint DEFAULT NULL,
  `left_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_chat_room_user` (`chat_room_id`,`user_id`),
  KEY `idx_cru_user_room` (`user_id`,`chat_room_id`),
  KEY `idx_cru_room_hidden` (`chat_room_id`,`hidden`),
  CONSTRAINT `FK368skiewasavvt4ltyep63dn8` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FKn7wfsq1ii61la6vi9gigw4pk1` FOREIGN KEY (`chat_room_id`) REFERENCES `chat_room` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `comment` (
  `anonymous` bit(1) NOT NULL,
  `depth` int NOT NULL,
  `dislike_count` int NOT NULL,
  `like_count` int NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint DEFAULT NULL,
  `post_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `comment_status` enum('ACTIVE','DELETED','HIDDEN') COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` tinytext COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKde3rfu96lep00br5ov0mdieyt` (`parent_id`),
  KEY `FK8kcum44fvpupyw6f5baccx25c` (`user_id`),
  CONSTRAINT `FK8kcum44fvpupyw6f5baccx25c` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FKde3rfu96lep00br5ov0mdieyt` FOREIGN KEY (`parent_id`) REFERENCES `comment` (`id`),
  CONSTRAINT `FKs1slvnkuemjsq2kj4h3vhx7i1` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `media` (
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `target_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `uploader_id` bigint NOT NULL,
  `url` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `media_type` enum('DOCUMENT','IMAGE','VIDEO') COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_type` enum('CHAT_MESSAGE','POST') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKs6ua5eqmpopc5hnvc9ikicvo2` (`uploader_id`),
  CONSTRAINT `FKs6ua5eqmpopc5hnvc9ikicvo2` FOREIGN KEY (`uploader_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `notification` (
  `is_read` bit(1) NOT NULL,
  `actor_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `target_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `board_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `message` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_type` enum('CHAT_MSG','COMMENT','INQUIRY','PENALTY','POST','REPORT','VERIFICATION_REQUEST','WARNING') COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` enum('ADMIN_INQUIRY','ADMIN_REPORT','ADMIN_VERIFICATION','CHAT','COMMENT','COMMENT_LIKE','INQUIRY','PENALTY','POST_LIKE','REPLY','SYSTEM','VERIFICATION_APPROVED','VERIFICATION_REJECTED','WARNING') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `penalty` (
  `status` tinyint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `expires_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `report_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `reason` enum('ABUSE','ETC','HARASSMENT','ILLEGAL','OBSCENE','SPAM') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKqlq6wk40lydo7jwa8bsjae3hp` (`report_id`),
  KEY `idx_penalty_user_expires` (`user_id`,`expires_at`),
  CONSTRAINT `FKi0duf4641rasurxr0kvqa8vt1` FOREIGN KEY (`report_id`) REFERENCES `report` (`id`),
  CONSTRAINT `FKnldcdm2661qwmocy5g4ejc5mo` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `penalty_chk_1` CHECK ((`status` between 0 and 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `post` (
  `anonymous` bit(1) NOT NULL,
  `dislike_count` int NOT NULL,
  `like_count` int NOT NULL,
  `view_count` int NOT NULL,
  `board_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `version` bigint DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `post_status` enum('ACTIVE','DELETED','HIDDEN') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_post_board_id` (`board_id`),
  KEY `idx_post_user_id` (`user_id`),
  KEY `idx_post_board_created` (`board_id`,`created_at`),
  KEY `idx_post_board_id_id` (`board_id`,`id`),
  CONSTRAINT `FK2t7katxxymxif93a9osshl0ns` FOREIGN KEY (`board_id`) REFERENCES `board` (`id`),
  CONSTRAINT `FK72mt33dhhs48hf9gcqrq4fxte` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `push_token` (
  `is_active` bit(1) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `token` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL,
  `platform` enum('ANDROID','IOS') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_push_token_token` (`token`),
  KEY `idx_push_token_user_active` (`user_id`,`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `reaction` (
  `disliked` bit(1) NOT NULL,
  `liked` bit(1) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `target_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `target_type` enum('COMMENT','POST') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_reaction_user_target` (`user_id`,`target_type`,`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `refresh_tokens` (
  `expires_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `token` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKghpmfn23vmxfu3spu3lfg4r2d` (`token`),
  KEY `FKjwc9veyjcjfkej6rnnbsijfvh` (`user_id`),
  CONSTRAINT `FKjwc9veyjcjfkej6rnnbsijfvh` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `region` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `logo_image_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `report` (
  `created_at` datetime(6) DEFAULT NULL,
  `handled_by` bigint DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `processed_at` datetime(6) DEFAULT NULL,
  `reported_user_id` bigint NOT NULL,
  `reporter_id` bigint NOT NULL,
  `target_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `report_reason` enum('ABUSE','ETC','HARASSMENT','ILLEGAL','OBSCENE','SPAM') COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('PENDING','REJECTED','RESOLVED','WARNED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_type` enum('COMMENT','POST','USER') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_report_reporter_target` (`reporter_id`,`target_type`,`target_id`),
  KEY `idx_report_target` (`target_type`,`target_id`),
  KEY `idx_report_status_created` (`status`,`created_at`),
  KEY `FKtra0lpxab2igssqe8pm8xmexx` (`handled_by`),
  KEY `FKgv5el6pnw9fbo9shq49ww3m4e` (`reported_user_id`),
  CONSTRAINT `FKgv5el6pnw9fbo9shq49ww3m4e` FOREIGN KEY (`reported_user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FKndpjl61ubcm2tkf7ml1ynq13t` FOREIGN KEY (`reporter_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FKtra0lpxab2igssqe8pm8xmexx` FOREIGN KEY (`handled_by`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `school` (
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `region_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `logo_image_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_school_name` (`name`),
  KEY `FKnhtgat3oodd4glhl227duqarj` (`region_id`),
  CONSTRAINT `FKnhtgat3oodd4glhl227duqarj` FOREIGN KEY (`region_id`) REFERENCES `region` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user` (
  `is_write_banned` bit(1) DEFAULT NULL,
  `write_ban_expired_at` datetime(6) DEFAULT NULL,
  `phone_verified` bit(1) NOT NULL,
  `verified` bit(1) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `deletion_requested_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `school_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `phone_number` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nickname` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `profile_image_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `gender` enum('FEMALE','MALE') COLLATE utf8mb4_unicode_ci NOT NULL,
  `grade` enum('FIRST','GRADUATED','SECOND','THIRD') COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` enum('ADMIN','USER') COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('ACTIVE','DELETED','INACTIVE','PENDING_DELETION') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK4bgmpi98dylab6qdvf9xyaxu4` (`phone_number`),
  UNIQUE KEY `UKob8kqyqqgmefl0aco34akdtpe` (`email`),
  UNIQUE KEY `UKn4swgcf30j6bmtb4l4cjryuym` (`nickname`),
  KEY `FKhbkxju61kpht7qnnhemgjv3u7` (`school_id`),
  CONSTRAINT `FKhbkxju61kpht7qnnhemgjv3u7` FOREIGN KEY (`school_id`) REFERENCES `school` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_block` (
  `blocked_id` bigint NOT NULL,
  `blocker_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_blocker_blocked` (`blocker_id`,`blocked_id`),
  KEY `FKccncjsehavren2hx4gmenhwim` (`blocked_id`),
  CONSTRAINT `FKccncjsehavren2hx4gmenhwim` FOREIGN KEY (`blocked_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FKla30ofkpxixhf1cmi2a2veban` FOREIGN KEY (`blocker_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_school_verification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `school_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `verified_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKgf8cafmkr2t27y7hk9dj031r` (`user_id`),
  KEY `FKr1gbfjuwit5beidhi4127u1yh` (`school_id`),
  CONSTRAINT `FK9136hupojqdpk3s9j72ad2vo8` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FKr1gbfjuwit5beidhi4127u1yh` FOREIGN KEY (`school_id`) REFERENCES `school` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_school_verification_request` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `processed_at` datetime(6) DEFAULT NULL,
  `processed_by` bigint DEFAULT NULL,
  `requested_at` datetime(6) NOT NULL,
  `school_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `admin_comment` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `request_image_url` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('APPROVED','PENDING','REJECTED') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK9wft94ccjngtlc9lpp10yq1qy` (`school_id`),
  KEY `FKordat2of3xcopot3bl577eke6` (`user_id`),
  CONSTRAINT `FK9wft94ccjngtlc9lpp10yq1qy` FOREIGN KEY (`school_id`) REFERENCES `school` (`id`),
  CONSTRAINT `FKordat2of3xcopot3bl577eke6` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_setting` (
  `allow_chat_notification` bit(1) NOT NULL,
  `allow_comment_notification` bit(1) NOT NULL,
  `allow_like_notification` bit(1) NOT NULL,
  `allow_push` bit(1) NOT NULL,
  `allow_reply_notification` bit(1) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_setting_user` (`user_id`),
  CONSTRAINT `FKg5ckmir2a8ejjtq4b0pcdewil` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `warning` (
  `is_read` bit(1) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `report_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `admin_comment` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKbj08ou4igatolq6fk8i9gl06k` (`report_id`),
  KEY `idx_warning_user_read` (`user_id`,`is_read`),
  CONSTRAINT `FK5888pn6spifbflqjuciqdykiy` FOREIGN KEY (`report_id`) REFERENCES `report` (`id`),
  CONSTRAINT `FK6ey4bv3wh1sd9rkv2yoqcopuw` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
