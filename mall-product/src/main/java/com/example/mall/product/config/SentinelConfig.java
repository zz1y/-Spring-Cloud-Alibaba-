package com.example.mall.product.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SentinelConfig implements CommandLineRunner {

    // 应用启动后，自动加载限流规则
    @Override
    public void run(String... args) {
        List<FlowRule> rules = new ArrayList<>();
        FlowRule rule = new FlowRule();
        rule.setResource("productList");            // 管谁：和 @SentinelResource 的 value 对应
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS); // 怎么限：按 QPS（每秒请求数）
        rule.setCount(1);                           // 限多少：每秒最多 1 个请求
        rules.add(rule);
        FlowRuleManager.loadRules(rules);
    }
}
