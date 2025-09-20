package org.example.service.UrbanRuralClassificationCodeDIstribution;

import lombok.extern.slf4j.Slf4j;

import org.example.entity.DemoTestData;
import org.example.entity.UrbanRuralClassificationCodeDIstribution.DemoTestUrbanRuralClientNumData;
import org.example.repository.DemoTestDataRepository;
import org.example.repository.UrbanRuralClassificationCodeDIstribution.DemoTestUrbanRuralClientNumDataRepository;
import org.example.service.algorithm.UrbanRuralClassificationCodeDistributionAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 城乡分类代码卷烟分配服务
 * 专门处理"档位+城乡分类代码"业务类型
 */
@Slf4j
@Service
public class UrbanRuralClassificationCodeDistributionService {
    
    @Autowired
    private DemoTestUrbanRuralClientNumDataRepository clientNumDataRepository;
    
    @Autowired
    private DemoTestDataRepository testDataRepository;
    
    @Autowired
    private UrbanRuralClassificationCodeDistributionAlgorithm distributionAlgorithm;
    
    
    @Autowired
    private org.example.util.KmpMatcher kmpMatcher;
    
    // 缓存投放区域列表
    private List<String> allRegionList;
    
    // 缓存区域客户数矩阵
    private BigDecimal[][] regionCustomerMatrix;
    
    /**
     * 获取所有投放区域列表（按id从小到大排序去重）
     */
    @Cacheable("allRegionList")
    public List<String> getAllRegionList() {
        if (allRegionList == null) {
            log.info("初始化城乡分类代码投放区域列表");
            allRegionList = clientNumDataRepository.findAllByOrderByIdAsc()
                    .stream()
                    .map(DemoTestUrbanRuralClientNumData::getUrbanRuralCode)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            log.info("城乡分类代码投放区域列表初始化完成，共{}个区域", allRegionList.size());
        }
        return allRegionList;
    }
    
    /**
     * 获取区域客户数矩阵
     */
    @Cacheable("regionCustomerMatrix")
    public BigDecimal[][] getRegionCustomerMatrix() {
        if (regionCustomerMatrix == null) {
            log.info("初始化城乡分类代码区域客户数矩阵");
            List<DemoTestUrbanRuralClientNumData> clientNumDataList = clientNumDataRepository.findAllByOrderByIdAsc();
            List<String> regions = getAllRegionList();
            
            regionCustomerMatrix = new BigDecimal[regions.size()][30];
            
            for (int i = 0; i < regions.size(); i++) {
                String region = regions.get(i);
                DemoTestUrbanRuralClientNumData clientData = clientNumDataList.stream()
                        .filter(data -> region.equals(data.getUrbanRuralCode()))
                        .findFirst()
                        .orElse(null);
                
                if (clientData != null) {
                    populateRegionCustomerData(regionCustomerMatrix, i, clientData);
                }
            }
            log.info("城乡分类代码区域客户数矩阵初始化完成，矩阵大小: {}x30", regions.size());
        }
        return regionCustomerMatrix;
    }
    
    /**
     * 获取卷烟的待投放区域列表
     * @param deliveryArea 投放区域字段值
     * @return 匹配成功的投放区域列表
     */
    public List<String> getTargetRegionList(String deliveryArea) {
        log.info("获取城乡分类代码卷烟投放区域，投放区域字段值: {}", deliveryArea);
        List<String> allRegions = getAllRegionList();
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
        List<String> allRegions = getAllRegionList();
        
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
            
            // 2. 获取区域-客户数矩阵
            BigDecimal[][] regionCustomerMatrix = getRegionCustomerMatrix();
            List<String> allRegions = getAllRegionList();
            
            // 3. 计算所有目标投放区域的实际投放量总和
            BigDecimal totalActualAmount = BigDecimal.ZERO;
            
            for (DemoTestData data : cigaretteData) {
                String deliveryArea = data.getDeliveryArea();
                if (deliveryArea == null || deliveryArea.trim().isEmpty()) {
                    continue;
                }
                
                // 找到该投放区域在矩阵中的索引
                int regionIndex = allRegions.indexOf(deliveryArea);
                if (regionIndex == -1) {
                    log.warn("城乡分类代码投放区域 {} 在区域列表中未找到，可用区域: {}", deliveryArea, allRegions);
                    continue;
                }
                log.debug("找到城乡分类代码投放区域 {} 的索引: {}", deliveryArea, regionIndex);
                
                // 4. 计算该区域的实际投放量：各档位的 客户数 * 档位投放量
                BigDecimal regionActualAmount = calculateRegionActualAmount(data, regionCustomerMatrix[regionIndex]);
                
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
    
    /**
     * 为单个记录计算实际投放量
     */
    public BigDecimal calculateActualAmountForRecord(DemoTestData data) {
        try {
            return calculateActualDistributionAmount(data.getCigCode(), data.getCigName(), 
                data.getYear(), data.getMonth(), data.getWeekSeq());
        } catch (Exception e) {
            log.error("计算城乡分类代码实际投放量失败，卷烟代码: {}, 卷烟名称: {}, 错误: {}", 
                data.getCigCode(), data.getCigName(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }
    
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 填充区域客户数据到矩阵
     */
    private void populateRegionCustomerData(BigDecimal[][] matrix, int regionIndex, DemoTestUrbanRuralClientNumData clientData) {
        matrix[regionIndex][0] = clientData.getD30();
        matrix[regionIndex][1] = clientData.getD29();
        matrix[regionIndex][2] = clientData.getD28();
        matrix[regionIndex][3] = clientData.getD27();
        matrix[regionIndex][4] = clientData.getD26();
        matrix[regionIndex][5] = clientData.getD25();
        matrix[regionIndex][6] = clientData.getD24();
        matrix[regionIndex][7] = clientData.getD23();
        matrix[regionIndex][8] = clientData.getD22();
        matrix[regionIndex][9] = clientData.getD21();
        matrix[regionIndex][10] = clientData.getD20();
        matrix[regionIndex][11] = clientData.getD19();
        matrix[regionIndex][12] = clientData.getD18();
        matrix[regionIndex][13] = clientData.getD17();
        matrix[regionIndex][14] = clientData.getD16();
        matrix[regionIndex][15] = clientData.getD15();
        matrix[regionIndex][16] = clientData.getD14();
        matrix[regionIndex][17] = clientData.getD13();
        matrix[regionIndex][18] = clientData.getD12();
        matrix[regionIndex][19] = clientData.getD11();
        matrix[regionIndex][20] = clientData.getD10();
        matrix[regionIndex][21] = clientData.getD9();
        matrix[regionIndex][22] = clientData.getD8();
        matrix[regionIndex][23] = clientData.getD7();
        matrix[regionIndex][24] = clientData.getD6();
        matrix[regionIndex][25] = clientData.getD5();
        matrix[regionIndex][26] = clientData.getD4();
        matrix[regionIndex][27] = clientData.getD3();
        matrix[regionIndex][28] = clientData.getD2();
        matrix[regionIndex][29] = clientData.getD1();
    }
    
    /**
     * 计算单个区域的实际投放量
     */
    private BigDecimal calculateRegionActualAmount(DemoTestData data, BigDecimal[] regionCustomerData) {
        BigDecimal regionActualAmount = BigDecimal.ZERO;
        
        // 获取该区域的档位投放量数组（按D30到D1的顺序）
        BigDecimal[] allocationArray = {
            data.getD30() != null ? data.getD30() : BigDecimal.ZERO,
            data.getD29() != null ? data.getD29() : BigDecimal.ZERO,
            data.getD28() != null ? data.getD28() : BigDecimal.ZERO,
            data.getD27() != null ? data.getD27() : BigDecimal.ZERO,
            data.getD26() != null ? data.getD26() : BigDecimal.ZERO,
            data.getD25() != null ? data.getD25() : BigDecimal.ZERO,
            data.getD24() != null ? data.getD24() : BigDecimal.ZERO,
            data.getD23() != null ? data.getD23() : BigDecimal.ZERO,
            data.getD22() != null ? data.getD22() : BigDecimal.ZERO,
            data.getD21() != null ? data.getD21() : BigDecimal.ZERO,
            data.getD20() != null ? data.getD20() : BigDecimal.ZERO,
            data.getD19() != null ? data.getD19() : BigDecimal.ZERO,
            data.getD18() != null ? data.getD18() : BigDecimal.ZERO,
            data.getD17() != null ? data.getD17() : BigDecimal.ZERO,
            data.getD16() != null ? data.getD16() : BigDecimal.ZERO,
            data.getD15() != null ? data.getD15() : BigDecimal.ZERO,
            data.getD14() != null ? data.getD14() : BigDecimal.ZERO,
            data.getD13() != null ? data.getD13() : BigDecimal.ZERO,
            data.getD12() != null ? data.getD12() : BigDecimal.ZERO,
            data.getD11() != null ? data.getD11() : BigDecimal.ZERO,
            data.getD10() != null ? data.getD10() : BigDecimal.ZERO,
            data.getD9() != null ? data.getD9() : BigDecimal.ZERO,
            data.getD8() != null ? data.getD8() : BigDecimal.ZERO,
            data.getD7() != null ? data.getD7() : BigDecimal.ZERO,
            data.getD6() != null ? data.getD6() : BigDecimal.ZERO,
            data.getD5() != null ? data.getD5() : BigDecimal.ZERO,
            data.getD4() != null ? data.getD4() : BigDecimal.ZERO,
            data.getD3() != null ? data.getD3() : BigDecimal.ZERO,
            data.getD2() != null ? data.getD2() : BigDecimal.ZERO,
            data.getD1() != null ? data.getD1() : BigDecimal.ZERO
        };
        
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
}
