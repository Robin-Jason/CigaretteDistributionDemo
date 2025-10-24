package org.example.service.algorithm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class countyCigaretteDistributionAlgorithm {

    private static final int GRADE_COUNT = 30; // 档位数（D30到D1）
    private static final int MAX_ITERATIONS = 100; // 微调迭代次数
    private static final BigDecimal INCREMENT = BigDecimal.ONE; // 每次调整的步长

    public BigDecimal[][] calculateDistribution(List<String> targetRegions,
                                                BigDecimal[][] regionCustomerMatrix,
                                                BigDecimal targetAmount) {
        if (targetRegions == null || targetRegions.isEmpty() || regionCustomerMatrix == null || targetAmount == null) {
            log.error("输入参数无效");
            return new BigDecimal[0][0];
        }

        int regionCount = targetRegions.size();
        BigDecimal[][] allocationMatrix = new BigDecimal[regionCount][GRADE_COUNT];
        for (int i = 0; i < regionCount; i++) {
            for (int j = 0; j < GRADE_COUNT; j++) {
                allocationMatrix[i][j] = BigDecimal.ZERO;
            }
        }

        try {
            // 1. 初始贪心分配
            greedyFill(allocationMatrix, regionCustomerMatrix, targetAmount);

            // 2. 迭代微调
            iterativeRefinement(allocationMatrix, regionCustomerMatrix, targetAmount);

            // 3. 最终强制非递增约束
            enforceMonotonicConstraint(allocationMatrix);

            BigDecimal finalAmount = calculateTotalAmount(allocationMatrix, regionCustomerMatrix);
            log.info("分配算法完成，目标投放量: {}, 实际投放量: {}, 误差: {}",
                    targetAmount, finalAmount, targetAmount.subtract(finalAmount).abs());

            return allocationMatrix;

        } catch (Exception e) {
            log.error("分配算法执行失败", e);
            return allocationMatrix;
        }
    }

    /**
     * 贪心填充算法
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
     * 迭代微调
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
     * 检查单行的非递增约束
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
     * 检查增加分配值是否满足非递增约束
     */
    private boolean isValidIncrement(BigDecimal[][] matrix, int region, int grade) {
        // 增加当前档位的值
        matrix[region][grade] = matrix[region][grade].add(INCREMENT);
        boolean valid = true;
        // 检查是否仍然满足非递增
        if (grade > 0 && matrix[region][grade].compareTo(matrix[region][grade - 1]) > 0) {
            valid = false;
        }
        // 撤销增加
        matrix[region][grade] = matrix[region][grade].subtract(INCREMENT);
        return valid;
    }

    /**
     * 强制实施非递增约束
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
     * 计算总投放量
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
