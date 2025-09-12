package org.example.algorithm;

import org.example.entity.DemoTestAdvData;
import org.example.entity.DemoTestCountyClientNumData;
import org.example.repository.DemoTestAdvDataRepository;
import org.example.repository.DemoTestCountyClientNumDataRepository;
import org.example.service.algorithm.countyCigaretteDistributionAlgorithm;
import org.example.util.KmpMatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * “档位+区县”业务逻辑的单元测试类。
 */
@SpringBootTest
@ActiveProfiles("test")
public class CountyCigaretteDistributionAlgorithmTest {

    @Autowired
    private DemoTestAdvDataRepository advDataRepository;

    @Autowired
    private DemoTestCountyClientNumDataRepository countyClientNumDataRepository;

    @Autowired
    private countyCigaretteDistributionAlgorithm distributionAlgorithm;

    @Autowired
    private KmpMatcher kmpMatcher;

    @Test
    public void testCountyAlgorithmWithRealData() {
        System.out.println("=== “档位+区县”分配算法测试 ===");

        // 1. 获取预投放量数据，并筛选出按“档位+区县”投放的卷烟
        List<DemoTestAdvData> advDataList = advDataRepository.findAll().stream()
                .filter(adv -> "档位+区县".equals(adv.getDeliveryEtype()))
                .collect(Collectors.toList());
        System.out.println("\n1. 预投放量数据 (档位+区县):");
        for (DemoTestAdvData advData : advDataList) {
            System.out.printf("卷烟代码: %s, 卷烟名称: %s, 预投放量: %s, 投放区域: %s%n",
                    advData.getCigCode(), advData.getCigName(), advData.getAdv(), advData.getDeliveryArea());
        }

        // 2. 获取所有区县的客户数数据
        List<DemoTestCountyClientNumData> clientNumDataList = countyClientNumDataRepository.findAllByOrderByIdAsc();
        System.out.println("\n2. 区县客户数数据:");
        for (DemoTestCountyClientNumData clientData : clientNumDataList) {
            System.out.printf("区县: %s, 总客户数: %s%n",
                    clientData.getCounty(), clientData.getTotal());
        }

        List<String> allCounties = clientNumDataList.stream()
                .map(DemoTestCountyClientNumData::getCounty)
                .collect(Collectors.toList());

        // 3. 测试KMP匹配
        System.out.println("\n3. KMP匹配测试:");
        for (DemoTestAdvData advData : advDataList) {
            if (advData.getDeliveryArea() != null) {
                List<String> matchedCounties = kmpMatcher.matchPatterns(advData.getDeliveryArea(), allCounties);
                System.out.printf("卷烟 '%s' 的投放区域 '%s' 匹配到: %s%n",
                        advData.getCigName(), advData.getDeliveryArea(), matchedCounties);
            }
        }

        // 4. 完整流程测试：匹配、构建矩阵、执行算法、验证结果
        System.out.println("\n4. 分配算法完整流程测试:");
        for (DemoTestAdvData advData : advDataList) {
            System.out.printf("\n--- 测试卷烟: %s (%s) ---%n", advData.getCigName(), advData.getCigCode());

            String deliveryArea = advData.getDeliveryArea();
            if (deliveryArea != null && !deliveryArea.trim().isEmpty()) {
                List<String> targetCounties = kmpMatcher.matchPatterns(deliveryArea, allCounties);
                System.out.printf("目标投放区县: %s%n", targetCounties);
                System.out.printf("预投放量: %s%n", advData.getAdv());

                if (!targetCounties.isEmpty()) {
                    // 构建目标区县的客户数矩阵
                    BigDecimal[][] countyCustomerMatrix = buildCountyCustomerMatrix(targetCounties, clientNumDataList);

                    // 执行分配算法
                    BigDecimal[][] allocationMatrix = distributionAlgorithm.calculateDistribution(
                            targetCounties, countyCustomerMatrix, advData.getAdv());

                    // 输出并验证分配结果
                    printAllocationResult(allocationMatrix, targetCounties, countyCustomerMatrix, advData.getAdv());
                }
            }
        }
    }

    /**
     * 根据目标区县列表，构建对应的客户数量矩阵。
     */
    private BigDecimal[][] buildCountyCustomerMatrix(List<String> targetCounties, List<DemoTestCountyClientNumData> allClientData) {
        BigDecimal[][] matrix = new BigDecimal[targetCounties.size()][30];
        for (int i = 0; i < targetCounties.size(); i++) {
            String targetCounty = targetCounties.get(i);
            DemoTestCountyClientNumData clientData = allClientData.stream()
                    .filter(data -> targetCounty.equals(data.getCounty()))
                    .findFirst()
                    .orElse(null);

            if (clientData != null) {
                // 档位从D30到D1，对应数组索引0到29
                matrix[i][0] = clientData.getD30(); matrix[i][1] = clientData.getD29();
                matrix[i][2] = clientData.getD28(); matrix[i][3] = clientData.getD27();
                matrix[i][4] = clientData.getD26(); matrix[i][5] = clientData.getD25();
                matrix[i][6] = clientData.getD24(); matrix[i][7] = clientData.getD23();
                matrix[i][8] = clientData.getD22(); matrix[i][9] = clientData.getD21();
                matrix[i][10] = clientData.getD20(); matrix[i][11] = clientData.getD19();
                matrix[i][12] = clientData.getD18(); matrix[i][13] = clientData.getD17();
                matrix[i][14] = clientData.getD16(); matrix[i][15] = clientData.getD15();
                matrix[i][16] = clientData.getD14(); matrix[i][17] = clientData.getD13();
                matrix[i][18] = clientData.getD12(); matrix[i][19] = clientData.getD11();
                matrix[i][20] = clientData.getD10(); matrix[i][21] = clientData.getD9();
                matrix[i][22] = clientData.getD8(); matrix[i][23] = clientData.getD7();
                matrix[i][24] = clientData.getD6(); matrix[i][25] = clientData.getD5();
                matrix[i][26] = clientData.getD4(); matrix[i][27] = clientData.getD3();
                matrix[i][28] = clientData.getD2(); matrix[i][29] = clientData.getD1();
            }
        }
        return matrix;
    }

    /**
     * 打印分配结果到控制台，并进行验证。
     */
    private void printAllocationResult(BigDecimal[][] allocationMatrix, List<String> targetCounties,
                                       BigDecimal[][] countyCustomerMatrix, BigDecimal targetAmount) {
        System.out.println("分配矩阵结果 (仅显示前6个档位):");
        System.out.println("区县/档位: " + String.format("%-8s", "D30") + String.format("%-8s", "D29") +
                String.format("%-8s", "D28") + String.format("%-8s", "D27") +
                String.format("%-8s", "D26") + String.format("%-8s", "D25") + "...");

        for (int i = 0; i < targetCounties.size(); i++) {
            System.out.printf("%-8s: ", targetCounties.get(i));
            for (int j = 0; j < 6; j++) { // 只显示前6个档位以简化输出
                System.out.printf("%-8s", allocationMatrix[i][j]);
            }
            System.out.println("...");
        }

        // 计算实际投放量
        BigDecimal actualAmount = BigDecimal.ZERO;
        for (int i = 0; i < targetCounties.size(); i++) {
            for (int j = 0; j < 30; j++) {
                if (allocationMatrix[i][j] != null && countyCustomerMatrix[i][j] != null) {
                    actualAmount = actualAmount.add(allocationMatrix[i][j].multiply(countyCustomerMatrix[i][j]));
                }
            }
        }

        System.out.printf("目标投放量: %s%n", targetAmount);
        System.out.printf("实际投放量: %s%n", actualAmount);
        System.out.printf("误    差: %s%n", targetAmount.subtract(actualAmount).abs());

        // 验证非递增约束
        boolean isValid = validateNonIncreasingConstraint(allocationMatrix);
        System.out.printf("非递增约束验证: %s%n", isValid ? "通过" : "失败");
    }

    /**
     * 验证分配矩阵是否满足非递增约束。
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