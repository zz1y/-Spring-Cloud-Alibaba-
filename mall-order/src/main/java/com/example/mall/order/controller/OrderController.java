package com.example.mall.order.controller;

import com.example.mall.common.Result;
import com.example.mall.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/create")
    public Result<String> create(@RequestParam Long productId, @RequestParam Integer count) {
        return Result.success(orderService.createOrder(productId, count));
    }
    // 模拟支付：生成支付二维码
    @PostMapping("/pay")
    public Result<String> pay(@RequestParam Long orderId) {
        return Result.success(orderService.pay(orderId));
    }

    // 模拟支付回调
    @PostMapping("/pay/callback")
    public Result<String> payCallback(@RequestParam Long orderId) {
        return Result.success(orderService.payCallback(orderId));
    }

}
