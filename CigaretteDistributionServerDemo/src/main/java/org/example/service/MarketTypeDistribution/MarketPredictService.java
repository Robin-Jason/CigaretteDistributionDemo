package org.example.service.MarketTypeDistribution;

import lombok.extern.slf4j.Slf4j;
import org.example.entity.CigaretteDistributionInfoData;
import org.example.entity.CigaretteDistributionPredictionData;
import org.example.entity.RegionClientNumData;
import org.example.repository.CigaretteDistributionInfoDataRepository;
import org.example.repository.CigaretteDistributionPredictionDataRepository;
import org.example.service.RegionClientNumDataService;
import org.example.service.algorithm.MarketProportionalCigaretteDistributionAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MarketPredictService {

    @Autowired
    private CigaretteDistributionInfoDataRepository advDataRepository;

    @Autowired
    private RegionClientNumDataService regionClientNumDataService;

    @Autowired
    private CigaretteDistributionPredictionDataRepository testDataRepository;

    @Autowired
    private MarketProportionalCigaretteDistributionAlgorithm distributionAlgorithm;

    /**
     * 执行卷烟投放量预测并写回数据库 (针对市场类型)
     *
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     */
    @Transactional
    public void predictAndWriteBackForMarket(Integer year, Integer month, Integer weekSeq) {
        log.info("开始预测卷烟投放量并写回数据库 (市场类型)，年份: {}, 月份: {}, 周序号: {}", year, month, weekSeq);

        // 1. 获取所有预投放量数据
        List<CigaretteDistributionInfoData> advDataList = advDataRepository.findAll();

        // 2. 筛选出投放方式为"按档位扩展投放"且扩展类型为"档位+市场类型"的数据
        List<CigaretteDistributionInfoData> filteredAdvData = advDataList.stream()
                .filter(data -> "按档位扩展投放".equals(data.getDeliveryMethod()) &&
                        "档位+市场类型".equals(data.getDeliveryEtype()) &&
                        year.equals(data.getYear()) &&
                        month.equals(data.getMonth()) &&
                        weekSeq.equals(data.getWeekSeq()))
                .collect(Collectors.toList());

        log.info("筛选出 {} 条符合条件的预投放量数据", filteredAdvData.size());

        // 3. 获取所有区域客户数数据（使用新的Repository）
        List<RegionClientNumData> marketClientNumDataList = regionClientNumDataService.findAllByTableName(regionClientNumDataService.generateTableName("按档位扩展投放", "档位+市场类型", false));
        List<String> allRegions = marketClientNumDataList.stream()
                .map(RegionClientNumData::getRegion)
                .collect(Collectors.toList());

        // 构建区域客户数矩阵
        BigDecimal[][] regionCustomerMatrix = new BigDecimal[marketClientNumDataList.size()][30];
        for (int i = 0; i < marketClientNumDataList.size(); i++) {
            RegionClientNumData clientData = marketClientNumDataList.get(i);
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

        // 4. 对每个符合条件的卷烟执行预测算法
        for (CigaretteDistributionInfoData advData : filteredAdvData) {
            String cigCode = advData.getCigCode();
            String cigName = advData.getCigName();
            BigDecimal targetAmount = advData.getAdv();

            log.info("开始预测卷烟: {} ({})，预投放量: {}", cigName, cigCode, targetAmount);

            // 5. 设定目标投放区域为"城网"和"农网"
            List<String> targetRegions = Arrays.asList("城网", "农网");

            // 筛选出目标区域的客户数矩阵
            BigDecimal[][] targetRegionCustomerMatrix = new BigDecimal[targetRegions.size()][30];
            for (int i = 0; i < targetRegions.size(); i++) {
                String targetRegion = targetRegions.get(i);
                int regionIndex = allRegions.indexOf(targetRegion);
                if (regionIndex != -1) {
                    System.arraycopy(regionCustomerMatrix[regionIndex], 0, targetRegionCustomerMatrix[i], 0, 30);
                }
            }

            // 6. 执行预测算法，使用0.4和0.6的比例
            BigDecimal urbanRatio = new BigDecimal("0.4");
            BigDecimal ruralRatio = new BigDecimal("0.6");

            BigDecimal[][] allocationMatrix = distributionAlgorithm.calculateDistribution(
                    targetRegions, targetRegionCustomerMatrix, targetAmount, urbanRatio, ruralRatio);

            // 7. 清除旧数据并写入新数据
            deleteExistingData(cigCode, cigName, year, month, weekSeq, targetRegions);
            writeBackPrediction(allocationMatrix, targetRegions, cigCode, cigName, year, month, weekSeq);
            log.info("卷烟: {} ({}) 预测完成，数据已写入", cigName, cigCode);
        }

        log.info("所有预测任务完成 (市场类型)");
    }

    /**
     * 删除指定卷烟、指定日期、指定区域的现有数据
     */
    private void deleteExistingData(String cigCode, String cigName, Integer year, Integer month, Integer weekSeq, List<String> regions) {
        log.info("开始删除卷烟 {} - {} 在 {}-{}-{} 的旧数据", cigName, cigCode, year, month, weekSeq);
        for (String region : regions) {
            CigaretteDistributionPredictionData existingData = testDataRepository.findByCigCodeAndCigNameAndDeliveryAreaAndYearAndMonthAndWeekSeq(
                    cigCode, cigName, region, year, month, weekSeq);
            if (existingData != null) {
                testDataRepository.delete(existingData);
            }
        }
        log.info("旧数据删除完成");
    }

    /**
     * 将预测结果写回数据库
     */
    private void writeBackPrediction(BigDecimal[][] allocationMatrix, List<String> targetRegions,
                                     String cigCode, String cigName, Integer year, Integer month, Integer weekSeq) {
        for (int i = 0; i < targetRegions.size(); i++) {
            String region = targetRegions.get(i);
            CigaretteDistributionPredictionData newData = new CigaretteDistributionPredictionData();
            newData.setCigCode(cigCode);
            newData.setCigName(cigName);
            newData.setDeliveryArea(region);
            newData.setYear(year);
            newData.setMonth(month);
            newData.setWeekSeq(weekSeq);
            newData.setBz("市场类型预测结果");

            newData.setD30(allocationMatrix[i][0]);
            newData.setD29(allocationMatrix[i][1]);
            newData.setD28(allocationMatrix[i][2]);
            newData.setD27(allocationMatrix[i][3]);
            newData.setD26(allocationMatrix[i][4]);
            newData.setD25(allocationMatrix[i][5]);
            newData.setD24(allocationMatrix[i][6]);
            newData.setD23(allocationMatrix[i][7]);
            newData.setD22(allocationMatrix[i][8]);
            newData.setD21(allocationMatrix[i][9]);
            newData.setD20(allocationMatrix[i][10]);
            newData.setD19(allocationMatrix[i][11]);
            newData.setD18(allocationMatrix[i][12]);
            newData.setD17(allocationMatrix[i][13]);
            newData.setD16(allocationMatrix[i][14]);
            newData.setD15(allocationMatrix[i][15]);
            newData.setD14(allocationMatrix[i][16]);
            newData.setD13(allocationMatrix[i][17]);
            newData.setD12(allocationMatrix[i][18]);
            newData.setD11(allocationMatrix[i][19]);
            newData.setD10(allocationMatrix[i][20]);
            newData.setD9(allocationMatrix[i][21]);
            newData.setD8(allocationMatrix[i][22]);
            newData.setD7(allocationMatrix[i][23]);
            newData.setD6(allocationMatrix[i][24]);
            newData.setD5(allocationMatrix[i][25]);
            newData.setD4(allocationMatrix[i][26]);
            newData.setD3(allocationMatrix[i][27]);
            newData.setD2(allocationMatrix[i][28]);
            newData.setD1(allocationMatrix[i][29]);

            testDataRepository.save(newData);
        }
    }
    
    /**
     * 获取目标市场类型列表
     */
    public List<String> getTargetMarketList(String deliveryArea) {
        log.info("开始匹配投放市场类型，输入文本: '{}'", deliveryArea);
        // 根据投放区域解析市场类型
        return Arrays.stream(deliveryArea.split("[,，]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
    
    /**
     * 计算市场类型分配矩阵
     */
    public BigDecimal[][] calculateMarketDistributionMatrix(List<String> targetMarkets, BigDecimal targetAmount) {
        log.info("准备计算市场类型分配矩阵，目标市场数: {}, 目标数量: {}", targetMarkets.size(), targetAmount);
        
        if (targetMarkets.isEmpty()) {
            return new BigDecimal[0][30];
        }
        
        // 获取市场客户数矩阵
        BigDecimal[][] marketCustomerMatrix = getMarketCustomerMatrix();
        
        // 设置城网和农网的默认比例（可以根据实际业务需求调整）
        BigDecimal urbanRatio = new BigDecimal("0.6");  // 城网60%
        BigDecimal ruralRatio = new BigDecimal("0.4");  // 农网40%
        
        // 使用分配算法计算
        return distributionAlgorithm.calculateDistribution(targetMarkets, marketCustomerMatrix, 
                                                         targetAmount, urbanRatio, ruralRatio);
    }
    
    /**
     * 获取市场客户数矩阵
     */
    private BigDecimal[][] getMarketCustomerMatrix() {
        List<RegionClientNumData> allMarketData = regionClientNumDataService.findAllByTableName(regionClientNumDataService.generateTableName("按档位扩展投放", "档位+市场类型", false));
        
        BigDecimal[][] matrix = new BigDecimal[allMarketData.size()][30];
        for (int i = 0; i < allMarketData.size(); i++) {
            RegionClientNumData data = allMarketData.get(i);
            matrix[i][0] = data.getD30() != null ? data.getD30() : BigDecimal.ZERO;
            matrix[i][1] = data.getD29() != null ? data.getD29() : BigDecimal.ZERO;
            matrix[i][2] = data.getD28() != null ? data.getD28() : BigDecimal.ZERO;
            matrix[i][3] = data.getD27() != null ? data.getD27() : BigDecimal.ZERO;
            matrix[i][4] = data.getD26() != null ? data.getD26() : BigDecimal.ZERO;
            matrix[i][5] = data.getD25() != null ? data.getD25() : BigDecimal.ZERO;
            matrix[i][6] = data.getD24() != null ? data.getD24() : BigDecimal.ZERO;
            matrix[i][7] = data.getD23() != null ? data.getD23() : BigDecimal.ZERO;
            matrix[i][8] = data.getD22() != null ? data.getD22() : BigDecimal.ZERO;
            matrix[i][9] = data.getD21() != null ? data.getD21() : BigDecimal.ZERO;
            matrix[i][10] = data.getD20() != null ? data.getD20() : BigDecimal.ZERO;
            matrix[i][11] = data.getD19() != null ? data.getD19() : BigDecimal.ZERO;
            matrix[i][12] = data.getD18() != null ? data.getD18() : BigDecimal.ZERO;
            matrix[i][13] = data.getD17() != null ? data.getD17() : BigDecimal.ZERO;
            matrix[i][14] = data.getD16() != null ? data.getD16() : BigDecimal.ZERO;
            matrix[i][15] = data.getD15() != null ? data.getD15() : BigDecimal.ZERO;
            matrix[i][16] = data.getD14() != null ? data.getD14() : BigDecimal.ZERO;
            matrix[i][17] = data.getD13() != null ? data.getD13() : BigDecimal.ZERO;
            matrix[i][18] = data.getD12() != null ? data.getD12() : BigDecimal.ZERO;
            matrix[i][19] = data.getD11() != null ? data.getD11() : BigDecimal.ZERO;
            matrix[i][20] = data.getD10() != null ? data.getD10() : BigDecimal.ZERO;
            matrix[i][21] = data.getD9() != null ? data.getD9() : BigDecimal.ZERO;
            matrix[i][22] = data.getD8() != null ? data.getD8() : BigDecimal.ZERO;
            matrix[i][23] = data.getD7() != null ? data.getD7() : BigDecimal.ZERO;
            matrix[i][24] = data.getD6() != null ? data.getD6() : BigDecimal.ZERO;
            matrix[i][25] = data.getD5() != null ? data.getD5() : BigDecimal.ZERO;
            matrix[i][26] = data.getD4() != null ? data.getD4() : BigDecimal.ZERO;
            matrix[i][27] = data.getD3() != null ? data.getD3() : BigDecimal.ZERO;
            matrix[i][28] = data.getD2() != null ? data.getD2() : BigDecimal.ZERO;
            matrix[i][29] = data.getD1() != null ? data.getD1() : BigDecimal.ZERO;
        }
        
        return matrix;
    }
}