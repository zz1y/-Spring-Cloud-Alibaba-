package com.example.mall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.mall.product.entity.Product;
import java.util.List;
public interface ProductService extends IService<Product> {
    // 查询所有商品（结果会被缓存）
    List<Product> getProductList();
}
