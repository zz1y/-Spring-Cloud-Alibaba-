package com.example.mall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.mall.order.entity.Order;

public interface OrderService extends IService<Order> {
    String createOrder(Long productId, Integer count);
    // 模拟支付：生成支付二维码
    String pay(Long orderId);

    // 模拟支付回调：订单状态改成已支付
    String payCallback(Long orderId);
}
