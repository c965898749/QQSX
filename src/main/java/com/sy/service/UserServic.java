package com.sy.service;

import com.sy.expection.CsdnExpection;
import com.sy.model.User;
import com.sy.model.resp.BaseResp;
import org.apache.ibatis.annotations.Param;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface UserServic {






    //注册新用户
    BaseResp addUser(String username, String userpassword) throws Exception;




    /**
     * 按主键查询用户
     *
     * @param user
     * @return
     */
    public User getUserById(User user) throws Exception;


    /**
     * 分页查询用户数
     *
     * @param user
     * @return
     * @throws Exception
     */
    public int count(User user) throws Exception;



    Integer updateuser(User user);
}
