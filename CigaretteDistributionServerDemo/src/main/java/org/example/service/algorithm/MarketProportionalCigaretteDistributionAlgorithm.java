package org.example.service.algorithm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MarketProportionalCigaretteDistributionAlgorithm {

    private static final int GRADE_COUNT = 30;
    private static final String URBAN_NETWORK = "城网";
    private static final String RURAL_NETWORK = "农网";
    private static final int MAX_ITERATIONS = 100;
    private static final BigDecimal INCREMENT = BigDecimal.ONE;

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

        for (int i = 0; i < targetRegions.size(); i++) {
            String region = targetRegions.get(i);
            if (region.contains("城")) {
                regionIndicesByNetwork.get(URBAN_NETWORK).add(i);
            } else if (region.contains("农")) {
                regionIndicesByNetwork.get(RURAL_NETWORK).add(i);
            }
        }

        BigDecimal effectiveUrbanRatio = (urbanRatio != null) ? urbanRatio : new BigDecimal("0.4");
        BigDecimal effectiveRuralRatio = (ruralRatio != null) ? ruralRatio : new BigDecimal("0.6");

        BigDecimal urbanTarget = targetAmount.multiply(effectiveUrbanRatio);
        BigDecimal ruralTarget = targetAmount.multiply(effectiveRuralRatio);

        Map<String, BigDecimal[][]> partialMatrices = new HashMap<>();

        if (!regionIndicesByNetwork.get(URBAN_NETWORK).isEmpty()) {
            List<String> urbanRegions = regionIndicesByNetwork.get(URBAN_NETWORK).stream()
                    .map(targetRegions::get).collect(Collectors.toList());
            BigDecimal[][] urbanCustomerMatrix = createPartialMatrix(regionIndicesByNetwork.get(URBAN_NETWORK), regionCustomerMatrix);
            partialMatrices.put(URBAN_NETWORK, runCoreAlgorithm(urbanRegions, urbanCustomerMatrix, urbanTarget));
        }

        if (!regionIndicesByNetwork.get(RURAL_NETWORK).isEmpty()) {
            List<String> ruralRegions = regionIndicesByNetwork.get(RURAL_NETWORK).stream()
                    .map(targetRegions::get).collect(Collectors.toList());
            BigDecimal[][] ruralCustomerMatrix = createPartialMatrix(regionIndicesByNetwork.get(RURAL_NETWORK), regionCustomerMatrix);
            partialMatrices.put(RURAL_NETWORK, runCoreAlgorithm(ruralRegions, ruralCustomerMatrix, ruralTarget));
        }

        BigDecimal[][] finalMatrix = new BigDecimal[targetRegions.size()][GRADE_COUNT];
        for (int i = 0; i < targetRegions.size(); i++) {
            for (int j = 0; j < GRADE_COUNT; j++) {
                finalMatrix[i][j] = BigDecimal.ZERO;
            }
        }

        List<Integer> urbanIndices = regionIndicesByNetwork.get(URBAN_NETWORK);
        if (!urbanIndices.isEmpty()) {
            BigDecimal[][] urbanMatrix = partialMatrices.get(URBAN_NETWORK);
            for (int i = 0; i < urbanIndices.size(); i++) {
                int originalIndex = urbanIndices.get(i);
                System.arraycopy(urbanMatrix[i], 0, finalMatrix[originalIndex], 0, GRADE_COUNT);
            }
        }

        List<Integer> ruralIndices = regionIndicesByNetwork.get(RURAL_NETWORK);
        if (!ruralIndices.isEmpty()) {
            BigDecimal[][] ruralMatrix = partialMatrices.get(RURAL_NETWORK);
            for (int i = 0; i < ruralIndices.size(); i++) {
                int originalIndex = ruralIndices.get(i);
                System.arraycopy(ruralMatrix[i], 0, finalMatrix[originalIndex], 0, GRADE_COUNT);
            }
        }

        enforceSmoothDecreaseConstraint(finalMatrix);

        BigDecimal finalAmount = calculateTotalAmount(finalMatrix, regionCustomerMatrix);
        log.info("按比例分配算法完成，目标投放量: {}, 实际投放量: {}, 误差: {}",
                targetAmount, finalAmount, targetAmount.subtract(finalAmount).abs());

        return finalMatrix;
    }

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
            greedyFill(allocationMatrix, customerMatrix, targetAmount);
            iterativeRefinement(allocationMatrix, customerMatrix, targetAmount);
            enforceSmoothDecreaseConstraint(allocationMatrix);

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
     * 【已修改】贪心填充算法，采用“分层填充”策略以实现最大程度的均匀分配。
     * 优先将所有能分配1个的位置分配完，再考虑分配第2个，以此类推。
     */
    private void greedyFill(BigDecimal[][] allocationMatrix,
                            BigDecimal[][] regionCustomerMatrix,
                            BigDecimal targetAmount) {
        BigDecimal currentAmount = BigDecimal.ZERO;

        // 设置一个合理的分配上限，避免在某些情况下无限循环
        int maxAllocationValue = allocationMatrix.length > 0 ? targetAmount.intValue() / allocationMatrix.length + GRADE_COUNT : GRADE_COUNT;

        for (int level = 0; level < maxAllocationValue; level++) {
            BigDecimal currentLevelValue = new BigDecimal(level);
            boolean allocatedInThisLevel;

            do {
                allocatedInThisLevel = false;
                // 从左到右（高档位到低档位）遍历，但增加了严格的平坦度检查
                for (int j = 0; j < GRADE_COUNT; j++) {
                    for (int i = 0; i < allocationMatrix.length; i++) {

                        // 检查1: 当前位置是否正好是我们要提升的层级
                        if (allocationMatrix[i][j].compareTo(currentLevelValue) == 0) {

                            // 检查2: 【核心变更】确保右侧不存在“洼地”
                            // 即，右侧所有档位的值都必须 >= 当前层级值
                            boolean isFlatEnough = true;
                            for (int k = j + 1; k < GRADE_COUNT; k++) {
                                if (allocationMatrix[i][k].compareTo(currentLevelValue) < 0) {
                                    isFlatEnough = false;
                                    break;
                                }
                            }

                            if (!isFlatEnough) {
                                continue; // 如果右边还有没填平的，就跳过当前这个，先去填右边的
                            }

                            // 检查3: 检查增加后是否满足平滑约束，并且投放总量不超过目标
                            if (canIncrementSmoothly(allocationMatrix, i, j)) {
                                BigDecimal customerCount = regionCustomerMatrix[i][j];
                                if (customerCount != null && customerCount.compareTo(BigDecimal.ZERO) > 0 &&
                                        currentAmount.add(customerCount).compareTo(targetAmount) <= 0) {

                                    allocationMatrix[i][j] = allocationMatrix[i][j].add(INCREMENT);
                                    currentAmount = currentAmount.add(customerCount);
                                    allocatedInThisLevel = true; // 标记本轮有分配发生
                                }
                            }
                        }
                    }
                }
            } while (allocatedInThisLevel);

            // 如果总投放量已达标，提前退出
            if (currentAmount.compareTo(targetAmount) >= 0) {
                break;
            }
        }
        log.info("贪心填充完成，当前投放量: {}", currentAmount);
    }
    private void iterativeRefinement(BigDecimal[][] allocationMatrix,
                                     BigDecimal[][] regionCustomerMatrix,
                                     BigDecimal targetAmount) {
        BigDecimal currentError = targetAmount.subtract(calculateTotalAmount(allocationMatrix, regionCustomerMatrix)).abs();
        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            BigDecimal bestError = currentError;
            int best_i = -1, best_j = -1;
            BigDecimal best_change = BigDecimal.ZERO;

            for (int i = 0; i < allocationMatrix.length; i++) {
                for (int j = 0; j < GRADE_COUNT; j++) {
                    allocationMatrix[i][j] = allocationMatrix[i][j].add(INCREMENT);
                    if (isSmoothlyDecreasing(allocationMatrix[i])) {
                        BigDecimal newError = targetAmount.subtract(calculateTotalAmount(allocationMatrix, regionCustomerMatrix)).abs();
                        if (newError.compareTo(bestError) < 0) {
                            bestError = newError;
                            best_i = i;
                            best_j = j;
                            best_change = INCREMENT;
                        }
                    }
                    allocationMatrix[i][j] = allocationMatrix[i][j].subtract(INCREMENT);

                    if (allocationMatrix[i][j].compareTo(BigDecimal.ZERO) > 0) {
                        allocationMatrix[i][j] = allocationMatrix[i][j].subtract(INCREMENT);
                        if (isSmoothlyDecreasing(allocationMatrix[i])) {
                            BigDecimal newError = targetAmount.subtract(calculateTotalAmount(allocationMatrix, regionCustomerMatrix)).abs();
                            if (newError.compareTo(bestError) < 0) {
                                bestError = newError;
                                best_i = i;
                                best_j = j;
                                best_change = INCREMENT.negate();
                            }
                        }
                        allocationMatrix[i][j] = allocationMatrix[i][j].add(INCREMENT);
                    }
                }
            }

            if (best_i != -1) {
                allocationMatrix[best_i][best_j] = allocationMatrix[best_i][best_j].add(best_change);
                currentError = bestError;
            } else {
                break;
            }
        }
        log.info("微调完成，误差: {}", currentError);
    }

    /**
     * 新增: 检查单行是否满足平滑非递增约束
     * 规则: 1. 必须非递增; 2. 相邻差值不能大于1
     */
    private boolean isSmoothlyDecreasing(BigDecimal[] row) {
        for (int j = 1; j < row.length; j++) {
            if (row[j].compareTo(row[j - 1]) > 0) {
                return false; // 规则1: 必须非递增
            }
            if (row[j - 1].subtract(row[j]).compareTo(BigDecimal.ONE) > 0) {
                return false; // 规则2: 相邻差值不能大于1
            }
        }
        return true;
    }

    /**
     * 新增: 检查在某点增加分配值后是否依然满足平滑约束
     * 会同时检查与前一个元素和后一个元素的关系
     */
    private boolean canIncrementSmoothly(BigDecimal[][] matrix, int region, int grade) {
        BigDecimal incrementedValue = matrix[region][grade].add(INCREMENT);

        // 检查与前一个档位(j-1)的关系：不能比它大
        if (grade > 0) {
            if (incrementedValue.compareTo(matrix[region][grade - 1]) > 0) {
                return false;
            }
        }
        // 检查与后一个档位(j+1)的关系：差值不能大于1
        if (grade < GRADE_COUNT - 1) {
            if (incrementedValue.subtract(matrix[region][grade + 1]).compareTo(BigDecimal.ONE) > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 新增: 强制实施平滑非递增约束
     */
    private void enforceSmoothDecreaseConstraint(BigDecimal[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 1; j < GRADE_COUNT; j++) {
                // 如果当前值 > 前一个值，则将其拉低至与前一个值相等
                if (matrix[i][j].compareTo(matrix[i][j - 1]) > 0) {
                    matrix[i][j] = matrix[i][j - 1];
                }
                // 如果 (前一个值 - 当前值) > 1，则将其拉高，使得差值为1
                else if (matrix[i][j - 1].subtract(matrix[i][j]).compareTo(BigDecimal.ONE) > 0) {
                    matrix[i][j] = matrix[i][j - 1].subtract(BigDecimal.ONE);
                }
            }
        }
    }

    // ----- 以下为对旧函数的重定向和辅助函数 -----

    private boolean isMonotonic(BigDecimal[] row) {
        return isSmoothlyDecreasing(row); // 重定向到新的检查函数
    }

    private boolean isValidIncrement(BigDecimal[][] matrix, int region, int grade) {
        return canIncrementSmoothly(matrix, region, grade); // 重定向到新的检查函数
    }

    private void enforceMonotonicConstraint(BigDecimal[][] matrix) {
        enforceSmoothDecreaseConstraint(matrix); // 重定向到新的约束函数
    }

    private BigDecimal[][] createPartialMatrix(List<Integer> originalIndices, BigDecimal[][] originalMatrix) {
        BigDecimal[][] newMatrix = new BigDecimal[originalIndices.size()][GRADE_COUNT];
        for (int i = 0; i < originalIndices.size(); i++) {
            int originalIndex = originalIndices.get(i);
            System.arraycopy(originalMatrix[originalIndex], 0, newMatrix[i], 0, GRADE_COUNT);
        }
        return newMatrix;
    }

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