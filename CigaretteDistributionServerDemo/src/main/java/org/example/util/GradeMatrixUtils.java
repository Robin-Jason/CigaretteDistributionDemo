package org.example.util;

import lombok.extern.slf4j.Slf4j;
import org.example.entity.CigaretteDistributionPredictionData;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 档位矩阵操作工具类
 * 
 * 【核心功能】
 * 统一处理30个档位（D30-D1）的各种操作，消除重复代码
 * 
 * 【支持的操作类型】
 * - 档位数组设置到实体对象
 * - 从档位数组构建SQL参数
 * - 档位数组转换和验证
 * - 批量档位操作
 * 
 * 【设计原则】
 * - DRY原则：消除30个档位的重复设置代码
 * - 类型安全：严格的参数验证和null处理
 * - 性能优化：避免重复的数组操作
 * - 易用性：提供简洁的API接口
 * 
 * @author Robin
 * @version 1.0
 * @since 2025-10-11
 */
@Slf4j
public class GradeMatrixUtils {
    
    /**
     * 档位总数常量
     */
    public static final int GRADE_COUNT = 30;
    
    /**
     * 档位名称数组（从D30到D1）
     */
    public static final String[] GRADE_NAMES = {
        "D30", "D29", "D28", "D27", "D26", "D25", "D24", "D23", "D22", "D21",
        "D20", "D19", "D18", "D17", "D16", "D15", "D14", "D13", "D12", "D11",
        "D10", "D9", "D8", "D7", "D6", "D5", "D4", "D3", "D2", "D1"
    };
    
    // ==================== 实体对象档位设置 ====================
    
    /**
     * 将档位数组设置到CigaretteDistributionPredictionData实体对象
     * 使用反射或直接调用，避免30行重复的setter代码
     * 
     * @param entity 实体对象
     * @param grades 档位数组（长度必须为30）
     * @throws IllegalArgumentException 当参数无效时抛出异常
     * 
     * @example
     * CigaretteDistributionPredictionData data = new CigaretteDistributionPredictionData();
     * BigDecimal[] grades = allocationMatrix[0]; // 某区域的档位分配
     * GradeMatrixUtils.setGradesToEntity(data, grades);
     */
    public static void setGradesToEntity(CigaretteDistributionPredictionData entity, BigDecimal[] grades) {
        if (entity == null) {
            throw new IllegalArgumentException("实体对象不能为null");
        }
        
        if (grades == null || grades.length != GRADE_COUNT) {
            throw new IllegalArgumentException("档位数组必须包含" + GRADE_COUNT + "个元素");
        }
        
        try {
            // 直接调用setter方法，按D30到D1的顺序
            entity.setD30(grades[0]);  entity.setD29(grades[1]);  entity.setD28(grades[2]);
            entity.setD27(grades[3]);  entity.setD26(grades[4]);  entity.setD25(grades[5]);
            entity.setD24(grades[6]);  entity.setD23(grades[7]);  entity.setD22(grades[8]);
            entity.setD21(grades[9]);  entity.setD20(grades[10]); entity.setD19(grades[11]);
            entity.setD18(grades[12]); entity.setD17(grades[13]); entity.setD16(grades[14]);
            entity.setD15(grades[15]); entity.setD14(grades[16]); entity.setD13(grades[17]);
            entity.setD12(grades[18]); entity.setD11(grades[19]); entity.setD10(grades[20]);
            entity.setD9(grades[21]);  entity.setD8(grades[22]);  entity.setD7(grades[23]);
            entity.setD6(grades[24]);  entity.setD5(grades[25]);  entity.setD4(grades[26]);
            entity.setD3(grades[27]);  entity.setD2(grades[28]);  entity.setD1(grades[29]);
            
            log.debug("成功设置30个档位到实体对象");
            
        } catch (Exception e) {
            log.error("设置档位到实体对象失败: {}", e.getMessage(), e);
            throw new RuntimeException("档位设置失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从CigaretteDistributionPredictionData实体对象提取档位数组
     * 
     * @param entity 实体对象
     * @return 档位数组（长度为30）
     * @throws IllegalArgumentException 当实体对象为null时抛出异常
     */
    public static BigDecimal[] extractGradesFromEntity(CigaretteDistributionPredictionData entity) {
        if (entity == null) {
            throw new IllegalArgumentException("实体对象不能为null");
        }
        
        BigDecimal[] grades = new BigDecimal[GRADE_COUNT];
        
        // 按D30到D1的顺序提取
        grades[0] = entity.getD30();  grades[1] = entity.getD29();  grades[2] = entity.getD28();
        grades[3] = entity.getD27();  grades[4] = entity.getD26();  grades[5] = entity.getD25();
        grades[6] = entity.getD24();  grades[7] = entity.getD23();  grades[8] = entity.getD22();
        grades[9] = entity.getD21();  grades[10] = entity.getD20(); grades[11] = entity.getD19();
        grades[12] = entity.getD18(); grades[13] = entity.getD17(); grades[14] = entity.getD16();
        grades[15] = entity.getD15(); grades[16] = entity.getD14(); grades[17] = entity.getD13();
        grades[18] = entity.getD12(); grades[19] = entity.getD11(); grades[20] = entity.getD10();
        grades[21] = entity.getD9();  grades[22] = entity.getD8();  grades[23] = entity.getD7();
        grades[24] = entity.getD6();  grades[25] = entity.getD5();  grades[26] = entity.getD4();
        grades[27] = entity.getD3();  grades[28] = entity.getD2();  grades[29] = entity.getD1();
        
        return grades;
    }
    
    // ==================== SQL参数构建 ====================
    
    /**
     * 从档位数组构建SQL参数列表
     * 用于INSERT/UPDATE语句的参数绑定
     * 
     * @param grades 档位数组（长度必须为30）
     * @return SQL参数数组
     * @throws IllegalArgumentException 当档位数组无效时抛出异常
     * 
     * @example
     * BigDecimal[] grades = allocationMatrix[0];
     * Object[] sqlParams = GradeMatrixUtils.buildGradeSqlParams(grades);
     * // 在SQL中使用：VALUES (..., sqlParams[0], sqlParams[1], ..., sqlParams[29])
     */
    public static Object[] buildGradeSqlParams(BigDecimal[] grades) {
        validateGradeArray(grades, "构建SQL参数");
        
        Object[] params = new Object[GRADE_COUNT];
        System.arraycopy(grades, 0, params, 0, GRADE_COUNT);
        
        log.debug("构建了{}个档位SQL参数", GRADE_COUNT);
        return params;
    }
    
    /**
     * 从分配矩阵的指定行构建SQL参数列表
     * 
     * @param allocationMatrix 分配矩阵
     * @param rowIndex 行索引
     * @return SQL参数数组
     * @throws IllegalArgumentException 当参数无效时抛出异常
     */
    public static Object[] buildGradeSqlParams(BigDecimal[][] allocationMatrix, int rowIndex) {
        if (allocationMatrix == null) {
            throw new IllegalArgumentException("分配矩阵不能为null");
        }
        
        if (rowIndex < 0 || rowIndex >= allocationMatrix.length) {
            throw new IllegalArgumentException("行索引超出范围: " + rowIndex);
        }
        
        BigDecimal[] row = allocationMatrix[rowIndex];
        return buildGradeSqlParams(row);
    }
    
    // ==================== 矩阵操作 ====================
    
    /**
     * 批量创建CigaretteDistributionPredictionData记录列表
     * 用于编码表达式生成等场景
     * 
     * @param cigCode 卷烟代码
     * @param cigName 卷烟名称
     * @param deliveryMethod 投放方法
     * @param deliveryEtype 投放类型
     * @param allocationMatrix 分配矩阵
     * @param targetList 目标区域列表
     * @return 记录列表
     * @throws IllegalArgumentException 当参数无效时抛出异常
     * 
     * @example
     * List<CigaretteDistributionPredictionData> records = GradeMatrixUtils.buildPredictionRecords(
     *     "42020181", "黄鹤楼", "按档位扩展投放", "档位+区县", allocationMatrix, targetRegions);
     */
    public static List<CigaretteDistributionPredictionData> buildPredictionRecords(
            String cigCode, String cigName, String deliveryMethod, String deliveryEtype,
            BigDecimal[][] allocationMatrix, List<String> targetList) {
        
        // 参数验证
        if (cigCode == null || cigCode.trim().isEmpty()) {
            throw new IllegalArgumentException("卷烟代码不能为空");
        }
        if (cigName == null || cigName.trim().isEmpty()) {
            throw new IllegalArgumentException("卷烟名称不能为空");
        }
        if (allocationMatrix == null) {
            throw new IllegalArgumentException("分配矩阵不能为null");
        }
        if (targetList == null || targetList.isEmpty()) {
            throw new IllegalArgumentException("目标列表不能为空");
        }
        if (allocationMatrix.length != targetList.size()) {
            throw new IllegalArgumentException("分配矩阵行数与目标列表大小不匹配");
        }
        
        List<CigaretteDistributionPredictionData> records = new ArrayList<>();
        
        for (int i = 0; i < targetList.size(); i++) {
            CigaretteDistributionPredictionData record = new CigaretteDistributionPredictionData();
            
            // 设置基本信息
            record.setCigCode(cigCode);
            record.setCigName(cigName);
            record.setDeliveryArea(targetList.get(i));
            record.setDeliveryMethod(deliveryMethod);
            record.setDeliveryEtype(deliveryEtype);
            
            // 使用工具方法设置30个档位
            setGradesToEntity(record, allocationMatrix[i]);
            
            records.add(record);
        }
        
        log.debug("成功构建{}条预测数据记录", records.size());
        return records;
    }
    
    // ==================== 数据验证和转换 ====================
    
    /**
     * 验证档位数组的有效性
     * 
     * @param grades 档位数组
     * @param context 上下文信息（用于错误日志）
     * @throws IllegalArgumentException 当档位数组无效时抛出异常
     */
    public static void validateGradeArray(BigDecimal[] grades, String context) {
        if (grades == null) {
            throw new IllegalArgumentException(context + ": 档位数组不能为null");
        }
        
        if (grades.length != GRADE_COUNT) {
            throw new IllegalArgumentException(context + ": 档位数组长度必须为" + GRADE_COUNT + "，实际长度: " + grades.length);
        }
        
        // 检查是否所有档位都为null（可能表示数据错误）
        boolean allNull = true;
        for (BigDecimal grade : grades) {
            if (grade != null) {
                allNull = false;
                break;
            }
        }
        
        if (allNull) {
            log.warn("{}: 所有档位值均为null", context);
        }
    }
    
    /**
     * 将null值的档位设置为ZERO
     * 
     * @param grades 档位数组
     * @return 处理后的档位数组
     */
    public static BigDecimal[] normalizeGrades(BigDecimal[] grades) {
        validateGradeArray(grades, "标准化档位数组");
        
        BigDecimal[] normalized = new BigDecimal[GRADE_COUNT];
        for (int i = 0; i < GRADE_COUNT; i++) {
            normalized[i] = grades[i] != null ? grades[i] : BigDecimal.ZERO;
        }
        
        return normalized;
    }
    
    /**
     * 检查档位数组是否符合非递增约束
     * 在卷烟分配算法中，通常要求从高档位到低档位非递增
     * 
     * @param grades 档位数组
     * @return true表示符合非递增约束
     */
    public static boolean isNonIncreasing(BigDecimal[] grades) {
        validateGradeArray(grades, "检查非递增约束");
        
        for (int i = 1; i < GRADE_COUNT; i++) {
            BigDecimal current = grades[i] != null ? grades[i] : BigDecimal.ZERO;
            BigDecimal previous = grades[i - 1] != null ? grades[i - 1] : BigDecimal.ZERO;
            
            if (current.compareTo(previous) > 0) {
                log.debug("档位 {} 值({}) 大于档位 {} 值({}), 违反非递增约束", 
                         GRADE_NAMES[i], current, GRADE_NAMES[i-1], previous);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 计算档位数组的总和
     * 
     * @param grades 档位数组
     * @return 总和
     */
    public static BigDecimal sumGrades(BigDecimal[] grades) {
        validateGradeArray(grades, "计算档位总和");
        
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal grade : grades) {
            if (grade != null) {
                sum = sum.add(grade);
            }
        }
        
        return sum;
    }
    
    /**
     * 统计非零档位的数量
     * 
     * @param grades 档位数组
     * @return 非零档位数量
     */
    public static int countNonZeroGrades(BigDecimal[] grades) {
        validateGradeArray(grades, "统计非零档位");
        
        int count = 0;
        for (BigDecimal grade : grades) {
            if (grade != null && grade.compareTo(BigDecimal.ZERO) > 0) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * 创建零值档位数组
     * 
     * @return 全为BigDecimal.ZERO的档位数组
     */
    public static BigDecimal[] createZeroGradeArray() {
        BigDecimal[] grades = new BigDecimal[GRADE_COUNT];
        Arrays.fill(grades, BigDecimal.ZERO);
        return grades;
    }
    
    /**
     * 复制档位数组
     * 
     * @param grades 原档位数组
     * @return 复制的档位数组
     */
    public static BigDecimal[] copyGradeArray(BigDecimal[] grades) {
        validateGradeArray(grades, "复制档位数组");
        return Arrays.copyOf(grades, GRADE_COUNT);
    }
}
