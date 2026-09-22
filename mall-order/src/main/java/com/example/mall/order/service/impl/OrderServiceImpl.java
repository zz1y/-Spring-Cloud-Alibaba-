package com.example.mall.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mall.common.Result;
import com.example.mall.order.entity.Order;
import com.example.mall.order.feign.ProductClient;
import com.example.mall.order.mapper.OrderMapper;
import com.example.mall.order.service.OrderService;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Autowired
    private ProductClient productClient;

    @GlobalTransactional   // ★★★ 分布式事务核心注解 ★★★
    @Override
    public String createOrder(Long productId, Integer count) {
        // ① 生成订单（写订单表）
        Order order = new Order();
        order.setOrderNo("NO" + System.currentTimeMillis());
        order.setProductId(productId);
        order.setCount(count);
        order.setTotalPrice(new BigDecimal("100"));
        order.setStatus(0);   // 新订单默认「待支付」

        this.save(order);

        // ② 调用商品服务扣库存（写商品表）
        Result<String> result = productClient.deductStock(productId, count);
        if (result.getCode() != 200) {
            throw new RuntimeException("扣库存失败：" + result.getMessage());
        }
        // ③ 模拟：扣库存成功后，订单服务又出错了（演示跨服务回滚）
        if (count == 3) {
            throw new RuntimeException("模拟下单后续失败");
        }

        return "下单成功";


    }
    // 模拟微信「统一下单」：生成支付二维码
    @Override
    public String pay(Long orderId) {
        Order order = this.getById(orderId);
        if (order == null) {
            return "订单不存在";
        }
        // 只有「待支付」状态才能支付（状态机）
        if (order.getStatus() != 0) {
            return "订单状态异常，无法支付";
        }
        // 真实场景：这里调微信统一下单接口拿 code_url；模拟直接造一个假的支付链接
        String codeUrl = "weixin://wxpay/bizpayurl?pr=fake_" + order.getOrderNo();
        return codeUrl;
    }

    // 模拟微信「支付回调」：把订单改成已支付
    @Override
    public String payCallback(Long orderId) {
        Order order = this.getById(orderId);
        if (order == null) {
            return "订单不存在";
        }
        // ★ 幂等处理：已支付的订单，重复回调直接返回，不重复处理
        if (order.getStatus() == 1) {
            return "订单已支付，重复回调忽略";
        }
        // 真实场景：这里要先「验签」微信的回调签名，防止伪造回调；模拟省略
        order.setStatus(1);   // 改成已支付
        this.updateById(order);
        return "支付成功";
    }

}
