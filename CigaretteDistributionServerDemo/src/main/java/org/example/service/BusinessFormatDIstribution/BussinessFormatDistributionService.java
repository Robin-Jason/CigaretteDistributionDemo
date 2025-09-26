package org.example.service.BusinessFormatDIstribution;

import lombok.extern.slf4j.Slf4j;

import org.example.entity.DemoTestData;
import org.example.repository.DemoTestDataRepository;
import org.example.service.CommonService;
//import org.example.service.DistributionCalculateService;
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
    private DemoTestDataRepository testDataRepository;
    
    @Autowired
    private BussinessFormatDistributionAlgorithm distributionAlgorithm;
    
    
    @Autowired
    private org.example.util.KmpMatcher kmpMatcher;
    
    @Autowired
    private CommonService commonService;
    
//    @Autowired
//    private DistributionCalculateService distributionCalculateService;
    
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
            log.info("初始化业态类型列表(通用服务)");
            allBusinessFormatList = commonService.getAllRegionList("按档位扩展投放", "档位+业态")
                    .stream()
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
    //这个应该可以删掉
//    @Cacheable("businessFormatCustomerMatrix")
//    public BigDecimal[][] getBusinessFormatCustomerMatrix() {
//        if (businessFormatCustomerMatrix == null) {
//            log.info("初始化业态类型客户数矩阵(通用服务)");
//            CommonService.RegionCustomerMatrix matrix = commonService.buildRegionCustomerMatrix("按档位扩展投放", "档位+业态");
//            List<BigDecimal[]> rows = matrix.getCustomerMatrix();
//            businessFormatCustomerMatrix = new BigDecimal[rows.size()][30];
//            for (int i = 0; i < rows.size(); i++) {
//                BigDecimal[] row = rows.get(i);
//                System.arraycopy(row, 0, businessFormatCustomerMatrix[i], 0, 30);
//            }
//            // 同步缓存的名称列表
//            this.allBusinessFormatList = matrix.getRegionNames();
//            log.info("业态类型客户数矩阵初始化完成，矩阵大小: {}x30", rows.size());
//        }
//        return businessFormatCustomerMatrix;
//    }
    
    /**
     * 获取卷烟的待投放业态类型列表
     * @param deliveryArea 投放区域字段值
     * @return 匹配成功的业态类型列表
     */
    //这里应该修改一下调用getAdvDataByDeliveryType
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
        // 使用通用服务获取完整矩阵和区域名，用于筛选
        CommonService.RegionCustomerMatrix fullMatrix = commonService.buildRegionCustomerMatrix("按档位扩展投放", "档位+业态");
        List<String> allFormats = fullMatrix.getRegionNames();
        List<BigDecimal[]> rows = fullMatrix.getCustomerMatrix();
        
        BigDecimal[][] targetMatrix = new BigDecimal[targetBusinessFormats.size()][30];
        for (int i = 0; i < targetBusinessFormats.size(); i++) {
            String fmt = targetBusinessFormats.get(i);
            int idx = allFormats.indexOf(fmt);
            if (idx >= 0) {
                BigDecimal[] row = rows.get(idx);
                System.arraycopy(row, 0, targetMatrix[i], 0, 30);
            } else {
                targetMatrix[i] = new BigDecimal[30];
                for (int g = 0; g < 30; g++) targetMatrix[i][g] = BigDecimal.ZERO;
            }
        }
        return distributionAlgorithm.calculateDistribution(targetBusinessFormats, targetMatrix, targetAmount);
    }
    
    /**
     * 根据算法需求描述计算卷烟实际投放量（业态类型版本）
     * 实际投放量 = 所有目标业态类型的客户数 * 档位投放量 的求和
     * S = Σ(i=1 to B) Σ(j=1 to G) x_{ij} * c_{ij}
     * 其中 x_{ij} 是分配给业态类型 i 档位 j 的卷烟数量，c_{ij} 是该业态类型该档位的客户数
     */
//    public BigDecimal calculateActualDistributionAmount(String cigCode, String cigName,
//                                                       Integer year, Integer month, Integer weekSeq) {
//        try {
//            log.debug("开始计算业态类型卷烟实际投放量，卷烟代码: {}, 卷烟名称: {}, 日期: {}-{}-{}",
//                cigCode, cigName, year, month, weekSeq);
//
//            // 1. 获取该卷烟在指定日期的所有投放区域记录
//            List<DemoTestData> cigaretteData = testDataRepository.findByCigCodeAndCigNameAndYearAndMonthAndWeekSeq(
//                cigCode, cigName, year, month, weekSeq);
//
//            if (cigaretteData.isEmpty()) {
//                log.warn("未找到业态类型卷烟数据，卷烟代码: {}, 卷烟名称: {}, 日期: {}-{}-{}",
//                    cigCode, cigName, year, month, weekSeq);
//                return BigDecimal.ZERO;
//            }
//
//            // 2. 计算所有目标投放业态类型的实际投放量总和（委托通用计算服务）
//            BigDecimal totalActualAmount = BigDecimal.ZERO;
//
//            for (DemoTestData data : cigaretteData) {
//                String deliveryArea = data.getDeliveryArea();
//                if (deliveryArea == null || deliveryArea.trim().isEmpty()) {
//                    continue;
//                }
//
//                // 组装该记录的30档位投放数组
//                BigDecimal[] allocationArray = {
//                    data.getD30() != null ? data.getD30() : BigDecimal.ZERO,
//                    data.getD29() != null ? data.getD29() : BigDecimal.ZERO,
//                    data.getD28() != null ? data.getD28() : BigDecimal.ZERO,
//                    data.getD27() != null ? data.getD27() : BigDecimal.ZERO,
//                    data.getD26() != null ? data.getD26() : BigDecimal.ZERO,
//                    data.getD25() != null ? data.getD25() : BigDecimal.ZERO,
//                    data.getD24() != null ? data.getD24() : BigDecimal.ZERO,
//                    data.getD23() != null ? data.getD23() : BigDecimal.ZERO,
//                    data.getD22() != null ? data.getD22() : BigDecimal.ZERO,
//                    data.getD21() != null ? data.getD21() : BigDecimal.ZERO,
//                    data.getD20() != null ? data.getD20() : BigDecimal.ZERO,
//                    data.getD19() != null ? data.getD19() : BigDecimal.ZERO,
//                    data.getD18() != null ? data.getD18() : BigDecimal.ZERO,
//                    data.getD17() != null ? data.getD17() : BigDecimal.ZERO,
//                    data.getD16() != null ? data.getD16() : BigDecimal.ZERO,
//                    data.getD15() != null ? data.getD15() : BigDecimal.ZERO,
//                    data.getD14() != null ? data.getD14() : BigDecimal.ZERO,
//                    data.getD13() != null ? data.getD13() : BigDecimal.ZERO,
//                    data.getD12() != null ? data.getD12() : BigDecimal.ZERO,
//                    data.getD11() != null ? data.getD11() : BigDecimal.ZERO,
//                    data.getD10() != null ? data.getD10() : BigDecimal.ZERO,
//                    data.getD9() != null ? data.getD9() : BigDecimal.ZERO,
//                    data.getD8() != null ? data.getD8() : BigDecimal.ZERO,
//                    data.getD7() != null ? data.getD7() : BigDecimal.ZERO,
//                    data.getD6() != null ? data.getD6() : BigDecimal.ZERO,
//                    data.getD5() != null ? data.getD5() : BigDecimal.ZERO,
//                    data.getD4() != null ? data.getD4() : BigDecimal.ZERO,
//                    data.getD3() != null ? data.getD3() : BigDecimal.ZERO,
//                    data.getD2() != null ? data.getD2() : BigDecimal.ZERO,
//                    data.getD1() != null ? data.getD1() : BigDecimal.ZERO
//                };
//
//                BigDecimal businessFormatActualAmount = distributionCalculateService
//                        .calculateActualDeliveryForRegion(deliveryArea, allocationArray, "档位+业态");
//
//                log.debug("业态类型 {} 的实际投放量: {}", deliveryArea, businessFormatActualAmount);
//                totalActualAmount = totalActualAmount.add(businessFormatActualAmount);
//            }
//
//            log.debug("业态类型卷烟 {} 的总实际投放量: {}", cigName, totalActualAmount);
//            return totalActualAmount;
//
//        } catch (Exception e) {
//            log.error("计算业态类型实际投放量失败，卷烟代码: {}, 卷烟名称: {}, 错误: {}",
//                cigCode, cigName, e.getMessage(), e);
//            return BigDecimal.ZERO;
//        }
//    }
    
    /**
     * 为单个记录计算实际投放量
     */
//    public BigDecimal calculateActualAmountForRecord(DemoTestData data) {
//        try {
//            return calculateActualDistributionAmount(data.getCigCode(), data.getCigName(),
//                data.getYear(), data.getMonth(), data.getWeekSeq());
//        } catch (Exception e) {
//            log.error("计算业态类型实际投放量失败，卷烟代码: {}, 卷烟名称: {}, 错误: {}",
//                data.getCigCode(), data.getCigName(), e.getMessage());
//            return BigDecimal.ZERO;
//        }
//    }
    
    // ==================== 私有辅助方法 ====================
}
