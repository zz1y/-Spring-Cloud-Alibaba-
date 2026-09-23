package com.example.mall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.mall.product.entity.Product;

import java.util.List;

public interface ProductService extends IService<Product> {

    // 查询所有商品（走缓存，带随机过期时间防雪崩）
    List<Product> getProductList();

    // 按 id 查询商品（缓存空值防穿透 + 互斥锁防击穿）
    Product getProductById(Long id);

    // 发布商品（新增，写时删除缓存）
    void publishProduct(Product product);

    // 更新商品（写时删除缓存）
    void updateProduct(Product product);

    // 删除商品（写时删除缓存）
    void deleteProduct(Long id);

    // 扣库存（写时删除缓存）
    // 返回值：null 表示成功；非 null 表示失败原因（供 Controller 映射成 Result）
    String deductStock(Long productId, Integer count);
}
