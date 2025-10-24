package org.example.service.strategy;

import lombok.extern.slf4j.Slf4j;
import org.example.service.CommonService;
import org.example.service.algorithm.MarketProportionalCigaretteDistributionAlgorithm;
import org.example.util.KmpMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map; // 确保导入 Map

/**
 * 市场类型分配策略实现
 *
 * 【适用场景】
 * 投放方法："按档位扩展投放"
 * 投放类型："档位+市场类型"
 *
 * 【算法特点】
 * - 按市场类型（城网/农网）进行分类投放
 * - 支持比例分配算法
 * - 使用市场预测算法
 *
 * @author Robin
 * @version 1.1 - 修改比例确定逻辑
 * @since 2025-10-22 // 修改版本和日期
 */
@Slf4j
@Component
public class MarketDistributionStrategy implements DistributionStrategy {

    @Autowired
    private CommonService commonService;

    @Autowired
    private MarketProportionalCigaretteDistributionAlgorithm distributionAlgorithm;

    @Autowired
    private KmpMatcher kmpMatcher;

    @Override
    public String getDeliveryType() {
        return "档位+市场类型";
    }

    @Override
    public List<String> getTargetList(String deliveryArea) {
        if (!isValidDeliveryArea(deliveryArea)) {
            throw new IllegalArgumentException("投放区域不能为空");
        }

        try {
            log.debug("解析市场类型投放目标，投放区域: {}", deliveryArea);

            // 直接通过CommonService获取所有市场类型列表
            List<String> allMarkets = commonService.getAllRegionList("按档位扩展投放", "档位+市场类型"); //

            // 使用KmpMatcher进行模式匹配
            List<String> targetList = kmpMatcher.matchPatterns(deliveryArea, allMarkets); //

            if (targetList == null || targetList.isEmpty()) { //
                throw new RuntimeException("未找到匹配的市场类型: " + deliveryArea); //
            }

            log.debug("解析到{}个市场类型目标: {}", targetList.size(), targetList); //
            return targetList; //

        } catch (Exception e) {
            log.error("解析市场类型投放目标失败，投放区域: {}", deliveryArea, e); //
            throw new RuntimeException("市场类型目标解析失败: " + e.getMessage(), e); //
        }
    }

    @Override
    public BigDecimal[][] calculateMatrix(List<String> targetList, BigDecimal targetAmount) {
        // 调用带额外参数的方法，不传递比例参数（使用默认值或根据逻辑调整）
        return calculateMatrix(targetList, targetAmount, null); //
    }

    @Override
    public BigDecimal[][] calculateMatrix(List<String> targetList, BigDecimal targetAmount,
                                          Map<String, Object> extraParams) { // 确保是 java.util.Map //
        if (!isValidTargetList(targetList)) { //
            throw new IllegalArgumentException("市场类型目标列表无效"); //
        }

        if (!isValidTargetAmount(targetAmount)) { //
            throw new IllegalArgumentException("预投放量无效: " + targetAmount); //
        }

        try {
            log.debug("使用市场类型算法计算分配矩阵，目标市场数: {}, 预投放量: {}", //
                    targetList.size(), targetAmount); //

            // 获取完整的市场客户数矩阵
            BigDecimal[][] marketCustomerMatrix = getMarketCustomerMatrix(); //

            // 根据目标市场筛选客户数矩阵
            BigDecimal[][] targetMarketCustomerMatrix = new BigDecimal[targetList.size()][30]; //
            List<String> allMarkets = commonService.getAllRegionList("按档位扩展投放", "档位+市场类型"); //

            for (int i = 0; i < targetList.size(); i++) { //
                String targetMarket = targetList.get(i); //
                int marketIndex = allMarkets.indexOf(targetMarket); //
                if (marketIndex >= 0 && marketIndex < marketCustomerMatrix.length) { //
                    System.arraycopy(marketCustomerMatrix[marketIndex], 0, targetMarketCustomerMatrix[i], 0, 30); //
                } else {
                    log.warn("未找到目标市场 '{}' 的客户数数据，使用零值", targetMarket); //
                    // 如果找不到对应市场的客户数据，使用零值数组
                    targetMarketCustomerMatrix[i] = new BigDecimal[30]; //
                    for (int j = 0; j < 30; j++) { //
                        targetMarketCustomerMatrix[i][j] = BigDecimal.ZERO; //
                    }
                }
            }

            // --- 新增逻辑：检查目标列表中实际包含的市场类型 ---
            boolean hasUrban = false; //
            boolean hasRural = false; //
            for (String market : targetList) { //
                if (market != null) { // 添加 null 检查
                    if (market.contains("城")) { //
                        hasUrban = true; //
                    } else if (market.contains("农")) { //
                        hasRural = true; //
                    }
                }
            }
            log.debug("市场类型检查：城网={}, 农网={}", hasUrban, hasRural);

            // --- 修改逻辑：根据实际包含的市场类型确定比例 ---
            BigDecimal urbanRatio;
            BigDecimal ruralRatio;

            if (hasUrban && hasRural) {
                // 同时存在城网和农网，才考虑前端传入的比例
                log.info("待投放区域同时包含城网和农网"); //
                if (extraParams != null &&
                        extraParams.containsKey("urbanRatio") && extraParams.get("urbanRatio") instanceof BigDecimal && // 检查类型
                        extraParams.containsKey("ruralRatio") && extraParams.get("ruralRatio") instanceof BigDecimal) { // 检查类型
                    // 使用前端传入的比例
                    urbanRatio = (BigDecimal) extraParams.get("urbanRatio"); //
                    ruralRatio = (BigDecimal) extraParams.get("ruralRatio"); //
                    log.info("使用前端传入的比例 - 城网: {}, 农网: {}", urbanRatio, ruralRatio); //
                } else {
                    // 使用默认比例：城网40%，农网60%
                    urbanRatio = new BigDecimal("0.4"); //
                    ruralRatio = new BigDecimal("0.6"); //
                    log.info("未提供有效比例参数或参数类型错误，使用默认比例 - 城网: {}, 农网: {}", urbanRatio, ruralRatio); //
                }
            } else if (hasUrban) {
                // 仅包含城网，强制比例为 100% / 0%
                urbanRatio = BigDecimal.ONE; //
                ruralRatio = BigDecimal.ZERO; //
                log.info("待投放区域仅包含城网，强制比例为 城网: 100%, 农网: 0%"); //
                if (extraParams != null && (extraParams.containsKey("urbanRatio") || extraParams.containsKey("ruralRatio"))) { //
                    log.warn("前端传入的比例参数被忽略，因为只包含城网"); //
                }
            } else if (hasRural) {
                // 仅包含农网，强制比例为 0% / 100%
                urbanRatio = BigDecimal.ZERO; //
                ruralRatio = BigDecimal.ONE; //
                log.info("待投放区域仅包含农网，强制比例为 城网: 0%, 农网: 100%"); //
                if (extraParams != null && (extraParams.containsKey("urbanRatio") || extraParams.containsKey("ruralRatio"))) { //
                    log.warn("前端传入的比例参数被忽略，因为只包含农网"); //
                }
            } else {
                // 既无城网也无农网（理论上不应发生，因为getTargetList会筛选），使用默认比例作为备用
                log.warn("目标列表既不包含城网也不包含农网，将使用默认比例 40%/60%");
                urbanRatio = new BigDecimal("0.4"); //
                ruralRatio = new BigDecimal("0.6"); //
            }

            // 调用分配算法计算分配矩阵（使用最终确定的比例）
            BigDecimal[][] matrix = distributionAlgorithm.calculateDistribution( //
                    targetList, targetMarketCustomerMatrix, targetAmount, urbanRatio, ruralRatio); //

            if (matrix == null || matrix.length == 0) { //
                throw new RuntimeException("市场类型算法返回空分配矩阵"); //
            }

            if (matrix.length != targetList.size()) { //
                throw new RuntimeException("分配矩阵行数与市场类型数不匹配"); //
            }

            log.debug("市场类型分配计算完成，矩阵维度: {}x{}", matrix.length, matrix[0].length); //
            return matrix; //

        } catch (Exception e) {
            log.error("市场类型分配计算失败，目标市场数: {}, 预投放量: {}", //
                    targetList.size(), targetAmount, e); //
            // 抛出运行时异常，以便事务回滚
            throw new RuntimeException("市场类型分配算法执行失败: " + e.getMessage(), e); //
        }
    }


    @Override
    public String getAlgorithmName() {
        return "MarketProportionalCigaretteDistributionAlgorithm"; //
    }

    @Override
    public String getTargetTypeDescription() {
        return "市场类型分配"; //
    }

    /**
     * 获取市场客户数矩阵
     * 从原Service类迁移而来，添加了缓存注解
     */
    @Cacheable(value = "marketCustomerMatrix") // 修正后的注解
    public BigDecimal[][] getMarketCustomerMatrix() {
        log.debug("通过CommonService获取市场类型客户数矩阵"); //
        CommonService.RegionCustomerMatrix regionCustomerMatrixObj =
                commonService.buildRegionCustomerMatrix("按档位扩展投放", "档位+市场类型"); //

        // 将List<BigDecimal[]>转换为BigDecimal[][]以保持接口兼容性
        List<BigDecimal[]> matrixList = regionCustomerMatrixObj.getCustomerMatrix(); //
        BigDecimal[][] matrix = new BigDecimal[matrixList.size()][30]; //

        for (int i = 0; i < matrixList.size(); i++) { //
            matrix[i] = matrixList.get(i); //
        }

        log.debug("市场类型客户数矩阵获取完成，矩阵大小: {}x30", matrix.length); //
        return matrix; //
    }
}