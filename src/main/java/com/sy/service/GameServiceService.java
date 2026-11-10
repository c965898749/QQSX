package com.sy.service;

import com.sy.model.User;
import com.sy.model.game.ChatMsg;
import com.sy.model.game.LevelUpResult;
import com.sy.model.game.TokenDto;
import com.sy.model.resp.BaseResp;

import javax.servlet.http.HttpServletRequest;

public interface GameServiceService {
    BaseResp loginGame(User user, HttpServletRequest request) throws Exception;
    BaseResp isTrue(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp gameVersion(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp registerGame(User user, HttpServletRequest request) throws Exception;
    BaseResp forgotPassword(User user, HttpServletRequest request) throws Exception;
    BaseResp updateGame(TokenDto token, HttpServletRequest request) throws Exception;
//    BaseResp updateTli(TokenDto token, HttpServletRequest request) throws Exception;
//    BaseResp updateTli3(TokenDto token, HttpServletRequest request) throws Exception;
//    BaseResp updateTli2(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp changeState(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp changeEqState(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp changeEqState2(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp changeName(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp getActivityList(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp getUserActivityDetail(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp pveDetail(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp participate(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp participate2(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp cardLevelUp(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp xina(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp cardFlyUp(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp cardFlyUp2(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp eqCardLevelUp(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp stopLevel(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp cardLevelUp2(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp eqCardLevelUp2(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp fenjie(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp changerHeader(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp itemUpdate(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp arenaItemUpdate(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp danChou(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp geremonialGiftListChou(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp dailyViewList(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp danChouEq(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp characteSell(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp characterCS(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp messageList(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp duoMessageList(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp arenaMessageList(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp receive(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp dailyReceive(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp livelyReceive(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp cailiao(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp hechenCailiao(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp yhechenCailiao(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp emailManage(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp mobilecode(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp getUserMine(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp getStore(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp getEqChares(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp chongzhi(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp chongzhi2(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp getStore2(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp getGongxunStore(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp buyGongxunStore(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp buyStore(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp buyStore3(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp buyStore2(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp giftExchangeCode(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp checkHechen(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp tuPuhenchenList(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp hechenCard(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp findHechenCard(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp soulChou(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp shiChou(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp soulShiChou(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp start(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp blessing(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp reviceblessing(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp njblessing(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp start3(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp ranking(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp ranking100(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp mapRanking100(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp arenaRanking100(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp mobai(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp bagItemList(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp equipmentNew(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp equipmentMessageList(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp ascensionPillDetai(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp useBagItem(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp chongZhiTower(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp yijiantansuo(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp hongb(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp isNewYear(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp start2(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp saodan(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp start5(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp getTower(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp start4(TokenDto token, HttpServletRequest request) throws Exception;

    /**
     * 世界BOSS挑战：按当天排班(world_boss_daily.base_boss_id)定位BOSS，真打一场战斗；
     * 战后按BOSS最终三维回写当日倍率，击杀(hp_rate=0)时全服广播；每次挑战增加用户功勋5，每次挑战CD 1分钟
     */
    BaseResp worldBossChallenge(TokenDto token, HttpServletRequest request) throws Exception;

    /**
     * 世界BOSS信息：返回当天排班的开启/结束时间、hp_rate、BOSS名及服务器当前时间，
     * 供前端控制挑战按钮状态与倒计时（未到点/已结束/已被击败）
     */
    BaseResp worldBossInfo(TokenDto token, HttpServletRequest request);
    BaseResp start6(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp feiShenhechen(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp jingji(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp duoquJingji(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp kuanList(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp videoList(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp bosList(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp toggleGem(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp xilianCard(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp tianJianInfo(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp savePlay(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp deleteVideo(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp saveChatMsg(ChatMsg chatMsg) throws Exception;
    BaseResp friendAllList(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp invitationSend(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp invitationHandle(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp playBattle(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp qiangdao(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp notice(HttpServletRequest request) throws Exception;
    BaseResp playBattle2(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp isSignedUp(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp arenaSignup(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp arenaTem(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp allCardList(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp geremonialGiftList(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp upgradeMine(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp collectAllSilver(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp robMine(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp queryBeRobLog(TokenDto token, HttpServletRequest request) throws Exception;
    BaseResp getArenaCards(TokenDto token, HttpServletRequest request) throws Exception;
    void sendRawrd();
    void worldBossSendReward();
    void executeMothlyTask();
    void deleteAll();
    void sendTuoRawrd();
    void addActCode();
    void syncLastWeekRank();
    BaseResp toggleEquipSuo(TokenDto token, HttpServletRequest request) throws Exception;

    /**
     * 装备技能书系统（Phase 1：数据 + 后端 + 前端 UI，不接入战斗）。
     * 技能等级实时按装备宝石计算：max(1, floor((Σ宝石等级 / ceil(星级)) / 10 * 2))。
     */
    /** 查询某装备的技能列表(区分使用中/待使用) + 凝神石余额 + 下次保存消耗 + 技能等级 */
    BaseResp eqSkillList(TokenDto token, HttpServletRequest request) throws Exception;
    /** 查询背包中拥有的技能书列表(低/中/高/英雄)，含持有数量 */
    BaseResp eqSkillBookList(TokenDto token, HttpServletRequest request) throws Exception;
    /** 确认：消耗 1 本技能书，从对应池按权重随机抽 1 个技能（仅返回预览，不落库） */
    BaseResp eqDrawSkill(TokenDto token, HttpServletRequest request) throws Exception;
    /** 保存：消耗凝神石，把预览技能写入该装备已学列表；相同技能/技能栏已满则拒绝 */
    BaseResp eqSaveSkill(TokenDto token, HttpServletRequest request) throws Exception;
    /** 替换：把某条已学技能设为使用中（免费），与当前使用中相同则拒绝 */
    BaseResp eqSetActiveSkill(TokenDto token, HttpServletRequest request) throws Exception;
}
