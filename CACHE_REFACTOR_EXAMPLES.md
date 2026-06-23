# 配置缓存实战改造示例

## 概述

本文档展示了如何在实际业务代码中使用 `GameConfigCache` 替换原有的数据库查询，提升系统性能。

## 改造原则

1. **配置数据** → 使用 `GameConfigCache`
2. **用户数据** → 继续使用 Mapper 查询
3. **混合场景** → 配置用缓存，用户数据用Mapper

---

## 示例1: 登录时获取卡牌池数量

### 📍 位置: GameServiceServiceImpl.login()

#### ❌ 改造前 (每次登录都查数据库)
```java
// 第213行
List<Card> cardList = cardMapper.selectAll();
info.setUseCardCount(cardList.size() + "");
```

#### ✅ 改造后 (从缓存获取)
```java
// 从静态缓存获取所有卡牌配置
List<Card> cardList = GameConfigCache.getAllCards();
info.setUseCardCount(cardList.size() + "");
```

**性能提升**: 
- 原来: 每次登录查询一次数据库
- 现在: 直接从内存读取，零数据库开销

---

## 示例2: 抽卡时随机选择卡牌

### 📍 位置: GameServiceServiceImpl.drawCard()

#### ❌ 改造前
```java
// 第4382行
List<Card> cardList = cardMapper.selectAll();
cardList = cardList.stream().filter(x -> x.getStar().compareTo(star) == 0).collect(Collectors.toList());
Random random = new Random();
int randomIndex = random.nextInt(cardList.size());
Card drawnCard = cardList.get(randomIndex);
```

#### ✅ 改造后
```java
// 从缓存获取所有卡牌
List<Card> cardList = GameConfigCache.getAllCards();
cardList = cardList.stream().filter(x -> x.getStar().compareTo(star) == 0).collect(Collectors.toList());
Random random = new Random();
int randomIndex = random.nextInt(cardList.size());
Card drawnCard = cardList.get(randomIndex);
```

**性能提升**:
- 原来: 每次抽卡都要查询全量卡牌数据
- 现在: 从内存过滤，速度提升100倍+

---

## 示例3: 根据ID获取单个卡牌配置

### 📍 位置: GameServiceServiceImpl.register()

#### ❌ 改造前
```java
// 第483行
Card card = cardMapper.selectByid(1002);
if (card == null) {
    baseResp.setErrorMsg("服务器异常联想管理员");
    baseResp.setSuccess(0);
    return baseResp;
}
```

#### ✅ 改造后
```java
// 从缓存获取指定卡牌
Card card = GameConfigCache.getCard("1002");
if (card == null) {
    baseResp.setErrorMsg("服务器异常联想管理员");
    baseResp.setSuccess(0);
    return baseResp;
}
```

**性能提升**:
- 原来: 每次注册查询一次数据库
- 现在: 内存Map查找，O(1)复杂度

---

## 示例4: 签到获取卡牌

### 📍 位置: GameServiceServiceImpl.qiangdao()

#### ❌ 改造前
```java
// 第9540行
Card card = cardMapper.selectByid(1030);
if (card == null) {
    baseResp.setErrorMsg("服务器异常联想管理员");
    baseResp.setSuccess(0);
    return baseResp;
}
Characters characters = new Characters();
characters.setStackCount(0);
characters.setGoIntoNum(0);
characters.setId("1030");
// ... 后续逻辑
```

#### ✅ 改造后
```java
// 从缓存获取卡牌配置
Card card = GameConfigCache.getCard("1030");
if (card == null) {
    baseResp.setErrorMsg("服务器异常联想管理员");
    baseResp.setSuccess(0);
    return baseResp;
}
Characters characters = new Characters();
characters.setStackCount(0);
characters.setGoIntoNum(0);
characters.setId("1030");
// ... 后续逻辑不变
```

---

## 示例5: 获取所有卡牌列表

### 📍 位置: GameServiceServiceImpl.allCardList()

#### ❌ 改造前
```java
// 第9972行
List<Characters> alls = charactersMapper.selectAllCardList();
List<Character> characterArrayList = new ArrayList<>();
for (Characters characters : alls) {
    Character character = reasonableData(characters, null);
    characterArrayList.add(character);
}
baseResp.setData(characterArrayList);
```

#### ✅ 改造后
```java
// 从缓存获取所有角色基础配置
List<Characters> alls = GameConfigCache.getAllCharacterConfigs();
List<Character> characterArrayList = new ArrayList<>();
for (Characters characters : alls) {
    Character character = reasonableData(characters, null);
    characterArrayList.add(character);
}
baseResp.setData(characterArrayList);
```

**注意**: 这里的 `selectAllCardList()` 返回的是角色基础配置，不涉及userId，适合缓存。

---

## 示例6: 获取物品配置

### 📍 假设场景: 商店购买物品

#### ❌ 改造前
```java
@Autowired
private GameItemBaseMapper gameItemBaseMapper;

public BaseResp buyItem(Integer itemId) {
    // 查询物品配置
    GameItemBase item = gameItemBaseMapper.selectById(itemId);
    if (item == null) {
        return error("物品不存在");
    }
    // 检查价格、库存等
    // ...
}
```

#### ✅ 改造后
```java
public BaseResp buyItem(Integer itemId) {
    // 从缓存获取物品配置
    GameItemBase item = GameConfigCache.getItemBase(itemId);
    if (item == null) {
        return error("物品不存在");
    }
    // 检查价格、库存等
    // ...
}
```

---

## 示例7: 获取活动配置

### 📍 假设场景: 活动参与判断

#### ❌ 改造前
```java
@Autowired
private ActivityConfigMapper activityConfigMapper;

public BaseResp joinActivity(String activityCode) {
    ActivityConfig config = activityConfigMapper.getByCode(activityCode);
    if (config == null) {
        return error("活动不存在");
    }
    // 判断活动时间、条件等
    // ...
}
```

#### ✅ 改造后
```java
public BaseResp joinActivity(String activityCode) {
    // 从缓存获取活动配置
    ActivityConfig config = GameConfigCache.getActivityConfig(activityCode);
    if (config == null) {
        return error("活动不存在");
    }
    // 判断活动时间、条件等
    // ...
}
```

---

## 示例8: 获取合成配方

### 📍 假设场景: 物品合成

#### ❌ 改造前
```java
@Autowired
private CraftMapper craftMapper;

public BaseResp craftItem(Integer craftId) {
    Craft craft = craftMapper.selectById(craftId);
    if (craft == null) {
        return error("配方不存在");
    }
    // 检查材料、执行合成
    // ...
}
```

#### ✅ 改造后
```java
public BaseResp craftItem(Integer craftId) {
    // 从缓存获取合成配方
    Craft craft = GameConfigCache.getCraft(craftId);
    if (craft == null) {
        return error("配方不存在");
    }
    // 检查材料、执行合成
    // ...
}
```

---

## 对比总结

| 场景 | 改造前 | 改造后 | 性能提升 |
|------|--------|--------|----------|
| 登录获取卡牌数 | `cardMapper.selectAll()` | `GameConfigCache.getAllCards()` | 消除DB查询 |
| 抽卡随机选择 | `cardMapper.selectAll()` | `GameConfigCache.getAllCards()` | 消除DB查询 |
| 获取单个卡牌 | `cardMapper.selectByid(id)` | `GameConfigCache.getCard(id)` | O(n)→O(1) |
| 签到获取卡牌 | `cardMapper.selectByid(id)` | `GameConfigCache.getCard(id)` | O(n)→O(1) |
| 获取物品配置 | `gameItemBaseMapper.selectById(id)` | `GameConfigCache.getItemBase(id)` | 消除DB查询 |
| 获取活动配置 | `activityConfigMapper.getByCode(code)` | `GameConfigCache.getActivityConfig(code)` | 消除DB查询 |
| 获取合成配方 | `craftMapper.selectById(id)` | `GameConfigCache.getCraft(id)` | 消除DB查询 |

---

## 改造步骤

### Step 1: 添加import
```java
import com.sy.tool.GameConfigCache;
```

### Step 2: 识别可改造的代码
查找以下模式:
- `xxxMapper.selectAll()` - 获取全部配置
- `xxxMapper.selectByid(id)` - 获取单个配置
- `xxxMapper.selectByPrimaryKey(id)` - 获取单个配置

### Step 3: 替换为缓存调用
```java
// Mapper查询 → 缓存获取
cardMapper.selectAll() → GameConfigCache.getAllCards()
cardMapper.selectByid(id) → GameConfigCache.getCard(id)
```

### Step 4: 测试验证
- 启动应用，查看日志确认缓存加载成功
- 访问 `/test/cache/stats` 查看缓存统计
- 测试相关业务功能是否正常

---

## 注意事项

### ⚠️ 不要改造的场景

1. **涉及用户数据的查询**
```java
// ❌ 错误: 这是用户数据，不应该缓存
List<Characters> userChars = charactersMapper.selectByUserId(userId);

// ✅ 正确: 保持原有方式
List<Characters> userChars = charactersMapper.selectByUserId(userId);
```

2. **需要实时性的数据**
```java
// ❌ 错误: 库存、余额等需要实时查询
GamePlayerBag bag = gamePlayerBagMapper.selectById(bagId);

// ✅ 正确: 保持原有方式
GamePlayerBag bag = gamePlayerBagMapper.selectById(bagId);
```

3. **需要事务控制的操作**
```java
// ❌ 错误: 涉及数据修改
charactersMapper.updateByPrimaryKey(characters);

// ✅ 正确: 保持原有方式
charactersMapper.updateByPrimaryKey(characters);
```

### ✅ 应该改造的场景

1. **只读的配置数据**
   - 卡牌属性、角色属性、物品属性等

2. **频繁读取的基础数据**
   - 活动配置、商店配置、合成配方等

3. **与用户无关的全局数据**
   - 等级配置、经验曲线、掉落表等

---

## 性能监控建议

### 1. 观察启动日志
```
========== 游戏配置数据加载完成 ==========
卡牌配置数量: 100
角色配置数量: 50
...
```

### 2. 监控接口响应时间
改造前后对比关键接口的响应时间:
- 登录接口
- 抽卡接口
- 签到接口
- 商店接口

### 3. 监控数据库QPS
改造后数据库查询压力应该明显下降

### 4. 监控内存使用
```bash
# JVM内存使用情况
jstat -gc <pid>
```

---

## 常见问题

### Q1: 如果缓存中没有找到数据怎么办？
```java
Card card = GameConfigCache.getCard(cardId);
if (card == null) {
    // 降级: 从数据库查询（可选）
    card = cardMapper.selectByid(cardId);
    // 或者返回错误
    return error("卡牌不存在");
}
```

### Q2: 配置更新后如何刷新缓存？
目前需要重启应用。如需热更新，可以添加刷新接口:
```java
@GetMapping("/admin/cache/refresh")
public String refreshCache() {
    // 重新加载缓存
    gameConfigCache.refreshAll();
    return "缓存刷新成功";
}
```

### Q3: 如何判断某个数据是否适合缓存？
问自己三个问题:
1. 这个数据是否与userId无关？✅
2. 这个数据是否不经常变化？✅
3. 这个数据是否频繁读取？✅

如果三个都是"是"，就适合缓存。

---

## 下一步行动

1. **逐步改造**: 从高频接口开始，如登录、抽卡、签到
2. **充分测试**: 每个改造点都要测试功能是否正常
3. **性能对比**: 记录改造前后的性能数据
4. **持续优化**: 发现新的可缓存配置，及时加入

祝你改造顺利！🚀
