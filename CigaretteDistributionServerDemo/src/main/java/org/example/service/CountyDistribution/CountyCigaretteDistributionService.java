package org.example.service.CountyDistribution;

import lombok.extern.slf4j.Slf4j;
import org.example.entity.CigaretteDistributionInfoData;
import org.example.entity.CigaretteDistributionPredictionData;
import org.example.entity.RegionClientNumData;
import org.example.repository.CigaretteDistributionInfoDataRepository;
import org.example.repository.CigaretteDistributionPredictionDataRepository;
import org.example.service.RegionClientNumDataService;
import org.example.service.algorithm.countyCigaretteDistributionAlgorithm;
import org.example.util.KmpMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CountyCigaretteDistributionService {

    @Autowired
    private CigaretteDistributionInfoDataRepository advDataRepository;

    @Autowired
    private RegionClientNumDataService regionClientNumDataService;

    @Autowired
    private CigaretteDistributionPredictionDataRepository testDataRepository;

    @Autowired
    private countyCigaretteDistributionAlgorithm distributionAlgorithm;

    @Autowired
    private KmpMatcher kmpMatcher;

    private List<String> allCountyList;
    private BigDecimal[][] countyCustomerMatrix;

    @Transactional
    public Map<String, Object> runAndSavePrediction(Integer year, Integer month, Integer weekSeq) {
        log.info("开始执行“档位+区县”预测并保存结果到 demo_test_data，时间: {}-{}-{}", year, month, weekSeq);
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> results = new ArrayList<>();
        int successCount = 0;

        List<CigaretteDistributionInfoData> advDataList = getAdvData();
        if (advDataList == null || advDataList.isEmpty()) {
            response.put("message", "预投放数据为空，无法执行预测");
            response.put("success", false);
            return response;
        }

        List<CigaretteDistributionInfoData> countyAdvData = advDataList.stream()
                .filter(adv -> "档位+区县".equals(adv.getDeliveryEtype()))
                .collect(Collectors.toList());

        if (countyAdvData.isEmpty()) {
            response.put("message", "没有需要按“档位+区县”方式投放的卷烟");
            response.put("success", true);
            return response;
        }

        // 精确删除：只删除当前要处理的卷烟在其目标区县的旧数据，保留其他投放区域的数据
        for (CigaretteDistributionInfoData advData : countyAdvData) {
            List<String> targetCounties = getTargetCountyList(advData.getDeliveryArea());
            if (!targetCounties.isEmpty()) {
                List<CigaretteDistributionPredictionData> oldCountyPredictions = testDataRepository.findByYearAndMonthAndWeekSeqAndCigCodeAndDeliveryAreaIn(
                    year, month, weekSeq, advData.getCigCode(), targetCounties);
                if (!oldCountyPredictions.isEmpty()) {
                    testDataRepository.deleteAll(oldCountyPredictions);
                    log.info("已删除卷烟 {} 在目标区县 {} 的 {} 条旧预测数据", 
                        advData.getCigName(), targetCounties, oldCountyPredictions.size());
                }
            }
        }


        for (CigaretteDistributionInfoData advData : countyAdvData) {
            Map<String, Object> cigResult = new HashMap<>();
            cigResult.put("cigCode", advData.getCigCode());
            cigResult.put("cigName", advData.getCigName());
            try {
                List<String> targetCounties = getTargetCountyList(advData.getDeliveryArea());
                if (targetCounties.isEmpty()) {
                    log.warn("卷烟 {} ({}) 的投放区域 '{}' 未匹配到任何区县，跳过预测。", advData.getCigName(), advData.getCigCode(), advData.getDeliveryArea());
                    cigResult.put("status", "跳过");
                    results.add(cigResult);
                    continue;
                }

                BigDecimal[][] allocationMatrix = calculateDistributionMatrix(targetCounties, advData.getAdv());

                List<CigaretteDistributionPredictionData> predictionsToSave = new ArrayList<>();
                for (int i = 0; i < targetCounties.size(); i++) {
                    predictionsToSave.add(createCigaretteDistributionPredictionDataEntity(advData, targetCounties.get(i), allocationMatrix[i], year, month, weekSeq));
                }
                testDataRepository.saveAll(predictionsToSave);
                successCount++;
                cigResult.put("status", "成功");
                results.add(cigResult);

            } catch (Exception e) {
                // 关键：添加日志打印，输出完整异常信息
                e.printStackTrace(); // 简单打印，或用日志框架（如log.info/log.error）
                // 原有catch逻辑（如事务回滚、返回错误信息）
                log.error("处理卷烟 {} ({}) 的预测时失败: {}", advData.getCigName(), advData.getCigCode(), e.getMessage(), e);
                cigResult.put("status", "失败");
                results.add(cigResult);
            }
        }

        response.put("success", true);
        response.put("message", String.format("预测完成，总任务数: %d, 成功: %d", countyAdvData.size(), successCount));
        response.put("details", results);
        return response;
    }

    @Cacheable("advData")
    public List<CigaretteDistributionInfoData> getAdvData() {
        log.info("正在从数据库获取预投放数据并缓存...");
        return advDataRepository.findAll();
    }

    @Cacheable("allCountyList")
    public List<String> getAllCountyList() {
        if (allCountyList == null) {
            log.info("正在从数据库初始化区县列表并缓存...");
            String tableName = regionClientNumDataService.generateTableName("按档位扩展投放", "档位+区县", false);
            allCountyList = regionClientNumDataService.findAllByTableName(tableName)
                    .stream()
                    .map(RegionClientNumData::getRegion)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            log.info("区县列表初始化完成，共有 {} 个区县", allCountyList.size());
        }
        return allCountyList;
    }

    @Cacheable("countyCustomerMatrix")
    public BigDecimal[][] getCountyCustomerMatrix() {
        if (countyCustomerMatrix == null) {
            log.info("正在从数据库初始化区县客户矩阵并缓存...");
            String tableName = regionClientNumDataService.generateTableName("按档位扩展投放", "档位+区县", false);
            List<RegionClientNumData> clientNumDataList = regionClientNumDataService.findAllByTableName(tableName);
            List<String> counties = getAllCountyList();

            countyCustomerMatrix = new BigDecimal[counties.size()][30];

            for (int i = 0; i < counties.size(); i++) {
                String county = counties.get(i);
                RegionClientNumData clientData = clientNumDataList.stream()
                        .filter(data -> county.equals(data.getRegion()))
                        .findFirst()
                        .orElse(null);

                if (clientData != null) {
                    BigDecimal[] gradeArray = clientData.getGradeArray();
                    for (int j = 0; j < 30 && j < gradeArray.length; j++) {
                        countyCustomerMatrix[i][j] = gradeArray[j] != null ? gradeArray[j] : BigDecimal.ZERO;
                    }
                }
            }
            log.info("区县客户矩阵初始化完成，大小为: {}x30", counties.size());
        }
        return countyCustomerMatrix;
    }

    public List<String> getTargetCountyList(String deliveryArea) {
        log.info("开始匹配投放区县，输入文本: '{}'", deliveryArea);
        List<String> allCounties = getAllCountyList();
        List<String> targetCounties = kmpMatcher.matchPatterns(deliveryArea, allCounties);
        log.info("匹配到的目标区县: {}", targetCounties);
        return targetCounties;
    }

    public BigDecimal[][] calculateDistributionMatrix(List<String> targetCounties, BigDecimal targetAmount) {
        log.info("准备计算分配矩阵，目标区县数: {}, 目标数量: {}", targetCounties.size(), targetAmount);
        BigDecimal[][] fullCustomerMatrix = getCountyCustomerMatrix();
        List<String> allCounties = getAllCountyList();

        BigDecimal[][] targetCountyCustomerMatrix = new BigDecimal[targetCounties.size()][30];
        for (int i = 0; i < targetCounties.size(); i++) {
            String targetCounty = targetCounties.get(i);
            int countyIndex = allCounties.indexOf(targetCounty);
            if (countyIndex >= 0) {
                System.arraycopy(fullCustomerMatrix[countyIndex], 0, targetCountyCustomerMatrix[i], 0, 30);
            }
        }

        return distributionAlgorithm.calculateDistribution(targetCounties, targetCountyCustomerMatrix, targetAmount);
    }

    private CigaretteDistributionPredictionData createCigaretteDistributionPredictionDataEntity(CigaretteDistributionInfoData advData, String county, BigDecimal[] allocation, Integer year, Integer month, Integer weekSeq) {
        CigaretteDistributionPredictionData entity = new CigaretteDistributionPredictionData();
        entity.setCigCode(advData.getCigCode());
        entity.setCigName(advData.getCigName());
        entity.setYear(year);
        entity.setMonth(month);
        entity.setWeekSeq(weekSeq);
        entity.setDeliveryArea(county);
        entity.setBz("算法自动生成");

        entity.setD30(allocation[0]);
        entity.setD29(allocation[1]);
        entity.setD28(allocation[2]);
        entity.setD27(allocation[3]);
        entity.setD26(allocation[4]);
        entity.setD25(allocation[5]);
        entity.setD24(allocation[6]);
        entity.setD23(allocation[7]);
        entity.setD22(allocation[8]);
        entity.setD21(allocation[9]);
        entity.setD20(allocation[10]);
        entity.setD19(allocation[11]);
        entity.setD18(allocation[12]);
        entity.setD17(allocation[13]);
        entity.setD16(allocation[14]);
        entity.setD15(allocation[15]);
        entity.setD14(allocation[16]);
        entity.setD13(allocation[17]);
        entity.setD12(allocation[18]);
        entity.setD11(allocation[19]);
        entity.setD10(allocation[20]);
        entity.setD9(allocation[21]);
        entity.setD8(allocation[22]);
        entity.setD7(allocation[23]);
        entity.setD6(allocation[24]);
        entity.setD5(allocation[25]);
        entity.setD4(allocation[26]);
        entity.setD3(allocation[27]);
        entity.setD2(allocation[28]);
        entity.setD1(allocation[29]);

        return entity;
    }
}