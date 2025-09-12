package org.example.service.algorithm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 卷烟分配算法 - 按市场类型（城网/农网）比例分配版本
 * 算法逻辑遵循：
 * 1. 按比例将总投放量分配给城网和农网。
 * 2. 对每个网络单独执行核心分配算法（贪心填充、迭代微调）。
 * 3. 合并所有分配结果并强制非递增约束。
 */
@Slf4j
@Service
public class MarketProportionalCigaretteDistributionAlgorithm {

    private static final int GRADE_COUNT = 30; // 档位数（D30到D1）
    private static final String URBAN_NETWORK = "城网";
    private static final String RURAL_NETWORK = "农网";
    private static final int MAX_ITERATIONS = 100; // 微调迭代次数 [cite: 126]
    private static final BigDecimal INCREMENT = BigDecimal.ONE; // 每次调整的步长 [cite: 126]

    /**
     * 卷烟分配算法 - 按比例分配版本
     * @param targetRegions 目标投放区域列表 (包含"城网"和"农网"下的具体区域)
     * @param regionCustomerMatrix 区域客户数矩阵 [区域数][档位数]
     * @param targetAmount 预投放量
     * @param urbanRatio 城网分配比例
     * @param ruralRatio 农网分配比例
     * @return 分配矩阵 [区域数][档位数]
     */
    public BigDecimal[][] calculateDistribution(List<String> targetRegions,
                                                BigDecimal[][] regionCustomerMatrix,
                                                BigDecimal targetAmount,
                                                BigDecimal urbanRatio,
                                                BigDecimal ruralRatio) {
        if (targetRegions == null || targetRegions.isEmpty() ||
                regionCustomerMatrix == null || targetAmount == null) {
            log.error("输入参数无效");
            return new BigDecimal[0][0];
        }

        Map<String, List<Integer>> regionIndicesByNetwork = new HashMap<>();
        regionIndicesByNetwork.put(URBAN_NETWORK, new ArrayList<>());
        regionIndicesByNetwork.put(RURAL_NETWORK, new ArrayList<>());

        // 1. 根据区域名称（城网/农网）将区域索引分组
        for (int i = 0; i < targetRegions.size(); i++) {
            String region = targetRegions.get(i);
            if (region.contains("城")) {
                regionIndicesByNetwork.get(URBAN_NETWORK).add(i);
            } else if (region.contains("农")) {
                regionIndicesByNetwork.get(RURAL_NETWORK).add(i);
            }
        }

        // 确保输入的比例不为空，否则使用默认值（0.4和0.6）
        BigDecimal effectiveUrbanRatio = (urbanRatio != null) ? urbanRatio : new BigDecimal("0.4");
        BigDecimal effectiveRuralRatio = (ruralRatio != null) ? ruralRatio : new BigDecimal("0.6");

        // 2. 根据比例计算每个网络的目标投放量
        BigDecimal urbanTarget = targetAmount.multiply(effectiveUrbanRatio);
        BigDecimal ruralTarget = targetAmount.multiply(effectiveRuralRatio);

        // 3. 分别为每个网络执行分配算法
        Map<String, BigDecimal[][]> partialMatrices = new HashMap<>();

        // 分配给城网
        if (!regionIndicesByNetwork.get(URBAN_NETWORK).isEmpty()) {
            List<String> urbanRegions = regionIndicesByNetwork.get(URBAN_NETWORK).stream()
                    .map(targetRegions::get).collect(Collectors.toList());
            BigDecimal[][] urbanCustomerMatrix = createPartialMatrix(regionIndicesByNetwork.get(URBAN_NETWORK), regionCustomerMatrix);
            partialMatrices.put(URBAN_NETWORK, runCoreAlgorithm(urbanRegions, urbanCustomerMatrix, urbanTarget));
        }

        // 分配给农网
        if (!regionIndicesByNetwork.get(RURAL_NETWORK).isEmpty()) {
            List<String> ruralRegions = regionIndicesByNetwork.get(RURAL_NETWORK).stream()
                    .map(targetRegions::get).collect(Collectors.toList());
            BigDecimal[][] ruralCustomerMatrix = createPartialMatrix(regionIndicesByNetwork.get(RURAL_NETWORK), regionCustomerMatrix);
            partialMatrices.put(RURAL_NETWORK, runCoreAlgorithm(ruralRegions, ruralCustomerMatrix, ruralTarget));
        }

        // 4. 合并所有分配矩阵
        BigDecimal[][] finalMatrix = new BigDecimal[targetRegions.size()][GRADE_COUNT];
        for (int i = 0; i < targetRegions.size(); i++) {
            for (int j = 0; j < GRADE_COUNT; j++) {
                finalMatrix[i][j] = BigDecimal.ZERO;
            }
        }

        // 合并城网结果
        List<Integer> urbanIndices = regionIndicesByNetwork.get(URBAN_NETWORK);
        if (!urbanIndices.isEmpty()) {
            BigDecimal[][] urbanMatrix = partialMatrices.get(URBAN_NETWORK);
            for (int i = 0; i < urbanIndices.size(); i++) {
                int originalIndex = urbanIndices.get(i);
                System.arraycopy(urbanMatrix[i], 0, finalMatrix[originalIndex], 0, GRADE_COUNT);
            }
        }

        // 合并农网结果
        List<Integer> ruralIndices = regionIndicesByNetwork.get(RURAL_NETWORK);
        if (!ruralIndices.isEmpty()) {
            BigDecimal[][] ruralMatrix = partialMatrices.get(RURAL_NETWORK);
            for (int i = 0; i < ruralIndices.size(); i++) {
                int originalIndex = ruralIndices.get(i);
                System.arraycopy(ruralMatrix[i], 0, finalMatrix[originalIndex], 0, GRADE_COUNT);
            }
        }

        // 5. 强制实施非递增约束（虽然子算法已经处理，但合并后再次检查以确保无误）
        enforceMonotonicConstraint(finalMatrix);

        BigDecimal finalAmount = calculateTotalAmount(finalMatrix, regionCustomerMatrix);
        log.info("按比例分配算法完成，目标投放量: {}, 实际投放量: {}, 误差: {}",
                targetAmount, finalAmount, targetAmount.subtract(finalAmount).abs());

        return finalMatrix;
    }

    /**
     * 核心分配算法，应用于单个网络（城网或农网）
     */
    private BigDecimal[][] runCoreAlgorithm(List<String> regions,
                                            BigDecimal[][] customerMatrix,
                                            BigDecimal targetAmount) {
        if (regions == null || regions.isEmpty() || customerMatrix == null || targetAmount == null) {
            log.error("输入参数无效");
            return new BigDecimal[0][0];
        }

        int regionCount = regions.size();
        BigDecimal[][] allocationMatrix = new BigDecimal[regionCount][GRADE_COUNT];
        for (int i = 0; i < regionCount; i++) {
            for (int j = 0; j < GRADE_COUNT; j++) {
                allocationMatrix[i][j] = BigDecimal.ZERO;
            }
        }

        try {
            // 1. 初始贪心分配 [cite: 126]
            greedyFill(allocationMatrix, customerMatrix, targetAmount);
            // 2. 迭代微调 [cite: 126]
            iterativeRefinement(allocationMatrix, customerMatrix, targetAmount);
            // 3. 最终强制非递增约束 [cite: 126]
            enforceMonotonicConstraint(allocationMatrix);

            BigDecimal finalAmount = calculateTotalAmount(allocationMatrix, customerMatrix);
            log.info("分配算法完成，目标投放量: {}, 实际投放量: {}, 误差: {}",
                    targetAmount, finalAmount, targetAmount.subtract(finalAmount).abs());
            return allocationMatrix;

        } catch (Exception e) {
            log.error("分配算法执行失败", e);
            return allocationMatrix;
        }
    }

    /**
     * [cite_start]贪心填充算法 [cite: 126]
     */
    private void greedyFill(BigDecimal[][] allocationMatrix,
                            BigDecimal[][] regionCustomerMatrix,
                            BigDecimal targetAmount) {
        BigDecimal currentAmount = BigDecimal.ZERO;
        boolean canStillAdd = true;

        while (canStillAdd) {
            canStillAdd = false;
            for (int j = 0; j < GRADE_COUNT; j++) {
                for (int i = 0; i < allocationMatrix.length; i++) {
                    // 使用修改后的isValidIncrement方法，该方法不修改矩阵 [cite: 126]
                    if (isValidIncrement(allocationMatrix, i, j)) {
                        BigDecimal customerCount = regionCustomerMatrix[i][j];
                        if (customerCount != null && currentAmount.add(customerCount).compareTo(targetAmount) <= 0) {
                            allocationMatrix[i][j] = allocationMatrix[i][j].add(INCREMENT);
                            currentAmount = currentAmount.add(customerCount);
                            canStillAdd = true;
                        }
                    }
                }
            }
        }
        log.info("贪心填充完成，当前投放量: {}", currentAmount);
    }

    /**
     * [cite_start]迭代微调 [cite: 126]
     */
    private void iterativeRefinement(BigDecimal[][] allocationMatrix,
                                     BigDecimal[][] regionCustomerMatrix,
                                     BigDecimal targetAmount) {
        BigDecimal currentError = targetAmount.subtract(calculateTotalAmount(allocationMatrix, regionCustomerMatrix)).abs();
        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            BigDecimal bestError = currentError;
            int best_i = -1, best_j = -1;
            BigDecimal best_change = BigDecimal.ZERO;

            // 尝试在每个点进行微小的调整（增或减）
            for (int i = 0; i < allocationMatrix.length; i++) {
                for (int j = 0; j < GRADE_COUNT; j++) {
                    // 尝试增加
                    allocationMatrix[i][j] = allocationMatrix[i][j].add(INCREMENT);
                    if (isMonotonic(allocationMatrix[i])) {
                        BigDecimal newError = targetAmount.subtract(calculateTotalAmount(allocationMatrix, regionCustomerMatrix)).abs();
                        if (newError.compareTo(bestError) < 0) {
                            bestError = newError;
                            best_i = i;
                            best_j = j;
                            best_change = INCREMENT;
                        }
                    }
                    allocationMatrix[i][j] = allocationMatrix[i][j].subtract(INCREMENT); // 回溯

                    // 尝试减少
                    if (allocationMatrix[i][j].compareTo(BigDecimal.ZERO) > 0) {
                        allocationMatrix[i][j] = allocationMatrix[i][j].subtract(INCREMENT);
                        if (isMonotonic(allocationMatrix[i])) {
                            BigDecimal newError = targetAmount.subtract(calculateTotalAmount(allocationMatrix, regionCustomerMatrix)).abs();
                            if (newError.compareTo(bestError) < 0) {
                                bestError = newError;
                                best_i = i;
                                best_j = j;
                                best_change = INCREMENT.negate();
                            }
                        }
                        allocationMatrix[i][j] = allocationMatrix[i][j].add(INCREMENT); // 回溯
                    }
                }
            }

            if (best_i != -1) {
                allocationMatrix[best_i][best_j] = allocationMatrix[best_i][best_j].add(best_change);
                currentError = bestError;
            } else {
                break; // 如果没有更好的移动，则停止
            }
        }
        log.info("微调完成，误差: {}", currentError);
    }

    /**
     * [cite_start]检查单行的非递增约束 [cite: 126]
     */
    private boolean isMonotonic(BigDecimal[] row) {
        for (int j = 1; j < row.length; j++) {
            if (row[j].compareTo(row[j - 1]) > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * [cite_start]检查增加分配值是否满足非递增约束（不修改矩阵） [cite: 126]
     */
    private boolean isValidIncrement(BigDecimal[][] matrix, int region, int grade) {
        if (grade == 0) return true; // 最高档位D30
        return matrix[region][grade].compareTo(matrix[region][grade - 1]) <= 0;
    }

    /**
     * 创建部分矩阵，只包含指定区域的客户数数据
     */
    private BigDecimal[][] createPartialMatrix(List<Integer> originalIndices, BigDecimal[][] originalMatrix) {
        BigDecimal[][] newMatrix = new BigDecimal[originalIndices.size()][GRADE_COUNT];
        for (int i = 0; i < originalIndices.size(); i++) {
            int originalIndex = originalIndices.get(i);
            System.arraycopy(originalMatrix[originalIndex], 0, newMatrix[i], 0, GRADE_COUNT);
        }
        return newMatrix;
    }

    /**
     * [cite_start]强制实施非递增约束 [cite: 126]
     */
    private void enforceMonotonicConstraint(BigDecimal[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 1; j < GRADE_COUNT; j++) {
                if (matrix[i][j].compareTo(matrix[i][j - 1]) > 0) {
                    matrix[i][j] = matrix[i][j - 1];
                }
            }
        }
    }

    /**
     * [cite_start]计算总投放量 [cite: 126]
     */
    private BigDecimal calculateTotalAmount(BigDecimal[][] allocationMatrix,
                                            BigDecimal[][] regionCustomerMatrix) {
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < allocationMatrix.length; i++) {
            for (int j = 0; j < GRADE_COUNT; j++) {
                if (allocationMatrix[i][j] != null && regionCustomerMatrix[i][j] != null) {
                    total = total.add(allocationMatrix[i][j].multiply(regionCustomerMatrix[i][j]));
                }
            }
        }
        return total;
    }

}