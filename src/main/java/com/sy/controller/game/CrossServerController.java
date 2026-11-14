package com.sy.controller.game;

import com.sy.model.game.CrossTeamReq;
import com.sy.model.resp.BaseResp;
import com.sy.service.GameServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 跨服内部接口：只给服务器实例之间调用，不面向客户端。
 * <p>没有 @CrossOrigin，浏览器脚本读不到返回；真正的准入是 crossserver.secret 口令，
 * 校验在 service 里做（和数据来源同一层，方便日志记录被拒原因）。
 */
@RestController
@RequestMapping("cross")
public class CrossServerController {

    @Autowired
    private GameServiceService gameServiceService;

    /**
     * 查本服某个玩家的战队信息，返回结构与 gameService 的 otherTeamInfo 完全一致，
     * 由发起方实例原样透传给玩家客户端
     */
    @PostMapping("otherTeamInfo")
    public BaseResp otherTeamInfo(@RequestBody CrossTeamReq req) {
        BaseResp baseResp = new BaseResp();
        try {
            return gameServiceService.crossOtherTeamInfo(req);
        } catch (Exception e) {
            e.printStackTrace();
            baseResp.setSuccess(0);
            baseResp.setErrorMsg("跨服查询失败");
            return baseResp;
        }
    }
}
