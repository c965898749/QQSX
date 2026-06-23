package com.sy.tool;

import com.sy.mapper.game.*;
import com.sy.model.game.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 游戏配置数据静态缓存管理器
 * 在应用启动时加载所有不涉及userId的配置数据到内存中
 */
@Slf4j
@Component
public class GameConfigCache implements CommandLineRunner {

    @Autowired
    private CardMapper cardMapper;
    
    @Autowired
    private CharactersMapper charactersMapper;
    
    @Autowired
    private EqCardMapper eqCardMapper;
    
    @Autowired
    private ActivityConfigMapper activityConfigMapper;
    
    @Autowired
    private GameItemBaseMapper gameItemBaseMapper;
    
    @Autowired
    private GameItemShopMapper gameItemShopMapper;
    
    @Autowired
    private GameItemPlayShopMapper gameItemPlayShopMapper;
    
    @Autowired
    private StarSynthesisMainMapper starSynthesisMainMapper;
    
    @Autowired
    private StarSynthesisMaterialsMapper starSynthesisMaterialsMapper;
    
    @Autowired
    private CraftMapper craftMapper;
    
    @Autowired
    private CeremonialGiftMapper ceremonialGiftMapper;
    
    @Autowired
    private QqCardExpMapper qqCardExpMapper;
    
    @Autowired
    private QqShenxianFlyupMapper qqShenxianFlyupMapper;
    
    @Autowired
    private ActivityDetailMapper activityDetailMapper;
    
    @Autowired
    private ActivityBossMapper activityBossMapper;
    
    @Autowired
    private ActivityRewardMapper activityRewardMapper;
    
    @Autowired
    private PveDetailMapper pveDetailMapper;
    
    @Autowired
    private PveBossDetailMapper pveBossDetailMapper;
    
    @Autowired
    private PveRewardMapper pveRewardMapper;

    // 卡牌配置缓存 key: cardId
    private static final Map<String, Card> CARD_CACHE = new ConcurrentHashMap<>();
    
    // 角色基础配置缓存 key: characterId
    private static final Map<String, Characters> CHARACTER_CONFIG_CACHE = new ConcurrentHashMap<>();
    
    // 装备卡牌配置缓存 key: eqCardId
    private static final Map<String, EqCard> EQ_CARD_CACHE = new ConcurrentHashMap<>();
    
    // 活动配置缓存 key: activityCode
    private static final Map<String, ActivityConfig> ACTIVITY_CONFIG_CACHE = new ConcurrentHashMap<>();
    
    // 物品基础配置缓存 key: itemId
    private static final Map<Integer, GameItemBase> ITEM_BASE_CACHE = new ConcurrentHashMap<>();
    
    // 商店物品配置缓存 key: shopItemId
    private static final Map<Integer, GameItemShop> ITEM_SHOP_CACHE = new ConcurrentHashMap<>();
    
    // 玩法商店配置缓存
    private static List<GameItemPlayShop> ITEM_PLAY_SHOP_CACHE = null;
    
    // 星合成主配置缓存
    private static List<StarSynthesisMain> STAR_SYNTHESIS_MAIN_CACHE = null;
    
    // 星合成材料配置缓存
    private static List<StarSynthesisMaterials> STAR_SYNTHESIS_MATERIALS_CACHE = null;
    
    // 合成配方配置缓存 key: craftId
    private static final Map<Integer, Craft> CRAFT_CACHE = new ConcurrentHashMap<>();
    
    // 礼仪礼品配置缓存 key: giftId
    private static final Map<Integer, CeremonialGift> CEREMONIAL_GIFT_CACHE = new ConcurrentHashMap<>();
    
    // 卡牌经验配置缓存（列表形式，因为可能有多条相同等级的配置）
    private static List<QqCardExp> QQ_CARD_EXP_CACHE = null;
    
    // 神仙飞升配置缓存（列表形式，按飞升次数过滤）
    private static List<QqShenxianFlyup> SHENXIAN_FLYUP_CACHE = null;
    
    // 活动详情配置缓存 key: detailCode
    private static final Map<String, ActivityDetail> ACTIVITY_DETAIL_CACHE = new ConcurrentHashMap<>();
    
    // 活动Boss配置缓存 key: detailCode -> List<ActivityBoss>
    private static final Map<String, List<ActivityBoss>> ACTIVITY_BOSS_CACHE = new ConcurrentHashMap<>();
    
    // 活动奖励配置缓存 key: detailCode -> List<ActivityReward>
    private static final Map<String, List<ActivityReward>> ACTIVITY_REWARD_CACHE = new ConcurrentHashMap<>();
    
    // PVE副本详情配置缓存 key: detailCode
    private static final Map<String, PveDetail> PVE_DETAIL_CACHE = new ConcurrentHashMap<>();
    
    // PVE副本Boss配置缓存 key: detailCode -> List<PveBossDetail>
    private static final Map<String, List<PveBossDetail>> PVE_BOSS_DETAIL_CACHE = new ConcurrentHashMap<>();
    
    // PVE副本奖励配置缓存 key: detailCode -> List<PveReward>
    private static final Map<String, List<PveReward>> PVE_REWARD_CACHE = new ConcurrentHashMap<>();

    /**
     * 获取卡牌配置
     */
    public static Card getCard(String cardId) {
        return CARD_CACHE.get(cardId);
    }
    
    /**
     * 获取所有卡牌配置
     */
    public static List<Card> getAllCards() {
        return CARD_CACHE.values().stream().toList();
    }
    
    /**
     * 获取角色基础配置
     */
    public static Characters getCharacterConfig(String characterId) {
        return CHARACTER_CONFIG_CACHE.get(characterId);
    }
    
    /**
     * 获取所有角色基础配置
     */
    public static List<Characters> getAllCharacterConfigs() {
        return CHARACTER_CONFIG_CACHE.values().stream().toList();
    }
    
    /**
     * 获取装备卡牌配置
     */
    public static EqCard getEqCard(String eqCardId) {
        return EQ_CARD_CACHE.get(eqCardId);
    }
    
    /**
     * 获取所有装备卡牌配置
     */
    public static List<EqCard> getAllEqCards() {
        return EQ_CARD_CACHE.values().stream().toList();
    }
    
    /**
     * 获取活动配置
     */
    public static ActivityConfig getActivityConfig(String activityCode) {
        return ACTIVITY_CONFIG_CACHE.get(activityCode);
    }
    
    /**
     * 获取所有活动配置
     */
    public static List<ActivityConfig> getAllActivityConfigs() {
        return ACTIVITY_CONFIG_CACHE.values().stream().toList();
    }
    
    /**
     * 获取物品基础配置
     */
    public static GameItemBase getItemBase(Integer itemId) {
        return ITEM_BASE_CACHE.get(itemId);
    }
    
    /**
     * 获取所有物品基础配置
     */
    public static List<GameItemBase> getAllItemBases() {
        return ITEM_BASE_CACHE.values().stream().toList();
    }
    
    /**
     * 获取商店物品配置
     */
    public static GameItemShop getItemShop(Integer shopItemId) {
        return ITEM_SHOP_CACHE.get(shopItemId);
    }
    
    /**
     * 获取所有商店物品配置
     */
    public static List<GameItemShop> getAllItemShops() {
        return ITEM_SHOP_CACHE.values().stream().toList();
    }
    
    /**
     * 获取玩法商店配置列表
     */
    public static List<GameItemPlayShop> getItemPlayShops() {
        return ITEM_PLAY_SHOP_CACHE;
    }
    
    /**
     * 获取星合成主配置列表
     */
    public static List<StarSynthesisMain> getStarSynthesisMains() {
        return STAR_SYNTHESIS_MAIN_CACHE;
    }
    
    /**
     * 获取星合成材料配置列表
     */
    public static List<StarSynthesisMaterials> getStarSynthesisMaterials() {
        return STAR_SYNTHESIS_MATERIALS_CACHE;
    }
    
    /**
     * 获取合成配方配置
     */
    public static Craft getCraft(Integer craftId) {
        return CRAFT_CACHE.get(craftId);
    }
    
    /**
     * 获取所有合成配方配置
     */
    public static List<Craft> getAllCrafts() {
        return CRAFT_CACHE.values().stream().toList();
    }
    
    /**
     * 获取礼仪礼品配置
     */
    public static CeremonialGift getCeremonialGift(Integer giftId) {
        return CEREMONIAL_GIFT_CACHE.get(giftId);
    }
    
    /**
     * 获取所有礼仪礼品配置
     */
    public static List<CeremonialGift> getAllCeremonialGifts() {
        return CEREMONIAL_GIFT_CACHE.values().stream().toList();
    }
    
    /**
     * 获取卡牌经验配置列表
     */
    public static List<QqCardExp> getQqCardExpList() {
        return QQ_CARD_EXP_CACHE;
    }
    
    /**
     * 获取神仙飞升配置列表
     */
    public static List<QqShenxianFlyup> getShenxianFlyupList() {
        return SHENXIAN_FLYUP_CACHE;
    }
    
    /**
     * 获取活动详情配置
     */
    public static ActivityDetail getActivityDetail(String detailCode) {
        return ACTIVITY_DETAIL_CACHE.get(detailCode);
    }
    
    /**
     * 获取所有活动详情配置
     */
    public static List<ActivityDetail> getAllActivityDetails() {
        return ACTIVITY_DETAIL_CACHE.values().stream().toList();
    }
    
    /**
     * 根据activityCode获取活动详情列表
     */
    public static List<ActivityDetail> getActivityDetailsByCode(String activityCode) {
        return ACTIVITY_DETAIL_CACHE.values().stream()
                .filter(d -> d.getActivityCode().equals(activityCode))
                .collect(Collectors.toList());
    }
    
    /**
     * 根据activityCode和day获取活动详情列表
     */
    public static List<ActivityDetail> getActivityDetailsByDay(String activityCode, Integer day) {
        return ACTIVITY_DETAIL_CACHE.values().stream()
                .filter(d -> d.getActivityCode().equals(activityCode) && d.getDay().equals(day.byteValue()))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取活动Boss配置列表
     */
    public static List<ActivityBoss> getActivityBosses(String detailCode) {
        return ACTIVITY_BOSS_CACHE.get(detailCode);
    }
    
    /**
     * 获取活动奖励配置列表
     */
    public static List<ActivityReward> getActivityRewards(String detailCode) {
        return ACTIVITY_REWARD_CACHE.get(detailCode);
    }
    
    /**
     * 获取PVE副本详情配置
     */
    public static PveDetail getPveDetail(String detailCode) {
        return PVE_DETAIL_CACHE.get(detailCode);
    }
    
    /**
     * 获取所有PVE副本详情配置
     */
    public static List<PveDetail> getAllPveDetails() {
        return PVE_DETAIL_CACHE.values().stream().toList();
    }
    
    /**
     * 获取PVE副本Boss配置列表
     */
    public static List<PveBossDetail> getPveBossDetails(String detailCode) {
        return PVE_BOSS_DETAIL_CACHE.get(detailCode);
    }
    
    /**
     * 获取PVE副本奖励配置列表
     */
    public static List<PveReward> getPveRewards(String detailCode) {
        return PVE_REWARD_CACHE.get(detailCode);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("========== 开始加载游戏配置数据到缓存 ==========");
        try {
            loadCardCache();
            loadCharacterConfigCache();
            loadEqCardCache();
            loadActivityConfigCache();
            loadItemBaseCache();
            loadItemShopCache();
            loadItemPlayShopCache();
            loadStarSynthesisCache();
            loadCraftCache();
            loadCeremonialGiftCache();
            loadQqCardExpCache();
            loadShenxianFlyupCache();
            loadActivityDetailCache();
            loadActivityBossCache();
            loadActivityRewardCache();
            loadPveDetailCache();
            loadPveBossDetailCache();
            loadPveRewardCache();
            
            log.info("========== 游戏配置数据加载完成 ==========");
            log.info("卡牌配置数量: {}", CARD_CACHE.size());
            log.info("角色配置数量: {}", CHARACTER_CONFIG_CACHE.size());
            log.info("装备卡牌配置数量: {}", EQ_CARD_CACHE.size());
            log.info("活动配置数量: {}", ACTIVITY_CONFIG_CACHE.size());
            log.info("物品基础配置数量: {}", ITEM_BASE_CACHE.size());
            log.info("商店物品配置数量: {}", ITEM_SHOP_CACHE.size());
            log.info("玩法商店配置数量: {}", ITEM_PLAY_SHOP_CACHE != null ? ITEM_PLAY_SHOP_CACHE.size() : 0);
            log.info("星合成主配置数量: {}", STAR_SYNTHESIS_MAIN_CACHE != null ? STAR_SYNTHESIS_MAIN_CACHE.size() : 0);
            log.info("星合成材料配置数量: {}", STAR_SYNTHESIS_MATERIALS_CACHE != null ? STAR_SYNTHESIS_MATERIALS_CACHE.size() : 0);
            log.info("合成配方配置数量: {}", CRAFT_CACHE.size());
            log.info("礼仪礼品配置数量: {}", CEREMONIAL_GIFT_CACHE.size());
            log.info("卡牌经验配置数量: {}", QQ_CARD_EXP_CACHE != null ? QQ_CARD_EXP_CACHE.size() : 0);
            log.info("神仙飞升配置数量: {}", SHENXIAN_FLYUP_CACHE != null ? SHENXIAN_FLYUP_CACHE.size() : 0);
            log.info("活动详情配置数量: {}", ACTIVITY_DETAIL_CACHE.size());
            log.info("活动Boss配置数量: {}", ACTIVITY_BOSS_CACHE.size());
            log.info("活动奖励配置数量: {}", ACTIVITY_REWARD_CACHE.size());
            log.info("PVE副本详情配置数量: {}", PVE_DETAIL_CACHE.size());
            log.info("PVE副本Boss配置数量: {}", PVE_BOSS_DETAIL_CACHE.size());
            log.info("PVE副本奖励配置数量: {}", PVE_REWARD_CACHE.size());
        } catch (Exception e) {
            log.error("加载游戏配置数据失败", e);
            throw e;
        }
    }

    private void loadCardCache() {
        List<Card> cards = cardMapper.selectAll();
        if (cards != null && !cards.isEmpty()) {
            for (Card card : cards) {
                if (card.getId() != null) {
                    CARD_CACHE.put(card.getId(), card);
                }
            }
        }
    }

    private void loadCharacterConfigCache() {
        List<Characters> characters = charactersMapper.selectAllCardList();
        if (characters != null && !characters.isEmpty()) {
            for (Characters character : characters) {
                if (character.getId() != null) {
                    CHARACTER_CONFIG_CACHE.put(character.getId(), character);
                }
            }
        }
    }

    private void loadEqCardCache() {
        List<EqCard> eqCards = eqCardMapper.selectAll();
        if (eqCards != null && !eqCards.isEmpty()) {
            for (EqCard eqCard : eqCards) {
                if (eqCard.getId() != null) {
                    EQ_CARD_CACHE.put(eqCard.getId(), eqCard);
                }
            }
        }
    }

    private void loadActivityConfigCache() {
        List<ActivityConfig> activityConfigs = activityConfigMapper.selectAll();
        if (activityConfigs != null && !activityConfigs.isEmpty()) {
            for (ActivityConfig config : activityConfigs) {
                if (config.getActivityCode() != null) {
                    ACTIVITY_CONFIG_CACHE.put(config.getActivityCode(), config);
                }
            }
        }
    }

    private void loadItemBaseCache() {
        List<GameItemBase> itemBases = gameItemBaseMapper.selectAll();
        if (itemBases != null && !itemBases.isEmpty()) {
            for (GameItemBase item : itemBases) {
                if (item.getItemId() != null) {
                    ITEM_BASE_CACHE.put(item.getItemId(), item);
                }
            }
        }
    }

    private void loadItemShopCache() {
        List<GameItemShop> itemShops = gameItemShopMapper.selectAll();
        if (itemShops != null && !itemShops.isEmpty()) {
            for (GameItemShop item : itemShops) {
                if (item.getId() != null) {
                    ITEM_SHOP_CACHE.put(item.getId(), item);
                }
            }
        }
    }

    private void loadItemPlayShopCache() {
        ITEM_PLAY_SHOP_CACHE = gameItemPlayShopMapper.selectAll();
    }

    private void loadStarSynthesisCache() {
        STAR_SYNTHESIS_MAIN_CACHE = starSynthesisMainMapper.selectAll();
        STAR_SYNTHESIS_MATERIALS_CACHE = starSynthesisMaterialsMapper.selectAll();
    }
    
    private void loadCraftCache() {
        List<Craft> crafts = craftMapper.selectList(null);
        if (crafts != null && !crafts.isEmpty()) {
            for (Craft craft : crafts) {
                if (craft.getId() != null) {
                    CRAFT_CACHE.put(craft.getId(), craft);
                }
            }
        }
    }
    
    private void loadCeremonialGiftCache() {
        List<CeremonialGift> gifts = ceremonialGiftMapper.selectList(null);
        if (gifts != null && !gifts.isEmpty()) {
            for (CeremonialGift gift : gifts) {
                if (gift.getItemId() != null) {
                    CEREMONIAL_GIFT_CACHE.put(gift.getItemId(), gift);
                }
            }
        }
    }
    
    private void loadQqCardExpCache() {
        QQ_CARD_EXP_CACHE = qqCardExpMapper.findAll();
    }
    
    private void loadShenxianFlyupCache() {
        SHENXIAN_FLYUP_CACHE = qqShenxianFlyupMapper.selectList(null);
    }
    
    private void loadActivityDetailCache() {
        List<ActivityDetail> details = activityDetailMapper.selectAll();
        if (details != null && !details.isEmpty()) {
            for (ActivityDetail detail : details) {
                if (detail.getDetailCode() != null) {
                    ACTIVITY_DETAIL_CACHE.put(detail.getDetailCode(), detail);
                }
            }
        }
    }
    
    private void loadActivityBossCache() {
        List<ActivityBoss> bosses = activityBossMapper.selectList(null);
        if (bosses != null && !bosses.isEmpty()) {
            // 按detailCode分组存储
            Map<String, List<ActivityBoss>> grouped = bosses.stream()
                    .collect(Collectors.groupingBy(ActivityBoss::getDetailCode));
            ACTIVITY_BOSS_CACHE.putAll(grouped);
        }
    }
    
    private void loadActivityRewardCache() {
        List<ActivityReward> rewards = activityRewardMapper.selectAll();
        if (rewards != null && !rewards.isEmpty()) {
            // 按detailCode分组存储
            Map<String, List<ActivityReward>> grouped = rewards.stream()
                    .collect(Collectors.groupingBy(ActivityReward::getDetailCode));
            ACTIVITY_REWARD_CACHE.putAll(grouped);
        }
    }
    
    private void loadPveDetailCache() {
        List<PveDetail> details = pveDetailMapper.selectAll();
        if (details != null && !details.isEmpty()) {
            for (PveDetail detail : details) {
                if (detail.getId() != null) {
                    PVE_DETAIL_CACHE.put(detail.getId(), detail);
                }
            }
        }
    }
    
    private void loadPveBossDetailCache() {
        List<PveBossDetail> bosses = pveBossDetailMapper.selectList(null);
        if (bosses != null && !bosses.isEmpty()) {
            // 按detailCode分组存储
            Map<String, List<PveBossDetail>> grouped = bosses.stream()
                    .collect(Collectors.groupingBy(PveBossDetail::getDetailCode));
            PVE_BOSS_DETAIL_CACHE.putAll(grouped);
        }
    }
    
    private void loadPveRewardCache() {
        List<PveReward> rewards = pveRewardMapper.selectList(null);
        if (rewards != null && !rewards.isEmpty()) {
            // 按detailCode分组存储
            Map<String, List<PveReward>> grouped = rewards.stream()
                    .collect(Collectors.groupingBy(PveReward::getDetailCode));
            PVE_REWARD_CACHE.putAll(grouped);
        }
    }
}
