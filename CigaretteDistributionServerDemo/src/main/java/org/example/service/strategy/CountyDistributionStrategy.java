package org.example.service.strategy;

import lombok.extern.slf4j.Slf4j;
import org.example.service.CommonService;
import org.example.service.algorithm.countyCigaretteDistributionAlgorithm;
import org.example.util.KmpMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 区县分配策略实现
 * 
 * 【适用场景】
 * 投放方法："按档位扩展投放"
 * 投放类型："档位+区县"
 * 
 * 【算法特点】
 * - 按区县进行精准投放
 * - 支持多个区县的组合投放
 * - 使用区县分配算法
 * 
 * @author Robin
 * @version 1.0
 * @since 2025-10-11
 */
@Slf4j
@Component
public class CountyDistributionStrategy implements DistributionStrategy {
    
    @Autowired
    private CommonService commonService;
    
    @Autowired
    private countyCigaretteDistributionAlgorithm distributionAlgorithm;
    
    @Autowired
    private KmpMatcher kmpMatcher;
    
    @Override
    public String getDeliveryType() {
        return "档位+区县";
    }
    
    @Override
    public List<String> getTargetList(String deliveryArea) {
        if (!isValidDeliveryArea(deliveryArea)) {
            throw new IllegalArgumentException("投放区域不能为空");
        }
        
        try {
            log.debug("解析区县投放目标，投放区域: {}", deliveryArea);
            
            // 直接通过CommonService获取所有区县列表
            List<String> allCounties = commonService.getAllRegionList("按档位扩展投放", "档位+区县");
            
            // 使用KmpMatcher进行模式匹配
            List<String> targetList = kmpMatcher.matchPatterns(deliveryArea, allCounties);
            
            if (targetList == null || targetList.isEmpty()) {
                throw new RuntimeException("未找到匹配的区县: " + deliveryArea);
            }
            
            log.debug("解析到{}个区县目标: {}", targetList.size(), targetList);
            return targetList;
            
        } catch (Exception e) {
            log.error("解析区县投放目标失败，投放区域: {}", deliveryArea, e);
            throw new RuntimeException("区县目标解析失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public BigDecimal[][] calculateMatrix(List<String> targetList, BigDecimal targetAmount) {
        if (!isValidTargetList(targetList)) {
            throw new IllegalArgumentException("区县目标列表无效");
        }
        
        if (!isValidTargetAmount(targetAmount)) {
            throw new IllegalArgumentException("预投放量无效: " + targetAmount);
        }
        
        try {
            log.debug("使用区县算法计算分配矩阵，目标区县数: {}, 预投放量: {}", 
                     targetList.size(), targetAmount);
            
            // 获取完整的区县客户数矩阵
            BigDecimal[][] countyCustomerMatrix = getCountyCustomerMatrix();
            
            // 根据目标区县筛选客户数矩阵
            BigDecimal[][] targetCountyCustomerMatrix = new BigDecimal[targetList.size()][30];
            List<String> allCounties = commonService.getAllRegionList("按档位扩展投放", "档位+区县");
            
            for (int i = 0; i < targetList.size(); i++) {
                String targetCounty = targetList.get(i);
                int countyIndex = allCounties.indexOf(targetCounty);
                if (countyIndex >= 0 && countyIndex < countyCustomerMatrix.length) {
                    System.arraycopy(countyCustomerMatrix[countyIndex], 0, targetCountyCustomerMatrix[i], 0, 30);
                } else {
                    log.warn("未找到目标区县 '{}' 的客户数数据，使用零值", targetCounty);
                    // 如果找不到对应区县的客户数据，使用零值数组
                    targetCountyCustomerMatrix[i] = new BigDecimal[30];
                    for (int j = 0; j < 30; j++) {
                        targetCountyCustomerMatrix[i][j] = BigDecimal.ZERO;
                    }
                }
            }
            
            // 调用分配算法计算分配矩阵
            BigDecimal[][] matrix = distributionAlgorithm.calculateDistribution(targetList, targetCountyCustomerMatrix, targetAmount);
            
            if (matrix == null || matrix.length == 0) {
                throw new RuntimeException("区县算法返回空分配矩阵");
            }
            
            if (matrix.length != targetList.size()) {
                throw new RuntimeException("分配矩阵行数与区县数不匹配");
            }
            
            log.debug("区县分配计算完成，矩阵维度: {}x{}", matrix.length, matrix[0].length);
            return matrix;
            
        } catch (Exception e) {
            log.error("区县分配计算失败，目标区县数: {}, 预投放量: {}", 
                     targetList.size(), targetAmount, e);
            throw new RuntimeException("区县分配算法执行失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String getAlgorithmName() {
        return "countyCigaretteDistributionAlgorithm";
    }
    
    @Override
    public String getTargetTypeDescription() {
        return "区县分配";
    }
    
    /**
     * 获取区县客户数矩阵
     * 从原Service类迁移而来，添加了缓存注解
     */
    @Cacheable("countyCustomerMatrix")
    private BigDecimal[][] getCountyCustomerMatrix() {
        log.debug("通过CommonService获取区县客户数矩阵");
        CommonService.RegionCustomerMatrix regionCustomerMatrixObj = 
            commonService.buildRegionCustomerMatrix("按档位扩展投放", "档位+区县");
        
        // 将List<BigDecimal[]>转换为BigDecimal[][]以保持接口兼容性
        List<BigDecimal[]> matrixList = regionCustomerMatrixObj.getCustomerMatrix();
        BigDecimal[][] matrix = new BigDecimal[matrixList.size()][30];
        
        for (int i = 0; i < matrixList.size(); i++) {
            matrix[i] = matrixList.get(i);
        }
        
        log.debug("区县客户数矩阵获取完成，矩阵大小: {}x30", matrix.length);
        return matrix;
    }
}
