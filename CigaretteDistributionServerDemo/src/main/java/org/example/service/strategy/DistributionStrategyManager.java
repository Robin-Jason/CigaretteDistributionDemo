package org.example.service.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分配策略管理器
 * 
 * 【核心功能】
 * 管理所有的分配策略实例，提供统一的策略选择和执行接口
 * 
 * 【支持的策略】
 * - CityDistributionStrategy：全市统一投放
 * - CountyDistributionStrategy：档位+区县
 * - MarketDistributionStrategy：档位+市场类型
 * - UrbanRuralDistributionStrategy：档位+城乡分类代码
 * - BusinessFormatDistributionStrategy：档位+业态
 * 
 * 【设计模式】
 * - 策略模式：封装算法族
 * - 工厂模式：根据类型创建策略
 * - 单例模式：Spring管理的单例组件
 * 
 * @author Robin
 * @version 1.0
 * @since 2025-10-11
 */
@Slf4j
@Component
public class DistributionStrategyManager {
    
    private final Map<String, DistributionStrategy> strategyMap = new HashMap<>();
    
    /**
     * 构造方法，自动注入所有策略实例
     */
    @Autowired
    public DistributionStrategyManager(List<DistributionStrategy> strategies) {
        // 将所有策略实例注册到映射表中
        for (DistributionStrategy strategy : strategies) {
            String deliveryType = strategy.getDeliveryType();
            strategyMap.put(deliveryType, strategy);
            log.debug("注册分配策略: {} -> {}", deliveryType, strategy.getClass().getSimpleName());
        }
        
        log.info("分配策略管理器初始化完成，共注册{}个策略", strategyMap.size());
    }
    
    /**
     * 根据投放方法和投放类型获取对应的策略
     * 
     * @param deliveryMethod 投放方法
     * @param deliveryEtype 投放类型
     * @return 对应的策略实例
     * @throws IllegalArgumentException 当策略不存在时抛出异常
     * 
     * @example
     * getStrategy("按档位统一投放", null) -> CityDistributionStrategy
     * getStrategy("按档位扩展投放", "档位+区县") -> CountyDistributionStrategy
     */
    public DistributionStrategy getStrategy(String deliveryMethod, String deliveryEtype) {
        String strategyKey = determineStrategyKey(deliveryMethod, deliveryEtype);
        
        DistributionStrategy strategy = strategyMap.get(strategyKey);
        
        if (strategy == null) {
            String errorMessage = String.format("未找到对应的分配策略: 投放方法=%s, 投放类型=%s, 策略键=%s", 
                                               deliveryMethod, deliveryEtype, strategyKey);
            log.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        
        log.debug("获取分配策略: {} -> {}", strategyKey, strategy.getClass().getSimpleName());
        return strategy;
    }
    
    /**
     * 检查是否支持指定的投放方法和类型
     * 
     * @param deliveryMethod 投放方法
     * @param deliveryEtype 投放类型
     * @return true表示支持，false表示不支持
     */
    public boolean isSupported(String deliveryMethod, String deliveryEtype) {
        String strategyKey = determineStrategyKey(deliveryMethod, deliveryEtype);
        return strategyMap.containsKey(strategyKey);
    }
    
    /**
     * 获取所有支持的投放类型
     * 
     * @return 投放类型列表
     */
    public List<String> getSupportedDeliveryTypes() {
        return strategyMap.keySet().stream().sorted().collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 获取策略的详细信息
     * 
     * @param deliveryMethod 投放方法
     * @param deliveryEtype 投放类型
     * @return 策略信息映射
     */
    public Map<String, String> getStrategyInfo(String deliveryMethod, String deliveryEtype) {
        DistributionStrategy strategy = getStrategy(deliveryMethod, deliveryEtype);
        
        Map<String, String> info = new HashMap<>();
        info.put("deliveryType", strategy.getDeliveryType());
        info.put("algorithmName", strategy.getAlgorithmName());
        info.put("targetTypeDescription", strategy.getTargetTypeDescription());
        info.put("strategyClass", strategy.getClass().getSimpleName());
        
        return info;
    }
    
    /**
     * 根据投放方法和类型确定策略键
     * 
     * @param deliveryMethod 投放方法
     * @param deliveryEtype 投放类型
     * @return 策略键
     */
    private String determineStrategyKey(String deliveryMethod, String deliveryEtype) {
        if ("按档位统一投放".equals(deliveryMethod)) {
            return "全市统一投放";
        } else if ("按档位扩展投放".equals(deliveryMethod)) {
            // 对于扩展投放，直接使用投放类型作为策略键
            return deliveryEtype != null ? deliveryEtype : "未知投放类型";
        } else {
            return "未知投放方式: " + deliveryMethod;
        }
    }
    
    /**
     * 验证策略配置的完整性
     * 用于应用启动时的自检
     * 
     * @return 验证结果描述
     */
    public String validateStrategies() {
        StringBuilder result = new StringBuilder();
        result.append("分配策略验证结果:\n");
        
        String[] expectedTypes = {
            "全市统一投放", "档位+区县", "档位+市场类型", "档位+城乡分类代码", "档位+业态"
        };
        
        for (String expectedType : expectedTypes) {
            DistributionStrategy strategy = strategyMap.get(expectedType);
            if (strategy != null) {
                result.append(String.format("✅ %s -> %s\n", expectedType, strategy.getClass().getSimpleName()));
            } else {
                result.append(String.format("❌ %s -> 策略缺失\n", expectedType));
            }
        }
        
        result.append(String.format("总计: %d个策略已注册", strategyMap.size()));
        
        String validationResult = result.toString();
        log.info("策略配置验证:\n{}", validationResult);
        
        return validationResult;
    }
}
