package org.example.util;

import lombok.extern.slf4j.Slf4j;

/**
 * 表名生成工具类
 * 
 * 根据导入表命名规则文档设计，提供三个核心表名生成功能：
 * 1. 卷烟预测输出表名生成
 * 2. 卷烟投放基本信息表名生成  
 * 3. 区域客户数表名生成
 * 
 * @author System
 * @version 1.0
 * @since 2025-10-10
 */
@Slf4j
public class TableNameGeneratorUtil {

    // 表名前缀常量
    private static final String PREDICTION_TABLE_PREFIX = "cigarette_distribution_prediction";
    private static final String DISTRIBUTION_INFO_TABLE_PREFIX = "cigarette_distribution_info";
    private static final String REGION_CLIENT_TABLE_PREFIX = "region_clientNum";

    // 投放类型序号映射常量
    private static final String UNIFIED_DELIVERY = "按档位统一投放";
    private static final String EXTENDED_DELIVERY = "按档位扩展投放";
    private static final String COUNTY_TYPE = "档位+区县";
    private static final String MARKET_TYPE = "档位+市场类型";
    private static final String URBAN_RURAL_TYPE = "档位+城乡分类代码";
    private static final String BUSINESS_FORMAT_TYPE = "档位+业态";

    /**
     * 生成卷烟预测输出表名
     * 
     * 表名格式：cigarette_distribution_prediction_{year}_{month}_{weekSeq}
     * 
     * @param year    年份 (2020-2099)
     * @param month   月份 (1-12)
     * @param weekSeq 周序号 (1-5)
     * @return 生成的表名
     * @throws IllegalArgumentException 如果参数不在有效范围内
     * 
     * @example
     * generatePredictionTableName(2025, 10, 1) -> "cigarette_distribution_prediction_2025_10_1"
     */
    public static String generatePredictionTableName(Integer year, Integer month, Integer weekSeq) {
        validateTimeParameters(year, month, weekSeq);
        
        String tableName = String.format("%s_%d_%d_%d", 
                PREDICTION_TABLE_PREFIX, year, month, weekSeq);
        
        log.debug("Generated prediction table name: {}", tableName);
        return tableName;
    }

    /**
     * 生成卷烟投放基本信息表名
     * 
     * 表名格式：cigarette_distribution_info_{year}_{month}_{weekSeq}
     * 
     * @param year    年份 (2020-2099)
     * @param month   月份 (1-12)
     * @param weekSeq 周序号 (1-5)
     * @return 生成的表名
     * @throws IllegalArgumentException 如果参数不在有效范围内
     * 
     * @example
     * generateDistributionInfoTableName(2025, 10, 1) -> "cigarette_distribution_info_2025_10_1"
     */
    public static String generateDistributionInfoTableName(Integer year, Integer month, Integer weekSeq) {
        validateTimeParameters(year, month, weekSeq);
        
        String tableName = String.format("%s_%d_%d_%d", 
                DISTRIBUTION_INFO_TABLE_PREFIX, year, month, weekSeq);
        
        log.debug("Generated distribution info table name: {}", tableName);
        return tableName;
    }

    /**
     * 生成区域客户数表名
     * 
     * 表名格式：region_clientNum_{主序号}_{子序号}
     * 
     * @param deliveryMethod    投放方法
     * @param deliveryEtype     扩展投放类型
     * @param isBiWeeklyFloat   是否双周上浮 (true: 子序号为2, false: 子序号为1)
     * @return 生成的表名
     * @throws IllegalArgumentException 如果投放类型组合无效
     * 
     * @example
     * generateRegionClientTableName("按档位扩展投放", "档位+区县", false) -> "region_clientNum_1_1"
     * generateRegionClientTableName("按档位扩展投放", "档位+区县", true) -> "region_clientNum_1_2"
     */
    public static String generateRegionClientTableName(String deliveryMethod, String deliveryEtype, Boolean isBiWeeklyFloat) {
        validateDeliveryParameters(deliveryMethod, deliveryEtype);
        
        Integer mainSequence = getMainSequenceNumber(deliveryMethod, deliveryEtype);
        Integer subSequence = getSubSequenceNumber(isBiWeeklyFloat);
        
        String tableName = String.format("%s_%d_%d", 
                REGION_CLIENT_TABLE_PREFIX, mainSequence, subSequence);
        
        log.debug("Generated region client table name: {} (deliveryMethod: {}, deliveryEtype: {}, isBiWeeklyFloat: {})", 
                tableName, deliveryMethod, deliveryEtype, isBiWeeklyFloat);
        return tableName;
    }

    /**
     * 根据投放类型组合获取主序号
     * 
     * 序号映射规则：
     * - 按档位统一投放: 0
     * - 档位+区县: 1  
     * - 档位+市场类型: 2
     * - 档位+城乡分类代码: 3
     * - 档位+业态: 4
     * 
     * @param deliveryMethod 投放方法
     * @param deliveryEtype  扩展投放类型
     * @return 主序号 (0-4)
     */
    private static Integer getMainSequenceNumber(String deliveryMethod, String deliveryEtype) {
        if (UNIFIED_DELIVERY.equals(deliveryMethod)) {
            return 0;
        } else if (EXTENDED_DELIVERY.equals(deliveryMethod)) {
            switch (deliveryEtype) {
                case COUNTY_TYPE:
                    return 1;
                case MARKET_TYPE:
                    return 2;
                case URBAN_RURAL_TYPE:
                    return 3;
                case BUSINESS_FORMAT_TYPE:
                    return 4;
                default:
                    throw new IllegalArgumentException("无效的扩展投放类型: " + deliveryEtype);
            }
        } else {
            throw new IllegalArgumentException("无效的投放方法: " + deliveryMethod);
        }
    }

    /**
     * 根据双周上浮标识获取子序号
     * 
     * 所有投放类型均支持双周上浮区分：
     * - false或null: 1 (非双周上浮)
     * - true: 2 (双周上浮)
     * 
     * @param isBiWeeklyFloat 是否双周上浮
     * @return 子序号 (1或2)
     */
    private static Integer getSubSequenceNumber(Boolean isBiWeeklyFloat) {
        return (isBiWeeklyFloat != null && isBiWeeklyFloat) ? 2 : 1;
    }

    /**
     * 验证时间参数的有效性
     * 
     * @param year    年份 (2020-2099)
     * @param month   月份 (1-12)
     * @param weekSeq 周序号 (1-5)
     * @throws IllegalArgumentException 如果参数不在有效范围内
     */
    private static void validateTimeParameters(Integer year, Integer month, Integer weekSeq) {
        if (year == null || year < 2020 || year > 2099) {
            throw new IllegalArgumentException("年份必须在2020-2099范围内，当前值: " + year);
        }
        if (month == null || month < 1 || month > 12) {
            throw new IllegalArgumentException("月份必须在1-12范围内，当前值: " + month);
        }
        if (weekSeq == null || weekSeq < 1 || weekSeq > 5) {
            throw new IllegalArgumentException("周序号必须在1-5范围内，当前值: " + weekSeq);
        }
    }

    /**
     * 验证投放类型参数的有效性
     * 
     * @param deliveryMethod 投放方法
     * @param deliveryEtype  扩展投放类型
     * @throws IllegalArgumentException 如果参数为空或无效
     */
    private static void validateDeliveryParameters(String deliveryMethod, String deliveryEtype) {
        if (deliveryMethod == null || deliveryMethod.trim().isEmpty()) {
            throw new IllegalArgumentException("投放方法不能为空");
        }
        
        if (EXTENDED_DELIVERY.equals(deliveryMethod)) {
            if (deliveryEtype == null || deliveryEtype.trim().isEmpty()) {
                throw new IllegalArgumentException("扩展投放类型不能为空");
            }
        }
    }

    /**
     * 获取投放类型的描述信息
     * 
     * @param deliveryMethod 投放方法
     * @param deliveryEtype  扩展投放类型
     * @return 投放类型描述
     */
    public static String getDeliveryTypeDescription(String deliveryMethod, String deliveryEtype) {
        if (UNIFIED_DELIVERY.equals(deliveryMethod)) {
            return "按档位统一投放";
        } else if (EXTENDED_DELIVERY.equals(deliveryMethod)) {
            return deliveryEtype;
        } else {
            return "未知投放类型";
        }
    }

    /**
     * 判断指定的投放类型组合是否有效
     * 
     * @param deliveryMethod 投放方法
     * @param deliveryEtype  扩展投放类型
     * @return true如果组合有效，否则false
     */
    public static boolean isValidDeliveryTypeCombination(String deliveryMethod, String deliveryEtype) {
        try {
            validateDeliveryParameters(deliveryMethod, deliveryEtype);
            getMainSequenceNumber(deliveryMethod, deliveryEtype);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 解析区域客户数表名，提取序号信息
     * 
     * @param tableName 表名 (格式: region_clientNum_{主序号}_{子序号})
     * @return 包含主序号和子序号的数组 [主序号, 子序号]，如果格式不正确返回null
     * 
     * @example
     * parseRegionClientTableName("region_clientNum_1_2") -> [1, 2]
     */
    public static int[] parseRegionClientTableName(String tableName) {
        if (tableName == null || !tableName.startsWith(REGION_CLIENT_TABLE_PREFIX)) {
            return null;
        }
        
        String[] parts = tableName.split("_");
        if (parts.length < 3) {
            return null;
        }
        
        try {
            int mainSeq = Integer.parseInt(parts[2]);
            int subSeq = parts.length > 3 ? Integer.parseInt(parts[3]) : 1; // 兼容旧格式
            return new int[]{mainSeq, subSeq};
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
