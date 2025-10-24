package org.example.service;

import org.example.dto.CigaretteImportRequestDto;
import org.example.dto.RegionClientNumImportRequestDto;

import java.util.Map;

/**
 * Excel导入服务接口
 * 
 * 【核心功能】
 * 负责各种Excel文件的解析、验证和数据导入处理，支持动态表名策略
 * 
 * 【主要职责】
 * - Excel文件解析：支持多种Excel格式的自动识别和解析
 * - 数据验证：对导入数据进行完整性和格式验证
 * - 动态表名生成：根据导入参数自动生成目标表名
 * - 批量数据处理：高效的批量插入和替换操作
 * 
 * 【支持的导入类型】
 * - 卷烟投放基础信息：cigarette_distribution_info_{year}_{month}_{weekSeq}表
 * - 区域客户数数据：region_clientNum_{主序号}_{子序号}表
 * 
 * 【导入策略】
 * - 数据清理：导入前自动清理目标表的现有数据
 * - 事务安全：确保导入操作的原子性，失败时自动回滚
 * - 性能优化：使用批量操作提高大文件导入效率
 * - 结果反馈：提供详细的导入统计和错误信息
 * 
 * @author System
 * @version 3.0 - 动态表名与批量导入优化版本
 * @since 2025-10-10
 */
public interface ExcelImportService {

    /**
     * 卷烟投放基础信息Excel导入
     * 
     * 解析Excel文件并导入到cigarette_distribution_info_{year}_{month}_{weekSeq}表中。
     * 使用TableNameGeneratorUtil动态生成表名，自动清理原有数据，支持完整的事务管理。
     * 
     * @param request 导入请求DTO，包含以下必填字段：
     *               - file: Excel文件（MultipartFile格式）
     *               - year: 年份（2020-2099）
     *               - month: 月份（1-12）
     *               - weekSeq: 周序号（1-5）
     * @return 导入结果Map，包含以下字段：
     *         - success: 导入操作是否成功
     *         - tableName: 生成的目标表名
     *         - insertedCount: 成功插入的记录数
     *         - totalRows: Excel文件总行数
     *         - skippedRows: 跳过的无效行数
     *         - message: 操作结果描述
     * 
     * @example
     * request包含2025年9月第3周的Excel文件
     * -> 生成表名 cigarette_distribution_info_2025_9_3
     * -> 清理表数据，解析Excel，批量插入
     * -> 返回详细的导入统计信息
     */
    Map<String, Object> importCigaretteDistributionInfo(CigaretteImportRequestDto request);

    /**
     * 区域客户数表Excel导入
     * 
     * 解析Excel文件并导入到region_clientNum_{主序号}_{子序号}表中。
     * 使用TableNameGeneratorUtil根据投放类型参数动态生成表名，支持自动建表和数据替换。
     * 
     * @param request 导入请求DTO，包含以下必填字段：
     *               - file: Excel文件（MultipartFile格式）
     *               - deliveryMethod: 投放方法（按档位统一投放/按档位扩展投放）
     *               - deliveryEtype: 扩展投放类型（档位+区县/档位+市场类型等）
     *               - isBiWeeklyFloat: 是否双周上浮（布尔值，默认false）
     * @return 导入结果Map，包含以下字段：
     *         - success: 导入操作是否成功
     *         - tableName: 生成的目标表名
     *         - insertedCount: 成功插入的记录数
     *         - totalRows: Excel文件总行数
     *         - tableCreated: 是否新建了表
     *         - message: 操作结果描述
     * 
     * @example
     * request包含"按档位扩展投放"+"档位+区县"的Excel文件
     * -> 生成表名 region_clientNum_1_1
     * -> 检查表是否存在，不存在则创建，清理数据，批量插入
     * -> 返回详细的导入统计信息
     */
    Map<String, Object> importRegionClientNumData(RegionClientNumImportRequestDto request);
}