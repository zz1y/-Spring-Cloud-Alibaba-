package com.example.mall.order.feign;

import com.example.mall.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "mall-product")
public interface ProductClient {

    // 调用商品服务的「扣库存」接口（商品服务那边马上建）
    @PostMapping("/product/deductStock")
    Result<String> deductStock(@RequestParam("productId") Long productId,
                               @RequestParam("count") Integer count);
}
