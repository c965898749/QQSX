# 游戏配置数据静态缓存使用说明

## 概述

`GameConfigCache` 是一个在应用启动时自动加载所有不涉及userId的配置数据到内存中的静态缓存管理器。这样可以避免频繁查询数据库,提高系统性能。

## 已缓存的配置数据

### 1. 卡牌配置 (Card)
- **缓存Key**: cardId
- **获取单个**: `GameConfigCache.getCard(cardId)`
- **获取全部**: `GameConfigCache.getAllCards()`

### 2. 角色基础配置 (Characters)
- **缓存Key**: characterId
- **获取单个**: `GameConfigCache.getCharacterConfig(characterId)`
- **获取全部**: `GameConfigCache.getAllCharacterConfigs()`

### 3. 装备卡牌配置 (EqCard)
- **缓存Key**: eqCardId
- **获取单个**: `GameConfigCache.getEqCard(eqCardId)`
- **获取全部**: `GameConfigCache.getAllEqCards()`

### 4. 活动配置 (ActivityConfig)
- **缓存Key**: activityCode
- **获取单个**: `GameConfigCache.getActivityConfig(activityCode)`
- **获取全部**: `GameConfigCache.getAllActivityConfigs()`

### 5. 物品基础配置 (GameItemBase)
- **缓存Key**: itemId
- **获取单个**: `GameConfigCache.getItemBase(itemId)`
- **获取全部**: `GameConfigCache.getAllItemBases()`

### 6. 商店物品配置 (GameItemShop)
- **缓存Key**: shopItemId
- **获取单个**: `GameConfigCache.getItemShop(shopItemId)`
- **获取全部**: `GameConfigCache.getAllItemShops()`

### 7. 玩法商店配置 (GameItemPlayShop)
- **类型**: List
- **获取列表**: `GameConfigCache.getItemPlayShops()`

### 8. 星合成主配置 (StarSynthesisMain)
- **类型**: List
- **获取列表**: `GameConfigCache.getStarSynthesisMains()`

### 9. 星合成材料配置 (StarSynthesisMaterials)
- **类型**: List
- **获取列表**: `GameConfigCache.getStarSynthesisMaterials()`

## 使用示例

### 示例1: 获取卡牌配置

**改造前:**
```java
// 每次都需要查询数据库
Card card = cardMapper.selectByid(cardId);
```

**改造后:**
```java
// 从静态缓存中获取,无需查询数据库
Card card = GameConfigCache.getCard(cardId);
```

### 示例2: 获取所有卡牌

**改造前:**
```java
// 每次都需要查询数据库
List<Card> cardList = cardMapper.selectAll();
```

**改造后:**
```java
// 从静态缓存中获取
List<Card> cardList = GameConfigCache.getAllCards();
```

### 示例3: 获取活动配置

**改造前:**
```java
// 需要查询数据库
ActivityConfig config = activityConfigMapper.getByCode(activityCode);
```

**改造后:**
```java
// 从静态缓存中获取
ActivityConfig config = GameConfigCache.getActivityConfig(activityCode);
```

### 示例4: 获取物品配置

**改造前:**
```java
// 需要查询数据库
GameItemBase item = gameItemBaseMapper.selectById(itemId);
```

**改造后:**
```java
// 从静态缓存中获取
GameItemBase item = GameConfigCache.getItemBase(itemId);
```

## 注意事项

1. **只读数据**: 这些配置数据应该是只读的,如果需要修改配置数据,需要同时更新数据库和缓存。

2. **缓存刷新**: 如果配置数据发生变化,需要重启应用才能生效。如需动态刷新,可以添加刷新方法。

3. **线程安全**: 使用 `ConcurrentHashMap` 保证读取的线程安全。

4. **适用场景**: 
   - ✅ 适合: 不经常变化的配置数据
   - ❌ 不适合: 频繁变化的用户数据、业务数据

5. **内存占用**: 所有配置数据都加载到内存中,需要注意内存使用情况。对于超大数据量的配置表,建议使用分页或其他策略。

## 扩展新的配置缓存

如果需要添加新的配置数据到缓存中,按照以下步骤操作:

### 1. 在 GameConfigCache 中添加Mapper注入
```java
@Autowired
private YourMapper yourMapper;
```

### 2. 添加缓存Map
```java
private static final Map<String, YourModel> YOUR_CACHE = new ConcurrentHashMap<>();
```

### 3. 添加获取方法
```java
public static YourModel getYourData(String id) {
    return YOUR_CACHE.get(id);
}

public static List<YourModel> getAllYourData() {
    return YOUR_CACHE.values().stream().toList();
}
```

### 4. 在 run 方法中添加加载逻辑
```java
loadYourCache();
```

### 5. 实现加载方法
```java
private void loadYourCache() {
    List<YourModel> dataList = yourMapper.selectAll();
    if (dataList != null && !dataList.isEmpty()) {
        for (YourModel data : dataList) {
            if (data.getId() != null) {
                YOUR_CACHE.put(data.getId(), data);
            }
        }
    }
}
```

### 6. 确保Mapper有selectAll方法
如果Mapper没有selectAll方法,需要添加:
- 在Mapper接口中添加: `List<YourModel> selectAll();`
- 在XML文件中添加对应的SQL映射

## 性能优势

- **减少数据库查询**: 配置数据只需在启动时查询一次
- **提高响应速度**: 从内存读取比数据库查询快得多
- **降低数据库压力**: 减少大量重复的配置数据查询

## 日志输出

应用启动时会输出缓存加载情况:
```
========== 开始加载游戏配置数据到缓存 ==========
========== 游戏配置数据加载完成 ==========
卡牌配置数量: 100
角色配置数量: 50
装备卡牌配置数量: 30
活动配置数量: 10
物品基础配置数量: 200
商店物品配置数量: 150
玩法商店配置数量: 20
星合成主配置数量: 5
星合成材料配置数量: 25
```
