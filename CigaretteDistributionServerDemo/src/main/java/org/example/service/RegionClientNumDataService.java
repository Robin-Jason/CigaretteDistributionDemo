package org.example.service;

import org.example.entity.RegionClientNumData;
import java.util.List;
import java.util.Map;

/**
 * 区域客户数统一数据服务接口
 * 
 * 【核心功能】
 * 提供全面的区域客户数数据操作服务，支持动态表名策略和多投放类型管理
 * 
 * 【主要职责】
 * - 动态表管理：支持基于投放类型的动态表名生成和管理
 * - 数据操作：提供完整的CRUD操作，包括查询、插入、更新、删除
 * - 表结构管理：支持自动建表、清理数据、重置自增ID等维护操作
 * - 序号映射：提供投放类型到表名序号的映射服务
 * 
 * 【支持的投放类型】
 * - 按档位统一投放：region_clientNum_0_1（全市统一）
 * - 档位+区县：region_clientNum_1_1（区县级投放）
 * - 档位+市场类型：region_clientNum_2_1（城网/农网分类）
 * - 档位+城乡分类代码：region_clientNum_3_1（城乡属性分类）
 * - 档位+业态：region_clientNum_4_1（业态类型分类）
 * 
 * 【表名生成规则】
 * - 基础格式：region_clientNum_{主序号}_{子序号}
 * - 主序号：根据投放方法和扩展类型确定（0-4）
 * - 子序号：根据是否双周上浮确定（1=非双周上浮，2=双周上浮）
 * 
 * 【技术特性】
 * - 使用TableNameGeneratorUtil统一管理表名生成逻辑
 * - 支持JdbcTemplate动态SQL操作
 * - 提供完整的事务管理和异常处理
 * - 优化的批量操作性能
 * 
 * @author System
 * @version 3.0 - 动态表名与多投放类型支持版本
 * @since 2025-10-10
 */
public interface RegionClientNumDataService {
    
    // ==================== 数据查询方法 ====================
    
    /**
     * 全表数据查询
     * 
     * 根据指定表名查询该表的所有区域客户数数据。
     * 支持动态表名，返回完整的区域和档位客户数信息。
     * 
     * @param tableName 表名（必填，如：region_clientNum_1_1）
     * @return 区域客户数数据列表，包含所有区域的完整档位客户数信息
     * 
     * @example
     * findAllByTableName("region_clientNum_1_1")
     * -> 查询档位+区县投放类型的所有区域数据
     * -> 返回包含region、D30-D1等字段的完整数据列表
     */
    List<RegionClientNumData> findAllByTableName(String tableName);
    
    /**
     * 区域条件查询
     * 
     * 根据表名和区域标识查询匹配的区域客户数数据。
     * 支持模糊匹配和精确匹配，用于特定区域的数据获取。
     * 
     * @param tableName 表名（必填）
     * @param region 区域标识（必填，支持完整区域名称）
     * @return 匹配的区域客户数数据列表
     * 
     * @example
     * findByTableNameAndRegion("region_clientNum_1_1", "房县")
     * -> 查询房县在档位+区县表中的客户数数据
     */
    List<RegionClientNumData> findByTableNameAndRegion(String tableName, String region);
    
    // ==================== 表管理方法 ====================
    
    /**
     * 表存在性检查
     * 
     * 检查指定的区域客户数表是否在数据库中存在。
     * 用于导入前的表结构验证和自动建表逻辑判断。
     * 
     * @param tableName 表名（必填）
     * @return true表示表存在，false表示表不存在
     * 
     * @example
     * tableExists("region_clientNum_1_1") -> true/false
     */
    boolean tableExists(String tableName);
    
    /**
     * 动态建表
     * 
     * 根据标准结构创建新的区域客户数表。
     * 自动创建包含region、D30-D1等字段的完整表结构。
     * 
     * @param tableName 表名（必填）
     * 
     * @example
     * createTable("region_clientNum_1_1")
     * -> 创建包含region和30个档位字段的表结构
     */
    void createTable(String tableName);
    
    /**
     * 表数据清理
     * 
     * 清空指定表的所有数据，保留表结构。
     * 通常在数据导入前使用，确保数据的一致性。
     * 
     * @param tableName 表名（必填）
     * 
     * @example
     * clearTableData("region_clientNum_1_1") -> 清空该表所有记录
     */
    void clearTableData(String tableName);
    
    /**
     * 重置自增ID
     * 
     * 重置表的自增ID计数器为1，通常在清理数据后使用。
     * 确保新插入数据的ID从1开始连续分配。
     * 
     * @param tableName 表名（必填）
     * 
     * @example
     * resetAutoIncrement("region_clientNum_1_1") -> 重置ID计数器
     */
    void resetAutoIncrement(String tableName);
    
    // ==================== 数据操作方法 ====================
    
    /**
     * 批量插入或替换数据
     * 
     * 将数据列表批量插入到指定表中，支持INSERT OR REPLACE策略。
     * 优化的批量操作性能，适用于大量数据的导入场景。
     * 
     * @param tableName 表名（必填）
     * @param dataList 数据列表（必填，Map格式，key为字段名，value为字段值）
     * @return 成功插入的记录数
     * 
     * @example
     * dataList包含多个区域的客户数数据
     * -> batchInsertOrReplace("region_clientNum_1_1", dataList)
     * -> 返回实际插入的记录数
     */
    int batchInsertOrReplace(String tableName, List<Map<String, Object>> dataList);
    
    // ==================== 工具方法 ====================
    
    /**
     * 投放类型表名生成
     * 
     * 根据投放类型参数生成对应的区域客户数表名。
     * 使用TableNameGeneratorUtil统一的表名生成逻辑。
     * 
     * @param deliveryMethod 投放方法（必填，如：按档位统一投放、按档位扩展投放）
     * @param deliveryEtype 扩展投放类型（必填，如：档位+区县、档位+市场类型等）
     * @param isBiWeeklyFloat 是否双周上浮（必填，true/false）
     * @return 生成的表名
     * 
     * @example
     * generateTableName("按档位扩展投放", "档位+区县", false)
     * -> 返回 "region_clientNum_1_1"
     */
    String generateTableName(String deliveryMethod, String deliveryEtype, Boolean isBiWeeklyFloat);
    
    /**
     * 主序号映射查询
     * 
     * 根据投放方法和扩展投放类型获取对应的主序号。
     * 用于表名生成和投放类型识别。
     * 
     * @param deliveryMethod 投放方法（必填）
     * @param deliveryEtype 扩展投放类型（必填）
     * @return 主序号（0-4）
     * 
     * @example
     * getSequenceNumber("按档位扩展投放", "档位+区县") -> 返回 1
     */
    Integer getSequenceNumber(String deliveryMethod, String deliveryEtype);
    
    /**
     * 子序号映射查询
     * 
     * 根据是否双周上浮参数获取对应的子序号。
     * 用于区分双周上浮和非双周上浮的数据表。
     * 
     * @param isBiWeeklyFloat 是否双周上浮（必填）
     * @return 子序号（1=非双周上浮，2=双周上浮）
     * 
     * @example
     * getSubSequenceNumber(false) -> 返回 1
     * getSubSequenceNumber(true) -> 返回 2
     */
    Integer getSubSequenceNumber(Boolean isBiWeeklyFloat);
}
