package com.example.mall.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mall.product.entity.Product;
import com.example.mall.product.mapper.ProductMapper;
import com.example.mall.product.service.ProductService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // ===== 缓存 key 与 TTL 约定 =====
    private static final String KEY_LIST = "product:list:all";   // 商品列表缓存
    private static final String KEY_PREFIX = "product:info:";    // 单个商品缓存前缀，如 product:info:1
    private static final String NULL_PLACEHOLDER = "__NULL__";   // 空值占位符（防穿透用）

    private static final long BASE_TTL_SECONDS = 30 * 60;        // 基础过期时间：30 分钟
    private static final long JITTER_SECONDS = 5 * 60;           // 随机浮动 0~5 分钟，让过期时间错开（防雪崩）
    private static final long NULL_TTL_SECONDS = 60;             // 空值缓存：60 秒，短一点（防穿透）
    private static final long LOCK_TTL_SECONDS = 10;             // 互斥锁：10 秒（防击穿）

    // ================= 读：查询所有商品（旁路缓存 + 随机过期防雪崩） =================
    @Override
    public List<Product> getProductList() {
        // ① 先查缓存
        String json = stringRedisTemplate.opsForValue().get(KEY_LIST);
        if (json != null) {
            try {
                return objectMapper.readValue(json, new TypeReference<List<Product>>() {});
            } catch (Exception e) {
                // 缓存数据损坏，走下面的 DB 回源重建
            }
        }
        // ② 缓存未命中，查 DB
        List<Product> list = this.list();
        // ③ 回填缓存（随机 TTL，避免大量 key 同时过期导致雪崩）
        putJson(KEY_LIST, list, randomTtl());
        return list;
    }

    // ================= 读：按 id 查询（空值防穿透 + 互斥锁防击穿） =================
    @Override
    public Product getProductById(Long id) {
        String key = KEY_PREFIX + id;

        // ① 查缓存
        Product cached = readProduct(key);
        if (cached != null) {
            return cached;
        }
        // 命中「空值缓存」：之前查过 DB 确认不存在，直接返回 null，不再打 DB（防穿透）
        if (isNullCached(key)) {
            return null;
        }

        // ② 缓存未命中，尝试拿互斥锁（防击穿：热点 key 过期后，只放一个线程回源重建）
        String lockKey = key + ":lock";
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", LOCK_TTL_SECONDS, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(locked)) {
            try {
                // 拿到锁后二次检查，可能别的线程已经重建好了
                cached = readProduct(key);
                if (cached != null) return cached;
                if (isNullCached(key)) return null;
                return loadAndCacheProduct(id, key);
            } finally {
                stringRedisTemplate.delete(lockKey);   // 释放锁
            }
        } else {
            // 没拿到锁：说明有线程正在重建，稍等后重试一次；仍不行就直连 DB 兜底
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            cached = readProduct(key);
            if (cached != null) return cached;
            if (isNullCached(key)) return null;
            return this.getById(id);   // 兜底：直连 DB，保证最终能返回结果
        }
    }

    // ================= 写：以下方法统一「写时删除」保证数据一致性 =================

    @Override
    public void publishProduct(Product product) {
        this.save(product);
        evictList();   // 新增商品 → 列表缓存失效，下次读自然重建
    }

    @Override
    public void updateProduct(Product product) {
        this.updateById(product);
        evictSingle(product.getId());   // 更新商品 → 单条 + 列表缓存都失效
    }

    @Override
    public void deleteProduct(Long id) {
        this.removeById(id);
        evictSingle(id);
    }

    @Override
    public String deductStock(Long productId, Integer count) {
        // 写操作直接读 DB，保证库存最新（不读缓存）
        Product product = this.getById(productId);
        if (product == null) {
            return "商品不存在";
        }
        if (product.getStock() < count) {
            return "库存不足";
        }
        product.setStock(product.getStock() - count);
        this.updateById(product);

        // 写时删除：清旧缓存。
        // 注意：即使 Seata 全局事务回滚（DB 库存恢复），删缓存也是安全的——缓存没了下次读会从 DB 重建。
        evictSingle(productId);
        return null;   // null 表示成功
    }

    // ================= 私有工具方法 =================

    // 随机过期时间：基础 30 分钟 + 0~5 分钟浮动（防雪崩）
    private long randomTtl() {
        return BASE_TTL_SECONDS + ThreadLocalRandom.current().nextLong(JITTER_SECONDS);
    }

    // 把对象序列化成 JSON 写入缓存（写失败不影响主流程）
    private void putJson(String key, Object value, long ttlSeconds) {
        try {
            stringRedisTemplate.opsForValue()
                    .set(key, objectMapper.writeValueAsString(value), ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            // 回填失败仅影响下次缓存命中率，不抛异常
        }
    }

    // 从缓存读单个商品（空值占位或损坏都返回 null）
    private Product readProduct(String key) {
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null || NULL_PLACEHOLDER.equals(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Product.class);
        } catch (Exception e) {
            return null;
        }
    }

    // 判断某个 key 是否命中「空值缓存」
    private boolean isNullCached(String key) {
        return NULL_PLACEHOLDER.equals(stringRedisTemplate.opsForValue().get(key));
    }

    // 回源查 DB 并回填缓存（空值也缓存，防穿透）
    private Product loadAndCacheProduct(Long id, String key) {
        Product product = this.getById(id);
        if (product == null) {
            // 缓存空值占位，短 TTL，防止别人反复用不存在的 id 打穿 DB
            stringRedisTemplate.opsForValue().set(key, NULL_PLACEHOLDER, NULL_TTL_SECONDS, TimeUnit.SECONDS);
        } else {
            putJson(key, product, randomTtl());
        }
        return product;
    }

    // 删除列表缓存
    private void evictList() {
        stringRedisTemplate.delete(KEY_LIST);
    }

    // 删除单个商品缓存 + 列表缓存（单条变了，列表也失效）
    private void evictSingle(Long id) {
        stringRedisTemplate.delete(KEY_PREFIX + id);
        stringRedisTemplate.delete(KEY_LIST);
    }
}
