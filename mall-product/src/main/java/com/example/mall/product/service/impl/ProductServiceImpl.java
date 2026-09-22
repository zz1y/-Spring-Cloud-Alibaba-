package com.example.mall.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mall.product.entity.Product;
import com.example.mall.product.mapper.ProductMapper;
import com.example.mall.product.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import java.util.List;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {
    // 查询所有商品，结果缓存到 Redis（缓存的 List<Product> 里，Product 已实现 Serializable）
    @Cacheable(value = "productList", key = "'all'")
    @Override
    public List<Product> getProductList() {
        return this.list();
    }
}
