package org.example.service.UrbanRuralClassificationCodeDIstribution;

import lombok.extern.slf4j.Slf4j;

import org.example.entity.DemoTestData;
import org.example.repository.DemoTestDataRepository;
import org.example.service.CommonService;
import org.example.service.algorithm.UrbanRuralClassificationCodeDistributionAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 城乡分类代码卷烟分配服务
 * 专门处理"档位+城乡分类代码"业务类型
 */
@Slf4j
@Service
public class UrbanRuralClassificationCodeDistributionService {
    
    @Autowired
    private CommonService commonService;
    
    @Autowired
    private DemoTestDataRepository testDataRepository;
    
    @Autowired
    private UrbanRuralClassificationCodeDistributionAlgorithm distributionAlgorithm;
    
    @Autowired
    private org.example.util.KmpMatcher kmpMatcher;
    
    /**
     * 获取区域客户数矩阵（通过CommonService获取）
     */
    @Cacheable("regionCustomerMatrix")
    public BigDecimal[][] getRegionCustomerMatrix() {
        log.info("通过CommonService获取城乡分类代码区域客户数矩阵");
        CommonService.RegionCustomerMatrix regionCustomerMatrixObj = 
            commonService.buildRegionCustomerMatrix("按档位扩展投放", "档位+城乡分类代码");
        
        // 将List<BigDecimal[]>转换为BigDecimal[][]以保持接口兼容性
        List<BigDecimal[]> matrixList = regionCustomerMatrixObj.getCustomerMatrix();
        BigDecimal[][] matrix = new BigDecimal[matrixList.size()][30];
        
        for (int i = 0; i < matrixList.size(); i++) {
            matrix[i] = matrixList.get(i);
        }
        
        log.info("城乡分类代码区域客户数矩阵获取完成，矩阵大小: {}x30", matrix.length);
        return matrix;
    }
    
    /**
     * 获取卷烟的待投放区域列表
     * @param deliveryArea 投放区域字段值
     * @return 匹配成功的投放区域列表
     */
    public List<String> getTargetRegionList(String deliveryArea) {
        log.info("获取城乡分类代码卷烟投放区域，投放区域字段值: {}", deliveryArea);
        List<String> allRegions = commonService.getAllRegionList("按档位扩展投放", "档位+城乡分类代码");
        List<String> targetRegions = kmpMatcher.matchPatterns(deliveryArea, allRegions);
        log.info("匹配成功的城乡分类代码投放区域: {}", targetRegions);
        return targetRegions;
    }
    
    /**
     * 计算卷烟分配矩阵
     * @param targetRegions 目标投放区域列表
     * @param targetAmount 预投放量
     * @return 分配矩阵 [区域数][档位数]
     */
    public BigDecimal[][] calculateDistributionMatrix(List<String> targetRegions, BigDecimal targetAmount) {
        log.info("计算城乡分类代码卷烟分配矩阵，目标区域数: {}, 预投放量: {}", targetRegions.size(), targetAmount);
        BigDecimal[][] regionCustomerMatrix = getRegionCustomerMatrix();
        
        // 根据目标区域筛选客户数矩阵
        BigDecimal[][] targetRegionCustomerMatrix = new BigDecimal[targetRegions.size()][30];
        List<String> allRegions = commonService.getAllRegionList("按档位扩展投放", "档位+城乡分类代码");
        
        for (int i = 0; i < targetRegions.size(); i++) {
            String targetRegion = targetRegions.get(i);
            int regionIndex = allRegions.indexOf(targetRegion);
            if (regionIndex >= 0) {
                System.arraycopy(regionCustomerMatrix[regionIndex], 0, targetRegionCustomerMatrix[i], 0, 30);
            }
        }
        
        return distributionAlgorithm.calculateDistribution(targetRegions, targetRegionCustomerMatrix, targetAmount);
    }
    
    /**
     * 根据算法需求描述计算卷烟实际投放量
     * 实际投放量 = 所有目标投放区域的客户数 * 档位投放量 的求和
     * S = Σ(i=1 to R) Σ(j=1 to B) x_{ij} * c_{ij}
     * 其中 x_{ij} 是分配给区域 i 档位 j 的卷烟数量，c_{ij} 是该区域该档位的客户数
     */
    public BigDecimal calculateActualDistributionAmount(String cigCode, String cigName, 
                                                       Integer year, Integer month, Integer weekSeq) {
        try {
            log.debug("开始计算城乡分类代码卷烟实际投放量，卷烟代码: {}, 卷烟名称: {}, 日期: {}-{}-{}", 
                cigCode, cigName, year, month, weekSeq);
            
            // 1. 获取该卷烟在指定日期的所有投放区域记录
            List<DemoTestData> cigaretteData = testDataRepository.findByCigCodeAndCigNameAndYearAndMonthAndWeekSeq(
                cigCode, cigName, year, month, weekSeq);
            
            if (cigaretteData.isEmpty()) {
                log.warn("未找到城乡分类代码卷烟数据，卷烟代码: {}, 卷烟名称: {}, 日期: {}-{}-{}", 
                    cigCode, cigName, year, month, weekSeq);
                return BigDecimal.ZERO;
            }
            
            // 2. 使用CommonService获取区域客户数矩阵对象
            CommonService.RegionCustomerMatrix regionCustomerMatrixObj = 
                commonService.buildRegionCustomerMatrix("按档位扩展投放", "档位+城乡分类代码");
            
            // 3. 计算所有目标投放区域的实际投放量总和
            BigDecimal totalActualAmount = BigDecimal.ZERO;
            
            for (DemoTestData data : cigaretteData) {
                String deliveryArea = data.getDeliveryArea();
                if (deliveryArea == null || deliveryArea.trim().isEmpty()) {
                    continue;
                }
                
                // 4. 通过RegionCustomerMatrix对象直接获取区域的客户数数组
                BigDecimal[] customerCounts = regionCustomerMatrixObj.getCustomerCountsByRegion(deliveryArea);
                if (customerCounts == null) {
                    log.warn("城乡分类代码投放区域 {} 在区域列表中未找到", deliveryArea);
                    continue;
                }
                log.debug("找到城乡分类代码投放区域 {} 的客户数数据", deliveryArea);
                
                // 5. 计算该区域的实际投放量：各档位的 客户数 * 档位投放量
                BigDecimal regionActualAmount = calculateRegionActualAmount(data, customerCounts);
                
                log.debug("城乡分类代码区域 {} 的实际投放量: {}", deliveryArea, regionActualAmount);
                totalActualAmount = totalActualAmount.add(regionActualAmount);
            }
            
            log.debug("城乡分类代码卷烟 {} 的总实际投放量: {}", cigName, totalActualAmount);
            return totalActualAmount;
            
        } catch (Exception e) {
            log.error("计算城乡分类代码实际投放量失败，卷烟代码: {}, 卷烟名称: {}, 错误: {}", 
                cigCode, cigName, e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }
    
    
    
    // ==================== 私有辅助方法 ====================
    /**
     * 计算单个区域的实际投放量
     * 使用更简洁的方式提取档位数据，避免重复代码
     */
    private BigDecimal calculateRegionActualAmount(DemoTestData data, BigDecimal[] regionCustomerData) {
        BigDecimal regionActualAmount = BigDecimal.ZERO;
        
        // 使用反射或直接方法调用来获取档位投放量数组
        BigDecimal[] allocationArray = extractAllocationArray(data);
        
        // 计算该区域各档位的实际投放量：客户数 * 档位投放量
        for (int j = 0; j < 30; j++) {
            if (regionCustomerData[j] != null && allocationArray[j] != null) {
                BigDecimal customerCount = regionCustomerData[j];
                BigDecimal allocation = allocationArray[j];
                BigDecimal segmentActual = customerCount.multiply(allocation);
                regionActualAmount = regionActualAmount.add(segmentActual);
                
                if (segmentActual.compareTo(BigDecimal.ZERO) > 0) {
                    log.debug("城乡分类代码档位 {} - 客户数: {}, 分配量: {}, 实际投放量: {}", 
                        (30-j), customerCount, allocation, segmentActual);
                }
            }
        }
        
        return regionActualAmount;
    }
    
    /**
     * 提取档位分配数组的工具方法（使用反射简化代码）
     * 统一处理DemoTestData的30个档位字段到数组的转换
     */
    private BigDecimal[] extractAllocationArray(DemoTestData data) {
        BigDecimal[] allocationArray = new BigDecimal[30];
        
        try {
            // 使用反射获取D30到D1的方法并调用
            for (int i = 0; i < 30; i++) {
                String methodName = "getD" + (30 - i); // D30对应索引0，D1对应索引29
                Method method = DemoTestData.class.getMethod(methodName); // 在DemoTestData类中找到名叫'getD*'的方法
                BigDecimal value = (BigDecimal) method.invoke(data); // 调用data.getD*()并获取返回值
                allocationArray[i] = value != null ? value : BigDecimal.ZERO;
            }
        } catch (Exception e) {
            log.error("提取档位分配数组失败，使用默认值", e);
            // 如果反射失败，填充默认值
            for (int i = 0; i < 30; i++) {
                allocationArray[i] = BigDecimal.ZERO;
            }
        }
        
        return allocationArray;
    }
}
