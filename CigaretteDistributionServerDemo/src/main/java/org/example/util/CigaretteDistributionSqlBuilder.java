package org.example.util;

/**
 * 卷烟分配SQL构建工具类
 * 
 * 【核心功能】
 * 为卷烟分配系统提供统一的SQL语句构建服务，支持动态表名策略
 * 
 * 【支持的SQL类型】
 * - 区域查询SQL：获取投放区域列表和客户数矩阵
 * - 卷烟数据查询SQL：根据投放类型查询基础信息
 * - 数据操作SQL：插入、删除、更新预测数据
 * 
 * 【设计原则】
 * - 类型安全：避免SQL注入，使用参数化查询
 * - 可读性：清晰的方法命名和SQL结构
 * - 可维护性：集中管理所有SQL构建逻辑
 * - 灵活性：支持动态表名和条件构建
 * 
 * @author Robin
 * @version 1.0
 * @since 2025-10-11
 */
public class CigaretteDistributionSqlBuilder {
    
    // ==================== 区域查询SQL ====================
    
    /**
     * 构建获取投放区域列表的SQL
     * 获取指定表中所有不重复的区域名称，按字母顺序排序
     * 
     * @param tableName 区域客户数表名（如：region_clientNum_1_1）
     * @param fieldName 区域字段名（通常为"region"）
     * @return 查询SQL语句
     * 
     * @example
     * buildRegionListSql("region_clientNum_1_1", "region")
     * -> "SELECT DISTINCT region FROM region_clientNum_1_1 WHERE region IS NOT NULL ORDER BY region"
     */
    public static String buildRegionListSql(String tableName, String fieldName) {
        return String.format("SELECT DISTINCT %s FROM %s WHERE %s IS NOT NULL ORDER BY %s", 
                           fieldName, tableName, fieldName, fieldName);
    }
    
    /**
     * 构建获取区域客户数矩阵的SQL - 全市统一投放版本
     * 用于"按档位统一投放"类型，查询单个"全市"区域的30个档位数据
     * 
     * @param tableName 区域客户数表名
     * @param fieldName 区域字段名
     * @return 查询SQL语句
     * 
     * @example
     * buildCityMatrixSql("region_clientNum_0_0", "region")
     * -> "SELECT region as region_name, D30, D29, ..., D1 FROM region_clientNum_0_0 WHERE region = '全市'"
     */
    public static String buildCityMatrixSql(String tableName, String fieldName) {
        String baseColumns = buildGradeColumns();
        return String.format("SELECT %s as region_name, %s FROM %s WHERE %s = '全市'", 
                           fieldName, baseColumns, tableName, fieldName);
    }
    
    /**
     * 构建获取区域客户数矩阵的SQL - 扩展投放版本
     * 用于扩展投放类型，查询所有区域的30个档位数据
     * 
     * @param tableName 区域客户数表名
     * @param fieldName 区域字段名
     * @return 查询SQL语句
     * 
     * @example
     * buildRegionMatrixSql("region_clientNum_1_1", "region")
     * -> "SELECT region as region_name, D30, D29, ..., D1 FROM region_clientNum_1_1 ORDER BY region"
     */
    public static String buildRegionMatrixSql(String tableName, String fieldName) {
        String baseColumns = buildGradeColumns();
        return String.format("SELECT %s as region_name, %s FROM %s ORDER BY %s", 
                           fieldName, baseColumns, tableName, fieldName);
    }
    
    // ==================== 卷烟数据查询SQL ====================
    
    /**
     * 构建按投放类型查询卷烟基础信息的SQL - 全市统一投放版本
     * 用于查询投放类型为NULL或'NULL'的记录
     * 
     * @param tableName 卷烟投放基础信息表名（如：cigarette_distribution_info_2025_9_3）
     * @return 查询SQL语句
     * 
     * @example
     * buildCityDeliveryQuerySql("cigarette_distribution_info_2025_9_3")
     * -> "SELECT * FROM cigarette_distribution_info_2025_9_3 WHERE (DELIVERY_ETYPE IS NULL OR DELIVERY_ETYPE = 'NULL')"
     */
    public static String buildCityDeliveryQuerySql(String tableName) {
        return String.format("SELECT * FROM %s WHERE (DELIVERY_ETYPE IS NULL OR DELIVERY_ETYPE = 'NULL')", 
                           tableName);
    }
    
    /**
     * 构建按投放类型查询卷烟基础信息的SQL - 扩展投放版本
     * 用于查询特定投放类型的记录，使用参数化查询
     * 
     * @param tableName 卷烟投放基础信息表名
     * @return 查询SQL语句和参数占位符
     * 
     * @example
     * buildDeliveryTypeQuerySql("cigarette_distribution_info_2025_9_3")
     * -> "SELECT * FROM cigarette_distribution_info_2025_9_3 WHERE DELIVERY_ETYPE = ?"
     */
    public static String buildDeliveryTypeQuerySql(String tableName) {
        return String.format("SELECT * FROM %s WHERE DELIVERY_ETYPE = ?", tableName);
    }
    
    // ==================== 数据操作SQL ====================
    
    /**
     * 构建批量插入预测数据的SQL（支持更新重复记录）
     * 插入到动态生成的cigarette_distribution_prediction表中，包含所有字段
     * 当遇到唯一约束冲突时，自动更新现有记录而不是失败
     * 
     * @param tableName 预测数据表名（如：cigarette_distribution_prediction_2025_9_3）
     * @return 插入/更新SQL语句
     * 
     * @example
     * buildBatchInsertSql("cigarette_distribution_prediction_2025_9_3")
     * -> "INSERT INTO cigarette_distribution_prediction_2025_9_3 (...) VALUES (...) ON DUPLICATE KEY UPDATE ..."
     */
    public static String buildBatchInsertSql(String tableName) {
        return String.format("INSERT INTO %s " +
                "(CIG_CODE, CIG_NAME, YEAR, MONTH, WEEK_SEQ, DELIVERY_AREA, DELIVERY_METHOD, DELIVERY_ETYPE, " +
                "D30, D29, D28, D27, D26, D25, D24, D23, D22, D21, D20, D19, D18, D17, D16, D15, D14, D13, D12, D11, D10, " +
                "D9, D8, D7, D6, D5, D4, D3, D2, D1, BZ, ACTUAL_DELIVERY, DEPLOYINFO_CODE) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "DELIVERY_METHOD=VALUES(DELIVERY_METHOD), DELIVERY_ETYPE=VALUES(DELIVERY_ETYPE), " +
                "D30=VALUES(D30), D29=VALUES(D29), D28=VALUES(D28), D27=VALUES(D27), D26=VALUES(D26), " +
                "D25=VALUES(D25), D24=VALUES(D24), D23=VALUES(D23), D22=VALUES(D22), D21=VALUES(D21), " +
                "D20=VALUES(D20), D19=VALUES(D19), D18=VALUES(D18), D17=VALUES(D17), D16=VALUES(D16), " +
                "D15=VALUES(D15), D14=VALUES(D14), D13=VALUES(D13), D12=VALUES(D12), D11=VALUES(D11), " +
                "D10=VALUES(D10), D9=VALUES(D9), D8=VALUES(D8), D7=VALUES(D7), D6=VALUES(D6), " +
                "D5=VALUES(D5), D4=VALUES(D4), D3=VALUES(D3), D2=VALUES(D2), D1=VALUES(D1), " +
                "BZ=VALUES(BZ), ACTUAL_DELIVERY=VALUES(ACTUAL_DELIVERY), DEPLOYINFO_CODE=VALUES(DEPLOYINFO_CODE)", 
                tableName);
    }
    
    /**
     * 构建检查记录存在性的SQL
     * 用于删除操作前的安全检查
     * 
     * @param tableName 预测数据表名
     * @return 查询SQL语句
     * 
     * @example
     * buildExistenceCheckSql("cigarette_distribution_prediction_2025_9_3")
     * -> "SELECT COUNT(*) FROM cigarette_distribution_prediction_2025_9_3 WHERE CIG_CODE = ? AND CIG_NAME = ? AND DELIVERY_AREA = ?"
     */
    public static String buildExistenceCheckSql(String tableName) {
        return String.format("SELECT COUNT(*) FROM %s WHERE CIG_CODE = ? AND CIG_NAME = ? AND DELIVERY_AREA = ?", 
                           tableName);
    }
    
    /**
     * 构建精确删除记录的SQL
     * 根据卷烟代码、名称和投放区域精确删除记录
     * 
     * @param tableName 预测数据表名
     * @return 删除SQL语句
     * 
     * @example
     * buildPreciseDeleteSql("cigarette_distribution_prediction_2025_9_3")
     * -> "DELETE FROM cigarette_distribution_prediction_2025_9_3 WHERE CIG_CODE = ? AND CIG_NAME = ? AND DELIVERY_AREA = ?"
     */
    public static String buildPreciseDeleteSql(String tableName) {
        return String.format("DELETE FROM %s WHERE CIG_CODE = ? AND CIG_NAME = ? AND DELIVERY_AREA = ?", 
                           tableName);
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 构建30个档位字段的列名字符串
     * 从D30到D1，用逗号分隔
     * 
     * @return 档位列名字符串
     * 
     * @example
     * buildGradeColumns() -> "D30, D29, D28, ..., D2, D1"
     */
    private static String buildGradeColumns() {
        StringBuilder columns = new StringBuilder();
        for (int i = 30; i >= 1; i--) {
            if (columns.length() > 0) {
                columns.append(", ");
            }
            columns.append("D").append(i);
        }
        return columns.toString();
    }
    
    // ==================== SQL验证和安全检查 ====================
    
    /**
     * 验证表名的安全性
     * 防止SQL注入，确保表名符合命名规范
     * 
     * @param tableName 待验证的表名
     * @return true表示表名安全，false表示存在安全风险
     * 
     * @example
     * isValidTableName("region_clientNum_1_1") -> true
     * isValidTableName("users; DROP TABLE--") -> false
     */
    public static boolean isValidTableName(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            return false;
        }
        
        // 检查是否包含危险字符
        String cleanTableName = tableName.trim();
        if (cleanTableName.matches(".*[;'\"\\-\\s].*")) {
            return false;
        }
        
        // 检查是否符合预期的表名模式
        return cleanTableName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$");
    }
    
    /**
     * 验证字段名的安全性
     * 防止SQL注入，确保字段名符合命名规范
     * 
     * @param fieldName 待验证的字段名
     * @return true表示字段名安全，false表示存在安全风险
     */
    public static boolean isValidFieldName(String fieldName) {
        if (fieldName == null || fieldName.trim().isEmpty()) {
            return false;
        }
        
        String cleanFieldName = fieldName.trim();
        return cleanFieldName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$");
    }
    
    /**
     * 构建安全的SQL语句
     * 在SQL构建前进行安全验证
     * 
     * @param tableName 表名
     * @param fieldName 字段名（可选）
     * @throws IllegalArgumentException 当表名或字段名不安全时抛出异常
     */
    public static void validateSqlComponents(String tableName, String fieldName) {
        if (!isValidTableName(tableName)) {
            throw new IllegalArgumentException("不安全的表名: " + tableName);
        }
        
        if (fieldName != null && !isValidFieldName(fieldName)) {
            throw new IllegalArgumentException("不安全的字段名: " + fieldName);
        }
    }
}
