package com.sy.controller.game;

import com.sy.tool.GameConfigCache;
import com.sy.model.game.Card;
import com.sy.model.game.Characters;
import com.sy.model.game.GameItemBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置缓存测试控制器
 * 用于测试静态配置缓存是否正常工作
 */
@Slf4j
@RestController
@RequestMapping("/test/cache")
public class CacheTestController {

    /**
     * 测试获取卡牌配置
     */
    @GetMapping("/card")
    public Map<String, Object> testCard() {
        Map<String, Object> result = new HashMap<>();
        
        // 获取所有卡牌
        List<Card> allCards = GameConfigCache.getAllCards();
        result.put("totalCards", allCards.size());
        
        // 获取第一个卡牌(如果存在)
        if (!allCards.isEmpty()) {
            Card firstCard = allCards.get(0);
            Card cardFromCache = GameConfigCache.getCard(firstCard.getId());
            result.put("firstCard", cardFromCache);
            result.put("cacheWorking", cardFromCache != null);
        }
        
        return result;
    }

    /**
     * 测试获取角色配置
     */
    @GetMapping("/character")
    public Map<String, Object> testCharacter() {
        Map<String, Object> result = new HashMap<>();
        
        List<Characters> allCharacters = GameConfigCache.getAllCharacterConfigs();
        result.put("totalCharacters", allCharacters.size());
        
        if (!allCharacters.isEmpty()) {
            Characters firstChar = allCharacters.get(0);
            Characters charFromCache = GameConfigCache.getCharacterConfig(firstChar.getId());
            result.put("firstCharacter", charFromCache);
            result.put("cacheWorking", charFromCache != null);
        }
        
        return result;
    }

    /**
     * 测试获取物品配置
     */
    @GetMapping("/item")
    public Map<String, Object> testItem() {
        Map<String, Object> result = new HashMap<>();
        
        List<GameItemBase> allItems = GameConfigCache.getAllItemBases();
        result.put("totalItems", allItems.size());
        
        if (!allItems.isEmpty()) {
            GameItemBase firstItem = allItems.get(0);
            GameItemBase itemFromCache = GameConfigCache.getItemBase(firstItem.getItemId());
            result.put("firstItem", itemFromCache);
            result.put("cacheWorking", itemFromCache != null);
        }
        
        return result;
    }

    /**
     * 测试获取所有配置统计信息
     */
    @GetMapping("/stats")
    public Map<String, Object> getConfigStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("cards", GameConfigCache.getAllCards().size());
        stats.put("characters", GameConfigCache.getAllCharacterConfigs().size());
        stats.put("eqCards", GameConfigCache.getAllEqCards().size());
        stats.put("activityConfigs", GameConfigCache.getAllActivityConfigs().size());
        stats.put("itemBases", GameConfigCache.getAllItemBases().size());
        stats.put("itemShops", GameConfigCache.getAllItemShops().size());
        stats.put("itemPlayShops", GameConfigCache.getItemPlayShops() != null ? 
                  GameConfigCache.getItemPlayShops().size() : 0);
        stats.put("starSynthesisMains", GameConfigCache.getStarSynthesisMains() != null ? 
                   GameConfigCache.getStarSynthesisMains().size() : 0);
        stats.put("starSynthesisMaterials", GameConfigCache.getStarSynthesisMaterials() != null ? 
                     GameConfigCache.getStarSynthesisMaterials().size() : 0);
        stats.put("crafts", GameConfigCache.getAllCrafts().size());
        stats.put("ceremonialGifts", GameConfigCache.getAllCeremonialGifts().size());
        
        return stats;
    }
}
