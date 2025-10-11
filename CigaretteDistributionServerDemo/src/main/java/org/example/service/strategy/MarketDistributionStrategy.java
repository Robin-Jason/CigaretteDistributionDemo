package org.example.service.strategy;

import lombok.extern.slf4j.Slf4j;
import org.example.service.MarketTypeDistribution.MarketPredictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 市场类型分配策略实现
 * 
 * 【适用场景】
 * 投放方法："按档位扩展投放"
 * 投放类型："档位+市场类型"
 * 
 * 【算法特点】
 * - 按市场类型（城网/农网）进行分类投放
 * - 支持比例分配算法
 * - 使用市场预测算法
 * 
 * @author Robin
 * @version 1.0
 * @since 2025-10-11
 */
@Slf4j
@Component
public class MarketDistributionStrategy implements DistributionStrategy {
    
    @Autowired
    private MarketPredictService marketService;
    
    @Override
    public String getDeliveryType() {
        return "档位+市场类型";
    }
    
    @Override
    public List<String> getTargetList(String deliveryArea) {
        if (!isValidDeliveryArea(deliveryArea)) {
            throw new IllegalArgumentException("投放区域不能为空");
        }
        
        try {
            log.debug("解析市场类型投放目标，投放区域: {}", deliveryArea);
            List<String> targetList = marketService.getTargetMarketList(deliveryArea);
            
            if (targetList == null || targetList.isEmpty()) {
                throw new RuntimeException("未找到匹配的市场类型: " + deliveryArea);
            }
            
            log.debug("解析到{}个市场类型目标: {}", targetList.size(), targetList);
            return targetList;
            
        } catch (Exception e) {
            log.error("解析市场类型投放目标失败，投放区域: {}", deliveryArea, e);
            throw new RuntimeException("市场类型目标解析失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public BigDecimal[][] calculateMatrix(List<String> targetList, BigDecimal targetAmount) {
        if (!isValidTargetList(targetList)) {
            throw new IllegalArgumentException("市场类型目标列表无效");
        }
        
        if (!isValidTargetAmount(targetAmount)) {
            throw new IllegalArgumentException("预投放量无效: " + targetAmount);
        }
        
        try {
            log.debug("使用市场类型算法计算分配矩阵，目标市场数: {}, 预投放量: {}", 
                     targetList.size(), targetAmount);
            
            BigDecimal[][] matrix = marketService.calculateMarketDistributionMatrix(targetList, targetAmount);
            
            if (matrix == null || matrix.length == 0) {
                throw new RuntimeException("市场类型算法返回空分配矩阵");
            }
            
            if (matrix.length != targetList.size()) {
                throw new RuntimeException("分配矩阵行数与市场类型数不匹配");
            }
            
            log.debug("市场类型分配计算完成，矩阵维度: {}x{}", matrix.length, matrix[0].length);
            return matrix;
            
        } catch (Exception e) {
            log.error("市场类型分配计算失败，目标市场数: {}, 预投放量: {}", 
                     targetList.size(), targetAmount, e);
            throw new RuntimeException("市场类型分配算法执行失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String getAlgorithmName() {
        return "MarketProportionalCigaretteDistributionAlgorithm";
    }
    
    @Override
    public String getTargetTypeDescription() {
        return "市场类型分配";
    }
}
