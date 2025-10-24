package org.example.service.strategy;

import lombok.extern.slf4j.Slf4j;
import org.example.service.CommonService;
import org.example.service.algorithm.BussinessFormatDistributionAlgorithm;
import org.example.util.KmpMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 业态类型分配策略实现
 * 
 * 【适用场景】
 * 投放方法："按档位扩展投放"
 * 投放类型："档位+业态"
 * 
 * 【算法特点】
 * - 按业态类型进行分类投放
 * - 支持多种业态组合
 * - 使用业态分配算法
 * 
 * @author Robin
 * @version 1.0
 * @since 2025-10-11
 */
@Slf4j
@Component
public class BusinessFormatDistributionStrategy implements DistributionStrategy {
    
    @Autowired
    private CommonService commonService;
    
    @Autowired
    private BussinessFormatDistributionAlgorithm distributionAlgorithm;
    
    @Autowired
    private KmpMatcher kmpMatcher;
    
    @Override
    public String getDeliveryType() {
        return "档位+业态";
    }
    
    @Override
    public List<String> getTargetList(String deliveryArea) {
        if (!isValidDeliveryArea(deliveryArea)) {
            throw new IllegalArgumentException("投放区域不能为空");
        }
        
        try {
            log.debug("解析业态类型投放目标，投放区域: {}", deliveryArea);
            
            // 直接通过CommonService获取所有业态类型列表
            List<String> allBusinessFormats = commonService.getAllRegionList("按档位扩展投放", "档位+业态");
            
            // 使用KmpMatcher进行模式匹配
            List<String> targetList = kmpMatcher.matchPatterns(deliveryArea, allBusinessFormats);
            
            if (targetList == null || targetList.isEmpty()) {
                throw new RuntimeException("未找到匹配的业态类型: " + deliveryArea);
            }
            
            log.debug("解析到{}个业态类型目标: {}", targetList.size(), targetList);
            return targetList;
            
        } catch (Exception e) {
            log.error("解析业态类型投放目标失败，投放区域: {}", deliveryArea, e);
            throw new RuntimeException("业态类型目标解析失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public BigDecimal[][] calculateMatrix(List<String> targetList, BigDecimal targetAmount) {
        if (!isValidTargetList(targetList)) {
            throw new IllegalArgumentException("业态类型目标列表无效");
        }
        
        if (!isValidTargetAmount(targetAmount)) {
            throw new IllegalArgumentException("预投放量无效: " + targetAmount);
        }
        
        try {
            log.debug("使用业态类型算法计算分配矩阵，目标业态数: {}, 预投放量: {}", 
                     targetList.size(), targetAmount);
            
            // 获取完整的业态客户数矩阵
            BigDecimal[][] businessFormatCustomerMatrix = getBusinessFormatCustomerMatrix();
            
            // 根据目标业态筛选客户数矩阵
            BigDecimal[][] targetBusinessFormatCustomerMatrix = new BigDecimal[targetList.size()][30];
            List<String> allBusinessFormats = commonService.getAllRegionList("按档位扩展投放", "档位+业态");
            
            for (int i = 0; i < targetList.size(); i++) {
                String targetBusinessFormat = targetList.get(i);
                int businessFormatIndex = allBusinessFormats.indexOf(targetBusinessFormat);
                if (businessFormatIndex >= 0 && businessFormatIndex < businessFormatCustomerMatrix.length) {
                    System.arraycopy(businessFormatCustomerMatrix[businessFormatIndex], 0, targetBusinessFormatCustomerMatrix[i], 0, 30);
                } else {
                    log.warn("未找到目标业态 '{}' 的客户数数据，使用零值", targetBusinessFormat);
                    // 如果找不到对应业态的客户数据，使用零值数组
                    targetBusinessFormatCustomerMatrix[i] = new BigDecimal[30];
                    for (int j = 0; j < 30; j++) {
                        targetBusinessFormatCustomerMatrix[i][j] = BigDecimal.ZERO;
                    }
                }
            }
            
            // 调用分配算法计算分配矩阵
            BigDecimal[][] matrix = distributionAlgorithm.calculateDistribution(targetList, targetBusinessFormatCustomerMatrix, targetAmount);
            
            if (matrix == null || matrix.length == 0) {
                throw new RuntimeException("业态类型算法返回空分配矩阵");
            }
            
            if (matrix.length != targetList.size()) {
                throw new RuntimeException("分配矩阵行数与业态类型数不匹配");
            }
            
            log.debug("业态类型分配计算完成，矩阵维度: {}x{}", matrix.length, matrix[0].length);
            return matrix;
            
        } catch (Exception e) {
            log.error("业态类型分配计算失败，目标业态数: {}, 预投放量: {}", 
                     targetList.size(), targetAmount, e);
            throw new RuntimeException("业态类型分配算法执行失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String getAlgorithmName() {
        return "BussinessFormatDistributionAlgorithm";
    }
    
    @Override
    public String getTargetTypeDescription() {
        return "业态类型分配";
    }
    
    /**
     * 获取业态客户数矩阵
     * 从原Service类迁移而来，添加了缓存注解
     */
    @Cacheable("businessFormatCustomerMatrix")
    private BigDecimal[][] getBusinessFormatCustomerMatrix() {
        log.debug("通过CommonService获取业态类型客户数矩阵");
        CommonService.RegionCustomerMatrix regionCustomerMatrixObj = 
            commonService.buildRegionCustomerMatrix("按档位扩展投放", "档位+业态");
        
        // 将List<BigDecimal[]>转换为BigDecimal[][]以保持接口兼容性
        List<BigDecimal[]> matrixList = regionCustomerMatrixObj.getCustomerMatrix();
        BigDecimal[][] matrix = new BigDecimal[matrixList.size()][30];
        
        for (int i = 0; i < matrixList.size(); i++) {
            matrix[i] = matrixList.get(i);
        }
        
        log.debug("业态类型客户数矩阵获取完成，矩阵大小: {}x30", matrix.length);
        return matrix;
    }
}
