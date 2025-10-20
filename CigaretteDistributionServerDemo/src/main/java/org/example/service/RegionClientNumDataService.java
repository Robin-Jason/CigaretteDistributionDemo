package org.example.service;

import org.example.entity.RegionClientNumData;
import java.util.List;

/**
 * 区域客户数查询服务接口
 * 
 * 【核心功能】
 * 提供区域客户数的精确查询服务，支持动态表名策略
 * 
 * 【主要职责】
 * - 按区域查询客户数数据
 * - 支持所有投放类型的动态表查询
 * 
 * 【支持的投放类型】
 * - 按档位统一投放：region_clientNum_0_1/2
 * - 档位+区县：region_clientNum_1_1/2
 * - 档位+市场类型：region_clientNum_2_1/2
 * - 档位+城乡分类代码：region_clientNum_3_1/2
 * - 档位+业态：region_clientNum_4_1/2
 * 
 * 【技术特性】
 * - 使用EntityManager支持动态表名
 * - 自动映射到RegionClientNumData实体
 * - 为实际投放量计算提供数据支持
 * 
 * @author Robin
 * @version 4.0 - 精简版，专注核心查询功能
 * @since 2025-10-19
 */
public interface RegionClientNumDataService {
    
    /**
     * 按区域查询客户数数据
     * 
     * 根据表名和区域标识查询匹配的区域客户数数据。
     * 用于计算实际投放量时获取特定区域的30个档位客户数。
     * 
     * @param tableName 表名（必填，如：region_clientNum_1_1）
     * @param region 区域标识（必填，支持完整区域名称）
     * @return 匹配的区域客户数数据列表
     * 
     * @example
     * findByTableNameAndRegion("region_clientNum_1_1", "房县")
     * -> 查询房县在档位+区县表中的客户数数据
     * -> 返回包含region、D30-D1等字段的RegionClientNumData对象
     */
    List<RegionClientNumData> findByTableNameAndRegion(String tableName, String region);
}
