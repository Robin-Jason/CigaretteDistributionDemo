package org.example.service;

import org.example.dto.DeleteAreasRequestDto;
import org.example.dto.UpdateCigaretteRequestDto;
import org.example.entity.CigaretteDistributionPredictionData;

import java.util.List;
import java.util.Map;

/**
 * 数据管理服务接口
 * 
 * 【核心功能】
 * 负责卷烟分配数据的综合管理，包括数据更新、删除、查询等关键操作，支持动态表名策略
 * 
 * 【主要职责】
 * - 卷烟信息更新：支持编码表达式批量更新和智能策略选择（删除重建vs增量更新）
 * - 投放区域管理：获取和删除指定卷烟的投放区域记录
 * - 数据查询服务：时间维度的数据查询和预投放量信息获取
 * - 数据清理功能：支持按时间维度的数据删除操作
 * 
 * 【动态表名支持】
 * - 卷烟投放预测数据表：cigarette_distribution_prediction_{year}_{month}_{weekSeq}
 * - 卷烟投放基本信息表：cigarette_distribution_info_{year}_{month}_{weekSeq}
 * - 自动根据时间参数生成对应表名，实现时间维度数据隔离
 * 
 * 【更新策略】
 * - 投放类型变更：采用删除重建策略确保数据一致性
 * - 投放类型不变：采用增量更新策略提高性能
 * - 编码表达式更新：支持语义化编码的批量解析和应用
 * 
 * @author Robin
 * @version 3.0 - 动态表名与智能更新策略版本
 * @since 2025-10-10
 */
public interface DataManagementService {
    
    /**
     * 编码表达式批量更新
     * 
     * 解析多条语义编码表达式，批量更新指定卷烟的投放分配信息。
     * 使用TableNameGeneratorUtil动态生成表名，支持完整的事务管理。
     * 
     * @param cigCode 卷烟代码（必填）
     * @param cigName 卷烟名称（必填）
     * @param year 年份（必填，2020-2099）
     * @param month 月份（必填，1-12）
     * @param weekSeq 周序号（必填，1-5）
     * @param encodedExpressions 编码表达式列表（必填，语义化编码格式）
     * @param remark 备注信息（可选）
     * @return 操作结果Map，包含以下字段：
     *         - success: 批量更新是否完全成功
     *         - totalExpressions: 总表达式数量
     *         - successCount: 成功处理数量
     *         - message: 操作结果描述
     * 
     * @example
     * encodedExpressions: ["按档位扩展投放-档位+区县-房县,郧西-D30:5.0,D29:3.0", ...]
     * -> 自动解析每个表达式的投放类型、区域和档位分配
     * -> 写入到 cigarette_distribution_prediction_2025_9_3 表
     */
    Map<String, Object> batchUpdateFromEncodedExpressions(String cigCode, String cigName, 
                                                                Integer year, Integer month, Integer weekSeq,
                                                          List<String> encodedExpressions, String remark);
    
    /**
     * 智能卷烟信息更新
     * 
     * 根据投放类型是否变更自动选择最优更新策略：
     * - 投放类型变更：删除重建策略，确保数据一致性
     * - 投放类型不变：增量更新策略，提高更新性能
     * 使用TableNameGeneratorUtil动态生成表名，支持完整的事务管理。
     * 
     * @param request 更新卷烟请求DTO，包含以下必填字段：
     *               - cigCode: 卷烟代码
     *               - cigName: 卷烟名称
     *               - year, month, weekSeq: 时间参数
     *               - deliveryMethod: 投放方法
     *               - deliveryEtype: 扩展投放类型
     *               - gradeAllocations: 档位分配数组（30个档位）
     * @return 操作结果Map，包含以下字段：
     *         - success: 更新操作是否成功
     *         - strategy: 采用的更新策略（DELETE_REBUILD/INCREMENTAL_UPDATE）
     *         - affectedRecords: 影响的记录数
     *         - message: 操作结果描述
     * 
     * @example
     * 投放类型从"档位+区县"变更为"档位+市场类型" -> 自动采用删除重建策略
     * 仅修改档位分配而投放类型不变 -> 自动采用增量更新策略
     */
    Map<String, Object> updateCigaretteInfo(UpdateCigaretteRequestDto request);
    
    /**
     * 投放区域查询
     * 
     * 根据时间参数从cigarette_distribution_prediction_{year}_{month}_{weekSeq}表中查询指定卷烟的所有投放区域。
     * 使用TableNameGeneratorUtil动态生成表名，返回去重排序后的区域列表。
     * 
     * @param cigCode 卷烟代码（必填）
     * @param cigName 卷烟名称（必填）
     * @param year 年份（必填，2020-2099）
     * @param month 月份（必填，1-12）
     * @param weekSeq 周序号（必填，1-5）
     * @return 投放区域名称列表，按字母顺序排序，去除重复项
     * 
     * @example
     * getCurrentDeliveryAreas("42020181", "黄鹤楼（1916中支）", 2025, 9, 3)
     * -> 查询 cigarette_distribution_prediction_2025_9_3 表
     * -> 返回 ["全市", "房县", "郧西", "竹山"] (已排序去重)
     */
    List<String> getCurrentDeliveryAreas(String cigCode, String cigName, 
                                        Integer year, Integer month, Integer weekSeq);

    /**
     * 精确删除投放区域记录
     * 
     * 根据时间参数从cigarette_distribution_prediction_{year}_{month}_{weekSeq}表中精确删除指定卷烟的特定投放区域记录。
     * 使用TableNameGeneratorUtil动态生成表名，支持批量区域删除和事务安全。
     * 
     * @param request 删除区域请求DTO，包含以下必填字段：
     *               - cigCode: 卷烟代码
     *               - cigName: 卷烟名称
     *               - year, month, weekSeq: 时间参数
     *               - deliveryAreas: 待删除的区域列表
     * @return 操作结果Map，包含以下字段：
     *         - success: 删除操作是否成功
     *         - deletedCount: 实际删除的记录数
     *         - requestedAreas: 请求删除的区域列表
     *         - message: 操作结果描述
     * 
     * @example
     * request包含区域["房县", "郧西"] -> 删除这两个区域的所有投放记录
     * -> DELETE FROM cigarette_distribution_prediction_2025_9_3 WHERE ... AND DELIVERY_AREA IN (...)
     */
    Map<String, Object> deleteDeliveryAreas(DeleteAreasRequestDto request);
    
    // ==================== 查询服务方法 ====================
    
    /**
     * 时间维度数据查询
     * 
     * 根据时间参数从cigarette_distribution_prediction_{year}_{month}_{weekSeq}表中查询指定时间周期的所有分配数据。
     * 使用TableNameGeneratorUtil动态生成表名，返回完整的分配预测数据列表。
     * 
     * @param year 年份（必填，2020-2099）
     * @param month 月份（必填，1-12）
     * @param weekSeq 周序号（必填，1-5）
     * @return 卷烟分配预测数据列表，包含该时间周期的所有卷烟投放记录
     * 
     * @example
     * queryTestDataByTime(2025, 9, 3)
     * -> 查询 cigarette_distribution_prediction_2025_9_3 表
     * -> 返回该周所有卷烟的完整分配数据（包含档位分配、实际投放量等）
     */
    List<CigaretteDistributionPredictionData> queryTestDataByTime(Integer year, Integer month, Integer weekSeq);
    
    /**
     * 预投放量信息查询
     * 
     * 根据时间参数从cigarette_distribution_info_{year}_{month}_{weekSeq}表中获取指定卷烟的基础投放信息。
     * 使用TableNameGeneratorUtil动态生成表名，返回预投放量、投放方法和扩展投放类型等关键信息。
     * 
     * @param cigCode 卷烟代码（必填）
     * @param cigName 卷烟名称（必填）
     * @param year 年份（必填，2020-2099）
     * @param month 月份（必填，1-12）
     * @param weekSeq 周序号（必填，1-5）
     * @return 预投放量信息Map，包含以下字段：
     *         - advAmount: 预投放量（ADV字段值）
     *         - deliveryMethod: 投放方法
     *         - deliveryEtype: 扩展投放类型
     *         - deliveryArea: 投放区域
     * 
     * @example
     * getAdvDataInfo("42020181", "黄鹤楼（1916中支）", 2025, 9, 3)
     * -> 查询 cigarette_distribution_info_2025_9_3 表
     * -> 返回 {advAmount: 1000, deliveryMethod: "按档位扩展投放", deliveryEtype: "档位+区县", ...}
     */
    Map<String, Object> getAdvDataInfo(String cigCode, String cigName, Integer year, Integer month, Integer weekSeq);
    
    /**
     * 时间维度数据清理
     * 
     * 根据时间参数从cigarette_distribution_prediction_{year}_{month}_{weekSeq}表中删除指定时间周期的所有分配数据。
     * 使用TableNameGeneratorUtil动态生成表名，支持完整的时间周期数据清理和事务安全。
     * 
     * @param year 年份（必填，2020-2099）
     * @param month 月份（必填，1-12）
     * @param weekSeq 周序号（必填，1-5）
     * @return 删除操作结果Map，包含以下字段：
     *         - success: 删除操作是否成功
     *         - deletedCount: 实际删除的记录数
     *         - tableName: 操作的表名
     *         - message: 操作结果描述
     * 
     * @example
     * deleteDistributionDataByTime(2025, 9, 3)
     * -> 清空 cigarette_distribution_prediction_2025_9_3 表的所有数据
     * -> 返回删除的记录总数和操作状态
     */
    Map<String, Object> deleteDistributionDataByTime(Integer year, Integer month, Integer weekSeq);
}