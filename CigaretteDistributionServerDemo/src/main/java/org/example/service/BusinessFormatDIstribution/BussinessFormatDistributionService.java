package org.example.service.BusinessFormatDIstribution;

import lombok.extern.slf4j.Slf4j;

import org.example.entity.CigaretteDistributionPredictionData;
import org.example.entity.RegionClientNumData;
import org.example.repository.CigaretteDistributionPredictionDataRepository;
import org.example.service.RegionClientNumDataService;
import org.example.service.algorithm.BussinessFormatDistributionAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 业态类型卷烟分配服务
 * 专门处理"档位+业态"业务类型
 */
@Slf4j
@Service
public class BussinessFormatDistributionService {
    
    @Autowired
    private RegionClientNumDataService regionClientNumDataService;
    
    @Autowired
    private CigaretteDistributionPredictionDataRepository testDataRepository;
    
    @Autowired
    private BussinessFormatDistributionAlgorithm distributionAlgorithm;
    
    
    @Autowired
    private org.example.util.KmpMatcher kmpMatcher;
    
    // 缓存业态类型列表
    private List<String> allBusinessFormatList;
    
    // 缓存业态类型客户数矩阵
    private BigDecimal[][] businessFormatCustomerMatrix;
    
    /**
     * 获取所有业态类型列表（按id从小到大排序去重）
     */
    @Cacheable("allBusinessFormatList")
    public List<String> getAllBusinessFormatList() {
        if (allBusinessFormatList == null) {
            log.info("初始化业态类型列表");
            String tableName = regionClientNumDataService.generateTableName("按档位扩展投放", "档位+业态", false);
            allBusinessFormatList = regionClientNumDataService.findAllByTableName(tableName)
                    .stream()
                    .map(RegionClientNumData::getRegion)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            log.info("业态类型列表初始化完成，共{}个业态类型", allBusinessFormatList.size());
        }
        return allBusinessFormatList;
    }
    
    /**
     * 获取业态类型客户数矩阵
     */
    @Cacheable("businessFormatCustomerMatrix")
    public BigDecimal[][] getBusinessFormatCustomerMatrix() {
        if (businessFormatCustomerMatrix == null) {
            log.info("初始化业态类型客户数矩阵");
            String tableName = regionClientNumDataService.generateTableName("按档位扩展投放", "档位+业态", false);
            List<RegionClientNumData> businessFormatClientNumDataList = regionClientNumDataService.findAllByTableName(tableName);
            List<String> businessFormats = getAllBusinessFormatList();
            
            businessFormatCustomerMatrix = new BigDecimal[businessFormats.size()][30];
            
            for (int i = 0; i < businessFormats.size(); i++) {
                String businessFormat = businessFormats.get(i);
                RegionClientNumData clientData = businessFormatClientNumDataList.stream()
                        .filter(data -> businessFormat.equals(data.getRegion()))
                        .findFirst()
                        .orElse(null);
                
                if (clientData != null) {
                    populateBusinessFormatCustomerData(businessFormatCustomerMatrix, i, clientData);
                }
            }
            log.info("业态类型客户数矩阵初始化完成，矩阵大小: {}x30", businessFormats.size());
        }
        return businessFormatCustomerMatrix;
    }
    
    /**
     * 获取卷烟的待投放业态类型列表
     * @param deliveryArea 投放区域字段值
     * @return 匹配成功的业态类型列表
     */
    public List<String> getTargetBusinessFormatList(String deliveryArea) {
        log.info("获取卷烟投放业态类型，投放区域字段值: {}", deliveryArea);
        List<String> allBusinessFormats = getAllBusinessFormatList();
        List<String> targetBusinessFormats = kmpMatcher.matchPatterns(deliveryArea, allBusinessFormats);
        log.info("匹配成功的业态类型: {}", targetBusinessFormats);
        return targetBusinessFormats;
    }
    
    /**
     * 计算卷烟分配矩阵（业态类型）
     * @param targetBusinessFormats 目标业态类型列表
     * @param targetAmount 预投放量
     * @return 分配矩阵 [业态类型数][档位数]
     */
    public BigDecimal[][] calculateBusinessFormatDistributionMatrix(List<String> targetBusinessFormats, BigDecimal targetAmount) {
        log.info("计算卷烟业态类型分配矩阵，目标业态类型数: {}, 预投放量: {}", targetBusinessFormats.size(), targetAmount);
        BigDecimal[][] businessFormatCustomerMatrix = getBusinessFormatCustomerMatrix();
        
        // 根据目标业态类型筛选客户数矩阵
        BigDecimal[][] targetBusinessFormatCustomerMatrix = new BigDecimal[targetBusinessFormats.size()][30];
        List<String> allBusinessFormats = getAllBusinessFormatList();
        
        for (int i = 0; i < targetBusinessFormats.size(); i++) {
            String targetBusinessFormat = targetBusinessFormats.get(i);
            int businessFormatIndex = allBusinessFormats.indexOf(targetBusinessFormat);
            if (businessFormatIndex >= 0) {
                System.arraycopy(businessFormatCustomerMatrix[businessFormatIndex], 0, targetBusinessFormatCustomerMatrix[i], 0, 30);
            }
        }
        
        return distributionAlgorithm.calculateDistribution(targetBusinessFormats, targetBusinessFormatCustomerMatrix, targetAmount);
    }
    
    /**
     * 根据算法需求描述计算卷烟实际投放量（业态类型版本）
     * 实际投放量 = 所有目标业态类型的客户数 * 档位投放量 的求和
     * S = Σ(i=1 to B) Σ(j=1 to G) x_{ij} * c_{ij}
     * 其中 x_{ij} 是分配给业态类型 i 档位 j 的卷烟数量，c_{ij} 是该业态类型该档位的客户数
     */
    public BigDecimal calculateActualDistributionAmount(String cigCode, String cigName, 
                                                       Integer year, Integer month, Integer weekSeq) {
        try {
            log.debug("开始计算业态类型卷烟实际投放量，卷烟代码: {}, 卷烟名称: {}, 日期: {}-{}-{}", 
                cigCode, cigName, year, month, weekSeq);
            
            // 1. 获取该卷烟在指定日期的所有投放区域记录
            List<CigaretteDistributionPredictionData> cigaretteData = testDataRepository.findByCigCodeAndCigNameAndYearAndMonthAndWeekSeq(
                cigCode, cigName, year, month, weekSeq);
            
            if (cigaretteData.isEmpty()) {
                log.warn("未找到业态类型卷烟数据，卷烟代码: {}, 卷烟名称: {}, 日期: {}-{}-{}", 
                    cigCode, cigName, year, month, weekSeq);
                return BigDecimal.ZERO;
            }
            
            // 2. 获取业态类型-客户数矩阵
            BigDecimal[][] businessFormatCustomerMatrix = getBusinessFormatCustomerMatrix();
            List<String> allBusinessFormats = getAllBusinessFormatList();
            
            // 3. 计算所有目标投放业态类型的实际投放量总和
            BigDecimal totalActualAmount = BigDecimal.ZERO;
            
            for (CigaretteDistributionPredictionData data : cigaretteData) {
                String deliveryArea = data.getDeliveryArea();
                if (deliveryArea == null || deliveryArea.trim().isEmpty()) {
                    continue;
                }
                
                // 找到该投放业态类型在矩阵中的索引
                int businessFormatIndex = allBusinessFormats.indexOf(deliveryArea);
                if (businessFormatIndex == -1) {
                    log.warn("业态类型 {} 在业态类型列表中未找到，可用业态类型: {}", deliveryArea, allBusinessFormats);
                    continue;
                }
                log.debug("找到业态类型 {} 的索引: {}", deliveryArea, businessFormatIndex);
                
                // 4. 计算该业态类型的实际投放量：各档位的 客户数 * 档位投放量
                BigDecimal businessFormatActualAmount = calculateBusinessFormatActualAmount(data, businessFormatCustomerMatrix[businessFormatIndex]);
                
                log.debug("业态类型 {} 的实际投放量: {}", deliveryArea, businessFormatActualAmount);
                totalActualAmount = totalActualAmount.add(businessFormatActualAmount);
            }
            
            log.debug("业态类型卷烟 {} 的总实际投放量: {}", cigName, totalActualAmount);
            return totalActualAmount;
            
        } catch (Exception e) {
            log.error("计算业态类型实际投放量失败，卷烟代码: {}, 卷烟名称: {}, 错误: {}", 
                cigCode, cigName, e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * 为单个记录计算实际投放量
     */
    public BigDecimal calculateActualAmountForRecord(CigaretteDistributionPredictionData data) {
        try {
            return calculateActualDistributionAmount(data.getCigCode(), data.getCigName(), 
                data.getYear(), data.getMonth(), data.getWeekSeq());
        } catch (Exception e) {
            log.error("计算业态类型实际投放量失败，卷烟代码: {}, 卷烟名称: {}, 错误: {}", 
                data.getCigCode(), data.getCigName(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 填充业态类型客户数据到矩阵
     */
    private void populateBusinessFormatCustomerData(BigDecimal[][] matrix, int businessFormatIndex, RegionClientNumData clientData) {
        BigDecimal[] gradeArray = clientData.getGradeArray();
        for (int j = 0; j < 30 && j < gradeArray.length; j++) {
            matrix[businessFormatIndex][j] = gradeArray[j] != null ? gradeArray[j] : BigDecimal.ZERO;
        }
    }
    
    /**
     * 计算单个业态类型的实际投放量
     */
    private BigDecimal calculateBusinessFormatActualAmount(CigaretteDistributionPredictionData data, BigDecimal[] businessFormatCustomerData) {
        BigDecimal businessFormatActualAmount = BigDecimal.ZERO;
        
        // 获取该业态类型的档位投放量数组（按D30到D1的顺序）
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
        
        // 计算该业态类型各档位的实际投放量：客户数 * 档位投放量
        for (int j = 0; j < 30; j++) {
            if (businessFormatCustomerData[j] != null && allocationArray[j] != null) {
                BigDecimal customerCount = businessFormatCustomerData[j];
                BigDecimal allocation = allocationArray[j];
                BigDecimal segmentActual = customerCount.multiply(allocation);
                businessFormatActualAmount = businessFormatActualAmount.add(segmentActual);
                
                if (segmentActual.compareTo(BigDecimal.ZERO) > 0) {
                    log.debug("业态类型档位 {} - 客户数: {}, 分配量: {}, 实际投放量: {}", 
                        (30-j), customerCount, allocation, segmentActual);
                }
            }
        }
        
        return businessFormatActualAmount;
    }
}
