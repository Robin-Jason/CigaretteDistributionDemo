package org.example.service.algorithm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays; // 引入 Arrays
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 卷烟分配算法 - 按市场类型（城网/农网）比例分配版本
 * 算法逻辑遵循：
 * 1. 按比例将总投放量分配给城网和农网（如果两者都存在）。
 * 2. 对每个网络单独执行核心分配算法（误差优先，平滑为辅）。
 * 3. 合并所有分配结果并强制平滑非递增约束。
 * 4. 【新约束】：支持仅有城网或仅有农网的非比例投放；比例不合法时抛出异常。
 * 5. 【优化目标】：误差控制在200以内 (<= 200)，尽可能接近目标值 (继续迭代优化即使已达标)。
 */
@Slf4j
@Service
public class MarketProportionalCigaretteDistributionAlgorithm {

    private static final int GRADE_COUNT = 30; // 档位数（D30到D1）
    private static final String URBAN_NETWORK = "城网";
    private static final String RURAL_NETWORK = "农网";
    // 增加迭代次数以提高精度
    private static final int MAX_ITERATIONS = 500; // 微调迭代次数
    private static final BigDecimal INCREMENT = BigDecimal.ONE; // 每次调整的步长
    // 定义误差阈值 (用于日志记录，不再用于提前退出)
    private static final BigDecimal ERROR_THRESHOLD = new BigDecimal("200");

    /**
     * 卷烟分配算法 - 按比例分配或非比例分配（市场类型）
     * @param targetRegions 目标投放区域列表 (包含"城网"和/或"农网")
     * @param regionCustomerMatrix 区域客户数矩阵 [区域数][档位数]
     * @param targetAmount 预投放量
     * @param urbanRatio 城网分配比例（可选）
     * @param ruralRatio 农网分配比例（可选）
     * @return 分配矩阵 [区域数][档位数]
     */
    public BigDecimal[][] calculateDistribution(List<String> targetRegions,
                                                BigDecimal[][] regionCustomerMatrix,
                                                BigDecimal targetAmount,
                                                BigDecimal urbanRatio,
                                                BigDecimal ruralRatio) {
        // --- 输入校验 (保持不变) ---
        if (targetRegions == null || targetRegions.isEmpty() ||
                regionCustomerMatrix == null || targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) < 0) {
            log.error("输入参数无效 (targetRegions empty={}, regionCustomerMatrix null={}, targetAmount null={}, targetAmount negative={})",
                    targetRegions == null || targetRegions.isEmpty(), regionCustomerMatrix == null, targetAmount == null, targetAmount != null && targetAmount.compareTo(BigDecimal.ZERO) < 0);
            int regionCount = (targetRegions != null) ? targetRegions.size() : 0;
            BigDecimal[][] zeroMatrix = new BigDecimal[regionCount][GRADE_COUNT];
            for(int i = 0; i < regionCount; i++){
                zeroMatrix[i] = new BigDecimal[GRADE_COUNT]; // Ensure row is initialized
                Arrays.fill(zeroMatrix[i], BigDecimal.ZERO);
            }
            return zeroMatrix;
        }


        Map<String, List<Integer>> regionIndicesByNetwork = new HashMap<>();
        List<Integer> urbanIndices = new ArrayList<>();
        List<Integer> ruralIndices = new ArrayList<>();

        // 1. 根据区域名称分组索引 (保持不变，使用 equals 精确匹配)
        for (int i = 0; i < targetRegions.size(); i++) {
            String region = targetRegions.get(i);
            if (URBAN_NETWORK.equals(region)) {
                urbanIndices.add(i);
            } else if (RURAL_NETWORK.equals(region)) {
                ruralIndices.add(i);
            }
        }
        regionIndicesByNetwork.put(URBAN_NETWORK, urbanIndices);
        regionIndicesByNetwork.put(RURAL_NETWORK, ruralIndices);

        boolean hasUrban = !urbanIndices.isEmpty();
        boolean hasRural = !ruralIndices.isEmpty();

        Map<String, BigDecimal[][]> partialMatrices = new HashMap<>();
        BigDecimal totalAmountTarget = BigDecimal.ZERO;

        // --- 检查目标区域有效性 (保持不变) ---
        if (!hasUrban && !hasRural) {
            log.error("目标区域列表既不包含城网也不包含农网，分配中止");
            BigDecimal[][] zeroMatrix = new BigDecimal[targetRegions.size()][GRADE_COUNT];
            for(int i = 0; i < targetRegions.size(); i++){
                zeroMatrix[i] = new BigDecimal[GRADE_COUNT];
                Arrays.fill(zeroMatrix[i], BigDecimal.ZERO);
            }
            return zeroMatrix;
        }

        // 2. 确定分配模式并执行核心算法 (保持不变)
        if (hasUrban && hasRural) {
            // 场景一：同时包含城网和农网 -> 按比例分配
            if (urbanRatio == null || ruralRatio == null ||
                    urbanRatio.compareTo(BigDecimal.ZERO) < 0 || ruralRatio.compareTo(BigDecimal.ZERO) < 0 ||
                    urbanRatio.add(ruralRatio).compareTo(BigDecimal.ONE) != 0) {
                log.error("城网和农网同时存在，但未提供合法比例（必须非null、非负且和为1）。urbanRatio={}, ruralRatio={}", urbanRatio, ruralRatio);
                throw new IllegalArgumentException("城网和农网同时存在时，比例参数(urbanRatio, ruralRatio)必须提供、非负且相加等于1。");
            }

            BigDecimal urbanTarget = targetAmount.multiply(urbanRatio);
            BigDecimal ruralTarget = targetAmount.multiply(ruralRatio);
            totalAmountTarget = targetAmount;
            log.info("城网/农网同时存在，按比例分配: 城网目标={}, 农网目标={}", urbanTarget, ruralTarget);

            BigDecimal[][] urbanCustomerMatrix = createPartialMatrix(urbanIndices, regionCustomerMatrix);
            partialMatrices.put(URBAN_NETWORK, runCoreAlgorithm(urbanIndices.stream().map(targetRegions::get).collect(Collectors.toList()), urbanCustomerMatrix, urbanTarget));

            BigDecimal[][] ruralCustomerMatrix = createPartialMatrix(ruralIndices, regionCustomerMatrix);
            partialMatrices.put(RURAL_NETWORK, runCoreAlgorithm(ruralIndices.stream().map(targetRegions::get).collect(Collectors.toList()), ruralCustomerMatrix, ruralTarget));

        } else {
            // 场景二：仅包含城网或仅包含农网 -> 非比例分配
            String network = hasUrban ? URBAN_NETWORK : RURAL_NETWORK;
            List<Integer> indices = hasUrban ? urbanIndices : ruralIndices;
            totalAmountTarget = targetAmount;
            log.info("仅有 {} 存在，非比例分配，目标为总投放量={}", network, targetAmount);

            BigDecimal[][] customerMatrix = createPartialMatrix(indices, regionCustomerMatrix);
            partialMatrices.put(network, runCoreAlgorithm(indices.stream().map(targetRegions::get).collect(Collectors.toList()), customerMatrix, targetAmount));
        }

        // 3. 合并结果矩阵 (保持不变，增加健壮性)
        BigDecimal[][] finalMatrix = new BigDecimal[targetRegions.size()][GRADE_COUNT];
        for (int i = 0; i < targetRegions.size(); i++) {
            finalMatrix[i] = new BigDecimal[GRADE_COUNT];
            Arrays.fill(finalMatrix[i], BigDecimal.ZERO);
        }

        if (hasUrban) {
            BigDecimal[][] urbanMatrix = partialMatrices.get(URBAN_NETWORK);
            if (urbanMatrix != null && urbanMatrix.length == urbanIndices.size()) {
                for (int i = 0; i < urbanIndices.size(); i++) {
                    int originalIndex = urbanIndices.get(i);
                    if (originalIndex >= 0 && originalIndex < finalMatrix.length && urbanMatrix[i] != null && urbanMatrix[i].length == GRADE_COUNT) {
                        System.arraycopy(urbanMatrix[i], 0, finalMatrix[originalIndex], 0, GRADE_COUNT);
                    } else {
                        log.error("合并城网结果时发生索引或维度错误 at index {}, originalIndex {}", i, originalIndex);
                    }
                }
            } else {
                log.error("城网部分矩阵为空或维度不匹配");
            }
        }

        if (hasRural) {
            BigDecimal[][] ruralMatrix = partialMatrices.get(RURAL_NETWORK);
            if (ruralMatrix != null && ruralMatrix.length == ruralIndices.size()) {
                for (int i = 0; i < ruralIndices.size(); i++) {
                    int originalIndex = ruralIndices.get(i);
                    if (originalIndex >= 0 && originalIndex < finalMatrix.length && ruralMatrix[i] != null && ruralMatrix[i].length == GRADE_COUNT) {
                        System.arraycopy(ruralMatrix[i], 0, finalMatrix[originalIndex], 0, GRADE_COUNT);
                    } else {
                        log.error("合并农网结果时发生索引或维度错误 at index {}, originalIndex {}", i, originalIndex);
                    }
                }
            } else {
                log.error("农网部分矩阵为空或维度不匹配");
            }
        }

        // 4. 强制执行平滑非递增约束 (保持不变)
        enforceSmoothDecreaseConstraint(finalMatrix);

        // 5. 计算最终实际投放量并记录日志 (保持不变)
        BigDecimal finalAmount = calculateTotalAmount(finalMatrix, regionCustomerMatrix);
        BigDecimal finalError = totalAmountTarget.subtract(finalAmount).abs();
        log.info("市场分配算法完成，目标投放量: {}, 实际投放量: {}, 误差: {}",
                totalAmountTarget, finalAmount, finalError);
        // 增加一个最终误差是否在200以内的日志
        if (finalError.compareTo(ERROR_THRESHOLD) <= 0) {
            log.info("最终误差 {} 小于等于 {}", finalError, ERROR_THRESHOLD);
        } else {
            log.warn("最终误差 {} 大于 {}", finalError, ERROR_THRESHOLD);
        }

        return finalMatrix;
    }

    /**
     * 核心分配算法 (保持不变)
     */
    private BigDecimal[][] runCoreAlgorithm(List<String> regions,
                                            BigDecimal[][] customerMatrix,
                                            BigDecimal targetAmount) {
        // --- 输入校验 (保持不变) ---
        if (regions == null || regions.isEmpty() || customerMatrix == null || targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) < 0) {
            log.error("核心算法输入参数无效 (regions empty={}, customerMatrix null={}, targetAmount null={}, targetAmount negative={})",
                    regions == null || regions.isEmpty(), customerMatrix == null, targetAmount == null, targetAmount != null && targetAmount.compareTo(BigDecimal.ZERO) < 0);
            int regionCount = (regions != null) ? regions.size() : 0;
            BigDecimal[][] zeroMatrix = new BigDecimal[regionCount][GRADE_COUNT];
            for(int i = 0; i < regionCount; i++){
                zeroMatrix[i] = new BigDecimal[GRADE_COUNT];
                Arrays.fill(zeroMatrix[i], BigDecimal.ZERO);
            }
            return zeroMatrix;
        }

        int regionCount = regions.size();
        BigDecimal[][] allocationMatrix = new BigDecimal[regionCount][GRADE_COUNT];
        for (int i = 0; i < regionCount; i++) {
            allocationMatrix[i] = new BigDecimal[GRADE_COUNT];
            Arrays.fill(allocationMatrix[i], BigDecimal.ZERO);
        }


        try {
            // 1. 初始贪心分配 (保持不变)
            greedyFill(allocationMatrix, customerMatrix, targetAmount);

            // --- 2. 迭代微调 (修改后的逻辑) ---
            iterativeRefinement(allocationMatrix, customerMatrix, targetAmount);

            // 3. 最终强制平滑非递增约束 (保持不变)
            enforceSmoothDecreaseConstraint(allocationMatrix);

            // 计算最终结果和误差 (保持不变)
            BigDecimal finalAmount = calculateTotalAmount(allocationMatrix, customerMatrix);
            BigDecimal error = targetAmount.subtract(finalAmount).abs();
            log.info("核心算法完成 ({}), 目标投放量: {}, 实际投放量: {}, 误差: {}",
                    String.join(",", regions), targetAmount, finalAmount, error);
            return allocationMatrix;

        } catch (Exception e) {
            log.error("核心算法执行失败 for regions: {}", String.join(",", regions), e);
            BigDecimal[][] zeroMatrix = new BigDecimal[regionCount][GRADE_COUNT];
            for(int i = 0; i < regionCount; i++){
                zeroMatrix[i] = new BigDecimal[GRADE_COUNT];
                Arrays.fill(zeroMatrix[i], BigDecimal.ZERO);
            }
            return zeroMatrix;
        }
    }

    /**
     * 贪心填充算法 (保持不变)
     */
    private void greedyFill(BigDecimal[][] allocationMatrix,
                            BigDecimal[][] regionCustomerMatrix,
                            BigDecimal targetAmount) {
        BigDecimal currentAmount = calculateTotalAmount(allocationMatrix, regionCustomerMatrix);
        boolean canStillAdd = true;
        int maxFillRounds = GRADE_COUNT * allocationMatrix.length * 2; // Safeguard
        int fillRound = 0;

        while (canStillAdd && fillRound < maxFillRounds) {
            canStillAdd = false;
            fillRound++;
            for (int j = 0; j < GRADE_COUNT; j++) {
                for (int i = 0; i < allocationMatrix.length; i++) {
                    if (allocationMatrix[i] == null) continue;

                    allocationMatrix[i][j] = allocationMatrix[i][j].add(INCREMENT);
                    boolean isValid = isSmoothlyDecreasing(allocationMatrix[i]);
                    allocationMatrix[i][j] = allocationMatrix[i][j].subtract(INCREMENT);

                    if (isValid) {
                        BigDecimal customerCount = (regionCustomerMatrix[i] != null && regionCustomerMatrix[i].length > j)
                                ? regionCustomerMatrix[i][j] : BigDecimal.ZERO;

                        if (customerCount != null && customerCount.compareTo(BigDecimal.ZERO) > 0
                                && currentAmount.add(customerCount).compareTo(targetAmount) <= 0) {
                            allocationMatrix[i][j] = allocationMatrix[i][j].add(INCREMENT);
                            currentAmount = currentAmount.add(customerCount);
                            canStillAdd = true;
                        }
                    }
                }
            }
            if(!canStillAdd) break;
        }
        if(fillRound >= maxFillRounds) {
            log.warn("Greedy fill reached max rounds ({}), potentially stuck. Current amount: {}", maxFillRounds, currentAmount);
        }
        log.info("贪心填充完成，当前投放量: {}", currentAmount);
    }


    /**
     * 迭代微调，优先减小误差，不因误差达标而提前退出
     */
    private void iterativeRefinement(BigDecimal[][] allocationMatrix,
                                     BigDecimal[][] regionCustomerMatrix,
                                     BigDecimal targetAmount) {
        BigDecimal currentAbsoluteError = targetAmount.subtract(calculateTotalAmount(allocationMatrix, regionCustomerMatrix)).abs();
        log.info("开始微调，初始误差: {}", currentAbsoluteError);

        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            // --- 移除了基于 ERROR_THRESHOLD 的提前退出逻辑 ---
            // if (currentAbsoluteError.compareTo(ERROR_THRESHOLD) <= 0) {
            //     log.info("微调提前退出，误差 {} 已小于等于阈值 {}", currentAbsoluteError, ERROR_THRESHOLD);
            //     break;
            // }

            BigDecimal bestNewAbsoluteError = currentAbsoluteError; // 记录本轮能找到的最小误差
            int best_i = -1, best_j = -1;
            BigDecimal best_change = BigDecimal.ZERO;

            // 遍历所有可能的调整点
            for (int i = 0; i < allocationMatrix.length; i++) {
                if (allocationMatrix[i] == null) continue;
                for (int j = 0; j < GRADE_COUNT; j++) {
                    // 尝试增加 INCREMENT
                    allocationMatrix[i][j] = allocationMatrix[i][j].add(INCREMENT);
                    if (isSmoothlyDecreasing(allocationMatrix[i])) {
                        BigDecimal newAmount = calculateTotalAmount(allocationMatrix, regionCustomerMatrix);
                        BigDecimal newAbsoluteError = targetAmount.subtract(newAmount).abs();

                        // 核心决策：必须是能减小误差的调整中，误差最小的那个
                        if (newAbsoluteError.compareTo(bestNewAbsoluteError) < 0) {
                            bestNewAbsoluteError = newAbsoluteError;
                            best_i = i;
                            best_j = j;
                            best_change = INCREMENT;
                        }
                    }
                    allocationMatrix[i][j] = allocationMatrix[i][j].subtract(INCREMENT); // 回溯

                    // 尝试减少 INCREMENT
                    if (allocationMatrix[i][j].compareTo(BigDecimal.ZERO) > 0) {
                        allocationMatrix[i][j] = allocationMatrix[i][j].subtract(INCREMENT);
                        if (isSmoothlyDecreasing(allocationMatrix[i])) {
                            BigDecimal newAmount = calculateTotalAmount(allocationMatrix, regionCustomerMatrix);
                            BigDecimal newAbsoluteError = targetAmount.subtract(newAmount).abs();

                            if (newAbsoluteError.compareTo(bestNewAbsoluteError) < 0) {
                                bestNewAbsoluteError = newAbsoluteError;
                                best_i = i;
                                best_j = j;
                                best_change = INCREMENT.negate();
                            }
                        }
                        allocationMatrix[i][j] = allocationMatrix[i][j].add(INCREMENT); // 回溯
                    }
                }
            }

            // 应用本轮找到的最佳调整
            if (best_i != -1) { // 如果找到了能减小误差的调整
                allocationMatrix[best_i][best_j] = allocationMatrix[best_i][best_j].add(best_change);
                currentAbsoluteError = bestNewAbsoluteError; // 更新当前误差
                log.debug("微调迭代 {}: 应用调整 at [{}][{}], change={}, 新误差: {}", iter + 1, best_i, best_j, best_change, currentAbsoluteError);
            } else {
                log.info("微调在第 {} 轮结束，未找到可进一步减小误差的调整方案 (当前误差 {}).", iter + 1, currentAbsoluteError);
                break; // 如果本轮没有任何调整能减小误差，则提前退出
            }
        }
        // 增加最终日志，无论是否达到阈值
        if (currentAbsoluteError.compareTo(ERROR_THRESHOLD) > 0) {
            log.warn("微调完成，但最终误差 {} 仍大于阈值 {}", currentAbsoluteError, ERROR_THRESHOLD);
        } else {
            log.info("微调完成，最终误差: {} (在阈值 {} 以内)", currentAbsoluteError, ERROR_THRESHOLD); // 明确说明在阈值内
        }
    }


    /**
     * 检查是否满足平滑非递增约束 (保持不变)
     */
    private boolean isSmoothlyDecreasing(BigDecimal[] row) {
        if (row == null) return false;
        for (int j = 1; j < row.length; j++) {
            BigDecimal current = (row[j] == null) ? BigDecimal.ZERO : row[j];
            BigDecimal previous = (row[j - 1] == null) ? BigDecimal.ZERO : row[j - 1];
            if (current.compareTo(previous) > 0) return false;
            if (previous.subtract(current).compareTo(BigDecimal.ONE) > 0) return false;
        }
        return true;
    }

    /**
     * 强制执行平滑非递增约束 (保持不变)
     */
    private void enforceSmoothDecreaseConstraint(BigDecimal[][] matrix) {
        if (matrix == null) return;
        for (int i = 0; i < matrix.length; i++) {
            if (matrix[i] == null) continue;
            if (matrix[i][0] == null || matrix[i][0].compareTo(BigDecimal.ZERO) < 0) {
                matrix[i][0] = BigDecimal.ZERO;
            }
            for (int j = 1; j < GRADE_COUNT; j++) {
                BigDecimal current = (matrix[i][j] == null) ? BigDecimal.ZERO : matrix[i][j];
                BigDecimal previous = (matrix[i][j - 1] == null) ? BigDecimal.ZERO : matrix[i][j - 1];

                if (current.compareTo(previous) > 0) {
                    current = previous;
                }
                if (current.compareTo(previous.subtract(BigDecimal.ONE)) < 0) {
                    current = previous.subtract(BigDecimal.ONE);
                    if (current.compareTo(BigDecimal.ZERO) < 0) {
                        current = BigDecimal.ZERO;
                    }
                }
                matrix[i][j] = current;
            }
        }
    }

    // ==================== 通用辅助方法 (保持不变) ====================

    private BigDecimal[][] createPartialMatrix(List<Integer> originalIndices, BigDecimal[][] originalMatrix) {
        if (originalIndices == null || originalMatrix == null ) {
            log.warn("创建部分矩阵的输入无效 (indices null={}, matrix null={})", originalIndices == null, originalMatrix == null);
            int rows = (originalIndices != null) ? originalIndices.size() : 0;
            BigDecimal[][] zeroMatrix = new BigDecimal[rows][GRADE_COUNT];
            for(int i = 0; i < rows; i++){
                zeroMatrix[i] = new BigDecimal[GRADE_COUNT];
                Arrays.fill(zeroMatrix[i], BigDecimal.ZERO);
            }
            return zeroMatrix;
        }
        if (originalMatrix.length == 0 || (originalMatrix.length > 0 && (originalMatrix[0] == null || originalMatrix[0].length != GRADE_COUNT))) {
            log.warn("创建部分矩阵时发现原始矩阵为空或列数不正确");
            int rows = originalIndices.size();
            BigDecimal[][] zeroMatrix = new BigDecimal[rows][GRADE_COUNT];
            for(int i = 0; i < rows; i++){
                zeroMatrix[i] = new BigDecimal[GRADE_COUNT];
                Arrays.fill(zeroMatrix[i], BigDecimal.ZERO);
            }
            return zeroMatrix;
        }

        BigDecimal[][] newMatrix = new BigDecimal[originalIndices.size()][GRADE_COUNT];
        for (int i = 0; i < originalIndices.size(); i++) {
            int originalIndex = originalIndices.get(i);
            newMatrix[i] = new BigDecimal[GRADE_COUNT];
            if (originalIndex >= 0 && originalIndex < originalMatrix.length && originalMatrix[originalIndex] != null && originalMatrix[originalIndex].length == GRADE_COUNT) {
                System.arraycopy(originalMatrix[originalIndex], 0, newMatrix[i], 0, GRADE_COUNT);
            } else {
                log.warn("创建部分矩阵时，原始索引 {} 无效或对应行为null/长度错误，使用零值填充", originalIndex);
                Arrays.fill(newMatrix[i], BigDecimal.ZERO);
            }
        }
        return newMatrix;
    }

    private BigDecimal calculateTotalAmount(BigDecimal[][] allocationMatrix,
                                            BigDecimal[][] regionCustomerMatrix) {
        BigDecimal total = BigDecimal.ZERO;
        if (allocationMatrix == null || regionCustomerMatrix == null || allocationMatrix.length != regionCustomerMatrix.length) {
            log.warn("计算总量时矩阵为空或维度不匹配 (alloc null={}, cust null={}, len mismatch={})",
                    allocationMatrix == null, regionCustomerMatrix == null,
                    allocationMatrix != null && regionCustomerMatrix != null && allocationMatrix.length != regionCustomerMatrix.length);
            return total;
        }
        for (int i = 0; i < allocationMatrix.length; i++) {
            if (allocationMatrix[i] == null || regionCustomerMatrix[i] == null || allocationMatrix[i].length != GRADE_COUNT || regionCustomerMatrix[i].length != GRADE_COUNT) {
                log.warn("计算总量时跳过无效行或维度不匹配的行: index {}", i);
                continue;
            }
            for (int j = 0; j < GRADE_COUNT; j++) {
                BigDecimal allocValue = (allocationMatrix[i][j] == null) ? BigDecimal.ZERO : allocationMatrix[i][j];
                BigDecimal customerCount = (regionCustomerMatrix[i][j] == null) ? BigDecimal.ZERO : regionCustomerMatrix[i][j];
                if (customerCount.compareTo(BigDecimal.ZERO) > 0) {
                    total = total.add(allocValue.multiply(customerCount));
                }
            }
        }
        return total;
    }

    // 深拷贝方法 (保持不变)
    private BigDecimal[][] deepCopyMatrix(BigDecimal[][] original) {
        if (original == null) return null;
        if (original.length == 0) return new BigDecimal[0][0];
        int cols = -1;
        if (original[0] != null) {
            cols = original[0].length;
        } else {
            log.warn("深拷贝时发现首行为null，无法确定列数，将创建空矩阵");
            return new BigDecimal[original.length][0];
        }
        if (cols != GRADE_COUNT) {
            log.warn("深拷贝时发现输入矩阵列数({})不为 {}", cols, GRADE_COUNT);
            cols = GRADE_COUNT;
            BigDecimal[][] zeroMatrix = new BigDecimal[original.length][cols];
            for(int i = 0; i < original.length; i++){
                zeroMatrix[i] = new BigDecimal[cols];
                Arrays.fill(zeroMatrix[i], BigDecimal.ZERO);
            }
            log.warn("返回一个 {}x{} 的零矩阵代替", original.length, cols);
            return zeroMatrix;
        }

        int rows = original.length;
        BigDecimal[][] copy = new BigDecimal[rows][cols];

        for (int i = 0; i < rows; i++) {
            copy[i] = new BigDecimal[cols];
            if (original[i] != null && original[i].length == cols) {
                for (int j = 0; j < cols; j++) {
                    copy[i][j] = (original[i][j] != null) ? original[i][j] : BigDecimal.ZERO;
                }
            } else {
                log.warn("深拷贝时发现第 {} 行数据为null或长度不为 {}，使用零值填充", i, cols);
                Arrays.fill(copy[i], BigDecimal.ZERO);
            }
        }
        return copy;
    }
}