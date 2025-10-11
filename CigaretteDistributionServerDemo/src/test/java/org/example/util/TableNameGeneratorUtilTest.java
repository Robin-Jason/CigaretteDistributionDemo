package org.example.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TableNameGeneratorUtil工具类测试
 * 
 * @author System
 * @version 1.0
 * @since 2025-10-10
 */
@DisplayName("表名生成工具类测试")
class TableNameGeneratorUtilTest {

    @Test
    @DisplayName("测试卷烟预测输出表名生成")
    void testGeneratePredictionTableName() {
        // 正常情况
        String tableName = TableNameGeneratorUtil.generatePredictionTableName(2025, 10, 1);
        assertEquals("cigarette_distribution_prediction_2025_10_1", tableName);
        
        // 边界值测试
        String tableName2020 = TableNameGeneratorUtil.generatePredictionTableName(2020, 1, 1);
        assertEquals("cigarette_distribution_prediction_2020_1_1", tableName2020);
        
        String tableName2099 = TableNameGeneratorUtil.generatePredictionTableName(2099, 12, 5);
        assertEquals("cigarette_distribution_prediction_2099_12_5", tableName2099);
        
        // 参数验证测试
        assertThrows(IllegalArgumentException.class, () -> 
            TableNameGeneratorUtil.generatePredictionTableName(2019, 10, 1), "年份小于2020应该抛出异常");
        
        assertThrows(IllegalArgumentException.class, () -> 
            TableNameGeneratorUtil.generatePredictionTableName(2100, 10, 1), "年份大于2099应该抛出异常");
        
        assertThrows(IllegalArgumentException.class, () -> 
            TableNameGeneratorUtil.generatePredictionTableName(2025, 0, 1), "月份小于1应该抛出异常");
        
        assertThrows(IllegalArgumentException.class, () -> 
            TableNameGeneratorUtil.generatePredictionTableName(2025, 13, 1), "月份大于12应该抛出异常");
        
        assertThrows(IllegalArgumentException.class, () -> 
            TableNameGeneratorUtil.generatePredictionTableName(2025, 10, 0), "周序号小于1应该抛出异常");
        
        assertThrows(IllegalArgumentException.class, () -> 
            TableNameGeneratorUtil.generatePredictionTableName(2025, 10, 6), "周序号大于5应该抛出异常");
        
        assertThrows(IllegalArgumentException.class, () -> 
            TableNameGeneratorUtil.generatePredictionTableName(null, 10, 1), "年份为null应该抛出异常");
    }

    @Test
    @DisplayName("测试卷烟投放基本信息表名生成")
    void testGenerateDistributionInfoTableName() {
        // 正常情况
        String tableName = TableNameGeneratorUtil.generateDistributionInfoTableName(2025, 9, 2);
        assertEquals("cigarette_distribution_info_2025_9_2", tableName);
        
        // 边界值测试
        String tableName2020 = TableNameGeneratorUtil.generateDistributionInfoTableName(2020, 1, 1);
        assertEquals("cigarette_distribution_info_2020_1_1", tableName2020);
        
        String tableName2099 = TableNameGeneratorUtil.generateDistributionInfoTableName(2099, 12, 5);
        assertEquals("cigarette_distribution_info_2099_12_5", tableName2099);
        
        // 参数验证测试
        assertThrows(IllegalArgumentException.class, () -> 
            TableNameGeneratorUtil.generateDistributionInfoTableName(2019, 10, 1), "年份小于2020应该抛出异常");
        
        assertThrows(IllegalArgumentException.class, () -> 
            TableNameGeneratorUtil.generateDistributionInfoTableName(2025, 0, 1), "月份小于1应该抛出异常");
        
        assertThrows(IllegalArgumentException.class, () -> 
            TableNameGeneratorUtil.generateDistributionInfoTableName(2025, 10, 6), "周序号大于5应该抛出异常");
    }

    @Test
    @DisplayName("测试区域客户数表名生成")
    void testGenerateRegionClientTableName() {
        // 按档位统一投放
        String unifiedTable = TableNameGeneratorUtil.generateRegionClientTableName(
                "按档位统一投放", null, false);
        assertEquals("region_clientNum_0_1", unifiedTable);
        
        String unifiedTableBiWeekly = TableNameGeneratorUtil.generateRegionClientTableName(
                "按档位统一投放", null, true);
        assertEquals("region_clientNum_0_2", unifiedTableBiWeekly);
        
        // 档位+区县
        String countyTable = TableNameGeneratorUtil.generateRegionClientTableName(
                "按档位扩展投放", "档位+区县", false);
        assertEquals("region_clientNum_1_1", countyTable);
        
        String countyTableBiWeekly = TableNameGeneratorUtil.generateRegionClientTableName(
                "按档位扩展投放", "档位+区县", true);
        assertEquals("region_clientNum_1_2", countyTableBiWeekly);
        
        // 档位+市场类型
        String marketTable = TableNameGeneratorUtil.generateRegionClientTableName(
                "按档位扩展投放", "档位+市场类型", false);
        assertEquals("region_clientNum_2_1", marketTable);
        
        String marketTableBiWeekly = TableNameGeneratorUtil.generateRegionClientTableName(
                "按档位扩展投放", "档位+市场类型", true);
        assertEquals("region_clientNum_2_2", marketTableBiWeekly);
        
        // 档位+城乡分类代码
        String urbanRuralTable = TableNameGeneratorUtil.generateRegionClientTableName(
                "按档位扩展投放", "档位+城乡分类代码", false);
        assertEquals("region_clientNum_3_1", urbanRuralTable);
        
        String urbanRuralTableBiWeekly = TableNameGeneratorUtil.generateRegionClientTableName(
                "按档位扩展投放", "档位+城乡分类代码", true);
        assertEquals("region_clientNum_3_2", urbanRuralTableBiWeekly);
        
        // 档位+业态
        String businessTable = TableNameGeneratorUtil.generateRegionClientTableName(
                "按档位扩展投放", "档位+业态", false);
        assertEquals("region_clientNum_4_1", businessTable);
        
        String businessTableBiWeekly = TableNameGeneratorUtil.generateRegionClientTableName(
                "按档位扩展投放", "档位+业态", true);
        assertEquals("region_clientNum_4_2", businessTableBiWeekly);
        
        // null值处理
        String nullBiWeeklyTable = TableNameGeneratorUtil.generateRegionClientTableName(
                "按档位扩展投放", "档位+区县", null);
        assertEquals("region_clientNum_1_1", nullBiWeeklyTable);
        
        // 参数验证测试
        assertThrows(IllegalArgumentException.class, () -> 
            TableNameGeneratorUtil.generateRegionClientTableName(null, "档位+区县", false), 
            "投放方法为null应该抛出异常");
        
        assertThrows(IllegalArgumentException.class, () -> 
            TableNameGeneratorUtil.generateRegionClientTableName("", "档位+区县", false), 
            "投放方法为空字符串应该抛出异常");
        
        assertThrows(IllegalArgumentException.class, () -> 
            TableNameGeneratorUtil.generateRegionClientTableName("按档位扩展投放", null, false), 
            "扩展投放类型为null应该抛出异常");
        
        assertThrows(IllegalArgumentException.class, () -> 
            TableNameGeneratorUtil.generateRegionClientTableName("按档位扩展投放", "", false), 
            "扩展投放类型为空字符串应该抛出异常");
        
        assertThrows(IllegalArgumentException.class, () -> 
            TableNameGeneratorUtil.generateRegionClientTableName("无效投放方法", "档位+区县", false), 
            "无效投放方法应该抛出异常");
        
        assertThrows(IllegalArgumentException.class, () -> 
            TableNameGeneratorUtil.generateRegionClientTableName("按档位扩展投放", "无效扩展类型", false), 
            "无效扩展投放类型应该抛出异常");
    }

    @Test
    @DisplayName("测试投放类型描述获取")
    void testGetDeliveryTypeDescription() {
        // 按档位统一投放
        String desc1 = TableNameGeneratorUtil.getDeliveryTypeDescription("按档位统一投放", null);
        assertEquals("按档位统一投放", desc1);
        
        // 按档位扩展投放
        String desc2 = TableNameGeneratorUtil.getDeliveryTypeDescription("按档位扩展投放", "档位+区县");
        assertEquals("档位+区县", desc2);
        
        String desc3 = TableNameGeneratorUtil.getDeliveryTypeDescription("按档位扩展投放", "档位+市场类型");
        assertEquals("档位+市场类型", desc3);
        
        // 未知类型
        String desc4 = TableNameGeneratorUtil.getDeliveryTypeDescription("未知类型", "测试");
        assertEquals("未知投放类型", desc4);
    }

    @Test
    @DisplayName("测试投放类型组合有效性验证")
    void testIsValidDeliveryTypeCombination() {
        // 有效组合
        assertTrue(TableNameGeneratorUtil.isValidDeliveryTypeCombination("按档位统一投放", null));
        assertTrue(TableNameGeneratorUtil.isValidDeliveryTypeCombination("按档位扩展投放", "档位+区县"));
        assertTrue(TableNameGeneratorUtil.isValidDeliveryTypeCombination("按档位扩展投放", "档位+市场类型"));
        assertTrue(TableNameGeneratorUtil.isValidDeliveryTypeCombination("按档位扩展投放", "档位+城乡分类代码"));
        assertTrue(TableNameGeneratorUtil.isValidDeliveryTypeCombination("按档位扩展投放", "档位+业态"));
        
        // 无效组合
        assertFalse(TableNameGeneratorUtil.isValidDeliveryTypeCombination(null, "档位+区县"));
        assertFalse(TableNameGeneratorUtil.isValidDeliveryTypeCombination("", "档位+区县"));
        assertFalse(TableNameGeneratorUtil.isValidDeliveryTypeCombination("按档位扩展投放", null));
        assertFalse(TableNameGeneratorUtil.isValidDeliveryTypeCombination("按档位扩展投放", ""));
        assertFalse(TableNameGeneratorUtil.isValidDeliveryTypeCombination("无效方法", "档位+区县"));
        assertFalse(TableNameGeneratorUtil.isValidDeliveryTypeCombination("按档位扩展投放", "无效扩展类型"));
    }

    @Test
    @DisplayName("测试区域客户数表名解析")
    void testParseRegionClientTableName() {
        // 正常格式
        int[] result1 = TableNameGeneratorUtil.parseRegionClientTableName("region_clientNum_1_2");
        assertArrayEquals(new int[]{1, 2}, result1);
        
        int[] result2 = TableNameGeneratorUtil.parseRegionClientTableName("region_clientNum_0_1");
        assertArrayEquals(new int[]{0, 1}, result2);
        
        int[] result3 = TableNameGeneratorUtil.parseRegionClientTableName("region_clientNum_4_2");
        assertArrayEquals(new int[]{4, 2}, result3);
        
        // 兼容旧格式（单序号）
        int[] result4 = TableNameGeneratorUtil.parseRegionClientTableName("region_clientNum_1");
        assertArrayEquals(new int[]{1, 1}, result4);
        
        // 无效格式
        assertNull(TableNameGeneratorUtil.parseRegionClientTableName(null));
        assertNull(TableNameGeneratorUtil.parseRegionClientTableName(""));
        assertNull(TableNameGeneratorUtil.parseRegionClientTableName("invalid_table_name"));
        assertNull(TableNameGeneratorUtil.parseRegionClientTableName("region_clientNum"));
        assertNull(TableNameGeneratorUtil.parseRegionClientTableName("region_clientNum_abc_def"));
    }

    @Test
    @DisplayName("测试与RegionClientNumImportRequestDto的一致性")
    void testConsistencyWithDto() {
        // 创建DTO并测试表名生成的一致性
        org.example.dto.RegionClientNumImportRequestDto dto = 
                new org.example.dto.RegionClientNumImportRequestDto();
        
        // 档位+区县，非双周上浮
        dto.setDeliveryMethod("按档位扩展投放");
        dto.setDeliveryEtype("档位+区县");
        dto.setIsBiWeeklyFloat(false);
        
        String dtoTableName = dto.getTableName();
        String utilTableName = TableNameGeneratorUtil.generateRegionClientTableName(
                "按档位扩展投放", "档位+区县", false);
        
        assertEquals(utilTableName, dtoTableName, "DTO和工具类生成的表名应该一致");
        assertEquals("region_clientNum_1_1", dtoTableName);
        
        // 档位+区县，双周上浮
        dto.setIsBiWeeklyFloat(true);
        
        String dtoTableName2 = dto.getTableName();
        String utilTableName2 = TableNameGeneratorUtil.generateRegionClientTableName(
                "按档位扩展投放", "档位+区县", true);
        
        assertEquals(utilTableName2, dtoTableName2, "DTO和工具类生成的表名应该一致");
        assertEquals("region_clientNum_1_2", dtoTableName2);
        
        // 按档位统一投放
        dto.setDeliveryMethod("按档位统一投放");
        dto.setDeliveryEtype(null);
        dto.setIsBiWeeklyFloat(false);
        
        String dtoTableName3 = dto.getTableName();
        String utilTableName3 = TableNameGeneratorUtil.generateRegionClientTableName(
                "按档位统一投放", null, false);
        
        assertEquals(utilTableName3, dtoTableName3, "DTO和工具类生成的表名应该一致");
        assertEquals("region_clientNum_0_1", dtoTableName3);
    }
}
