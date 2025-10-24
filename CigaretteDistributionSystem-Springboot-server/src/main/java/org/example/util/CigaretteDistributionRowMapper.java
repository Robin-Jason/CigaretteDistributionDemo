package org.example.util;

import lombok.extern.slf4j.Slf4j;
import org.example.entity.CigaretteDistributionInfoData;
import org.example.entity.CigaretteDistributionPredictionData;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 卷烟分配数据映射工具类
 * 
 * 【核心功能】
 * 为卷烟分配系统提供统一的数据库结果集映射和参数构建服务
 * 
 * 【支持的映射类型】
 * - 查询结果映射：将Map<String, Object>转换为实体对象
 * - 参数数组构建：将实体对象转换为SQL参数数组
 * - 数据类型转换：处理各种数据库字段类型
 * 
 * 【设计原则】
 * - 健壮性：处理null值和类型转换异常
 * - 一致性：统一的字段映射规则
 * - 可扩展性：易于添加新的映射规则
 * - 性能优化：避免重复的类型检查和转换
 * 
 * @author Robin
 * @version 1.0
 * @since 2025-10-11
 */
@Slf4j
public class CigaretteDistributionRowMapper {
    
    // ==================== 查询结果映射 ====================
    
    /**
     * 将数据库查询结果行转换为CigaretteDistributionInfoData对象
     * 处理卷烟投放基础信息表的查询结果映射
     * 
     * @param row 数据库查询结果行
     * @return CigaretteDistributionInfoData对象，如果转换失败返回null
     * 
     * @example
     * Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM cigarette_distribution_info_2025_9_3 WHERE id = 1");
     * CigaretteDistributionInfoData data = mapToCigaretteDistributionInfoData(row);
     */
    public static CigaretteDistributionInfoData mapToCigaretteDistributionInfoData(Map<String, Object> row) {
        if (row == null || row.isEmpty()) {
            log.warn("查询结果行为空，无法进行映射");
            return null;
        }
        
        try {
            CigaretteDistributionInfoData data = new CigaretteDistributionInfoData();
            
            // 基本字段映射
            data.setId(extractInteger(row, "id"));
            data.setCigCode(extractString(row, "CIG_CODE"));
            data.setCigName(extractString(row, "CIG_NAME"));
            data.setYear(extractYear(row, "YEAR"));
            data.setMonth(extractInteger(row, "MONTH"));
            data.setWeekSeq(extractInteger(row, "WEEK_SEQ"));
            data.setDeliveryArea(extractString(row, "DELIVERY_AREA"));
            data.setDeliveryMethod(extractString(row, "DELIVERY_METHOD"));
            data.setDeliveryEtype(extractString(row, "DELIVERY_ETYPE"));
            
            // BigDecimal字段映射
            data.setAdv(extractBigDecimal(row, "ADV"));
            data.setUrs(extractBigDecimal(row, "URS"));
            
            log.debug("成功映射CigaretteDistributionInfoData: 卷烟{}-{}", 
                    data.getCigCode(), data.getCigName());
            return data;
            
        } catch (Exception e) {
            log.error("映射CigaretteDistributionInfoData失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 从查询结果行中提取30个档位的客户数
     * 用于区域客户数矩阵的构建
     * 
     * @param row 数据库查询结果行
     * @return 30个档位的客户数数组（按D30到D1的顺序）
     * 
     * @example
     * Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM region_clientNum_1_1 WHERE region = '丹江'");
     * BigDecimal[] customerCounts = extractCustomerCounts(row);
     * // customerCounts[0] = D30的值, customerCounts[29] = D1的值
     */
    public static BigDecimal[] extractCustomerCounts(Map<String, Object> row) {
        BigDecimal[] customerCounts = new BigDecimal[30];
        
        if (row == null) {
            // 如果行为空，返回全零数组
            for (int i = 0; i < 30; i++) {
                customerCounts[i] = BigDecimal.ZERO;
            }
            return customerCounts;
        }
        
        for (int i = 0; i < 30; i++) {
            String columnName = "D" + (30 - i); // D30对应数组索引0，D1对应数组索引29
            customerCounts[i] = extractBigDecimal(row, columnName);
        }
        
        return customerCounts;
    }
    
    // ==================== 参数构建 ====================
    
    /**
     * 构建插入预测数据的参数数组
     * 将CigaretteDistributionPredictionData对象转换为SQL参数数组
     * 
     * @param data CigaretteDistributionPredictionData对象
     * @return SQL参数数组，按INSERT语句字段顺序排列
     * 
     * @example
     * CigaretteDistributionPredictionData data = new CigaretteDistributionPredictionData();
     * data.setCigCode("42020181");
     * data.setCigName("黄鹤楼（1916中支）");
     * // ... 设置其他字段
     * Object[] params = buildInsertParams(data);
     * jdbcTemplate.update(insertSql, params);
     */
    public static Object[] buildInsertParams(CigaretteDistributionPredictionData data) {
        if (data == null) {
            log.warn("CigaretteDistributionPredictionData对象为null，无法构建参数数组");
            return new Object[0];
        }
        
        return new Object[]{
            // 基本信息字段
            data.getCigCode(),
            data.getCigName(),
            data.getYear(),
            data.getMonth(),
            data.getWeekSeq(),
            data.getDeliveryArea(),
            data.getDeliveryMethod(),
            data.getDeliveryEtype(),
            
            // D30到D1的30个档位字段
            data.getD30(), data.getD29(), data.getD28(), data.getD27(), data.getD26(),
            data.getD25(), data.getD24(), data.getD23(), data.getD22(), data.getD21(),
            data.getD20(), data.getD19(), data.getD18(), data.getD17(), data.getD16(),
            data.getD15(), data.getD14(), data.getD13(), data.getD12(), data.getD11(),
            data.getD10(), data.getD9(), data.getD8(), data.getD7(), data.getD6(),
            data.getD5(), data.getD4(), data.getD3(), data.getD2(), data.getD1(),
            
            // 其他字段
            data.getBz(),
            data.getActualDelivery(),
            data.getDeployinfoCode()
        };
    }
    
    // ==================== 数据类型转换辅助方法 ====================
    
    /**
     * 从结果集中安全提取字符串值
     * 处理null值和类型转换
     * 
     * @param row 结果集行
     * @param columnName 列名
     * @return 字符串值，如果为null则返回null
     */
    private static String extractString(Map<String, Object> row, String columnName) {
        Object value = getValueIgnoreCase(row, columnName);
        return value != null ? value.toString() : null;
    }
    
    /**
     * 从结果集中安全提取整数值
     * 处理null值和类型转换
     * 
     * @param row 结果集行
     * @param columnName 列名
     * @return Integer值，如果为null或转换失败则返回null
     */
    public static Integer extractInteger(Map<String, Object> row, String columnName) {
        Object value = getValueIgnoreCase(row, columnName);
        
        if (value == null) {
            return null;
        }
        
        try {
            if (value instanceof Integer) {
                return (Integer) value;
            } else if (value instanceof Number) {
                return ((Number) value).intValue();
            } else {
                return Integer.parseInt(value.toString());
            }
        } catch (NumberFormatException e) {
            log.warn("无法将值 '{}' 转换为Integer，列名: {}", value, columnName);
            return null;
        }
    }
    
    /**
     * 从结果集中安全提取年份值
     * 特殊处理年份字段的各种数据类型
     * 
     * @param row 结果集行
     * @param columnName 列名
     * @return Integer年份值
     */
    public static Integer extractYear(Map<String, Object> row, String columnName) {
        Object yearData = getValueIgnoreCase(row, columnName);
        
        if (yearData == null) {
            return null;
        }
        
        try {
            if (yearData instanceof Integer) {
                return (Integer) yearData;
            } else if (yearData instanceof java.sql.Date) {
                java.sql.Date date = (java.sql.Date) yearData;
                java.time.LocalDate localDate = date.toLocalDate();
                return localDate.getYear();
            } else if (yearData instanceof java.util.Date) {
                java.util.Date date = (java.util.Date) yearData;
                java.time.LocalDate localDate = new java.sql.Date(date.getTime()).toLocalDate();
                return localDate.getYear();
            } else {
                String yearStr = yearData.toString();
                if (yearStr.length() >= 4) {
                    return Integer.parseInt(yearStr.substring(0, 4));
                } else {
                    return Integer.parseInt(yearStr);
                }
            }
        } catch (Exception e) {
            log.warn("无法解析年份数据: {}, 列名: {}", yearData, columnName);
            return null;
        }
    }
    
    /**
     * 从结果集中安全提取BigDecimal值
     * 处理null值和类型转换
     * 
     * @param row 结果集行
     * @param columnName 列名
     * @return BigDecimal值，如果为null或转换失败则返回ZERO
     */
    private static BigDecimal extractBigDecimal(Map<String, Object> row, String columnName) {
        Object value = getValueIgnoreCase(row, columnName);
        
        if (value == null) {
            return BigDecimal.ZERO;
        }
        
        try {
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            } else if (value instanceof Number) {
                return new BigDecimal(value.toString());
            } else {
                return new BigDecimal(value.toString());
            }
        } catch (NumberFormatException e) {
            log.warn("无法将值 '{}' 转换为BigDecimal，列名: {}，使用默认值0", value, columnName);
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * 忽略大小写获取Map中的值
     * 先尝试原始列名，再尝试大写、小写版本
     * 
     * @param row 结果集行
     * @param columnName 列名
     * @return 列值，如果不存在则返回null
     */
    private static Object getValueIgnoreCase(Map<String, Object> row, String columnName) {
        if (row == null || columnName == null) {
            return null;
        }
        
        // 优先尝试原始列名
        Object value = row.get(columnName);
        if (value != null) {
            return value;
        }
        
        // 尝试大写列名
        value = row.get(columnName.toUpperCase());
        if (value != null) {
            return value;
        }
        
        // 尝试小写列名
        value = row.get(columnName.toLowerCase());
        return value;
    }
    
    // ==================== 数据验证工具 ====================
    
    /**
     * 验证CigaretteDistributionPredictionData对象的必要字段
     * 确保插入操作的数据完整性
     * 
     * @param data 待验证的对象
     * @return true表示数据有效，false表示存在必要字段缺失
     */
    public static boolean validatePredictionData(CigaretteDistributionPredictionData data) {
        if (data == null) {
            log.warn("CigaretteDistributionPredictionData对象为null");
            return false;
        }
        
        // 检查必要字段
        if (data.getCigCode() == null || data.getCigCode().trim().isEmpty()) {
            log.warn("卷烟代码为空");
            return false;
        }
        
        if (data.getCigName() == null || data.getCigName().trim().isEmpty()) {
            log.warn("卷烟名称为空");
            return false;
        }
        
        if (data.getYear() == null || data.getMonth() == null || data.getWeekSeq() == null) {
            log.warn("时间字段不完整: 年份={}, 月份={}, 周序号={}", 
                    data.getYear(), data.getMonth(), data.getWeekSeq());
            return false;
        }
        
        return true;
    }
    
    /**
     * 统计对象中非null的档位字段数量
     * 用于数据质量评估
     * 
     * @param data CigaretteDistributionPredictionData对象
     * @return 非null档位字段的数量
     */
    public static int countNonNullGrades(CigaretteDistributionPredictionData data) {
        if (data == null) {
            return 0;
        }
        
        int count = 0;
        BigDecimal[] grades = {
            data.getD30(), data.getD29(), data.getD28(), data.getD27(), data.getD26(),
            data.getD25(), data.getD24(), data.getD23(), data.getD22(), data.getD21(),
            data.getD20(), data.getD19(), data.getD18(), data.getD17(), data.getD16(),
            data.getD15(), data.getD14(), data.getD13(), data.getD12(), data.getD11(),
            data.getD10(), data.getD9(), data.getD8(), data.getD7(), data.getD6(),
            data.getD5(), data.getD4(), data.getD3(), data.getD2(), data.getD1()
        };
        
        for (BigDecimal grade : grades) {
            if (grade != null) {
                count++;
            }
        }
        
        return count;
    }
}
