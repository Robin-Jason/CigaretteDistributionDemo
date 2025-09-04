package org.example.algorithm;

import org.example.entity.DemoTestAdvData;
import org.example.entity.DemoTestClientNumData;
import org.example.entity.DemoTestData;
import org.example.repository.DemoTestAdvDataRepository;
import org.example.repository.DemoTestClientNumDataRepository;
import org.example.repository.DemoTestDataRepository;
import org.example.service.algorithm.CigaretteDistributionAlgorithm;
import org.example.util.KmpMatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
public class CigaretteDistributionAlgorithmTest {

    @Autowired
    private DemoTestAdvDataRepository advDataRepository;

    @Autowired
    private DemoTestClientNumDataRepository clientNumDataRepository;

    @Autowired
    private DemoTestDataRepository testDataRepository;

    @Autowired
    private CigaretteDistributionAlgorithm distributionAlgorithm;

    @Autowired
    private KmpMatcher kmpMatcher;

    @Test
    public void testAlgorithmWithRealData() {
        System.out.println("=== 卷烟分配算法测试 ===");
        
        // 1. 获取预投放量数据
        List<DemoTestAdvData> advDataList = advDataRepository.findAll();
        System.out.println("\n1. 预投放量数据:");
        for (DemoTestAdvData advData : advDataList) {
            System.out.printf("卷烟代码: %s, 卷烟名称: %s, 预投放量: %s, URS: %s%n", 
                advData.getCigCode(), advData.getCigName(), advData.getAdv(), advData.getUrs());
        }

        // 2. 获取投放区域和客户数数据
        List<DemoTestClientNumData> clientNumDataList = clientNumDataRepository.findAllByOrderByIdAsc();
        System.out.println("\n2. 投放区域客户数数据:");
        for (DemoTestClientNumData clientData : clientNumDataList) {
            System.out.printf("投放区域: %s, 总客户数: %s%n", 
                clientData.getUrbanRuralCode(), clientData.getTotal());
        }

        // 3. 获取卷烟测试数据
        List<DemoTestData> testDataList = testDataRepository.findByYearAndMonthAndWeekSeq(2024, 1, 1);
        System.out.println("\n3. 卷烟测试数据:");
        for (DemoTestData testData : testDataList) {
            System.out.printf("卷烟代码: %s, 卷烟名称: %s, 投放区域: %s%n", 
                testData.getCigCode(), testData.getCigName(), testData.getDeliveryArea());
        }

        // 4. 测试KMP匹配
        System.out.println("\n4. KMP匹配测试:");
        List<String> allRegions = clientNumDataRepository.findAllByOrderByIdAsc()
                .stream()
                .map(DemoTestClientNumData::getUrbanRuralCode)
                .collect(java.util.stream.Collectors.toList());
        
        for (DemoTestData testData : testDataList) {
            if (testData.getDeliveryArea() != null) {
                List<String> matchedRegions = kmpMatcher.matchPatterns(testData.getDeliveryArea(), allRegions);
                System.out.printf("卷烟 %s 的投放区域 '%s' 匹配到: %s%n", 
                    testData.getCigName(), testData.getDeliveryArea(), matchedRegions);
            }
        }

        // 5. 测试分配算法
        System.out.println("\n5. 分配算法测试:");
        for (DemoTestAdvData advData : advDataList) {
            System.out.printf("\n--- 测试卷烟: %s (%s) ---%n", advData.getCigName(), advData.getCigCode());
            
            // 找到对应的测试数据
            List<DemoTestData> relatedTestData = testDataRepository.findByCigCodeAndCigName(
                advData.getCigCode(), advData.getCigName());
            
            if (!relatedTestData.isEmpty()) {
                // 获取投放区域
                String deliveryArea = relatedTestData.get(0).getDeliveryArea();
                List<String> targetRegions = kmpMatcher.matchPatterns(deliveryArea, allRegions);
                
                System.out.printf("目标投放区域: %s%n", targetRegions);
                System.out.printf("预投放量: %s%n", advData.getAdv());
                
                if (!targetRegions.isEmpty()) {
                    // 构建区域客户数矩阵
                    BigDecimal[][] regionCustomerMatrix = buildRegionCustomerMatrix(targetRegions, clientNumDataList);
                    
                    // 执行分配算法
                    BigDecimal[][] allocationMatrix = distributionAlgorithm.calculateDistribution(
                        targetRegions, regionCustomerMatrix, advData.getAdv());
                    
                    // 输出分配结果
                    printAllocationResult(allocationMatrix, targetRegions, regionCustomerMatrix, advData.getAdv());
                }
            }
        }
    }

    /**
     * 构建区域客户数矩阵
     */
    private BigDecimal[][] buildRegionCustomerMatrix(List<String> targetRegions, 
                                                   List<DemoTestClientNumData> clientNumDataList) {
        BigDecimal[][] matrix = new BigDecimal[targetRegions.size()][30];
        
        for (int i = 0; i < targetRegions.size(); i++) {
            String targetRegion = targetRegions.get(i);
            DemoTestClientNumData clientData = clientNumDataList.stream()
                    .filter(data -> targetRegion.equals(data.getUrbanRuralCode()))
                    .findFirst()
                    .orElse(null);
            
            if (clientData != null) {
                matrix[i][0] = clientData.getD30();
                matrix[i][1] = clientData.getD29();
                matrix[i][2] = clientData.getD28();
                matrix[i][3] = clientData.getD27();
                matrix[i][4] = clientData.getD26();
                matrix[i][5] = clientData.getD25();
                matrix[i][6] = clientData.getD24();
                matrix[i][7] = clientData.getD23();
                matrix[i][8] = clientData.getD22();
                matrix[i][9] = clientData.getD21();
                matrix[i][10] = clientData.getD20();
                matrix[i][11] = clientData.getD19();
                matrix[i][12] = clientData.getD18();
                matrix[i][13] = clientData.getD17();
                matrix[i][14] = clientData.getD16();
                matrix[i][15] = clientData.getD15();
                matrix[i][16] = clientData.getD14();
                matrix[i][17] = clientData.getD13();
                matrix[i][18] = clientData.getD12();
                matrix[i][19] = clientData.getD11();
                matrix[i][20] = clientData.getD10();
                matrix[i][21] = clientData.getD9();
                matrix[i][22] = clientData.getD8();
                matrix[i][23] = clientData.getD7();
                matrix[i][24] = clientData.getD6();
                matrix[i][25] = clientData.getD5();
                matrix[i][26] = clientData.getD4();
                matrix[i][27] = clientData.getD3();
                matrix[i][28] = clientData.getD2();
                matrix[i][29] = clientData.getD1();
            }
        }
        
        return matrix;
    }

    /**
     * 输出分配结果
     */
    private void printAllocationResult(BigDecimal[][] allocationMatrix, 
                                     List<String> targetRegions,
                                     BigDecimal[][] regionCustomerMatrix,
                                     BigDecimal targetAmount) {
        System.out.println("分配矩阵结果:");
        System.out.println("档位:    " + String.format("%-8s", "D30") + String.format("%-8s", "D29") + 
                          String.format("%-8s", "D28") + String.format("%-8s", "D27") + 
                          String.format("%-8s", "D26") + String.format("%-8s", "D25") + "...");
        
        for (int i = 0; i < targetRegions.size(); i++) {
            System.out.printf("区域%-4s: ", targetRegions.get(i));
            for (int j = 0; j < 30; j++) {
                if (j < 6) { // 只显示前6个档位
                    System.out.printf("%-8s", allocationMatrix[i][j]);
                }
            }
            System.out.println("...");
        }
        
        // 计算实际投放量
        BigDecimal actualAmount = BigDecimal.ZERO;
        for (int i = 0; i < targetRegions.size(); i++) {
            for (int j = 0; j < 30; j++) {
                if (allocationMatrix[i][j] != null && regionCustomerMatrix[i][j] != null) {
                    actualAmount = actualAmount.add(allocationMatrix[i][j].multiply(regionCustomerMatrix[i][j]));
                }
            }
        }
        
        System.out.printf("目标投放量: %s%n", targetAmount);
        System.out.printf("实际投放量: %s%n", actualAmount);
        System.out.printf("误差: %s%n", targetAmount.subtract(actualAmount).abs());
        
        // 验证非递增约束
        boolean isValid = validateNonIncreasingConstraint(allocationMatrix);
        System.out.printf("非递增约束验证: %s%n", isValid ? "通过" : "失败");
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
}
