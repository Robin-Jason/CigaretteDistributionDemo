package org.example.service.CityUnifiedDistribution;

import lombok.extern.slf4j.Slf4j;

import org.example.entity.CigaretteDistributionInfoData;
import org.example.entity.CigaretteDistributionPredictionData;
import org.example.entity.RegionClientNumData;
import org.example.repository.CigaretteDistributionInfoDataRepository;
import org.example.repository.CigaretteDistributionPredictionDataRepository;
import org.example.service.RegionClientNumDataService;
import org.example.service.algorithm.CityCigaretteDistributionAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class CityPredictionService {

    @Autowired
    private CigaretteDistributionInfoDataRepository advDataRepository;

    @Autowired
    private RegionClientNumDataService regionClientNumDataService;

    @Autowired
    private CigaretteDistributionPredictionDataRepository testDataRepository;

    @Autowired
    private CityCigaretteDistributionAlgorithm distributionAlgorithm;

    @Transactional
    public void predictAndSave(Integer year, Integer month, Integer weekSeq) {
        log.info("开始全市投放量预测，年份: {}, 月份: {}, 周序号: {}", year, month, weekSeq);

        // 1. 获取所有预投放量数据
        List<CigaretteDistributionInfoData> advDataList = advDataRepository.findAll();
        log.info("获取到 {} 条预投放量数据", advDataList.size());

        // 2. 获取全市的客户数量数据
        List<RegionClientNumData> cityDataList = regionClientNumDataService.findByTableNameAndRegion(regionClientNumDataService.generateTableName("按档位统一投放", null, false), "全市");
        if (cityDataList.isEmpty()) {
            log.error("未找到'全市'的客户数量数据，预测中止");
            throw new RuntimeException("未找到'全市'的客户数量数据");
        }
        RegionClientNumData cityData = cityDataList.get(0);

        // 3. 准备算法需要的客户数矩阵
        BigDecimal[][] regionCustomerMatrix = new BigDecimal[1][30];
        regionCustomerMatrix[0] = cityData.getGradeArray();

        // 4. 遍历按全市统一投放的每个卷烟进行预测并写回数据库
        for (CigaretteDistributionInfoData advData : advDataList) {
            log.info("正在处理卷烟: {} ({}), 投放方式: {}", advData.getCigName(), advData.getCigCode(), advData.getDeliveryMethod());

            // 判断是否为按档位统一投放
            if (!"按档位统一投放".equals(advData.getDeliveryMethod())) {
                log.info("卷烟 {} 的投放方式为 {}，跳过全市统一分配", advData.getCigName(), advData.getDeliveryMethod());
                continue;
            }
            // 5. 执行分配算法
            BigDecimal[][] allocationMatrix = distributionAlgorithm.calculateDistribution(
                    Collections.singletonList("全市"),
                    regionCustomerMatrix,
                    advData.getAdv()
            );

            if (allocationMatrix.length > 0 && allocationMatrix[0].length == 30) {
                // 6. 将预测结果写入 demo_test_data 表
                CigaretteDistributionPredictionData newTestData = new CigaretteDistributionPredictionData();
                newTestData.setCigCode(advData.getCigCode());
                newTestData.setCigName(advData.getCigName());
                newTestData.setYear(year);
                newTestData.setMonth(month);
                newTestData.setWeekSeq(weekSeq);
                newTestData.setDeliveryArea("全市");

                // 设置档位值
                newTestData.setD30(allocationMatrix[0][0]);
                newTestData.setD29(allocationMatrix[0][1]);
                newTestData.setD28(allocationMatrix[0][2]);
                newTestData.setD27(allocationMatrix[0][3]);
                newTestData.setD26(allocationMatrix[0][4]);
                newTestData.setD25(allocationMatrix[0][5]);
                newTestData.setD24(allocationMatrix[0][6]);
                newTestData.setD23(allocationMatrix[0][7]);
                newTestData.setD22(allocationMatrix[0][8]);
                newTestData.setD21(allocationMatrix[0][9]);
                newTestData.setD20(allocationMatrix[0][10]);
                newTestData.setD19(allocationMatrix[0][11]);
                newTestData.setD18(allocationMatrix[0][12]);
                newTestData.setD17(allocationMatrix[0][13]);
                newTestData.setD16(allocationMatrix[0][14]);
                newTestData.setD15(allocationMatrix[0][15]);
                newTestData.setD14(allocationMatrix[0][16]);
                newTestData.setD13(allocationMatrix[0][17]);
                newTestData.setD12(allocationMatrix[0][18]);
                newTestData.setD11(allocationMatrix[0][19]);
                newTestData.setD10(allocationMatrix[0][20]);
                newTestData.setD9(allocationMatrix[0][21]);
                newTestData.setD8(allocationMatrix[0][22]);
                newTestData.setD7(allocationMatrix[0][23]);
                newTestData.setD6(allocationMatrix[0][24]);
                newTestData.setD5(allocationMatrix[0][25]);
                newTestData.setD4(allocationMatrix[0][26]);
                newTestData.setD3(allocationMatrix[0][27]);
                newTestData.setD2(allocationMatrix[0][28]);
                newTestData.setD1(allocationMatrix[0][29]);

                testDataRepository.save(newTestData);
                log.info("卷烟 {} 的预测数据已成功写入数据库", advData.getCigName());
            } else {
                log.warn("卷烟 {} 的算法分配结果为空，跳过写入", advData.getCigName());
            }
        }
    }
    
    /**
     * 计算城市分配矩阵（通用方法）
     */
    public BigDecimal[][] calculateCityDistributionMatrix(List<String> targetList, BigDecimal targetAmount) {
        log.info("准备计算城市分配矩阵，目标列表: {}, 目标数量: {}", targetList, targetAmount);
        
        if (targetList.isEmpty()) {
            return new BigDecimal[0][30];
        }
        
        // 获取全市的客户数量数据
        List<RegionClientNumData> cityDataList = regionClientNumDataService.findByTableNameAndRegion(regionClientNumDataService.generateTableName("按档位统一投放", null, false), "全市");
        if (cityDataList.isEmpty()) {
            log.error("未找到'全市'的客户数量数据");
            return new BigDecimal[0][30];
        }
        
        RegionClientNumData cityData = cityDataList.get(0);
        
        // 构建客户数量矩阵（只有一行，因为全市投放只有一个目标）
        BigDecimal[][] customerMatrix = new BigDecimal[1][30];
        customerMatrix[0] = cityData.getGradeArray();
        
        // 使用分配算法计算
        BigDecimal[][] matrix = distributionAlgorithm.calculateDistribution(targetList, customerMatrix, targetAmount);
        
        return matrix;
    }
}