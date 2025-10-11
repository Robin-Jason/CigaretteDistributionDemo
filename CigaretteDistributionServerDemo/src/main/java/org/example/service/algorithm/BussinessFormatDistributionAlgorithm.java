package org.example.service.algorithm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

/**
 * 业态类型卷烟分配算法
 * 专门处理"档位+业态"业务类型的分配算法
 */
@Slf4j
@Service
public class BussinessFormatDistributionAlgorithm {
    
    private static final int GRADE_COUNT = 30; // 档位数（D30到D1）
    
    /**
     * 卷烟分配算法 - 根据需求描述优化版本
     * @param targetBusinessFormats 目标业态类型列表
     * @param businessFormatCustomerMatrix 业态类型客户数矩阵 [业态类型数][档位数]
     * @param targetAmount 预投放量
     * @return 分配矩阵 [业态类型数][档位数]
     */
    public BigDecimal[][] calculateDistribution(List<String> targetBusinessFormats, 
                                             BigDecimal[][] businessFormatCustomerMatrix, 
                                             BigDecimal targetAmount) {
        if (targetBusinessFormats == null || targetBusinessFormats.isEmpty() || 
            businessFormatCustomerMatrix == null || targetAmount == null) {
            log.error("输入参数无效");
            return new BigDecimal[0][0];
        }
        
        int businessFormatCount = targetBusinessFormats.size();
        BigDecimal[][] allocationMatrix = new BigDecimal[businessFormatCount][GRADE_COUNT];
        
        // 1. 初始化分配矩阵：将所有 x_{ij} 初始化为 0
        for (int i = 0; i < businessFormatCount; i++) {
            for (int j = 0; j < GRADE_COUNT; j++) {
                allocationMatrix[i][j] = BigDecimal.ZERO;
            }
        }
        
        try {
            // 2. 粗调过程：从最高档位（D30）开始，逐列增加
            BigDecimal currentAmount = BigDecimal.ZERO;
            int lastFullGrade = -1;
            
            for (int grade = 0; grade < GRADE_COUNT; grade++) {
                BigDecimal gradeAmount = BigDecimal.ZERO;
                
                // 计算该档位所有业态类型增加1后的总投放量
                for (int businessFormat = 0; businessFormat < businessFormatCount; businessFormat++) {
                    if (businessFormatCustomerMatrix[businessFormat][grade] != null) {
                        gradeAmount = gradeAmount.add(businessFormatCustomerMatrix[businessFormat][grade]);
                    }
                }
                
                // 如果增加该档位后总投放量超过目标值，停止并回退
                if (currentAmount.add(gradeAmount).compareTo(targetAmount) > 0) {
                    break;
                }
                
                // 该档位所有业态类型都增加1
                for (int businessFormat = 0; businessFormat < businessFormatCount; businessFormat++) {
                    allocationMatrix[businessFormat][grade] = BigDecimal.ONE;
                }
                
                currentAmount = currentAmount.add(gradeAmount);
                lastFullGrade = grade;
            }
            
            log.info("业态类型算法粗调完成，当前投放量: {}, 目标投放量: {}, 最后完整档位: {}", 
                    currentAmount, targetAmount, lastFullGrade);
            
            // 3. 生成候选方案并选择最佳方案
            BigDecimal[][] bestMatrix = generateBestCandidate(allocationMatrix, businessFormatCustomerMatrix, 
                                                           targetAmount, currentAmount, lastFullGrade);
            
            // 4. 最终验证和调整，确保满足非递增约束
            bestMatrix = enforceMonotonicConstraint(bestMatrix);
            
            BigDecimal finalAmount = calculateTotalAmount(bestMatrix, businessFormatCustomerMatrix);
            log.info("业态类型分配算法完成，目标投放量: {}, 实际投放量: {}, 误差: {}", 
                    targetAmount, finalAmount, targetAmount.subtract(finalAmount).abs());
            
            return bestMatrix;
            
        } catch (Exception e) {
            log.error("业态类型分配算法执行失败", e);
            return allocationMatrix;
        }
    }
    
    /**
     * 生成最佳候选方案
     */
    private BigDecimal[][] generateBestCandidate(BigDecimal[][] baseMatrix, 
                                               BigDecimal[][] businessFormatCustomerMatrix,
                                               BigDecimal targetAmount, 
                                               BigDecimal currentAmount,
                                               int lastFullGrade) {
        BigDecimal[][] bestMatrix = deepCopyMatrix(baseMatrix);
        BigDecimal bestError = targetAmount.subtract(currentAmount).abs();
        
        // 候选方案1：粗调结果本身
        BigDecimal error1 = bestError;
        log.debug("候选方案1误差: {}", error1);
        
        // 候选方案2：在较低档位中选择某些业态类型增加分配值，满足非递增约束
        BigDecimal[][] candidate2 = generateCandidate2(baseMatrix, businessFormatCustomerMatrix, 
                                                     targetAmount, currentAmount, lastFullGrade);
        if (candidate2 != null) {
            BigDecimal amount2 = calculateTotalAmount(candidate2, businessFormatCustomerMatrix);
            BigDecimal error2 = targetAmount.subtract(amount2).abs();
            log.debug("候选方案2误差: {}", error2);
            if (error2.compareTo(bestError) < 0) {
                bestMatrix = candidate2;
                bestError = error2;
            }
        }
        
        // 候选方案3：再次整列增加并调整，确保整体分布均匀
        BigDecimal[][] candidate3 = generateCandidate3(baseMatrix, businessFormatCustomerMatrix, 
                                                     targetAmount, currentAmount, lastFullGrade);
        if (candidate3 != null) {
            BigDecimal amount3 = calculateTotalAmount(candidate3, businessFormatCustomerMatrix);
            BigDecimal error3 = targetAmount.subtract(amount3).abs();
            log.debug("候选方案3误差: {}", error3);
            if (error3.compareTo(bestError) < 0) {
                bestMatrix = candidate3;
                bestError = error3;
            }
        }
        
        log.debug("选择最佳方案，误差: {}", bestError);
        return bestMatrix;
    }
    
    /**
     * 生成候选方案2：在较低档位中选择某些业态类型增加分配值，满足非递增约束
     */
    private BigDecimal[][] generateCandidate2(BigDecimal[][] baseMatrix, 
                                            BigDecimal[][] businessFormatCustomerMatrix,
                                            BigDecimal targetAmount, 
                                            BigDecimal currentAmount,
                                            int lastFullGrade) {
        int businessFormatCount = baseMatrix.length;
        BigDecimal[][] candidate = deepCopyMatrix(baseMatrix);
        BigDecimal tempAmount = currentAmount;
        
        // 在较低档位中尝试增加某些业态类型的分配值
        for (int grade = lastFullGrade + 1; grade < GRADE_COUNT; grade++) {
            for (int businessFormat = 0; businessFormat < businessFormatCount; businessFormat++) {
                // 检查是否满足非递增约束
                if (isValidIncrement(candidate, businessFormat, grade)) {
                    BigDecimal increment = businessFormatCustomerMatrix[businessFormat][grade];
                    if (increment != null && 
                        tempAmount.add(increment).compareTo(targetAmount) <= 0) {
                        candidate[businessFormat][grade] = candidate[businessFormat][grade].add(BigDecimal.ONE);
                        tempAmount = tempAmount.add(increment);
                    }
                }
            }
        }
        
        return candidate;
    }
    
    /**
     * 生成候选方案3：再次整列增加并调整，确保整体分布均匀
     */
    private BigDecimal[][] generateCandidate3(BigDecimal[][] baseMatrix, 
                                            BigDecimal[][] businessFormatCustomerMatrix,
                                            BigDecimal targetAmount, 
                                            BigDecimal currentAmount,
                                            int lastFullGrade) {
        int businessFormatCount = baseMatrix.length;
        BigDecimal[][] candidate = deepCopyMatrix(baseMatrix);
        BigDecimal tempAmount = currentAmount;
        
        // 再次从档位1开始整列增加，直到接近目标值
        for (int grade = 0; grade < GRADE_COUNT; grade++) {
            BigDecimal gradeAmount = BigDecimal.ZERO;
            
            for (int businessFormat = 0; businessFormat < businessFormatCount; businessFormat++) {
                if (businessFormatCustomerMatrix[businessFormat][grade] != null) {
                    gradeAmount = gradeAmount.add(businessFormatCustomerMatrix[businessFormat][grade]);
                }
            }
            
            if (tempAmount.add(gradeAmount).compareTo(targetAmount) > 0) {
                // 在该列附近调整某些业态类型增加1，使S接近T
                adjustNearbyGrades(candidate, businessFormatCustomerMatrix, targetAmount, tempAmount, grade);
                break;
            }
            
            for (int businessFormat = 0; businessFormat < businessFormatCount; businessFormat++) {
                candidate[businessFormat][grade] = candidate[businessFormat][grade].add(BigDecimal.ONE);
            }
            tempAmount = tempAmount.add(gradeAmount);
        }
        
        return candidate;
    }
    
    /**
     * 检查增加分配值是否满足非递增约束
     * 非递增约束：每个业态类型的分配值必须从高档位到低档位非递增
     * D30为最高档位，D1为最低档位
     */
    private boolean isValidIncrement(BigDecimal[][] matrix, int businessFormat, int grade) {
        if (grade == 0) return true; // 最高档位D30
        
        // 检查是否满足非递增约束：高档位值必须大于等于低档位值
        // grade-1 是更高档位，grade 是当前档位
        return matrix[businessFormat][grade - 1].compareTo(matrix[businessFormat][grade]) >= 0;
    }
    
    /**
     * 在附近档位调整分配值，确保整体分布均匀
     */
    private void adjustNearbyGrades(BigDecimal[][] matrix, 
                                   BigDecimal[][] businessFormatCustomerMatrix,
                                   BigDecimal targetAmount, 
                                   BigDecimal currentAmount,
                                   int grade) {
        int businessFormatCount = matrix.length;
        
        // 在附近档位中选择某些业态类型增加1，使S接近T
        for (int g = Math.max(0, grade - 2); g <= Math.min(GRADE_COUNT - 1, grade + 2); g++) {
            for (int businessFormat = 0; businessFormat < businessFormatCount; businessFormat++) {
                if (isValidIncrement(matrix, businessFormat, g)) {
                    BigDecimal increment = businessFormatCustomerMatrix[businessFormat][g];
                    if (increment != null && 
                        currentAmount.add(increment).compareTo(targetAmount) <= 0) {
                        matrix[businessFormat][g] = matrix[businessFormat][g].add(BigDecimal.ONE);
                        currentAmount = currentAmount.add(increment);
                    }
                }
            }
        }
    }
    
    /**
     * 强制实施非递增约束
     * 确保每个业态类型的分配值从高档位到低档位非递增
     */
    private BigDecimal[][] enforceMonotonicConstraint(BigDecimal[][] matrix) {
        int businessFormatCount = matrix.length;
        BigDecimal[][] result = deepCopyMatrix(matrix);
        
        for (int businessFormat = 0; businessFormat < businessFormatCount; businessFormat++) {
            // 从高档位到低档位检查并调整
            for (int grade = 1; grade < GRADE_COUNT; grade++) {
                // 如果当前档位值大于前一个档位值，则调整当前档位值
                if (result[businessFormat][grade].compareTo(result[businessFormat][grade - 1]) > 0) {
                    result[businessFormat][grade] = result[businessFormat][grade - 1];
                    log.debug("业态类型 {} 档位 {} 调整值从 {} 到 {} 以满足非递增约束", 
                            businessFormat, grade, matrix[businessFormat][grade], result[businessFormat][grade]);
                }
            }
        }
        
        return result;
    }
    
    /**
     * 计算总投放量
     */
    private BigDecimal calculateTotalAmount(BigDecimal[][] allocationMatrix, 
                                          BigDecimal[][] businessFormatCustomerMatrix) {
        BigDecimal total = BigDecimal.ZERO;
        int businessFormatCount = allocationMatrix.length;
        
        for (int i = 0; i < businessFormatCount; i++) {
            for (int j = 0; j < GRADE_COUNT; j++) {
                if (allocationMatrix[i][j] != null && businessFormatCustomerMatrix[i][j] != null) {
                    total = total.add(allocationMatrix[i][j].multiply(businessFormatCustomerMatrix[i][j]));
                }
            }
        }
        
        return total;
    }
    
    /**
     * 深拷贝矩阵
     */
    private BigDecimal[][] deepCopyMatrix(BigDecimal[][] original) {
        int rows = original.length;
        int cols = original[0].length;
        BigDecimal[][] copy = new BigDecimal[rows][cols];
        
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                copy[i][j] = original[i][j];
            }
        }
        
        return copy;
    }
}
