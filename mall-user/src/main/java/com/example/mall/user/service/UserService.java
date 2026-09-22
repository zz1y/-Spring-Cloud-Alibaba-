package com.example.mall.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.mall.user.entity.User;

public interface UserService extends IService<User> {

    // 注册：返回 true 成功，false 用户名已存在
    boolean register(User user);

    // 登录：成功返回 token，失败返回 null
    String login(User user);
}
