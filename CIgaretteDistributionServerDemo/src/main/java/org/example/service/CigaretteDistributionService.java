package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.entity.DemoTestAdvData;
import org.example.entity.DemoTestClientNumData;
import org.example.entity.DemoTestData;
import org.example.entity.DemoTestBusinessFormatClientNumData;
import org.example.repository.DemoTestAdvDataRepository;
import org.example.repository.DemoTestClientNumDataRepository;
import org.example.repository.DemoTestDataRepository;
import org.example.repository.DemoTestBusinessFormatClientNumDataRepository;
import org.example.service.algorithm.CigaretteDistributionAlgorithm;
import org.example.util.KmpMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import org.example.dto.UpdateCigaretteRequestDto;
import org.example.dto.DeleteAreasRequestDto;

@Slf4j
@Service
public class CigaretteDistributionService {
    
    @Autowired
    private DemoTestAdvDataRepository advDataRepository;
    
    @Autowired
    private DemoTestClientNumDataRepository clientNumDataRepository;
    
    @Autowired
    private DemoTestBusinessFormatClientNumDataRepository businessFormatClientNumDataRepository;
    
    @Autowired
    private DemoTestDataRepository testDataRepository;
    
    @Autowired
    private CigaretteDistributionAlgorithm distributionAlgorithm;
    
    @Autowired
    private KmpMatcher kmpMatcher;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    // 缓存投放区域列表
    private List<String> allRegionList;
    
    // 缓存区域客户数矩阵
    private BigDecimal[][] regionCustomerMatrix;
    
    // 缓存业态类型列表
    private List<String> allBusinessFormatList;
    
    // 缓存业态类型客户数矩阵
    private BigDecimal[][] businessFormatCustomerMatrix;
    
    /**
     * 获取JdbcTemplate实例
     */
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }
    
    /**
     * 获取KmpMatcher实例
     */
    public KmpMatcher getKmpMatcher() {
        return kmpMatcher;
    }
    
    /**
     * 获取卷烟代码、名称和预投放量
     */
    @Cacheable("advData")
    public List<DemoTestAdvData> getAdvData() {
        log.info("获取预投放量数据");
        return advDataRepository.findAll();
    }
    
    /**
     * 获取所有投放区域列表（按id从小到大排序去重）
     */
    @Cacheable("allRegionList")
    public List<String> getAllRegionList() {
        if (allRegionList == null) {
            log.info("初始化投放区域列表");
            allRegionList = clientNumDataRepository.findAllByOrderByIdAsc()
                    .stream()
                    .map(DemoTestClientNumData::getUrbanRuralCode)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            log.info("投放区域列表初始化完成，共{}个区域", allRegionList.size());
        }
        return allRegionList;
    }
    
    /**
     * 获取所有业态类型列表（按id从小到大排序去重）
     */
    @Cacheable("allBusinessFormatList")
    public List<String> getAllBusinessFormatList() {
        if (allBusinessFormatList == null) {
            log.info("初始化业态类型列表");
            allBusinessFormatList = businessFormatClientNumDataRepository.findAllByOrderByIdAsc()
                    .stream()
                    .map(DemoTestBusinessFormatClientNumData::getBusinessFormatCode)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            log.info("业态类型列表初始化完成，共{}个业态类型", allBusinessFormatList.size());
        }
        return allBusinessFormatList;
    }
    
    /**
     * 获取区域客户数矩阵
     */
    @Cacheable("regionCustomerMatrix")
    public BigDecimal[][] getRegionCustomerMatrix() {
        if (regionCustomerMatrix == null) {
            log.info("初始化区域客户数矩阵");
            List<DemoTestClientNumData> clientNumDataList = clientNumDataRepository.findAllByOrderByIdAsc();
            List<String> regions = getAllRegionList();
            
            regionCustomerMatrix = new BigDecimal[regions.size()][30];
            
            for (int i = 0; i < regions.size(); i++) {
                String region = regions.get(i);
                DemoTestClientNumData clientData = clientNumDataList.stream()
                        .filter(data -> region.equals(data.getUrbanRuralCode()))
                        .findFirst()
                        .orElse(null);
                
                if (clientData != null) {
                    regionCustomerMatrix[i][0] = clientData.getD30();
                    regionCustomerMatrix[i][1] = clientData.getD29();
                    regionCustomerMatrix[i][2] = clientData.getD28();
                    regionCustomerMatrix[i][3] = clientData.getD27();
                    regionCustomerMatrix[i][4] = clientData.getD26();
                    regionCustomerMatrix[i][5] = clientData.getD25();
                    regionCustomerMatrix[i][6] = clientData.getD24();
                    regionCustomerMatrix[i][7] = clientData.getD23();
                    regionCustomerMatrix[i][8] = clientData.getD22();
                    regionCustomerMatrix[i][9] = clientData.getD21();
                    regionCustomerMatrix[i][10] = clientData.getD20();
                    regionCustomerMatrix[i][11] = clientData.getD19();
                    regionCustomerMatrix[i][12] = clientData.getD18();
                    regionCustomerMatrix[i][13] = clientData.getD17();
                    regionCustomerMatrix[i][14] = clientData.getD16();
                    regionCustomerMatrix[i][15] = clientData.getD15();
                    regionCustomerMatrix[i][16] = clientData.getD14();
                    regionCustomerMatrix[i][17] = clientData.getD13();
                    regionCustomerMatrix[i][18] = clientData.getD12();
                    regionCustomerMatrix[i][19] = clientData.getD11();
                    regionCustomerMatrix[i][20] = clientData.getD10();
                    regionCustomerMatrix[i][21] = clientData.getD9();
                    regionCustomerMatrix[i][22] = clientData.getD8();
                    regionCustomerMatrix[i][23] = clientData.getD7();
                    regionCustomerMatrix[i][24] = clientData.getD6();
                    regionCustomerMatrix[i][25] = clientData.getD5();
                    regionCustomerMatrix[i][26] = clientData.getD4();
                    regionCustomerMatrix[i][27] = clientData.getD3();
                    regionCustomerMatrix[i][28] = clientData.getD2();
                    regionCustomerMatrix[i][29] = clientData.getD1();
                }
            }
            log.info("区域客户数矩阵初始化完成，矩阵大小: {}x30", regions.size());
        }
        return regionCustomerMatrix;
    }
    
    /**
     * 获取业态类型客户数矩阵
     */
    @Cacheable("businessFormatCustomerMatrix")
    public BigDecimal[][] getBusinessFormatCustomerMatrix() {
        if (businessFormatCustomerMatrix == null) {
            log.info("初始化业态类型客户数矩阵");
            List<DemoTestBusinessFormatClientNumData> businessFormatClientNumDataList = businessFormatClientNumDataRepository.findAllByOrderByIdAsc();
            List<String> businessFormats = getAllBusinessFormatList();
            
            businessFormatCustomerMatrix = new BigDecimal[businessFormats.size()][30];
            
            for (int i = 0; i < businessFormats.size(); i++) {
                String businessFormat = businessFormats.get(i);
                DemoTestBusinessFormatClientNumData clientData = businessFormatClientNumDataList.stream()
                        .filter(data -> businessFormat.equals(data.getBusinessFormatCode()))
                        .findFirst()
                        .orElse(null);
                
                if (clientData != null) {
                    businessFormatCustomerMatrix[i][0] = clientData.getD30();
                    businessFormatCustomerMatrix[i][1] = clientData.getD29();
                    businessFormatCustomerMatrix[i][2] = clientData.getD28();
                    businessFormatCustomerMatrix[i][3] = clientData.getD27();
                    businessFormatCustomerMatrix[i][4] = clientData.getD26();
                    businessFormatCustomerMatrix[i][5] = clientData.getD25();
                    businessFormatCustomerMatrix[i][6] = clientData.getD24();
                    businessFormatCustomerMatrix[i][7] = clientData.getD23();
                    businessFormatCustomerMatrix[i][8] = clientData.getD22();
                    businessFormatCustomerMatrix[i][9] = clientData.getD21();
                    businessFormatCustomerMatrix[i][10] = clientData.getD20();
                    businessFormatCustomerMatrix[i][11] = clientData.getD19();
                    businessFormatCustomerMatrix[i][12] = clientData.getD18();
                    businessFormatCustomerMatrix[i][13] = clientData.getD17();
                    businessFormatCustomerMatrix[i][14] = clientData.getD16();
                    businessFormatCustomerMatrix[i][15] = clientData.getD15();
                    businessFormatCustomerMatrix[i][16] = clientData.getD14();
                    businessFormatCustomerMatrix[i][17] = clientData.getD13();
                    businessFormatCustomerMatrix[i][18] = clientData.getD12();
                    businessFormatCustomerMatrix[i][19] = clientData.getD11();
                    businessFormatCustomerMatrix[i][20] = clientData.getD10();
                    businessFormatCustomerMatrix[i][21] = clientData.getD9();
                    businessFormatCustomerMatrix[i][22] = clientData.getD8();
                    businessFormatCustomerMatrix[i][23] = clientData.getD7();
                    businessFormatCustomerMatrix[i][24] = clientData.getD6();
                    businessFormatCustomerMatrix[i][25] = clientData.getD5();
                    businessFormatCustomerMatrix[i][26] = clientData.getD4();
                    businessFormatCustomerMatrix[i][27] = clientData.getD3();
                    businessFormatCustomerMatrix[i][28] = clientData.getD2();
                    businessFormatCustomerMatrix[i][29] = clientData.getD1();
                }
            }
            log.info("业态类型客户数矩阵初始化完成，矩阵大小: {}x30", businessFormats.size());
        }
        return businessFormatCustomerMatrix;
    }
    
    /**
     * 获取卷烟的待投放区域列表
     * @param deliveryArea 投放区域字段值
     * @return 匹配成功的投放区域列表
     */
    public List<String> getTargetRegionList(String deliveryArea) {
        log.info("获取卷烟投放区域，投放区域字段值: {}", deliveryArea);
        List<String> allRegions = getAllRegionList();
        List<String> targetRegions = kmpMatcher.matchPatterns(deliveryArea, allRegions);
        log.info("匹配成功的投放区域: {}", targetRegions);
        return targetRegions;
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
     * 计算卷烟分配矩阵
     * @param targetRegions 目标投放区域列表
     * @param targetAmount 预投放量
     * @return 分配矩阵 [区域数][档位数]
     */
    public BigDecimal[][] calculateDistributionMatrix(List<String> targetRegions, BigDecimal targetAmount) {
        log.info("计算卷烟分配矩阵，目标区域数: {}, 预投放量: {}", targetRegions.size(), targetAmount);
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
     * 根据年份、月份、周序号查询卷烟分配数据
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> queryCigaretteDistribution(Integer year, Integer month, Integer weekSeq) {
        log.info("查询卷烟分配数据，年份: {}, 月份: {}, 周序号: {}", year, month, weekSeq);
        
        List<DemoTestData> dataList = testDataRepository.findByYearAndMonthAndWeekSeq(year, month, weekSeq);
        List<DemoTestAdvData> advDataList = getAdvData();
        
        // 按卷烟代码和名称分组
        Map<String, List<DemoTestData>> groupedData = dataList.stream()
                .collect(Collectors.groupingBy(data -> data.getCigCode() + "_" + data.getCigName()));
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Map.Entry<String, List<DemoTestData>> entry : groupedData.entrySet()) {
            List<DemoTestData> groupData = entry.getValue();
            if (groupData.isEmpty()) continue;
            
            DemoTestData firstData = groupData.get(0);
            
            // 查找对应的预投放量、投放方式、扩展投放方式
            DemoTestAdvData matchedAdvData = advDataList.stream()
                    .filter(advData -> advData.getCigCode().equals(firstData.getCigCode()) &&
                                     advData.getCigName().equals(firstData.getCigName()))
                    .findFirst()
                    .orElse(null);
            
            BigDecimal adv = matchedAdvData != null ? matchedAdvData.getAdv() : BigDecimal.ZERO;
            String deliveryMethod = matchedAdvData != null ? matchedAdvData.getDeliveryMethod() : "";
            String deliveryEtype = matchedAdvData != null ? matchedAdvData.getDeliveryEtype() : "";
            
            // 合并投放区域
            String deliveryAreas = groupData.stream()
                    .map(DemoTestData::getDeliveryArea)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.joining("，"));
            
            // 计算实际投放量
            BigDecimal actualDelivery = BigDecimal.ZERO;
            for (DemoTestData data : groupData) {
                actualDelivery = actualDelivery.add(data.getD30() != null ? data.getD30() : BigDecimal.ZERO);
                actualDelivery = actualDelivery.add(data.getD29() != null ? data.getD29() : BigDecimal.ZERO);
                actualDelivery = actualDelivery.add(data.getD28() != null ? data.getD28() : BigDecimal.ZERO);
                actualDelivery = actualDelivery.add(data.getD27() != null ? data.getD27() : BigDecimal.ZERO);
                actualDelivery = actualDelivery.add(data.getD26() != null ? data.getD26() : BigDecimal.ZERO);
                actualDelivery = actualDelivery.add(data.getD25() != null ? data.getD25() : BigDecimal.ZERO);
                actualDelivery = actualDelivery.add(data.getD24() != null ? data.getD24() : BigDecimal.ZERO);
                actualDelivery = actualDelivery.add(data.getD23() != null ? data.getD23() : BigDecimal.ZERO);
                actualDelivery = actualDelivery.add(data.getD22() != null ? data.getD22() : BigDecimal.ZERO);
                actualDelivery = actualDelivery.add(data.getD21() != null ? data.getD21() : BigDecimal.ZERO);
                actualDelivery = actualDelivery.add(data.getD20() != null ? data.getD20() : BigDecimal.ZERO);
                actualDelivery = actualDelivery.add(data.getD19() != null ? data.getD19() : BigDecimal.ZERO);
                actualDelivery = actualDelivery.add(data.getD18() != null ? data.getD18() : BigDecimal.ZERO);
                actualDelivery = actualDelivery.add(data.getD17() != null ? data.getD17() : BigDecimal.ZERO);
                actualDelivery = actualDelivery.add(data.getD16() != null ? data.getD16() : BigDecimal.ZERO);
                actualDelivery = actualDelivery.add(data.getD15() != null ? data.getD15() : BigDecimal.ZERO);
                actualDelivery = actualDelivery.add(data.getD14() != null ? data.getD14() : BigDecimal.ZERO);
                actualDelivery = actualDelivery.add(data.getD13() != null ? data.getD13() : BigDecimal.ZERO);
                actualDelivery = actualDelivery.add(data.getD12() != null ? data.getD12() : BigDecimal.ZERO);
                actualDelivery = actualDelivery.add(data.getD11() != null ? data.getD11() : BigDecimal.ZERO);
                actualDelivery = actualDelivery.add(data.getD10() != null ? data.getD10() : BigDecimal.ZERO);
                actualDelivery = actualDelivery.add(data.getD9() != null ? data.getD9() : BigDecimal.ZERO);
                actualDelivery = actualDelivery.add(data.getD8() != null ? data.getD8() : BigDecimal.ZERO);
                actualDelivery = actualDelivery.add(data.getD7() != null ? data.getD7() : BigDecimal.ZERO);
                actualDelivery = actualDelivery.add(data.getD6() != null ? data.getD6() : BigDecimal.ZERO);
                actualDelivery = actualDelivery.add(data.getD5() != null ? data.getD5() : BigDecimal.ZERO);
                actualDelivery = actualDelivery.add(data.getD4() != null ? data.getD4() : BigDecimal.ZERO);
                actualDelivery = actualDelivery.add(data.getD3() != null ? data.getD3() : BigDecimal.ZERO);
                actualDelivery = actualDelivery.add(data.getD2() != null ? data.getD2() : BigDecimal.ZERO);
                actualDelivery = actualDelivery.add(data.getD1() != null ? data.getD1() : BigDecimal.ZERO);
            }
            
            Map<String, Object> resultItem = new HashMap<>();
            resultItem.put("cigCode", firstData.getCigCode());
            resultItem.put("cigName", firstData.getCigName());
            resultItem.put("deliveryMethod", deliveryMethod);
            resultItem.put("deliveryEtype", deliveryEtype);
            resultItem.put("adv", adv);
            resultItem.put("actualDelivery", actualDelivery);
            resultItem.put("d30", firstData.getD30());
            resultItem.put("d29", firstData.getD29());
            resultItem.put("d28", firstData.getD28());
            resultItem.put("d27", firstData.getD27());
            resultItem.put("d26", firstData.getD26());
            resultItem.put("d25", firstData.getD25());
            resultItem.put("d24", firstData.getD24());
            resultItem.put("d23", firstData.getD23());
            resultItem.put("d22", firstData.getD22());
            resultItem.put("d21", firstData.getD21());
            resultItem.put("d20", firstData.getD20());
            resultItem.put("d19", firstData.getD19());
            resultItem.put("d18", firstData.getD18());
            resultItem.put("d17", firstData.getD17());
            resultItem.put("d16", firstData.getD16());
            resultItem.put("d15", firstData.getD15());
            resultItem.put("d14", firstData.getD14());
            resultItem.put("d13", firstData.getD13());
            resultItem.put("d12", firstData.getD12());
            resultItem.put("d11", firstData.getD11());
            resultItem.put("d10", firstData.getD10());
            resultItem.put("d9", firstData.getD9());
            resultItem.put("d8", firstData.getD8());
            resultItem.put("d7", firstData.getD7());
            resultItem.put("d6", firstData.getD6());
            resultItem.put("d5", firstData.getD5());
            resultItem.put("d4", firstData.getD4());
            resultItem.put("d3", firstData.getD3());
            resultItem.put("d2", firstData.getD2());
            resultItem.put("d1", firstData.getD1());
            resultItem.put("deliveryAreas", deliveryAreas);
            resultItem.put("remark", firstData.getBz());
            
            result.add(resultItem);
        }
        
        log.info("查询完成，共返回{}条记录", result.size());
        return result;
    }
    
    /**
     * 更新卷烟分配数据
     */
    @Transactional
    public boolean updateCigaretteDistribution(String cigCode, String cigName, List<BigDecimal> distribution) {
        log.info("更新卷烟分配数据，卷烟代码: {}, 卷烟名称: {}", cigCode, cigName);
        
        List<DemoTestData> dataList = testDataRepository.findByCigCodeAndCigName(cigCode, cigName);
        if (dataList.isEmpty()) {
            log.warn("未找到对应的卷烟数据");
            return false;
        }
        
        try {
            for (DemoTestData data : dataList) {
                if (distribution.size() >= 30) {
                    data.setD30(distribution.get(0));
                    data.setD29(distribution.get(1));
                    data.setD28(distribution.get(2));
                    data.setD27(distribution.get(3));
                    data.setD26(distribution.get(4));
                    data.setD25(distribution.get(5));
                    data.setD24(distribution.get(6));
                    data.setD23(distribution.get(7));
                    data.setD22(distribution.get(8));
                    data.setD21(distribution.get(9));
                    data.setD20(distribution.get(10));
                    data.setD19(distribution.get(11));
                    data.setD18(distribution.get(12));
                    data.setD17(distribution.get(13));
                    data.setD16(distribution.get(14));
                    data.setD15(distribution.get(15));
                    data.setD14(distribution.get(16));
                    data.setD13(distribution.get(17));
                    data.setD12(distribution.get(18));
                    data.setD11(distribution.get(19));
                    data.setD10(distribution.get(20));
                    data.setD9(distribution.get(21));
                    data.setD8(distribution.get(22));
                    data.setD7(distribution.get(23));
                    data.setD6(distribution.get(24));
                    data.setD5(distribution.get(25));
                    data.setD4(distribution.get(26));
                    data.setD3(distribution.get(27));
                    data.setD2(distribution.get(28));
                    data.setD1(distribution.get(29));
                }
            }
            
            testDataRepository.saveAll(dataList);
                    log.info("卷烟分配数据更新成功");
        return true;
        
    } catch (Exception e) {
        log.error("更新卷烟分配数据失败", e);
        return false;
    }
}
    
    /**
     * 更新卷烟分配数据 - 将复合区域记录拆分后写回数据库
     */
    @Transactional
    public boolean updateCigaretteDistributionWithSplit(String cigCode, String cigName, 
                                                       List<BigDecimal> distribution,
                                                       String deliveryAreas, 
                                                       Integer year, Integer month, Integer weekSeq) {
        log.info("更新卷烟分配数据并拆分区域，卷烟代码: {}, 卷烟名称: {}, 投放区域: {}", 
                cigCode, cigName, deliveryAreas);
        
        try {
            // 先删除原有的相关记录
            List<DemoTestData> existingData = testDataRepository.findByCigCodeAndCigNameAndYearAndMonthAndWeekSeq(
                cigCode, cigName, year, month, weekSeq);
            
            if (!existingData.isEmpty()) {
                testDataRepository.deleteAll(existingData);
                log.info("删除原有记录{}条", existingData.size());
            }
            
            // 拆分投放区域
            String[] regions = deliveryAreas.split(",");
            
            // 为每个区域创建新记录
            for (String region : regions) {
                String trimmedRegion = region.trim();
                if (trimmedRegion.isEmpty()) continue;
                
                DemoTestData newData = new DemoTestData();
                newData.setCigCode(cigCode);
                newData.setCigName(cigName);
                newData.setDeliveryArea(trimmedRegion);
                newData.setYear(year);
                newData.setMonth(month);
                newData.setWeekSeq(weekSeq);
                newData.setBz("前端更新");
                
                // 设置档位分配值
                if (distribution.size() >= 30) {
                    newData.setD30(distribution.get(0));
                    newData.setD29(distribution.get(1));
                    newData.setD28(distribution.get(2));
                    newData.setD27(distribution.get(3));
                    newData.setD26(distribution.get(4));
                    newData.setD25(distribution.get(5));
                    newData.setD24(distribution.get(6));
                    newData.setD23(distribution.get(7));
                    newData.setD22(distribution.get(8));
                    newData.setD21(distribution.get(9));
                    newData.setD20(distribution.get(10));
                    newData.setD19(distribution.get(11));
                    newData.setD18(distribution.get(12));
                    newData.setD17(distribution.get(13));
                    newData.setD16(distribution.get(14));
                    newData.setD15(distribution.get(15));
                    newData.setD14(distribution.get(16));
                    newData.setD13(distribution.get(17));
                    newData.setD12(distribution.get(18));
                    newData.setD11(distribution.get(19));
                    newData.setD10(distribution.get(20));
                    newData.setD9(distribution.get(21));
                    newData.setD8(distribution.get(22));
                    newData.setD7(distribution.get(23));
                    newData.setD6(distribution.get(24));
                    newData.setD5(distribution.get(25));
                    newData.setD4(distribution.get(26));
                    newData.setD3(distribution.get(27));
                    newData.setD2(distribution.get(28));
                    newData.setD1(distribution.get(29));
                }
                
                // 保存到数据库
                testDataRepository.save(newData);
                log.debug("区域 {} 的记录已保存", trimmedRegion);
            }
            
            log.info("卷烟 {} 的分配数据更新成功，拆分为{}个区域", cigName, regions.length);
            return true;
            
        } catch (Exception e) {
            log.error("更新卷烟分配数据失败，卷烟: {}, 错误: {}", cigName, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 测试分配算法（用于调试）
     */
    public void testDistributionAlgorithm() {
        log.info("=== 开始测试分配算法 ===");
        
        try {
            // 获取预投放量数据
            List<DemoTestAdvData> advDataList = getAdvData();
            log.info("预投放量数据数量: {}", advDataList.size());
            
            // 获取投放区域列表
            List<String> allRegions = getAllRegionList();
            log.info("投放区域数量: {}", allRegions.size());
            
            // 获取区域客户数矩阵
            BigDecimal[][] regionCustomerMatrix = getRegionCustomerMatrix();
            log.info("区域客户数矩阵大小: {}x30", regionCustomerMatrix.length);
            
            // 测试每个卷烟的分配
            for (DemoTestAdvData advData : advDataList) {
                log.info("\n--- 测试卷烟: {} ({}) ---", advData.getCigName(), advData.getCigCode());
                log.info("预投放量: {}", advData.getAdv());
                
                // 找到对应的测试数据
                // 直接从advData获取投放区域
                String deliveryArea = advData.getDeliveryArea();
                log.info("投放区域字段: {}", deliveryArea);
                
                if (deliveryArea != null && !deliveryArea.trim().isEmpty()) {
                    // 获取目标投放区域
                    List<String> targetRegions = getTargetRegionList(deliveryArea);
                    log.info("匹配到的投放区域: {}", targetRegions);
                    
                    if (!targetRegions.isEmpty()) {
                        // 根据目标区域筛选客户数矩阵
                        BigDecimal[][] targetRegionCustomerMatrix = new BigDecimal[targetRegions.size()][30];
                        for (int i = 0; i < targetRegions.size(); i++) {
                            String targetRegion = targetRegions.get(i);
                            int regionIndex = allRegions.indexOf(targetRegion);
                            if (regionIndex >= 0) {
                                System.arraycopy(regionCustomerMatrix[regionIndex], 0, targetRegionCustomerMatrix[i], 0, 30);
                            }
                        }
                        
                        // 执行分配算法
                        BigDecimal[][] allocationMatrix = distributionAlgorithm.calculateDistribution(
                            targetRegions, targetRegionCustomerMatrix, advData.getAdv());
                        
                        // 输出分配结果
                        logAllocationResult(allocationMatrix, targetRegions, targetRegionCustomerMatrix, advData.getAdv());
                    }
                }
            }
            
            log.info("=== 分配算法测试完成 ===");
            
        } catch (Exception e) {
            log.error("测试分配算法时发生错误", e);
        }
    }
    
    /**
     * 输出分配结果到日志
     */
    private void logAllocationResult(BigDecimal[][] allocationMatrix, 
                                   List<String> targetRegions,
                                   BigDecimal[][] regionCustomerMatrix,
                                   BigDecimal targetAmount) {
        log.info("分配矩阵结果:");
        
        // 计算实际投放量
        BigDecimal actualAmount = BigDecimal.ZERO;
        for (int i = 0; i < targetRegions.size(); i++) {
            for (int j = 0; j < 30; j++) {
                if (allocationMatrix[i][j] != null && regionCustomerMatrix[i][j] != null) {
                    actualAmount = actualAmount.add(allocationMatrix[i][j].multiply(regionCustomerMatrix[i][j]));
                }
            }
        }
        
        log.info("目标投放量: {}", targetAmount);
        log.info("实际投放量: {}", actualAmount);
        log.info("误差: {}", targetAmount.subtract(actualAmount).abs());
        
        // 验证非递增约束
        boolean isValid = validateNonIncreasingConstraint(allocationMatrix);
        log.info("非递增约束验证: {}", isValid ? "通过" : "失败");
        
        // 输出前几个档位的分配值
        for (int i = 0; i < targetRegions.size(); i++) {
            StringBuilder sb = new StringBuilder();
            sb.append("区域 ").append(targetRegions.get(i)).append(": ");
            for (int j = 0; j < Math.min(10, 30); j++) {
                sb.append("D").append(30-j).append("=").append(allocationMatrix[i][j]).append(" ");
            }
            log.info(sb.toString());
        }
    }
    
    /**
     * 验证非递增约束
     */
    private boolean validateNonIncreasingConstraint(BigDecimal[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 1; j < matrix[i].length; j++) {
                if (matrix[i][j-1] != null && matrix[i][j] != null) {
                    if (matrix[i][j-1].compareTo(matrix[i][j]) < 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * 将算法输出的分配矩阵写回数据库demo_test_data表（支持业态类型）
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @return 写回结果
     */
    public Map<String, Object> writeBackAllocationMatrix(Integer year, Integer month, Integer weekSeq) {
        log.info("开始将分配矩阵写回数据库，年份: {}, 月份: {}, 周序号: {}", year, month, weekSeq);
        
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> writeBackResults = new ArrayList<>();
        
        try {
            // 使用JdbcTemplate直接获取预投放量数据
            String advDataSql = "SELECT cig_code, cig_name, adv, delivery_area, delivery_method, delivery_etype FROM demo_test_advdata";
            List<Map<String, Object>> advDataList = jdbcTemplate.queryForList(advDataSql);
            log.info("预投放量数据数量: {}", advDataList.size());
            
            // 获取业态类型数据（通过Repository，避免列名不一致问题）
            List<DemoTestBusinessFormatClientNumData> businessFormatEntities = businessFormatClientNumDataRepository.findAllByOrderByIdAsc();
            List<String> allBusinessFormats = businessFormatEntities.stream()
                .map(DemoTestBusinessFormatClientNumData::getBusinessFormatCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            log.info("业态类型数量: {}", allBusinessFormats.size());

            BigDecimal[][] businessFormatCustomerMatrix = new BigDecimal[businessFormatEntities.size()][30];
            for (int i = 0; i < businessFormatEntities.size(); i++) {
                DemoTestBusinessFormatClientNumData row = businessFormatEntities.get(i);
                businessFormatCustomerMatrix[i][0] = row.getD30();
                businessFormatCustomerMatrix[i][1] = row.getD29();
                businessFormatCustomerMatrix[i][2] = row.getD28();
                businessFormatCustomerMatrix[i][3] = row.getD27();
                businessFormatCustomerMatrix[i][4] = row.getD26();
                businessFormatCustomerMatrix[i][5] = row.getD25();
                businessFormatCustomerMatrix[i][6] = row.getD24();
                businessFormatCustomerMatrix[i][7] = row.getD23();
                businessFormatCustomerMatrix[i][8] = row.getD22();
                businessFormatCustomerMatrix[i][9] = row.getD21();
                businessFormatCustomerMatrix[i][10] = row.getD20();
                businessFormatCustomerMatrix[i][11] = row.getD19();
                businessFormatCustomerMatrix[i][12] = row.getD18();
                businessFormatCustomerMatrix[i][13] = row.getD17();
                businessFormatCustomerMatrix[i][14] = row.getD16();
                businessFormatCustomerMatrix[i][15] = row.getD15();
                businessFormatCustomerMatrix[i][16] = row.getD14();
                businessFormatCustomerMatrix[i][17] = row.getD13();
                businessFormatCustomerMatrix[i][18] = row.getD12();
                businessFormatCustomerMatrix[i][19] = row.getD11();
                businessFormatCustomerMatrix[i][20] = row.getD10();
                businessFormatCustomerMatrix[i][21] = row.getD9();
                businessFormatCustomerMatrix[i][22] = row.getD8();
                businessFormatCustomerMatrix[i][23] = row.getD7();
                businessFormatCustomerMatrix[i][24] = row.getD6();
                businessFormatCustomerMatrix[i][25] = row.getD5();
                businessFormatCustomerMatrix[i][26] = row.getD4();
                businessFormatCustomerMatrix[i][27] = row.getD3();
                businessFormatCustomerMatrix[i][28] = row.getD2();
                businessFormatCustomerMatrix[i][29] = row.getD1();
            }
            log.info("业态类型客户数矩阵大小: {}x30", businessFormatCustomerMatrix.length);
            
            // 使用JdbcTemplate直接获取投放区域列表
            String regionSql = "SELECT URBAN_RURAL_CODE FROM demo_test_clientNumdata ORDER BY id ASC";
            List<Map<String, Object>> regionData = jdbcTemplate.queryForList(regionSql);
            List<String> allRegions = regionData.stream()
                .map(row -> (String) row.get("URBAN_RURAL_CODE"))
                .collect(Collectors.toList());
            log.info("投放区域数量: {}", allRegions.size());
            
            // 使用JdbcTemplate直接获取区域客户数矩阵
            String clientDataSql = "SELECT URBAN_RURAL_CODE, D30, D29, D28, D27, D26, D25, D24, D23, D22, D21, D20, " +
                "D19, D18, D17, D16, D15, D14, D13, D12, D11, D10, D9, D8, D7, D6, D5, D4, D3, D2, D1, TOTAL " +
                "FROM demo_test_clientNumdata ORDER BY id ASC";
            
            List<Map<String, Object>> clientDataList = jdbcTemplate.queryForList(clientDataSql);
            BigDecimal[][] regionCustomerMatrix = new BigDecimal[clientDataList.size()][30];
            
            for (int i = 0; i < clientDataList.size(); i++) {
                Map<String, Object> row = clientDataList.get(i);
                
                // 填充D30到D1的数据
                for (int j = 0; j < 30; j++) {
                    String columnName = "D" + (30 - j);
                    Object value = row.get(columnName);
                    if (value instanceof BigDecimal) {
                        regionCustomerMatrix[i][j] = (BigDecimal) value;
                    } else if (value instanceof Number) {
                        regionCustomerMatrix[i][j] = new BigDecimal(value.toString());
                    } else {
                        regionCustomerMatrix[i][j] = BigDecimal.ZERO;
                    }
                }
            }
            log.info("区域客户数矩阵大小: {}x30", regionCustomerMatrix.length);
            
            int successCount = 0;
            int totalCount = 0;
            
            // 处理每个卷烟
            for (Map<String, Object> advData : advDataList) {
                totalCount++;
                String cigCode = (String) advData.get("cig_code");
                String cigName = (String) advData.get("cig_name");
                BigDecimal adv = (BigDecimal) advData.get("adv");
                
                Map<String, Object> cigResult = new HashMap<>();
                cigResult.put("cigCode", cigCode);
                cigResult.put("cigName", cigName);
                cigResult.put("adv", adv);
                
                try {
                    // 直接从advData获取投放区域和投放类型
                    String deliveryArea = (String) advData.get("delivery_area");
                    String deliveryEtype = (String) advData.get("delivery_etype");
                    
                    cigResult.put("deliveryArea", deliveryArea);
                    cigResult.put("deliveryEtype", deliveryEtype);
                    
                    if (deliveryArea != null && !deliveryArea.trim().isEmpty()) {
                        BigDecimal[][] allocationMatrix = null;
                        List<String> targetList = null;
                        String targetType = null;
                        
                        // 根据DELIVERY_ETYPE决定处理方式
                        if ("档位+业态类型".equals(deliveryEtype)) {
                            // 使用业态类型处理
                            targetList = kmpMatcher.matchPatterns(deliveryArea, allBusinessFormats);
                            cigResult.put("targetBusinessFormats", targetList);
                            targetType = "业态类型";
                            
                            if (!targetList.isEmpty()) {
                                // 根据目标业态类型筛选客户数矩阵
                                BigDecimal[][] targetBusinessFormatCustomerMatrix = new BigDecimal[targetList.size()][30];
                                for (int i = 0; i < targetList.size(); i++) {
                                    String targetBusinessFormat = targetList.get(i);
                                    int businessFormatIndex = allBusinessFormats.indexOf(targetBusinessFormat);
                                    if (businessFormatIndex >= 0) {
                                        System.arraycopy(businessFormatCustomerMatrix[businessFormatIndex], 0, targetBusinessFormatCustomerMatrix[i], 0, 30);
                                    }
                                }
                                
                                // 执行分配算法
                                allocationMatrix = distributionAlgorithm.calculateDistribution(
                                    targetList, targetBusinessFormatCustomerMatrix, adv);
                                
                                // 计算实际投放量
                                BigDecimal actualAmount = calculateActualAmount(allocationMatrix, targetBusinessFormatCustomerMatrix);
                                cigResult.put("actualAmount", actualAmount);
                                cigResult.put("error", adv.subtract(actualAmount).abs());
                                cigResult.put("errorPercentage", adv.subtract(actualAmount).abs()
                                    .divide(adv, 4, BigDecimal.ROUND_HALF_UP)
                                    .multiply(new BigDecimal("100")));
                            }
                        } else if("档位+城乡分类代码".equals(deliveryEtype)) {
                            // 使用城乡分类代码处理
                            targetList = kmpMatcher.matchPatterns(deliveryArea, allRegions);
                            cigResult.put("targetRegions", targetList);
                            targetType = "城乡分类代码";
                            
                            if (!targetList.isEmpty()) {
                                // 根据目标区域筛选客户数矩阵
                                BigDecimal[][] targetRegionCustomerMatrix = new BigDecimal[targetList.size()][30];
                                for (int i = 0; i < targetList.size(); i++) {
                                    String targetRegion = targetList.get(i);
                                    int regionIndex = allRegions.indexOf(targetRegion);
                                    if (regionIndex >= 0) {
                                        System.arraycopy(regionCustomerMatrix[regionIndex], 0, targetRegionCustomerMatrix[i], 0, 30);
                                    }
                                }
                                
                                // 执行分配算法
                                allocationMatrix = distributionAlgorithm.calculateDistribution(
                                    targetList, targetRegionCustomerMatrix, adv);
                                
                                // 计算实际投放量
                                BigDecimal actualAmount = calculateActualAmount(allocationMatrix, targetRegionCustomerMatrix);
                                cigResult.put("actualAmount", actualAmount);
                                cigResult.put("error", adv.subtract(actualAmount).abs());
                                cigResult.put("errorPercentage", adv.subtract(actualAmount).abs()
                                    .divide(adv, 4, BigDecimal.ROUND_HALF_UP)
                                    .multiply(new BigDecimal("100")));
                            }
                        }
                        
                        if (allocationMatrix != null && !targetList.isEmpty()) {
                            // 验证约束
                            boolean constraintValid = validateNonIncreasingConstraint(allocationMatrix);
                            cigResult.put("constraintValid", constraintValid);
                            cigResult.put("targetType", targetType);
                            
                            // 写回数据库
                            boolean writeBackSuccess = writeBackToDatabase(allocationMatrix, targetList, 
                                cigCode, cigName, year, month, weekSeq);
                            
                            if (writeBackSuccess) {
                                successCount++;
                                cigResult.put("writeBackStatus", "成功");
                                cigResult.put("writeBackMessage", "分配矩阵已成功写回数据库");
                            } else {
                                cigResult.put("writeBackStatus", "失败");
                                cigResult.put("writeBackMessage", "分配矩阵写回数据库失败");
                            }
                            
                            // 添加分配矩阵详情
                            Map<String, Object> allocationDetails = new HashMap<>();
                            for (int i = 0; i < targetList.size(); i++) {
                                Map<String, Object> regionAllocation = new HashMap<>();
                                regionAllocation.put("target", targetList.get(i));
                                
                                // 添加所有30个档位的分配值
                                for (int j = 0; j < 30; j++) {
                                    String columnName = "d" + (30 - j);
                                    regionAllocation.put(columnName, allocationMatrix[i][j]);
                                }
                                
                                allocationDetails.put("region_" + i, regionAllocation);
                            }
                            cigResult.put("allocationMatrix", allocationDetails);
                            
                        } else {
                            cigResult.put("writeBackStatus", "跳过");
                            cigResult.put("writeBackMessage", "未找到匹配的投放区域");
                        }
                    } else {
                        cigResult.put("writeBackStatus", "跳过");
                        cigResult.put("writeBackMessage", "未找到对应的测试数据");
                    }
                    
                } catch (Exception e) {
                    log.error("处理卷烟 {} 时发生错误", cigCode, e);
                    cigResult.put("writeBackStatus", "错误");
                    cigResult.put("writeBackMessage", "处理过程中发生错误: " + e.getMessage());
                }
                
                writeBackResults.add(cigResult);
            }
            
            result.put("success", true);
            result.put("message", String.format("分配矩阵写回完成，成功: %d/%d", successCount, totalCount));
            result.put("totalCount", totalCount);
            result.put("successCount", successCount);
            result.put("results", writeBackResults);
            
            log.info("分配矩阵写回完成，成功: {}/{}", successCount, totalCount);
            
        } catch (Exception e) {
            log.error("分配矩阵写回过程中发生错误", e);
            result.put("success", false);
            result.put("message", "分配矩阵写回失败: " + e.getMessage());
            result.put("error", e.toString());
        }
        
        return result;
    }
    
    /**
     * 将分配矩阵写回数据库（支持业态类型和城乡分类代码）
     */
    private boolean writeBackToDatabase(BigDecimal[][] allocationMatrix, 
                                      List<String> targetList,
                                      String cigCode, 
                                      String cigName,
                                      Integer year, 
                                      Integer month, 
                                      Integer weekSeq) {
        try {
            // 使用JdbcTemplate直接执行SQL，避免JPA事务问题
            String insertSql = "INSERT INTO demo_test_data (cig_code, cig_name, delivery_area, year, month, week_seq, " +
                "d30, d29, d28, d27, d26, d25, d24, d23, d22, d21, d20, d19, d18, d17, d16, d15, d14, d13, d12, d11, d10, d9, d8, d7, d6, d5, d4, d3, d2, d1, bz) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "d30=VALUES(d30), d29=VALUES(d29), d28=VALUES(d28), d27=VALUES(d27), d26=VALUES(d26), " +
                "d25=VALUES(d25), d24=VALUES(d24), d23=VALUES(d23), d22=VALUES(d22), d21=VALUES(d21), " +
                "d20=VALUES(d20), d19=VALUES(d19), d18=VALUES(d18), d17=VALUES(d17), d16=VALUES(d16), " +
                "d15=VALUES(d15), d14=VALUES(d14), d13=VALUES(d13), d12=VALUES(d12), d11=VALUES(d11), " +
                "d10=VALUES(d10), d9=VALUES(d9), d8=VALUES(d8), d7=VALUES(d7), d6=VALUES(d6), " +
                "d5=VALUES(d5), d4=VALUES(d4), d3=VALUES(d3), d2=VALUES(d2), d1=VALUES(d1), " +
                "bz=VALUES(bz)";
            
            // 为每个目标（区域或业态类型）执行插入或更新
            for (int i = 0; i < targetList.size(); i++) {
                String target = targetList.get(i);
                
                Object[] params = {
                    cigCode, cigName, target, year, month, weekSeq,
                    allocationMatrix[i][0],  // D30
                    allocationMatrix[i][1],  // D29
                    allocationMatrix[i][2],  // D28
                    allocationMatrix[i][3],  // D27
                    allocationMatrix[i][4],  // D26
                    allocationMatrix[i][5],  // D25
                    allocationMatrix[i][6],  // D24
                    allocationMatrix[i][7],  // D23
                    allocationMatrix[i][8],  // D22
                    allocationMatrix[i][9],  // D21
                    allocationMatrix[i][10], // D20
                    allocationMatrix[i][11], // D19
                    allocationMatrix[i][12], // D18
                    allocationMatrix[i][13], // D17
                    allocationMatrix[i][14], // D16
                    allocationMatrix[i][15], // D15
                    allocationMatrix[i][16], // D14
                    allocationMatrix[i][17], // D13
                    allocationMatrix[i][18], // D12
                    allocationMatrix[i][19], // D11
                    allocationMatrix[i][20], // D10
                    allocationMatrix[i][21], // D9
                    allocationMatrix[i][22], // D8
                    allocationMatrix[i][23], // D7
                    allocationMatrix[i][24], // D6
                    allocationMatrix[i][25], // D5
                    allocationMatrix[i][26], // D4
                    allocationMatrix[i][27], // D3
                    allocationMatrix[i][28], // D2
                    allocationMatrix[i][29], // D1
                    "算法自动生成"
                };
                
                // 执行SQL
                jdbcTemplate.update(insertSql, params);
                log.debug("目标 {} 的分配矩阵已写入数据库", target);
            }
            
            log.info("卷烟 {} 的分配矩阵已成功写回数据库", cigName);
            return true;
            
        } catch (Exception e) {
            log.error("写回数据库失败，卷烟: {}, 错误: {}", cigName, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 计算实际投放量
     */
    private BigDecimal calculateActualAmount(BigDecimal[][] allocationMatrix, BigDecimal[][] regionCustomerMatrix) {
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < allocationMatrix.length; i++) {
            for (int j = 0; j < 30; j++) {
                if (allocationMatrix[i][j] != null && regionCustomerMatrix[i][j] != null) {
                    total = total.add(allocationMatrix[i][j].multiply(regionCustomerMatrix[i][j]));
                }
            }
        }
        return total;
    }
    
    /**
     * 为单个记录计算实际投放量
     * 调用calActualDistributionAmount方法来计算每个卷烟的实际投放量
     */
    public BigDecimal calculateActualAmountForRecord(DemoTestData data) {
        try {
            // 调用新的实际投放量计算方法
            return calActualDistributionAmount(data.getCigCode(), data.getCigName(), 
                data.getYear(), data.getMonth(), data.getWeekSeq());
        } catch (Exception e) {
            log.error("计算实际投放量失败，卷烟代码: {}, 卷烟名称: {}, 错误: {}", 
                data.getCigCode(), data.getCigName(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * 根据算法需求描述计算卷烟实际投放量
     * 实际投放量 = 所有目标投放区域的客户数 * 档位投放量 的求和
     * S = Σ(i=1 to R) Σ(j=1 to B) x_{ij} * c_{ij}
     * 其中 x_{ij} 是分配给区域 i 档位 j 的卷烟数量，c_{ij} 是该区域该档位的客户数
     */
    public BigDecimal calActualDistributionAmount(String cigCode, String cigName, 
                                                   Integer year, Integer month, Integer weekSeq) {
        try {
            log.debug("开始计算卷烟实际投放量，卷烟代码: {}, 卷烟名称: {}, 日期: {}-{}-{}", 
                cigCode, cigName, year, month, weekSeq);
            
            // 1. 获取该卷烟在指定日期的所有投放区域记录
            List<DemoTestData> cigaretteData = testDataRepository.findByCigCodeAndCigNameAndYearAndMonthAndWeekSeq(
                cigCode, cigName, year, month, weekSeq);
            
            if (cigaretteData.isEmpty()) {
                log.warn("未找到卷烟数据，卷烟代码: {}, 卷烟名称: {}, 日期: {}-{}-{}", 
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
                    log.warn("投放区域 {} 在区域列表中未找到，可用区域: {}", deliveryArea, allRegions);
                    continue;
                }
                log.debug("找到投放区域 {} 的索引: {}", deliveryArea, regionIndex);
                
                // 4. 计算该区域的实际投放量：各档位的 客户数 * 档位投放量
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
                    if (regionCustomerMatrix[regionIndex][j] != null && allocationArray[j] != null) {
                        BigDecimal customerCount = regionCustomerMatrix[regionIndex][j];
                        BigDecimal allocation = allocationArray[j];
                        BigDecimal segmentActual = customerCount.multiply(allocation);
                        regionActualAmount = regionActualAmount.add(segmentActual);
                        
                        if (segmentActual.compareTo(BigDecimal.ZERO) > 0) {
                            log.debug("档位 {} - 客户数: {}, 分配量: {}, 实际投放量: {}", 
                                (30-j), customerCount, allocation, segmentActual);
                        }
                    }
                }
                
                log.debug("区域 {} 的实际投放量: {}", deliveryArea, regionActualAmount);
                totalActualAmount = totalActualAmount.add(regionActualAmount);
            }
            
            log.debug("卷烟 {} 的总实际投放量: {}", cigName, totalActualAmount);
            return totalActualAmount;
            
        } catch (Exception e) {
            log.error("计算实际投放量失败，卷烟代码: {}, 卷烟名称: {}, 错误: {}", 
                cigCode, cigName, e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 更新卷烟信息（包括投放方式、扩展投放方式、投放区域、档位分配、备注）
     * 这是一个复杂的更新逻辑，需要同时处理两个表
     */
    @Transactional
    public Map<String, Object> updateCigaretteInfo(UpdateCigaretteRequestDto request) {
        log.info("开始修改卷烟信息，卷烟代码: {}, 卷烟名称: {}", request.getCigCode(), request.getCigName());
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 检查卷烟在demo_test_ADVdata表中是否存在
            DemoTestAdvData existingAdvData = advDataRepository.findByCigCodeAndCigName(
                request.getCigCode(), request.getCigName());
            
            if (existingAdvData == null) {
                result.put("success", false);
                result.put("message", "卷烟在预投放量表中不存在");
                return result;
            }
            
            // 2. 检查投放方式和扩展投放方式是否发生变化
            boolean deliveryMethodChanged = !existingAdvData.getDeliveryMethod().equals(request.getDeliveryMethod());
            boolean deliveryEtypeChanged = !existingAdvData.getDeliveryEtype().equals(request.getDeliveryEtype());
            
            // 3. 更新demo_test_ADVdata表中的投放方式和扩展投放方式
            existingAdvData.setDeliveryMethod(request.getDeliveryMethod());
            existingAdvData.setDeliveryEtype(request.getDeliveryEtype());
            advDataRepository.save(existingAdvData);
            
            // 4. 根据投放方式是否变化决定处理策略
            if (deliveryMethodChanged || deliveryEtypeChanged) {
                // 投放方式发生变化，删除所有相关记录并重新创建
                log.info("投放方式发生变化，删除所有相关记录并重新创建");
                if (deleteExistingRecords(request.getCigCode(), request.getCigName(), 
                    request.getYear(), request.getMonth(), request.getWeekSeq())) {
                    if (createNewRecords(request)) {
                        result.put("success", true);
                        result.put("message", "卷烟信息修改成功（投放方式变化，重新创建记录）");
                    } else {
                        result.put("success", false);
                        result.put("message", "创建新记录失败");
                    }
                } else {
                    result.put("success", false);
                    result.put("message", "删除原有记录失败");
                }
            } else {
                // 投放方式未变化，更新现有记录
                log.info("投放方式未变化，更新现有记录");
                if (updateExistingRecords(request)) {
                    result.put("success", true);
                    result.put("message", "卷烟信息修改成功（更新现有记录）");
                } else {
                    result.put("success", false);
                    result.put("message", "更新现有记录失败");
                }
            }
            
        } catch (Exception e) {
            log.error("修改卷烟信息时发生错误", e);
            result.put("success", false);
            result.put("message", "修改过程中发生错误: " + e.getMessage());
            result.put("error", e.toString());
        }
        
        return result;
    }
    
    /**
     * 删除指定卷烟指定日期的所有记录
     */
    private boolean deleteExistingRecords(String cigCode, String cigName, Integer year, Integer month, Integer weekSeq) {
        try {
            List<DemoTestData> existingData = testDataRepository.findByCigCodeAndCigNameAndYearAndMonthAndWeekSeq(
                cigCode, cigName, year, month, weekSeq);
            
            if (!existingData.isEmpty()) {
                testDataRepository.deleteAll(existingData);
                log.info("删除了{}条原有记录", existingData.size());
            }
            return true;
        } catch (Exception e) {
            log.error("删除原有记录失败", e);
            return false;
        }
    }
    
    /**
     * 创建新的记录
     */
    private boolean createNewRecords(UpdateCigaretteRequestDto request) {
        try {
            // 拆分投放区域
            String[] regions = request.getDeliveryArea().split(",");
            int createdCount = 0;
            
            for (String region : regions) {
                String trimmedRegion = region.trim();
                if (trimmedRegion.isEmpty()) continue;
                
                DemoTestData newData = createDemoTestData(request, trimmedRegion);
                testDataRepository.save(newData);
                createdCount++;
                log.debug("为区域 {} 创建了新记录", trimmedRegion);
            }
            
            log.info("成功创建了{}条新记录", createdCount);
            return true;
        } catch (Exception e) {
            log.error("创建新记录失败", e);
            return false;
        }
    }
    
    /**
     * 更新现有记录
     * 投放方式未变化时的处理逻辑：
     * 1. 如果待修改的记录投放区域在demo_test_data中存在对应区域的记录，更新对应记录的档位设置D30~D1和备注
     * 2. 如果待修改的记录投放区域在demo_test_data中不存在对应区域的记录，新增指定日期卷烟该区域的记录
     */
    private boolean updateExistingRecords(UpdateCigaretteRequestDto request) {
        try {
            String[] targetRegions = request.getDeliveryArea().split(",");
            int updatedCount = 0;
            int createdCount = 0;
            
            for (String region : targetRegions) {
                String trimmedRegion = region.trim();
                if (trimmedRegion.isEmpty()) continue;
                
                // 查找是否已存在对应区域的记录
                DemoTestData existingData = testDataRepository.findByCigCodeAndCigNameAndDeliveryAreaAndYearAndMonthAndWeekSeq(
                    request.getCigCode(), request.getCigName(), trimmedRegion, 
                    request.getYear(), request.getMonth(), request.getWeekSeq());
                
                if (existingData != null) {
                    // 情况1：存在对应区域的记录，仅更新档位设置和备注，不修改投放区域
                    updateDemoTestDataForExistingRecord(existingData, request);
                    testDataRepository.save(existingData);
                    updatedCount++;
                    log.debug("更新了区域 {} 的现有记录（档位设置和备注）", trimmedRegion);
                } else {
                    // 情况2：不存在对应区域的记录，创建新记录
                    DemoTestData newData = createDemoTestData(request, trimmedRegion);
                    testDataRepository.save(newData);
                    createdCount++;
                    log.debug("为区域 {} 创建了新记录", trimmedRegion);
                }
            }
            
            log.info("更新完成：更新{}条记录，创建{}条记录", updatedCount, createdCount);
            return true;
        } catch (Exception e) {
            log.error("更新现有记录失败", e);
            return false;
        }
    }
    
    /**
     * 创建新的DemoTestData记录
     */
    private DemoTestData createDemoTestData(UpdateCigaretteRequestDto request, String deliveryArea) {
        DemoTestData data = new DemoTestData();
        data.setCigCode(request.getCigCode());
        data.setCigName(request.getCigName());
        data.setDeliveryArea(deliveryArea);
        data.setYear(request.getYear());
        data.setMonth(request.getMonth());
        data.setWeekSeq(request.getWeekSeq());
        data.setBz(request.getRemark() != null ? request.getRemark() : "前端更新");
        
        // 设置档位分配值
        if (request.getDistribution() != null && request.getDistribution().size() >= 30) {
            data.setD30(request.getDistribution().get(0));
            data.setD29(request.getDistribution().get(1));
            data.setD28(request.getDistribution().get(2));
            data.setD27(request.getDistribution().get(3));
            data.setD26(request.getDistribution().get(4));
            data.setD25(request.getDistribution().get(5));
            data.setD24(request.getDistribution().get(6));
            data.setD23(request.getDistribution().get(7));
            data.setD22(request.getDistribution().get(8));
            data.setD21(request.getDistribution().get(9));
            data.setD20(request.getDistribution().get(10));
            data.setD19(request.getDistribution().get(11));
            data.setD18(request.getDistribution().get(12));
            data.setD17(request.getDistribution().get(13));
            data.setD16(request.getDistribution().get(14));
            data.setD15(request.getDistribution().get(15));
            data.setD14(request.getDistribution().get(16));
            data.setD13(request.getDistribution().get(17));
            data.setD12(request.getDistribution().get(18));
            data.setD11(request.getDistribution().get(19));
            data.setD10(request.getDistribution().get(20));
            data.setD9(request.getDistribution().get(21));
            data.setD8(request.getDistribution().get(22));
            data.setD7(request.getDistribution().get(23));
            data.setD6(request.getDistribution().get(24));
            data.setD5(request.getDistribution().get(25));
            data.setD4(request.getDistribution().get(26));
            data.setD3(request.getDistribution().get(27));
            data.setD2(request.getDistribution().get(28));
            data.setD1(request.getDistribution().get(29));
        }
        
        return data;
    }
    
    /**
     * 更新现有的DemoTestData记录（投放方式变化时使用）
     * 会更新投放区域、档位设置和备注
     */
    private void updateDemoTestData(DemoTestData data, UpdateCigaretteRequestDto request) {
        // 更新投放区域
        data.setDeliveryArea(request.getDeliveryArea());
        
        // 更新备注
        if (request.getRemark() != null) {
            data.setBz(request.getRemark());
        }
        
        // 更新档位分配值
        updateDistributionValues(data, request);
    }
    
    /**
     * 更新现有记录的档位设置和备注（投放方式未变化时使用）
     * 不修改投放区域，仅更新档位设置D30~D1和备注
     */
    private void updateDemoTestDataForExistingRecord(DemoTestData data, UpdateCigaretteRequestDto request) {
        // 不更新投放区域，保持原有区域
        
        // 更新备注
        if (request.getRemark() != null) {
            data.setBz(request.getRemark());
        }
        
        // 更新档位分配值
        updateDistributionValues(data, request);
    }
    
    /**
     * 更新档位分配值的通用方法
     */
    private void updateDistributionValues(DemoTestData data, UpdateCigaretteRequestDto request) {
        if (request.getDistribution() != null && request.getDistribution().size() >= 30) {
            data.setD30(request.getDistribution().get(0));
            data.setD29(request.getDistribution().get(1));
            data.setD28(request.getDistribution().get(2));
            data.setD27(request.getDistribution().get(3));
            data.setD26(request.getDistribution().get(4));
            data.setD25(request.getDistribution().get(5));
            data.setD24(request.getDistribution().get(6));
            data.setD23(request.getDistribution().get(7));
            data.setD22(request.getDistribution().get(8));
            data.setD21(request.getDistribution().get(9));
            data.setD20(request.getDistribution().get(10));
            data.setD19(request.getDistribution().get(11));
            data.setD18(request.getDistribution().get(12));
            data.setD17(request.getDistribution().get(13));
            data.setD16(request.getDistribution().get(14));
            data.setD15(request.getDistribution().get(15));
            data.setD14(request.getDistribution().get(16));
            data.setD13(request.getDistribution().get(17));
            data.setD12(request.getDistribution().get(18));
            data.setD11(request.getDistribution().get(19));
            data.setD10(request.getDistribution().get(20));
            data.setD9(request.getDistribution().get(21));
            data.setD8(request.getDistribution().get(22));
            data.setD7(request.getDistribution().get(23));
            data.setD6(request.getDistribution().get(24));
            data.setD5(request.getDistribution().get(25));
            data.setD4(request.getDistribution().get(26));
            data.setD3(request.getDistribution().get(27));
            data.setD2(request.getDistribution().get(28));
            data.setD1(request.getDistribution().get(29));
        }
    }
    
    /**
     * 删除指定卷烟的投放区域记录
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> deleteDeliveryAreas(DeleteAreasRequestDto request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("开始删除投放区域，卷烟: {} - {}, 年份: {}, 月份: {}, 周序号: {}, 要删除的区域: {}", 
                    request.getCigName(), request.getCigCode(), request.getYear(), 
                    request.getMonth(), request.getWeekSeq(), request.getAreasToDelete());
            
            // 1. 验证参数
            if (!validateDeleteRequest(request)) {
                result.put("success", false);
                result.put("message", "请求参数无效");
                return result;
            }
            
            // 2. 查询要删除的记录
            List<DemoTestData> recordsToDelete = testDataRepository.findByYearAndMonthAndWeekSeq(
                    request.getYear(), request.getMonth(), request.getWeekSeq())
                    .stream()
                    .filter(record -> request.getCigCode().equals(record.getCigCode()) && 
                                    request.getCigName().equals(record.getCigName()) &&
                                    request.getAreasToDelete().contains(record.getDeliveryArea()))
                    .collect(Collectors.toList());
            
            if (recordsToDelete.isEmpty()) {
                result.put("success", false);
                result.put("message", "未找到要删除的记录");
                return result;
            }
            
            // 3. 执行批量删除
            int deletedCount = 0;
            for (DemoTestData record : recordsToDelete) {
                try {
                    testDataRepository.delete(record);
                    deletedCount++;
                    log.debug("删除记录，ID: {}, 投放区域: {}", record.getId(), record.getDeliveryArea());
                } catch (Exception e) {
                    log.error("删除记录失败，ID: {}, 投放区域: {}, 错误: {}", 
                            record.getId(), record.getDeliveryArea(), e.getMessage());
                }
            }
            
            // 4. 记录删除结果
            log.info("删除投放区域完成，总共删除 {} 条记录", deletedCount);
            
            result.put("success", true);
            result.put("message", "删除成功");
            result.put("deletedCount", deletedCount);
            result.put("requestedDeleteCount", recordsToDelete.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("删除投放区域时发生错误", e);
            result.put("success", false);
            result.put("message", "删除过程中发生错误: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 验证删除请求参数
     */
    private boolean validateDeleteRequest(DeleteAreasRequestDto request) {
        if (request == null) {
            return false;
        }
        
        if (request.getCigCode() == null || request.getCigCode().trim().isEmpty()) {
            log.warn("卷烟代码为空");
            return false;
        }
        
        if (request.getCigName() == null || request.getCigName().trim().isEmpty()) {
            log.warn("卷烟名称为空");
            return false;
        }
        
        if (request.getYear() == null || request.getYear() <= 0) {
            log.warn("年份无效: {}", request.getYear());
            return false;
        }
        
        if (request.getMonth() == null || request.getMonth() < 1 || request.getMonth() > 12) {
            log.warn("月份无效: {}", request.getMonth());
            return false;
        }
        
        if (request.getWeekSeq() == null || request.getWeekSeq() < 1 || request.getWeekSeq() > 5) {
            log.warn("周序号无效: {}", request.getWeekSeq());
            return false;
        }
        
        if (request.getAreasToDelete() == null || request.getAreasToDelete().isEmpty()) {
            log.warn("要删除的投放区域列表为空");
            return false;
        }
        
        // 检查是否有重复的区域
        Set<String> uniqueAreas = new HashSet<>(request.getAreasToDelete());
        if (uniqueAreas.size() != request.getAreasToDelete().size()) {
            log.warn("要删除的投放区域列表包含重复项");
        }
        
        return true;
    }
    
    /**
     * 获取指定卷烟在指定时间的所有投放区域
     */
    public List<String> getCurrentDeliveryAreas(String cigCode, String cigName, 
                                               Integer year, Integer month, Integer weekSeq) {
        try {
            List<DemoTestData> records = testDataRepository.findByYearAndMonthAndWeekSeq(year, month, weekSeq);
            
            return records.stream()
                    .filter(record -> cigCode.equals(record.getCigCode()) && 
                                    cigName.equals(record.getCigName()))
                    .map(DemoTestData::getDeliveryArea)
                    .filter(area -> area != null && !area.trim().isEmpty())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("获取当前投放区域失败", e);
            return new ArrayList<>();
        }
    }
}
