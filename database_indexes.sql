-- =============================================
-- 数据库索引优化SQL脚本
-- 生成时间: 2026-06-24
-- 说明: 为高频查询字段添加索引以提升查询性能
-- =============================================

-- =============================================
-- 1. characters (角色卡牌表)
-- =============================================
ALTER TABLE `characters` ADD INDEX idx_characters_user_delete (`user_id`, `is_delete`);
ALTER TABLE `characters` ADD INDEX idx_characters_user_id_delete (`user_id`, `id`, `is_delete`);
ALTER TABLE `characters` ADD INDEX idx_characters_go_into (`user_id`, `go_into_num`, `is_delete`);

-- =============================================
-- 2. user (用户表)
-- =============================================
ALTER TABLE `user` ADD INDEX idx_user_openid (`openid`);
ALTER TABLE `user` ADD INDEX idx_user_username (`username`);
ALTER TABLE `user` ADD INDEX idx_user_my_code (`my_code`);
ALTER TABLE `user` ADD INDEX idx_user_yao_code (`yao_code`);
ALTER TABLE `user` ADD INDEX idx_user_game_ranking (`game_ranking`);
ALTER TABLE `user` ADD INDEX idx_user_token (`token`);

-- =============================================
-- 3. friend_relation (好友关系表)
-- =============================================
ALTER TABLE `friend_relation` ADD INDEX idx_friend_relation_user_status (`user_id`, `status`);
ALTER TABLE `friend_relation` ADD INDEX idx_friend_relation_friend_status (`friend_id`, `status`);

-- =============================================
-- 4. friend_blessing (好友祝福表)
-- =============================================
ALTER TABLE `friend_blessing` ADD INDEX idx_blessing_sender_receiver_time (`sender_id`, `receiver_id`, `send_time`);
ALTER TABLE `friend_blessing` ADD INDEX idx_blessing_receiver_sender_time (`receiver_id`, `sender_id`, `send_time`);

-- =============================================
-- 5. game_player_bag (玩家背包表)
-- =============================================
ALTER TABLE `game_player_bag` ADD INDEX idx_bag_user_delete (`user_id`, `is_delete`);
ALTER TABLE `game_player_bag` ADD INDEX idx_bag_user_item_delete (`user_id`, `item_id`, `is_delete`);

-- =============================================
-- 6. game_shop_record (商店购买记录表)
-- =============================================
ALTER TABLE `game_shop_record` ADD INDEX idx_shop_record_daily (`user_id`, `item_id`, `get_time`);

-- =============================================
-- 7. game_gift_record (礼包领取记录表)
-- =============================================
ALTER TABLE `game_gift_record` ADD INDEX idx_gift_record_check (`user_id`, `gift_id`, `status`);

-- =============================================
-- 8. friend_invitation (好友邀请表)
-- =============================================
ALTER TABLE `friend_invitation` ADD INDEX idx_invitation_inviter_time (`inviter_id`, `create_time`);
ALTER TABLE `friend_invitation` ADD INDEX idx_invitation_invitee_status (`invitee_id`, `status`);

-- =============================================
-- 9. game_arena_signup (竞技场报名表)
-- =============================================
ALTER TABLE `game_arena_signup` ADD INDEX idx_arena_signup_match (`week_num`, `arena_level`, `arena_score`);
ALTER TABLE `game_arena_signup` ADD INDEX idx_arena_signup_user (`user_id`, `week_num`, `arena_level`);

-- =============================================
-- 10. game_arena_rank (竞技场排名表)
-- =============================================
ALTER TABLE `game_arena_rank` ADD INDEX idx_arena_rank_week_level_score (`week_num`, `arena_level`, `arena_score` DESC);

-- =============================================
-- 11. user_activity_records (活动参与记录表)
-- =============================================
ALTER TABLE `user_activity_records` ADD INDEX idx_activity_daily (`user_id`, `activity_code`, `participation_date`, `status`);
ALTER TABLE `user_activity_records` ADD INDEX idx_activity_daily_detail (`user_id`, `detail_code`, `participation_date`, `status`);

-- =============================================
-- 12. player_bronze_tower (铜人塔记录表)
-- =============================================
ALTER TABLE `player_bronze_tower` ADD INDEX idx_bronze_player_type (`player_id`, `bronze_type`);
ALTER TABLE `player_bronze_tower` ADD INDEX idx_bronze_ranking (`bronze_type`, `floor_num` DESC, `pass_time` ASC);

-- =============================================
-- 13. game_time_record (游戏时间记录表)
-- =============================================
ALTER TABLE `game_time_record` ADD INDEX idx_time_record_user (`user_id`);

-- =============================================
-- 14. ceremonial_gift_record (礼仪礼物记录表)
-- =============================================
ALTER TABLE `ceremonial_gift_record` ADD INDEX idx_ceremonial_user_item_time (`user_id`, `item_id`, `get_time`);

-- =============================================
-- 15. lively_gift_record (活跃礼包记录表)
-- =============================================
ALTER TABLE `lively_gift_record` ADD INDEX idx_lively_user_gift_time (`user_id`, `gift_id`, `get_time`);

-- =============================================
-- 16. item_purchase_record (物品购买记录表)
-- =============================================
ALTER TABLE `item_purchase_record` ADD INDEX idx_purchase_player_item_time (`player_id`, `item_id`, `purchase_time`);

-- =============================================
-- 17. mine_rob_log (挖矿掠夺日志表)
-- =============================================
ALTER TABLE `mine_rob_log` ADD INDEX idx_mine_rob_target_time (`target_user_id`, `create_time`);
ALTER TABLE `mine_rob_log` ADD INDEX idx_mine_rob_attacker_time (`attacker_user_id`, `create_time`);

-- =============================================
-- 18. pill_rob_record (丹药掠夺记录表)
-- =============================================
ALTER TABLE `pill_rob_record` ADD INDEX idx_pill_rob_victim_time (`victim_id`, `rob_time`);
ALTER TABLE `pill_rob_record` ADD INDEX idx_pill_rob_robber_time (`robber_id`, `rob_time`);

-- =============================================
-- 19. revenge_record (复仇记录表)
-- =============================================
ALTER TABLE `revenge_record` ADD INDEX idx_revenge_player_time (`revenge_player_id`, `create_time`);
ALTER TABLE `revenge_record` ADD INDEX idx_revenge_target_time (`target_player_id`, `create_time`);

-- =============================================
-- 20. star_synthesis_main (星合成配置表)
-- =============================================
-- 注: 此表为配置表,无用户ID字段,无需添加用户相关索引

-- =============================================
-- 21. qq_card_exp (QQ卡经验配置表)
-- =============================================
-- 注: 此表为配置表,无用户ID字段,无需添加用户相关索引

-- =============================================
-- 22. qq_shenxian_player_flyup (修仙飞升表)
-- =============================================
ALTER TABLE `qq_shenxian_player_flyup` ADD INDEX idx_qq_flyup_player (`player_id`);

-- =============================================
-- 23. daily_view_record (每日视图记录表)
-- =============================================
ALTER TABLE `daily_view_record` ADD INDEX idx_daily_view_user_gift_time (`user_id`, `gift_code`, `get_time`, `status`);

-- =============================================
-- 24. player_task_progress (任务进度表)
-- =============================================
ALTER TABLE `player_task_progress` ADD INDEX idx_task_progress_player_task (`player_id`, `task_id`);

-- =============================================
-- 25. game_eq_record (装备记录表)
-- =============================================
-- 注: 此表每用户一条记录,picked字段存JSON格式装备列表
ALTER TABLE `game_eq_record` ADD INDEX idx_eq_record_user (`user_id`);

-- =============================================
-- 26. game_fight (战斗记录表)
-- =============================================
ALTER TABLE `game_fight` ADD INDEX idx_fight_user_time (`user_id`, `createtime`);

-- =============================================
-- 27. game_notice (游戏公告表)
-- =============================================
-- 注: 此表为临时公告表,定时任务会清空,仅保留创建时间索引用于排序
ALTER TABLE `game_notice` ADD INDEX idx_notice_create_time (`create_time`);

-- =============================================
-- 28. game_mesage (游戏消息表)
-- =============================================

-- =============================================
-- 29. eq_characters_record (装备角色记录表)
-- =============================================
-- 注: 此表为装备获得记录表,id字段存装备ID,无eq_id和character_uuid字段
ALTER TABLE `eq_characters_record` ADD INDEX idx_eq_char_user_time (`user_id`, `get_time`);

-- =============================================
-- 30. craft (锻造配方配置表)
-- =============================================
-- 注: 此表为锻造配方配置表,无用户ID字段,无需添加用户相关索引

-- =============================================
-- 31. card (卡牌配置表)
-- =============================================
ALTER TABLE `card` ADD INDEX idx_card_id (`id`);

-- =============================================
-- 32. game_item_base (物品基础表)
-- =============================================
ALTER TABLE `game_item_base` ADD INDEX idx_item_type (`item_type`);

-- =============================================
-- 33. pve_detail (PVE关卡配置表)
-- =============================================
-- 注: 此表为PVE关卡配置表,已通过GameConfigCache缓存,无需添加索引

-- =============================================
-- 34. game_arena_battle (竞技场战斗记录表)
-- =============================================
ALTER TABLE `game_arena_battle` ADD INDEX idx_arena_battle_user (`user_id`);

-- =============================================
-- 35. game_arena_battlecharacters (竞技场战斗角色表)
-- =============================================
ALTER TABLE `game_arena_battlecharacters` ADD INDEX idx_arena_battle_char_battle (`battle_id`);

-- =============================================
-- 验证索引创建情况
-- =============================================
-- 查看所有索引
-- SHOW INDEX FROM characters;
-- SHOW INDEX FROM user;
-- SHOW INDEX FROM friend_relation;
-- SHOW INDEX FROM game_player_bag;

-- =============================================
-- 索引使用分析
-- =============================================
-- 查看慢查询
-- SHOW VARIABLES LIKE 'slow_query_log%';
-- SET GLOBAL slow_query_log = 'ON';
-- SET GLOBAL long_query_time = 2;

-- 分析表以更新统计信息
-- ANALYZE TABLE characters;
-- ANALYZE TABLE user;
-- ANALYZE TABLE friend_relation;
-- ANALYZE TABLE game_player_bag;

-- =============================================
-- 注意事项
-- =============================================
-- 1. 在生产环境执行前,请先在测试环境验证
-- 2. 大表创建索引可能需要较长时间,建议在低峰期执行
-- 3. 可以使用 CREATE INDEX ... ALGORITHM=INPLACE, LOCK=NONE 在线创建索引(MySQL 5.6+)
-- 4. 定期监控索引使用情况,删除未使用的索引
-- 5. 索引会增加INSERT/UPDATE/DELETE的开销,需要权衡利弊
-- 6. 建议配合EXPLAIN分析查询计划,确保索引被正确使用
