package com.example.mall.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mall.user.entity.User;
import com.example.mall.user.mapper.UserMapper;
import com.example.mall.user.service.UserService;
import com.example.mall.user.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    // 密码加密器：BCrypt
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Redis 模板：用来存 token
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean register(User user) {
        User exist = this.getOne(new QueryWrapper<User>().eq("username", user.getUsername()));
        if (exist != null) {
            return false;
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return this.save(user);
    }

    @Override
    public String login(User user) {
        // 1. 按用户名查用户
        User dbUser = this.getOne(new QueryWrapper<User>().eq("username", user.getUsername()));
        if (dbUser == null) {
            return null; // 用户不存在
        }
        // 2. 校验密码：拿「输入的明文」和「库里的密文」比对
        if (!passwordEncoder.matches(user.getPassword(), dbUser.getPassword())) {
            return null; // 密码错误
        }
        // 3. 生成 JWT token
        String token = JwtUtil.generateToken(dbUser.getUsername());
        // 4. token 存 Redis，30 分钟过期
        stringRedisTemplate.opsForValue().set("login:token:" + token, dbUser.getUsername(), Duration.ofMinutes(30));
        // 5. 返回 token
        return token;
    }
}
