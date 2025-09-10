//package org.example.service.algorithm;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import java.math.BigDecimal;
//import java.util.ArrayList;
//import java.util.List;
//
//@Slf4j
//@Service
//public class CigaretteDistributionAlgorithm {
//
//    private static final int GRADE_COUNT = 30; // 档位数（D30到D1）
//
//    /**
//     * 卷烟分配算法 - 根据需求描述优化版本
//     * @param targetRegions 目标投放区域列表
//     * @param regionCustomerMatrix 区域客户数矩阵 [区域数][档位数]
//     * @param targetAmount 预投放量
//     * @return 分配矩阵 [区域数][档位数]
//     */
//    public BigDecimal[][] calculateDistribution(List<String> targetRegions,
//                                             BigDecimal[][] regionCustomerMatrix,
//                                             BigDecimal targetAmount) {
//        if (targetRegions == null || targetRegions.isEmpty() ||
//            regionCustomerMatrix == null || targetAmount == null) {
//            log.error("输入参数无效");
//            return new BigDecimal[0][0];
//        }
//
//        int regionCount = targetRegions.size();
//        BigDecimal[][] allocationMatrix = new BigDecimal[regionCount][GRADE_COUNT];
//
//        // 1. 初始化分配矩阵：将所有 x_{ij} 初始化为 0
//        for (int i = 0; i < regionCount; i++) {
//            for (int j = 0; j < GRADE_COUNT; j++) {
//                allocationMatrix[i][j] = BigDecimal.ZERO;
//            }
//        }
//
//        try {
//            // 2. 粗调过程：从最高档位（D30）开始，逐列增加
//            BigDecimal currentAmount = BigDecimal.ZERO;
//            int lastFullGrade = -1;
//
//            for (int grade = 0; grade < GRADE_COUNT; grade++) {
//                BigDecimal gradeAmount = BigDecimal.ZERO;
//
//                // 计算该档位所有区域增加1后的总投放量
//                for (int region = 0; region < regionCount; region++) {
//                    if (regionCustomerMatrix[region][grade] != null) {
//                        gradeAmount = gradeAmount.add(regionCustomerMatrix[region][grade]);
//                    }
//                }
//
//                // 如果增加该档位后总投放量超过目标值，停止并回退
//                if (currentAmount.add(gradeAmount).compareTo(targetAmount) > 0) {
//                    break;
//                }
//
//                // 该档位所有区域都增加1
//                for (int region = 0; region < regionCount; region++) {
//                    allocationMatrix[region][grade] = BigDecimal.ONE;
//                }
//
//                currentAmount = currentAmount.add(gradeAmount);
//                lastFullGrade = grade;
//            }
//
//            log.info("粗调完成，当前投放量: {}, 目标投放量: {}, 最后完整档位: {}",
//                    currentAmount, targetAmount, lastFullGrade);
//
//            // 3. 生成候选方案并选择最佳方案
//            BigDecimal[][] bestMatrix = generateBestCandidate(allocationMatrix, regionCustomerMatrix,
//                                                           targetAmount, currentAmount, lastFullGrade);
//
//            // 4. 最终验证和调整，确保满足非递增约束
//            bestMatrix = enforceMonotonicConstraint(bestMatrix);
//
//            BigDecimal finalAmount = calculateTotalAmount(bestMatrix, regionCustomerMatrix);
//            log.info("分配算法完成，目标投放量: {}, 实际投放量: {}, 误差: {}",
//                    targetAmount, finalAmount, targetAmount.subtract(finalAmount).abs());
//
//            return bestMatrix;
//
//        } catch (Exception e) {
//            log.error("分配算法执行失败", e);
//            return allocationMatrix;
//        }
//    }
//
//    /**
//     * 生成最佳候选方案
//     */
//    private BigDecimal[][] generateBestCandidate(BigDecimal[][] baseMatrix,
//                                               BigDecimal[][] regionCustomerMatrix,
//                                               BigDecimal targetAmount,
//                                               BigDecimal currentAmount,
//                                               int lastFullGrade) {
//        int regionCount = baseMatrix.length;
//        BigDecimal[][] bestMatrix = deepCopyMatrix(baseMatrix);
//        BigDecimal bestError = targetAmount.subtract(currentAmount).abs();
//
//        // 候选方案1：粗调结果本身
//        BigDecimal error1 = bestError;
//        log.debug("候选方案1误差: {}", error1);
//
//        // 候选方案2：在较低档位中选择某些区域增加分配值，满足非递增约束
//        BigDecimal[][] candidate2 = generateCandidate2(baseMatrix, regionCustomerMatrix,
//                                                     targetAmount, currentAmount, lastFullGrade);
//        if (candidate2 != null) {
//            BigDecimal amount2 = calculateTotalAmount(candidate2, regionCustomerMatrix);
//            BigDecimal error2 = targetAmount.subtract(amount2).abs();
//            log.debug("候选方案2误差: {}", error2);
//            if (error2.compareTo(bestError) < 0) {
//                bestMatrix = candidate2;
//                bestError = error2;
//            }
//        }
//
//        // 候选方案3：再次整列增加并调整，确保整体分布均匀
//        BigDecimal[][] candidate3 = generateCandidate3(baseMatrix, regionCustomerMatrix,
//                                                     targetAmount, currentAmount, lastFullGrade);
//        if (candidate3 != null) {
//            BigDecimal amount3 = calculateTotalAmount(candidate3, regionCustomerMatrix);
//            BigDecimal error3 = targetAmount.subtract(amount3).abs();
//            log.debug("候选方案3误差: {}", error3);
//            if (error3.compareTo(bestError) < 0) {
//                bestMatrix = candidate3;
//                bestError = error3;
//            }
//        }
//
//        log.debug("选择最佳方案，误差: {}", bestError);
//        return bestMatrix;
//    }
//
//    /**
//     * 生成候选方案2：在较低档位中选择某些区域增加分配值，满足非递增约束
//     */
//    private BigDecimal[][] generateCandidate2(BigDecimal[][] baseMatrix,
//                                            BigDecimal[][] regionCustomerMatrix,
//                                            BigDecimal targetAmount,
//                                            BigDecimal currentAmount,
//                                            int lastFullGrade) {
//        int regionCount = baseMatrix.length;
//        BigDecimal[][] candidate = deepCopyMatrix(baseMatrix);
//        BigDecimal tempAmount = currentAmount;
//
//        // 在较低档位中尝试增加某些区域的分配值
//        for (int grade = lastFullGrade + 1; grade < GRADE_COUNT; grade++) {
//            for (int region = 0; region < regionCount; region++) {
//                // 检查是否满足非递增约束
//                if (isValidIncrement(candidate, region, grade)) {
//                    BigDecimal increment = regionCustomerMatrix[region][grade];
//                    if (increment != null &&
//                        tempAmount.add(increment).compareTo(targetAmount) <= 0) {
//                        candidate[region][grade] = candidate[region][grade].add(BigDecimal.ONE);
//                        tempAmount = tempAmount.add(increment);
//                    }
//                }
//            }
//        }
//
//        return candidate;
//    }
//
//    /**
//     * 生成候选方案3：再次整列增加并调整，确保整体分布均匀
//     */
//    private BigDecimal[][] generateCandidate3(BigDecimal[][] baseMatrix,
//                                            BigDecimal[][] regionCustomerMatrix,
//                                            BigDecimal targetAmount,
//                                            BigDecimal currentAmount,
//                                            int lastFullGrade) {
//        int regionCount = baseMatrix.length;
//        BigDecimal[][] candidate = deepCopyMatrix(baseMatrix);
//        BigDecimal tempAmount = currentAmount;
//
//        // 再次从档位1开始整列增加，直到接近目标值
//        for (int grade = 0; grade < GRADE_COUNT; grade++) {
//            BigDecimal gradeAmount = BigDecimal.ZERO;
//
//            for (int region = 0; region < regionCount; region++) {
//                if (regionCustomerMatrix[region][grade] != null) {
//                    gradeAmount = gradeAmount.add(regionCustomerMatrix[region][grade]);
//                }
//            }
//
//            if (tempAmount.add(gradeAmount).compareTo(targetAmount) > 0) {
//                // 在该列附近调整某些区域增加1，使S接近T
//                adjustNearbyGrades(candidate, regionCustomerMatrix, targetAmount, tempAmount, grade);
//                break;
//            }
//
//            for (int region = 0; region < regionCount; region++) {
//                candidate[region][grade] = candidate[region][grade].add(BigDecimal.ONE);
//            }
//            tempAmount = tempAmount.add(gradeAmount);
//        }
//
//        return candidate;
//    }
//
//    /**
//     * 检查增加分配值是否满足非递增约束
//     * 非递增约束：每个区域的分配值必须从高档位到低档位非递增
//     * D30为最高档位，D1为最低档位
//     */
//    private boolean isValidIncrement(BigDecimal[][] matrix, int region, int grade) {
//        if (grade == 0) return true; // 最高档位D30
//
//        // 检查是否满足非递增约束：高档位值必须大于等于低档位值
//        // grade-1 是更高档位，grade 是当前档位
//        return matrix[region][grade - 1].compareTo(matrix[region][grade]) >= 0;
//    }
//
//    /**
//     * 在附近档位调整分配值，确保整体分布均匀
//     */
//    private void adjustNearbyGrades(BigDecimal[][] matrix,
//                                   BigDecimal[][] regionCustomerMatrix,
//                                   BigDecimal targetAmount,
//                                   BigDecimal currentAmount,
//                                   int grade) {
//        int regionCount = matrix.length;
//
//        // 在附近档位中选择某些区域增加1，使S接近T
//        for (int g = Math.max(0, grade - 2); g <= Math.min(GRADE_COUNT - 1, grade + 2); g++) {
//            for (int region = 0; region < regionCount; region++) {
//                if (isValidIncrement(matrix, region, g)) {
//                    BigDecimal increment = regionCustomerMatrix[region][g];
//                    if (increment != null &&
//                        currentAmount.add(increment).compareTo(targetAmount) <= 0) {
//                        matrix[region][g] = matrix[region][g].add(BigDecimal.ONE);
//                        currentAmount = currentAmount.add(increment);
//                    }
//                }
//            }
//        }
//    }
//
//    /**
//     * 强制实施非递增约束
//     * 确保每个区域的分配值从高档位到低档位非递增
//     */
//    private BigDecimal[][] enforceMonotonicConstraint(BigDecimal[][] matrix) {
//        int regionCount = matrix.length;
//        BigDecimal[][] result = deepCopyMatrix(matrix);
//
//        for (int region = 0; region < regionCount; region++) {
//            // 从高档位到低档位检查并调整
//            for (int grade = 1; grade < GRADE_COUNT; grade++) {
//                // 如果当前档位值大于前一个档位值，则调整当前档位值
//                if (result[region][grade].compareTo(result[region][grade - 1]) > 0) {
//                    result[region][grade] = result[region][grade - 1];
//                    log.debug("区域 {} 档位 {} 调整值从 {} 到 {} 以满足非递增约束",
//                            region, grade, matrix[region][grade], result[region][grade]);
//                }
//            }
//        }
//
//        return result;
//    }
//
//    /**
//     * 计算总投放量
//     */
//    private BigDecimal calculateTotalAmount(BigDecimal[][] allocationMatrix,
//                                          BigDecimal[][] regionCustomerMatrix) {
//        BigDecimal total = BigDecimal.ZERO;
//        int regionCount = allocationMatrix.length;
//
//        for (int i = 0; i < regionCount; i++) {
//            for (int j = 0; j < GRADE_COUNT; j++) {
//                if (allocationMatrix[i][j] != null && regionCustomerMatrix[i][j] != null) {
//                    total = total.add(allocationMatrix[i][j].multiply(regionCustomerMatrix[i][j]));
//                }
//            }
//        }
//
//        return total;
//    }
//
//    /**
//     * 深拷贝矩阵
//     */
//    private BigDecimal[][] deepCopyMatrix(BigDecimal[][] original) {
//        int rows = original.length;
//        int cols = original[0].length;
//        BigDecimal[][] copy = new BigDecimal[rows][cols];
//
//        for (int i = 0; i < rows; i++) {
//            for (int j = 0; j < cols; j++) {
//                copy[i][j] = original[i][j];
//            }
//        }
//
//        return copy;
//    }
//}
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
