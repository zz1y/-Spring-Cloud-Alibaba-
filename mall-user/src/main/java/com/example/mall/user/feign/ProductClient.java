package com.example.mall.user.feign;

import com.example.mall.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

// 声明：我要调用 Nacos 里的 mall-product 服务
@FeignClient(name = "mall-product")
public interface ProductClient {

    // 这个方法的「签名」要和商品服务的接口一模一样
    @GetMapping("/product/test")
    Result<String> productTest();
}
