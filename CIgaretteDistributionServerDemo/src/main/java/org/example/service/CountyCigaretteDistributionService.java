package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.entity.DemoTestAdvData;
import org.example.entity.DemoTestCountyClientNumData;
import org.example.entity.DemoTestData;
import org.example.repository.DemoTestAdvDataRepository;
import org.example.repository.DemoTestCountyClientNumDataRepository;
import org.example.repository.DemoTestDataRepository;
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
    private DemoTestAdvDataRepository advDataRepository;

    @Autowired
    private DemoTestCountyClientNumDataRepository countyClientNumDataRepository;

    @Autowired
    private DemoTestDataRepository testDataRepository;

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

        List<DemoTestAdvData> advDataList = getAdvData();
        if (advDataList == null || advDataList.isEmpty()) {
            response.put("message", "预投放数据为空，无法执行预测");
            response.put("success", false);
            return response;
        }

        List<DemoTestAdvData> countyAdvData = advDataList.stream()
                .filter(adv -> "档位+区县".equals(adv.getDeliveryEtype()))
                .collect(Collectors.toList());

        if (countyAdvData.isEmpty()) {
            response.put("message", "没有需要按“档位+区县”方式投放的卷烟");
            response.put("success", true);
            return response;
        }

        // 删除旧数据
        List<DemoTestData> oldPredictions = testDataRepository.findByYearAndMonthAndWeekSeq(year, month, weekSeq);
        if (!oldPredictions.isEmpty()) {
            testDataRepository.deleteAll(oldPredictions);
            log.info("已删除 {} 条旧的预测数据", oldPredictions.size());
        }


        for (DemoTestAdvData advData : countyAdvData) {
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

                List<DemoTestData> predictionsToSave = new ArrayList<>();
                for (int i = 0; i < targetCounties.size(); i++) {
                    predictionsToSave.add(createDemoTestDataEntity(advData, targetCounties.get(i), allocationMatrix[i], year, month, weekSeq));
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
    public List<DemoTestAdvData> getAdvData() {
        log.info("正在从数据库获取预投放数据并缓存...");
        return advDataRepository.findAll();
    }

    @Cacheable("allCountyList")
    public List<String> getAllCountyList() {
        if (allCountyList == null) {
            log.info("正在从数据库初始化区县列表并缓存...");
            allCountyList = countyClientNumDataRepository.findAllByOrderByIdAsc()
                    .stream()
                    .map(DemoTestCountyClientNumData::getCounty)
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
            List<DemoTestCountyClientNumData> clientNumDataList = countyClientNumDataRepository.findAllByOrderByIdAsc();
            List<String> counties = getAllCountyList();

            countyCustomerMatrix = new BigDecimal[counties.size()][30];

            for (int i = 0; i < counties.size(); i++) {
                String county = counties.get(i);
                DemoTestCountyClientNumData clientData = clientNumDataList.stream()
                        .filter(data -> county.equals(data.getCounty()))
                        .findFirst()
                        .orElse(null);

                if (clientData != null) {
                    countyCustomerMatrix[i][0] = clientData.getD30();
                    countyCustomerMatrix[i][1] = clientData.getD29();
                    countyCustomerMatrix[i][2] = clientData.getD28();
                    countyCustomerMatrix[i][3] = clientData.getD27();
                    countyCustomerMatrix[i][4] = clientData.getD26();
                    countyCustomerMatrix[i][5] = clientData.getD25();
                    countyCustomerMatrix[i][6] = clientData.getD24();
                    countyCustomerMatrix[i][7] = clientData.getD23();
                    countyCustomerMatrix[i][8] = clientData.getD22();
                    countyCustomerMatrix[i][9] = clientData.getD21();
                    countyCustomerMatrix[i][10] = clientData.getD20();
                    countyCustomerMatrix[i][11] = clientData.getD19();
                    countyCustomerMatrix[i][12] = clientData.getD18();
                    countyCustomerMatrix[i][13] = clientData.getD17();
                    countyCustomerMatrix[i][14] = clientData.getD16();
                    countyCustomerMatrix[i][15] = clientData.getD15();
                    countyCustomerMatrix[i][16] = clientData.getD14();
                    countyCustomerMatrix[i][17] = clientData.getD13();
                    countyCustomerMatrix[i][18] = clientData.getD12();
                    countyCustomerMatrix[i][19] = clientData.getD11();
                    countyCustomerMatrix[i][20] = clientData.getD10();
                    countyCustomerMatrix[i][21] = clientData.getD9();
                    countyCustomerMatrix[i][22] = clientData.getD8();
                    countyCustomerMatrix[i][23] = clientData.getD7();
                    countyCustomerMatrix[i][24] = clientData.getD6();
                    countyCustomerMatrix[i][25] = clientData.getD5();
                    countyCustomerMatrix[i][26] = clientData.getD4();
                    countyCustomerMatrix[i][27] = clientData.getD3();
                    countyCustomerMatrix[i][28] = clientData.getD2();
                    countyCustomerMatrix[i][29] = clientData.getD1();
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

    private DemoTestData createDemoTestDataEntity(DemoTestAdvData advData, String county, BigDecimal[] allocation, Integer year, Integer month, Integer weekSeq) {
        DemoTestData entity = new DemoTestData();
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