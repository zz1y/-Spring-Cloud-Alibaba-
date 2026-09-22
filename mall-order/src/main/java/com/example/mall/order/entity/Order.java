package com.example.mall.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_order")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;        // 订单号
    private Long productId;        // 商品id
    private Integer count;         // 数量
    private BigDecimal totalPrice; // 总价
    private LocalDateTime createTime;
    private Integer status;   // 订单状态：0待支付 1已支付

}
