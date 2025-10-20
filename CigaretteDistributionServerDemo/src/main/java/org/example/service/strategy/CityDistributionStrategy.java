package org.example.service.strategy;

import lombok.extern.slf4j.Slf4j;
import org.example.service.CommonService;
import org.example.service.algorithm.CityCigaretteDistributionAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * 全市统一投放策略实现
 * 
 * 【适用场景】
 * 投放方法："按档位统一投放"
 * 投放类型：null（全市统一）
 * 
 * 【算法特点】
 * - 投放目标固定为"全市"
 * - 使用城市算法进行统一分配
 * - 不需要解析投放区域字段
 * 
 * @author Robin
 * @version 1.0
 * @since 2025-10-11
 */
@Slf4j
@Component
public class CityDistributionStrategy implements DistributionStrategy {
    
    @Autowired
    private CommonService commonService;
    
    @Autowired
    private CityCigaretteDistributionAlgorithm distributionAlgorithm;
    
    @Override
    public String getDeliveryType() {
        return "全市统一投放";
    }
    
    @Override
    public List<String> getTargetList(String deliveryArea) {
        log.debug("全市统一投放，投放目标固定为[全市]");
        // 全市统一投放时，投放目标固定为"全市"，不依赖deliveryArea参数
        return Arrays.asList("全市");
    }
    
    @Override
    public BigDecimal[][] calculateMatrix(List<String> targetList, BigDecimal targetAmount) {
        if (!isValidTargetList(targetList)) {
            throw new IllegalArgumentException("目标列表无效");
        }
        
        if (!isValidTargetAmount(targetAmount)) {
            throw new IllegalArgumentException("预投放量无效: " + targetAmount);
        }
        
        try {
            log.debug("使用城市算法计算全市统一分配，预投放量: {}", targetAmount);
            
            // 获取完整的全市客户数矩阵
            BigDecimal[][] cityCustomerMatrix = getCityCustomerMatrix();
            
            // 对于全市统一投放，目标列表只有"全市"一个元素
            BigDecimal[][] targetCityCustomerMatrix = new BigDecimal[targetList.size()][30];
            
            // 全市统一投放只使用第一行数据（全市汇总数据）
            if (cityCustomerMatrix.length > 0) {
                System.arraycopy(cityCustomerMatrix[0], 0, targetCityCustomerMatrix[0], 0, 30);
            } else {
                log.warn("未找到全市客户数数据，使用零值");
                targetCityCustomerMatrix[0] = new BigDecimal[30];
                for (int j = 0; j < 30; j++) {
                    targetCityCustomerMatrix[0][j] = BigDecimal.ZERO;
                }
            }
            
            // 调用分配算法计算分配矩阵
            BigDecimal[][] matrix = distributionAlgorithm.calculateDistribution(targetList, targetCityCustomerMatrix, targetAmount);
            
            if (matrix == null || matrix.length == 0) {
                throw new RuntimeException("城市算法返回空分配矩阵");
            }
            
            log.debug("全市统一分配计算完成，矩阵维度: {}x{}", matrix.length, matrix[0].length);
            return matrix;
            
        } catch (Exception e) {
            log.error("全市统一分配计算失败，预投放量: {}", targetAmount, e);
            throw new RuntimeException("全市统一分配算法执行失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String getAlgorithmName() {
        return "CityCigaretteDistributionAlgorithm";
    }
    
    @Override
    public String getTargetTypeDescription() {
        return "全市统一投放";
    }
    
    /**
     * 获取全市客户数矩阵
     * 从原Service类迁移而来，添加了缓存注解
     */
    @Cacheable("cityCustomerMatrix")
    private BigDecimal[][] getCityCustomerMatrix() {
        log.debug("通过CommonService获取全市客户数矩阵");
        CommonService.RegionCustomerMatrix regionCustomerMatrixObj = 
            commonService.buildRegionCustomerMatrix("按档位统一投放", null);
        
        // 将List<BigDecimal[]>转换为BigDecimal[][]以保持接口兼容性
        List<BigDecimal[]> matrixList = regionCustomerMatrixObj.getCustomerMatrix();
        BigDecimal[][] matrix = new BigDecimal[matrixList.size()][30];
        
        for (int i = 0; i < matrixList.size(); i++) {
            matrix[i] = matrixList.get(i);
        }
        
        log.debug("全市客户数矩阵获取完成，矩阵大小: {}x30", matrix.length);
        return matrix;
    }
}
