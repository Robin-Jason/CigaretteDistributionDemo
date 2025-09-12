package org.example.algorithm;

import org.example.entity.DemoTestAdvData;
import org.example.entity.DemoMarketTestClientNumData;
import org.example.repository.DemoTestAdvDataRepository;
import org.example.repository.DemoMarketTestClientNumDataRepository;
import org.example.service.algorithm.MarketProportionalCigaretteDistributionAlgorithm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
public class MarketProportionalCigaretteDistributionAlgorithmTest {

    @Autowired
    private DemoTestAdvDataRepository advDataRepository;

    @Autowired
    private DemoMarketTestClientNumDataRepository marketClientNumDataRepository;

    @Autowired
    private MarketProportionalCigaretteDistributionAlgorithm distributionAlgorithm;

    @Test
    public void testProportionalAlgorithmWithMarketData() {
        System.out.println("=== 按比例分配算法测试 (档位+市场类型) ===");

        // 1. 获取所有预投放量数据
        List<DemoTestAdvData> allAdvDataList = advDataRepository.findAll();

        // 2. 筛选出投放方式为"按档位扩展投放"且扩展类型为"档位+市场类型"的数据
        List<DemoTestAdvData> filteredAdvData = allAdvDataList.stream()
                .filter(data -> "按档位扩展投放".equals(data.getDeliveryMethod()) &&
                        "档位+市场类型".equals(data.getDeliveryEtype()))
                .collect(Collectors.toList());

        System.out.printf("\n筛选出 %d 条符合条件的预投放量数据%n", filteredAdvData.size());

        // 3. 获取所有区域客户数数据 (使用正确的Repository)
        List<DemoMarketTestClientNumData> marketClientNumDataList = marketClientNumDataRepository.findAllByOrderByUrbanRuralCodeAsc();
        List<String> allRegions = marketClientNumDataList.stream()
                .map(DemoMarketTestClientNumData::getUrbanRuralCode)
                .collect(Collectors.toList());

        // 4. 构建完整的区域客户数矩阵
        BigDecimal[][] fullRegionCustomerMatrix = buildMarketRegionCustomerMatrix(allRegions, marketClientNumDataList);

        // 5. 对每个符合条件的卷烟执行分配算法
        for (DemoTestAdvData advData : filteredAdvData) {
            System.out.printf("\n--- 测试卷烟: %s (%s) ---%n", advData.getCigName(), advData.getCigCode());

            BigDecimal targetAmount = advData.getAdv();

            // 目标投放区域为 "城网" 和 "农网"
            List<String> targetRegions = Arrays.asList("城网", "农网");

            // 筛选出目标区域的客户数矩阵
            BigDecimal[][] targetRegionCustomerMatrix = new BigDecimal[targetRegions.size()][30];
            for (int i = 0; i < targetRegions.size(); i++) {
                String targetRegion = targetRegions.get(i);
                int regionIndex = allRegions.indexOf(targetRegion);
                if (regionIndex >= 0) {
                    System.arraycopy(fullRegionCustomerMatrix[regionIndex], 0, targetRegionCustomerMatrix[i], 0, 30);
                }
            }

            // 执行按比例分配算法，使用0.4和0.6的比例
            BigDecimal urbanRatio = new BigDecimal("0.4");
            BigDecimal ruralRatio = new BigDecimal("0.6");
            BigDecimal[][] allocationMatrix = distributionAlgorithm.calculateDistribution(
                    targetRegions, targetRegionCustomerMatrix, targetAmount, urbanRatio, ruralRatio);

            // 输出分配结果
            printAllocationResult(allocationMatrix, targetRegions, targetRegionCustomerMatrix, targetAmount);

            // 验证非递增约束
            boolean isConstraintValid = validateNonIncreasingConstraint(allocationMatrix);
            assertTrue(isConstraintValid, "分配矩阵必须满足非递增约束");
        }

        System.out.println("=== 按比例分配算法测试完成 ===");
    }

    /**
     * 构建区域客户数矩阵 (针对市场类型)
     */
    private BigDecimal[][] buildMarketRegionCustomerMatrix(List<String> regions, List<DemoMarketTestClientNumData> clientNumDataList) {
        BigDecimal[][] matrix = new BigDecimal[regions.size()][30];

        for (int i = 0; i < regions.size(); i++) {
            String region = regions.get(i);
            DemoMarketTestClientNumData clientData = clientNumDataList.stream()
                    .filter(data -> region.equals(data.getUrbanRuralCode()))
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