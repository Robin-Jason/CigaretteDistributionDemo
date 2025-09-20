package org.example.service.CityUnifiedDistribution;

import lombok.extern.slf4j.Slf4j;

import org.example.entity.DemoTestAdvData;
import org.example.entity.DemoTestData;
import org.example.entity.CityUnifiedDistribution.CityClientNumData;
import org.example.repository.DemoTestAdvDataRepository;
import org.example.repository.DemoTestDataRepository;
import org.example.repository.CityUnifiedDistribution.CityClientNumDataRepository;
import org.example.service.algorithm.CityCigaretteDistributionAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class CityPredictionService {

    @Autowired
    private DemoTestAdvDataRepository advDataRepository;

    @Autowired
    private CityClientNumDataRepository cityClientNumDataRepository;

    @Autowired
    private DemoTestDataRepository testDataRepository;

    @Autowired
    private CityCigaretteDistributionAlgorithm distributionAlgorithm;

    @Transactional
    public void predictAndSave(Integer year, Integer month, Integer weekSeq) {
        log.info("开始全市投放量预测，年份: {}, 月份: {}, 周序号: {}", year, month, weekSeq);

        // 1. 获取所有预投放量数据
        List<DemoTestAdvData> advDataList = advDataRepository.findAll();
        log.info("获取到 {} 条预投放量数据", advDataList.size());

        // 2. 获取全市的客户数量数据
        Optional<CityClientNumData> cityDataOptional = cityClientNumDataRepository.findByUrbanRuralCode("全市");
        if (!cityDataOptional.isPresent()) {
            log.error("未找到'全市'的客户数量数据，预测中止");
            throw new RuntimeException("未找到'全市'的客户数量数据");
        }
        CityClientNumData cityData = cityDataOptional.get();

        // 3. 准备算法需要的客户数矩阵
        BigDecimal[][] regionCustomerMatrix = new BigDecimal[1][30];
        regionCustomerMatrix[0] = new BigDecimal[]{
                cityData.getD30(), cityData.getD29(), cityData.getD28(), cityData.getD27(), cityData.getD26(),
                cityData.getD25(), cityData.getD24(), cityData.getD23(), cityData.getD22(), cityData.getD21(),
                cityData.getD20(), cityData.getD19(), cityData.getD18(), cityData.getD17(), cityData.getD16(),
                cityData.getD15(), cityData.getD14(), cityData.getD13(), cityData.getD12(), cityData.getD11(),
                cityData.getD10(), cityData.getD9(), cityData.getD8(), cityData.getD7(), cityData.getD6(),
                cityData.getD5(), cityData.getD4(), cityData.getD3(), cityData.getD2(), cityData.getD1()
        };

        // 4. 遍历按全市统一投放的每个卷烟进行预测并写回数据库
        for (DemoTestAdvData advData : advDataList) {
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
                DemoTestData newTestData = new DemoTestData();
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
        Optional<CityClientNumData> cityDataOptional = cityClientNumDataRepository.findByUrbanRuralCode("全市");
        if (!cityDataOptional.isPresent()) {
            log.error("未找到'全市'的客户数量数据");
            return new BigDecimal[0][30];
        }
        
        CityClientNumData cityData = cityDataOptional.get();
        
        // 构建客户数量数组
        BigDecimal[] customerCounts = new BigDecimal[30];
        customerCounts[0] = cityData.getD30() != null ? cityData.getD30() : BigDecimal.ZERO;
        customerCounts[1] = cityData.getD29() != null ? cityData.getD29() : BigDecimal.ZERO;
        customerCounts[2] = cityData.getD28() != null ? cityData.getD28() : BigDecimal.ZERO;
        customerCounts[3] = cityData.getD27() != null ? cityData.getD27() : BigDecimal.ZERO;
        customerCounts[4] = cityData.getD26() != null ? cityData.getD26() : BigDecimal.ZERO;
        customerCounts[5] = cityData.getD25() != null ? cityData.getD25() : BigDecimal.ZERO;
        customerCounts[6] = cityData.getD24() != null ? cityData.getD24() : BigDecimal.ZERO;
        customerCounts[7] = cityData.getD23() != null ? cityData.getD23() : BigDecimal.ZERO;
        customerCounts[8] = cityData.getD22() != null ? cityData.getD22() : BigDecimal.ZERO;
        customerCounts[9] = cityData.getD21() != null ? cityData.getD21() : BigDecimal.ZERO;
        customerCounts[10] = cityData.getD20() != null ? cityData.getD20() : BigDecimal.ZERO;
        customerCounts[11] = cityData.getD19() != null ? cityData.getD19() : BigDecimal.ZERO;
        customerCounts[12] = cityData.getD18() != null ? cityData.getD18() : BigDecimal.ZERO;
        customerCounts[13] = cityData.getD17() != null ? cityData.getD17() : BigDecimal.ZERO;
        customerCounts[14] = cityData.getD16() != null ? cityData.getD16() : BigDecimal.ZERO;
        customerCounts[15] = cityData.getD15() != null ? cityData.getD15() : BigDecimal.ZERO;
        customerCounts[16] = cityData.getD14() != null ? cityData.getD14() : BigDecimal.ZERO;
        customerCounts[17] = cityData.getD13() != null ? cityData.getD13() : BigDecimal.ZERO;
        customerCounts[18] = cityData.getD12() != null ? cityData.getD12() : BigDecimal.ZERO;
        customerCounts[19] = cityData.getD11() != null ? cityData.getD11() : BigDecimal.ZERO;
        customerCounts[20] = cityData.getD10() != null ? cityData.getD10() : BigDecimal.ZERO;
        customerCounts[21] = cityData.getD9() != null ? cityData.getD9() : BigDecimal.ZERO;
        customerCounts[22] = cityData.getD8() != null ? cityData.getD8() : BigDecimal.ZERO;
        customerCounts[23] = cityData.getD7() != null ? cityData.getD7() : BigDecimal.ZERO;
        customerCounts[24] = cityData.getD6() != null ? cityData.getD6() : BigDecimal.ZERO;
        customerCounts[25] = cityData.getD5() != null ? cityData.getD5() : BigDecimal.ZERO;
        customerCounts[26] = cityData.getD4() != null ? cityData.getD4() : BigDecimal.ZERO;
        customerCounts[27] = cityData.getD3() != null ? cityData.getD3() : BigDecimal.ZERO;
        customerCounts[28] = cityData.getD2() != null ? cityData.getD2() : BigDecimal.ZERO;
        customerCounts[29] = cityData.getD1() != null ? cityData.getD1() : BigDecimal.ZERO;
        
        // 构建客户数量矩阵（只有一行，因为全市投放只有一个目标）
        BigDecimal[][] customerMatrix = new BigDecimal[1][30];
        customerMatrix[0] = customerCounts;
        
        // 使用分配算法计算
        BigDecimal[][] matrix = distributionAlgorithm.calculateDistribution(targetList, customerMatrix, targetAmount);
        
        return matrix;
    }
}