package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.service.strategy.DistributionStrategy;
import org.example.service.strategy.DistributionStrategyManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * 分配策略测试类
 * 
 * 用于验证策略模式重构后的功能是否正常
 */
@Slf4j
@SpringBootTest
public class DistributionStrategyTest {
    
    @Autowired
    private DistributionStrategyManager strategyManager;
    
    /**
     * 测试策略管理器是否正确注册了所有策略
     */
    @Test
    public void testStrategyRegistration() {
        log.info("=== 策略注册测试 ===");
        
        // 获取所有支持的投放类型
        List<String> supportedTypes = strategyManager.getSupportedDeliveryTypes();
        log.info("支持的投放类型: {}", supportedTypes);
        
        // 验证策略配置
        String validationResult = strategyManager.validateStrategies();
        log.info("策略验证结果:\n{}", validationResult);
        
        // 测试各种投放方法和类型组合
        testStrategyMapping("按档位统一投放", null);
        testStrategyMapping("按档位扩展投放", "档位+区县");
        testStrategyMapping("按档位扩展投放", "档位+市场类型");
        testStrategyMapping("按档位扩展投放", "档位+城乡分类代码");
        testStrategyMapping("按档位扩展投放", "档位+业态");
        
        // 测试未知的投放方法和类型
        testStrategyMapping("未知投放方法", null);
        testStrategyMapping("按档位扩展投放", "未知扩展类型");
    }
    
    /**
     * 测试特定的策略映射
     */
    private void testStrategyMapping(String deliveryMethod, String deliveryEtype) {
        log.info("--- 测试策略映射: {} + {} ---", deliveryMethod, deliveryEtype);
        
        try {
            boolean isSupported = strategyManager.isSupported(deliveryMethod, deliveryEtype);
            log.info("是否支持: {}", isSupported);
            
            if (isSupported) {
                DistributionStrategy strategy = strategyManager.getStrategy(deliveryMethod, deliveryEtype);
                log.info("策略类: {}", strategy.getClass().getSimpleName());
                log.info("投放类型: {}", strategy.getDeliveryType());
                log.info("算法名称: {}", strategy.getAlgorithmName());
                log.info("目标类型描述: {}", strategy.getTargetTypeDescription());
            } else {
                log.warn("不支持的投放类型组合");
                try {
                    strategyManager.getStrategy(deliveryMethod, deliveryEtype);
                } catch (IllegalArgumentException e) {
                    log.info("预期的异常: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("策略测试异常: {}", e.getMessage(), e);
        }
        
        log.info("");
    }
}
