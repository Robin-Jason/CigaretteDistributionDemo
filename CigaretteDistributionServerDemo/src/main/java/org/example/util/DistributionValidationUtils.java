package org.example.util;

import lombok.extern.slf4j.Slf4j;
import org.example.entity.CigaretteDistributionPredictionData;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 分配计算参数验证工具类
 * 
 * 【核心功能】
 * 提供统一的参数验证逻辑，避免重复的验证代码
 * 
 * 【验证类型】
 * - 基础参数验证：非空、类型检查
 * - 业务参数验证：卷烟信息、投放参数
 * - 数据结构验证：矩阵、列表完整性
 * - 数值范围验证：年月日、投放量范围
 * 
 * 【设计原则】
 * - 快速失败：参数无效时立即抛出异常
 * - 清晰错误：提供详细的错误信息
 * - 统一标准：所有验证使用相同的规则
 * - 性能优化：避免重复验证
 * 
 * @author Robin
 * @version 1.0
 * @since 2025-10-11
 */
@Slf4j
public class DistributionValidationUtils {
    
    // ==================== 基础参数验证 ====================
    
    /**
     * 验证字符串参数非空
     * 
     * @param value 字符串值
     * @param paramName 参数名称（用于错误提示）
     * @throws IllegalArgumentException 当参数为空时抛出异常
     * 
     * @example
     * validateNotEmpty(cigCode, "卷烟代码");
     */
    public static void validateNotEmpty(String value, String paramName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(paramName + "不能为空");
        }
    }
    
    /**
     * 验证对象参数非null
     * 
     * @param value 对象值
     * @param paramName 参数名称（用于错误提示）
     * @throws IllegalArgumentException 当参数为null时抛出异常
     */
    public static void validateNotNull(Object value, String paramName) {
        if (value == null) {
            throw new IllegalArgumentException(paramName + "不能为null");
        }
    }
    
    /**
     * 验证列表参数非空且包含元素
     * 
     * @param list 列表
     * @param paramName 参数名称
     * @throws IllegalArgumentException 当列表为空时抛出异常
     */
    public static void validateNotEmpty(List<?> list, String paramName) {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException(paramName + "不能为空");
        }
    }
    
    // ==================== 业务参数验证 ====================
    
    /**
     * 验证卷烟基本信息
     * 
     * @param cigCode 卷烟代码
     * @param cigName 卷烟名称
     * @throws IllegalArgumentException 当参数无效时抛出异常
     */
    public static void validateCigaretteInfo(String cigCode, String cigName) {
        validateNotEmpty(cigCode, "卷烟代码");
        validateNotEmpty(cigName, "卷烟名称");
        
        // 卷烟代码长度验证（通常为8位数字）
        if (!cigCode.matches("\\d{8}")) {
            throw new IllegalArgumentException("卷烟代码格式无效，应为8位数字: " + cigCode);
        }
        
        // 卷烟名称长度验证
        if (cigName.length() > 100) {
            throw new IllegalArgumentException("卷烟名称过长，最大100字符: " + cigName);
        }
    }
    
    /**
     * 清洗卷烟代码
     * 去除前后空格、换行符等不规范字符，确保代码格式正确
     * 
     * @param cigCode 原始卷烟代码
     * @return 清洗后的卷烟代码
     * 
     * @example
     * sanitizeCigaretteCode(" 42020157 \n") -> "42020157"
     * sanitizeCigaretteCode("42020009") -> "42020009"
     */
    public static String sanitizeCigaretteCode(String cigCode) {
        if (cigCode == null) {
            return null;
        }
        
        // 清除前后空格、换行符、制表符等空白字符
        String cleaned = cigCode.trim().replaceAll("\\s+", "");
        
        // 记录清洗过程
        if (!cigCode.equals(cleaned)) {
            log.warn("卷烟代码已清洗: 原始[{}] -> 清洗后[{}]", cigCode, cleaned);
        }
        
        return cleaned;
    }
    
    /**
     * 清洗和验证卷烟基本信息
     * 先进行数据清洗，再进行验证
     * 
     * @param cigCode 原始卷烟代码
     * @param cigName 卷烟名称  
     * @return 清洗后的卷烟代码
     * @throws IllegalArgumentException 当参数无效时抛出异常
     */
    public static String sanitizeAndValidateCigaretteCode(String cigCode, String cigName) {
        // 先清洗代码
        String cleanedCode = sanitizeCigaretteCode(cigCode);
        
        // 再验证清洗后的数据
        validateCigaretteInfo(cleanedCode, cigName);
        
        return cleanedCode;
    }
    
    /**
     * 验证投放参数
     * 
     * @param deliveryMethod 投放方法
     * @param deliveryEtype 投放类型
     * @param deliveryArea 投放区域
     * @throws IllegalArgumentException 当参数无效时抛出异常
     */
    public static void validateDeliveryParams(String deliveryMethod, String deliveryEtype, String deliveryArea) {
        validateNotEmpty(deliveryMethod, "投放方法");
        validateNotEmpty(deliveryArea, "投放区域");
        
        // 验证投放方法的有效性
        if (!"按档位统一投放".equals(deliveryMethod) && !"按档位投放".equals(deliveryMethod) && !"按档位扩展投放".equals(deliveryMethod)) {
            throw new IllegalArgumentException("不支持的投放方法: " + deliveryMethod);
        }
        
        // 扩展投放必须有投放类型
        if ("按档位扩展投放".equals(deliveryMethod)) {
            validateNotEmpty(deliveryEtype, "扩展投放类型");
            
            String[] validEtypes = {"档位+区县", "档位+市场类型", "档位+城乡分类代码", "档位+业态"};
            boolean isValidEtype = false;
            for (String validEtype : validEtypes) {
                if (validEtype.equals(deliveryEtype)) {
                    isValidEtype = true;
                    break;
                }
            }
            
            if (!isValidEtype) {
                throw new IllegalArgumentException("不支持的扩展投放类型: " + deliveryEtype);
            }
        }
    }
    
    /**
     * 验证时间参数
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @throws IllegalArgumentException 当时间参数无效时抛出异常
     */
    public static void validateTimeParams(Integer year, Integer month, Integer weekSeq) {
        validateNotNull(year, "年份");
        validateNotNull(month, "月份");
        validateNotNull(weekSeq, "周序号");
        
        if (year < 2020 || year > 2099) {
            throw new IllegalArgumentException("年份超出有效范围[2020-2099]: " + year);
        }
        
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("月份超出有效范围[1-12]: " + month);
        }
        
        if (weekSeq < 1 || weekSeq > 5) {
            throw new IllegalArgumentException("周序号超出有效范围[1-5]: " + weekSeq);
        }
    }
    
    /**
     * 验证预投放量
     * 
     * @param targetAmount 预投放量
     * @throws IllegalArgumentException 当预投放量无效时抛出异常
     */
    public static void validateTargetAmount(BigDecimal targetAmount) {
        validateNotNull(targetAmount, "预投放量");
        
        if (targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("预投放量必须大于0: " + targetAmount);
        }
        
        // 检查预投放量是否过大（防止异常数据）
        BigDecimal maxAmount = new BigDecimal("999999999");
        if (targetAmount.compareTo(maxAmount) > 0) {
            throw new IllegalArgumentException("预投放量超出合理范围: " + targetAmount);
        }
    }
    
    // ==================== 数据结构验证 ====================
    
    /**
     * 验证分配矩阵的完整性
     * 
     * @param allocationMatrix 分配矩阵
     * @param targetList 目标列表
     * @throws IllegalArgumentException 当矩阵无效时抛出异常
     */
    public static void validateAllocationMatrix(BigDecimal[][] allocationMatrix, List<String> targetList) {
        validateNotNull(allocationMatrix, "分配矩阵");
        validateNotEmpty(targetList, "目标列表");
        
        if (allocationMatrix.length == 0) {
            throw new IllegalArgumentException("分配矩阵不能为空");
        }
        
        if (allocationMatrix.length != targetList.size()) {
            throw new IllegalArgumentException(
                String.format("分配矩阵行数(%d)与目标列表大小(%d)不匹配", 
                            allocationMatrix.length, targetList.size()));
        }
        
        // 验证每行的档位数量
        for (int i = 0; i < allocationMatrix.length; i++) {
            BigDecimal[] row = allocationMatrix[i];
            if (row == null) {
                throw new IllegalArgumentException("分配矩阵第" + (i + 1) + "行为null");
            }
            
            if (row.length != GradeMatrixUtils.GRADE_COUNT) {
                throw new IllegalArgumentException(
                    String.format("分配矩阵第%d行档位数量(%d)不正确，应为%d", 
                                i + 1, row.length, GradeMatrixUtils.GRADE_COUNT));
            }
        }
        
        log.debug("分配矩阵验证通过: {}行x{}列", allocationMatrix.length, GradeMatrixUtils.GRADE_COUNT);
    }
    
    /**
     * 验证预测数据记录的完整性
     * 
     * @param predictionData 预测数据
     * @throws IllegalArgumentException 当数据无效时抛出异常
     */
    public static void validatePredictionData(CigaretteDistributionPredictionData predictionData) {
        validateNotNull(predictionData, "预测数据");
        
        validateCigaretteInfo(predictionData.getCigCode(), predictionData.getCigName());
        validateTimeParams(predictionData.getYear(), predictionData.getMonth(), predictionData.getWeekSeq());
        
        validateNotEmpty(predictionData.getDeliveryArea(), "投放区域");
        validateNotEmpty(predictionData.getDeliveryMethod(), "投放方法");
        
        // 验证档位数据（至少有一个非null值）
        BigDecimal[] grades = GradeMatrixUtils.extractGradesFromEntity(predictionData);
        boolean hasNonNullGrade = false;
        for (BigDecimal grade : grades) {
            if (grade != null) {
                hasNonNullGrade = true;
                break;
            }
        }
        
        if (!hasNonNullGrade) {
            throw new IllegalArgumentException("预测数据必须包含至少一个非空档位值");
        }
    }
    
    /**
     * 验证预测数据列表
     * 
     * @param predictionDataList 预测数据列表
     * @throws IllegalArgumentException 当列表无效时抛出异常
     */
    public static void validatePredictionDataList(List<CigaretteDistributionPredictionData> predictionDataList) {
        validateNotEmpty(predictionDataList, "预测数据列表");
        
        for (int i = 0; i < predictionDataList.size(); i++) {
            try {
                validatePredictionData(predictionDataList.get(i));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("预测数据列表第" + (i + 1) + "条记录无效: " + e.getMessage(), e);
            }
        }
        
        log.debug("预测数据列表验证通过: {}条记录", predictionDataList.size());
    }
    
    // ==================== 数据库查询结果验证 ====================
    
    /**
     * 验证数据库查询结果
     * 
     * @param queryResult 查询结果列表
     * @param contextInfo 上下文信息（用于错误提示）
     * @throws RuntimeException 当查询结果为空时抛出异常
     */
    public static void validateQueryResult(List<?> queryResult, String contextInfo) {
        if (queryResult == null || queryResult.isEmpty()) {
            throw new RuntimeException(contextInfo + ": 查询结果为空");
        }
        
        log.debug("{}: 查询返回{}条记录", contextInfo, queryResult.size());
    }
    
    /**
     * 验证Map类型的查询结果
     * 
     * @param queryResult Map类型的查询结果
     * @param contextInfo 上下文信息
     * @param requiredKeys 必需的键列表
     * @throws RuntimeException 当结果无效时抛出异常
     */
    public static void validateMapResult(Map<String, Object> queryResult, String contextInfo, String... requiredKeys) {
        if (queryResult == null || queryResult.isEmpty()) {
            throw new RuntimeException(contextInfo + ": 查询结果为空");
        }
        
        for (String key : requiredKeys) {
            if (!queryResult.containsKey(key) || queryResult.get(key) == null) {
                throw new RuntimeException(contextInfo + ": 缺少必需字段 '" + key + "'");
            }
        }
        
        log.debug("{}: Map结果验证通过，包含{}个字段", contextInfo, queryResult.size());
    }
    
    // ==================== 组合验证方法 ====================
    
    /**
     * 验证分配写回操作的所有参数
     * 
     * @param allocationMatrix 分配矩阵
     * @param targetList 目标列表
     * @param cigCode 卷烟代码
     * @param cigName 卷烟名称
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @param deliveryMethod 投放方法
     * @param deliveryEtype 投放类型
     * @throws IllegalArgumentException 当任何参数无效时抛出异常
     */
    public static void validateWriteBackParams(
            BigDecimal[][] allocationMatrix, List<String> targetList,
            String cigCode, String cigName, 
            Integer year, Integer month, Integer weekSeq,
            String deliveryMethod, String deliveryEtype) {
        
        // 验证基础信息
        validateCigaretteInfo(cigCode, cigName);
        validateTimeParams(year, month, weekSeq);
        validateDeliveryParams(deliveryMethod, deliveryEtype, "临时");
        
        // 验证矩阵和目标
        validateAllocationMatrix(allocationMatrix, targetList);
        
        log.debug("写回参数验证通过: 卷烟{}-{}, 时间{}-{}-{}, 目标数{}", 
                 cigCode, cigName, year, month, weekSeq, targetList.size());
    }
    
    /**
     * 验证算法计算的所有参数
     * 
     * @param targetList 目标列表
     * @param targetAmount 预投放量
     * @param deliveryMethod 投放方法
     * @param deliveryEtype 投放类型
     * @throws IllegalArgumentException 当任何参数无效时抛出异常
     */
    public static void validateCalculationParams(
            List<String> targetList, BigDecimal targetAmount,
            String deliveryMethod, String deliveryEtype) {
        
        validateNotEmpty(targetList, "目标列表");
        validateTargetAmount(targetAmount);
        validateNotEmpty(deliveryMethod, "投放方法");
        
        if ("按档位扩展投放".equals(deliveryMethod)) {
            validateNotEmpty(deliveryEtype, "扩展投放类型");
        }
        
        log.debug("算法计算参数验证通过: 目标数{}, 预投放量{}, 投放方法{}", 
                 targetList.size(), targetAmount, deliveryMethod);
    }
}
