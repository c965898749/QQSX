package com.sy.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sy.expection.CsdnExpection;
import com.sy.mapper.*;
import com.sy.model.*;
import com.sy.model.resp.BaseResp;
import com.sy.service.UserServic;
import com.sy.tool.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Transactional(readOnly = true)
public class UserServicImpl implements UserServic {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private InformationMapper informationMapper;
    @Autowired
    public RedisTemplate redisTemplate;




    //注册新用户
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public BaseResp addUser(String username, String userpassword) throws Exception {
        BaseResp baseResp = new BaseResp();
        User user = new User();
        user.setUsername(username);
        String password = DigestUtils.md5DigestAsHex(userpassword.getBytes());
        user.setUserpassword(password);
        List<User> userList = userMapper.SelectAllUser();
        List<String> usernamelist = new ArrayList<>();
        for (User user1 : userList) {
            usernamelist.add(user1.getUsername());
        }
        if (usernamelist.contains(username)) {
            baseResp.setSuccess(0);
            baseResp.setErrorMsg("您输入的账号已存在，请重新输入");
            return baseResp;
        } else {
            //设置昵称
            String nickname = null;
            Integer flag = 1;
            Integer len = 4;
            userList.clear();
            for (User user1 : userList) {
                usernamelist.add(user1.getNickname());
            }
            while (flag != 0) {
                nickname = RandomName.randomName(false, len);
                if (!usernamelist.contains(nickname)) {
                    flag = 0;
                } else {
                    flag++;
                    if (flag > 1100000000 && flag < 2100000000) {
                        len++;
                        flag = 1;
                    }
                }
            }
            user.setNickname(nickname);
            int max=6,min=1;
            int ran2 = (int) (Math.random()*(max-min)+min);
            String url="/imgs/headimg/"+ran2+".jpg";
            user.setHeadImg(url);
            user.setDownloadmoney((double)0);
            user.setRanking(9999);
            user.setGameRanking(9999);
            user.setLevel(2);
            user.setCollectCount(0);
            user.setBlogCount(0);
            user.setAttentionCount(0);
            user.setFansCount(0);
            user.setResourceCount(0);
            user.setForumCount(0);
            user.setAskCount(0);
            user.setCommentCount(0);
            user.setLikeCount(0);
            user.setVisitorCount(0);
            user.setDownCount(0);
            user.setUnreadreplaycount(0);
            user.setReadquerylikecount(0);
            user.setUnreadfanscount(0);
            user.setIsEmil("0");
            user.setStatus(1);
            int result = userMapper.insertUser(user);
            if (result > 0) {
                baseResp.setSuccess(1);
                baseResp.setErrorMsg("注册成功！");
                return baseResp;
            } else {
                baseResp.setSuccess(0);
                baseResp.setErrorMsg("注册失败！");
                return baseResp;
            }
        }
    }












    @Override
    public User getUserById(User user) throws Exception {
        return userMapper.getUserById(user);
    }






    @Override
    public int count(User user) throws Exception {
        return userMapper.count(user);
    }







    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public Integer updateuser(User user){
     return userMapper.updateuser(user);
    }

}
