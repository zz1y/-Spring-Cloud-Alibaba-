package com.example.mall.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mall.common.Result;
import com.example.mall.product.entity.Product;
import com.example.mall.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;

import java.util.List;

@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    // 发布商品（新增）
    @PostMapping("/publish")
    public Result<Product> publish(@RequestBody Product product) {
        productService.save(product);
        return Result.success(product);
    }

    // 删除商品
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        productService.removeById(id);
        return Result.success("删除成功");
    }




    // 搜索商品（按名称模糊查询 + 分页）
    @GetMapping("/search")
    public Result<IPage<Product>> search(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {

        Page<Product> pageParam = new Page<>(page, size);

        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like("name", name);
        }

        IPage<Product> result = productService.page(pageParam, wrapper);
        return Result.success(result);
    }

    // 测试接口
    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("商品服务启动成功");
    }
    // 查询所有商品（走缓存）
    @GetMapping("/list")
    @SentinelResource(value = "productList", blockHandler = "productListBlock")
    public Result<List<Product>> list() {
        List<Product> products = productService.getProductList();
        return Result.success(products);
    }

    // 被限流时走这个方法（降级兜底）
    public Result<List<Product>> productListBlock(BlockException e) {
        return Result.error("请求太频繁，请稍后再试");
    }
    // 扣库存（供订单服务调用，参与分布式事务）
    @PostMapping("/deductStock")
    public Result<String> deductStock(@RequestParam Long productId, @RequestParam Integer count) {
        Product product = productService.getById(productId);
        if (product == null) {
            return Result.error("商品不存在");
        }
        if (product.getStock() < count) {
            return Result.error("库存不足");   // 库存不够 → 返回失败 → 订单那边回滚
        }
        product.setStock(product.getStock() - count);
        productService.updateById(product);
        return Result.success("扣库存成功");
    }


}
