package com.example.mall.user.controller;

import com.example.mall.common.Result;
import com.example.mall.user.entity.User;
import com.example.mall.user.feign.ProductClient;
import com.example.mall.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("用户服务启动成功");
    }

    @PostMapping("/register")
    public Result<String> register(@RequestBody User user) {
        boolean ok = userService.register(user);
        if (ok) {
            return Result.success("注册成功");
        }
        return Result.error("用户名已存在");
    }

    @PostMapping("/login")
    public Result<String> login(@RequestBody User user) {
        String token = userService.login(user);
        if (token == null) {
            return Result.error("用户名或密码错误");
        }
        return Result.success(token);
    }

    // 受保护接口：需要先登录（拦截器会校验 token）
    @GetMapping("/info")
    public Result<String> info(HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        return Result.success("当前登录用户：" + username);
    }
    @Autowired
    private ProductClient productClient;   // 注入 Feign 接口

    // 用户服务调用商品服务（跨服务调用演示）
    @GetMapping("/productTest")
    public Result<String> productTest() {
        return productClient.productTest();
    }
}

