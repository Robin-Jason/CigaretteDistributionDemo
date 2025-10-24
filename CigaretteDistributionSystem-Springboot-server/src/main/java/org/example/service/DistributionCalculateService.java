package org.example.service;

import org.example.entity.CigaretteDistributionPredictionData;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 分配计算服务接口
 * 
 * 【核心功能】
 * 负责卷烟投放分配的算法计算和分配矩阵写回等核心计算功能，支持动态表名策略
 * 
 * 【主要职责】
 * - 算法协调：统筹五种投放类型的分配算法计算
 * - 矩阵计算：严格按照公式计算实际投放量（档位分配值 × 区域客户数档位值）
 * - 数据写回：将计算结果写入对应的预测数据表
 * - 统计分析：按卷烟维度计算总实际投放量统计
 * 
 * 【算法支持】
 * - 按档位统一投放：全市统一分配算法
 * - 档位+区县：区县级精准分配算法
 * - 档位+市场类型：城网/农网分类算法
 * - 档位+城乡分类代码：城乡属性分类算法
 * - 档位+业态：业态类型分类算法
 * 
 * 
 * @author Robin
 * @version 3.0 - 动态表名与多算法协调版本
 * @since 2025-10-10
 */
public interface DistributionCalculateService {
    
    /**
     * 一键生成分配方案
     * 
     * 协调五种投放类型的算法计算完整的分配矩阵，并将结果写回数据库。
     * 从cigarette_distribution_info_{year}_{month}_{weekSeq}表获取卷烟基础信息，
     * 调用对应的分配算法，计算实际投放量，写入cigarette_distribution_prediction_{year}_{month}_{weekSeq}表。
     * 
     * @param year 年份（必填，2020-2099）
     * @param month 月份（必填，1-12）
     * @param weekSeq 周序号（必填，1-5）
     * @return 写回结果Map，包含以下字段：
     *         - success: 整体操作是否成功
     *         - totalCigarettes: 处理的卷烟总数
     *         - successCount: 成功计算的卷烟数
     *         - failureCount: 失败的卷烟数
     *         - algorithmStats: 各算法的统计信息
     *         - detailedResults: 详细的分配结果列表
     *         - message: 操作结果描述
     * 
     * @example
     * getAndwriteBackAllocationMatrix(2025, 9, 3)
     * -> 读取 cigarette_distribution_info_2025_9_3 表
     * -> 按投放类型分组调用对应算法
     * -> 计算实际投放量并写入 cigarette_distribution_prediction_2025_9_3 表
     */
    Map<String, Object> getAndwriteBackAllocationMatrix(Integer year, Integer month, Integer weekSeq);
    
    /**
     * 一键生成分配方案（支持市场类型比例参数）
     * 
     * 协调五种投放类型的算法计算完整的分配矩阵，并将结果写回数据库。
     * 支持为"档位+市场类型"传入城网/农网比例参数。
     * 
     * @param year 年份（必填，2020-2099）
     * @param month 月份（必填，1-12）
     * @param weekSeq 周序号（必填，1-5）
     * @param marketRatios 市场类型比例参数（可选，仅用于档位+市场类型）
     *                     包含 urbanRatio（城网比例）和 ruralRatio（农网比例）
     *                     如果为null或未提供比例，使用默认值40%/60%
     * @return 写回结果Map，包含以下字段：
     *         - success: 整体操作是否成功
     *         - totalCigarettes: 处理的卷烟总数
     *         - successCount: 成功计算的卷烟数
     *         - failureCount: 失败的卷烟数
     *         - algorithmStats: 各算法的统计信息
     *         - detailedResults: 详细的分配结果列表
     *         - message: 操作结果描述
     * 
     * @example
     * Map<String, BigDecimal> ratios = new HashMap<>();
     * ratios.put("urbanRatio", new BigDecimal("0.45"));
     * ratios.put("ruralRatio", new BigDecimal("0.55"));
     * getAndwriteBackAllocationMatrix(2025, 9, 3, ratios)
     * -> "档位+市场类型"将使用45%/55%的城网/农网比例
     */
    Map<String, Object> getAndwriteBackAllocationMatrix(Integer year, Integer month, Integer weekSeq, 
                                                        java.util.Map<String, java.math.BigDecimal> marketRatios);
    
    /**
     * 区域实际投放量计算
     * 
     * 严格按照标准公式计算卷烟在指定区域的实际投放量：
     * 实际投放量 = ∑（档位分配值 × 对应区域客户数档位值）
     * 使用TableNameGeneratorUtil动态获取区域客户数表，确保计算准确性。
     * 
     * @param target 目标区域名称（必填）
     * @param allocationRow 档位分配数组（必填，包含30个档位的分配值，从D30到D1）
     * @param deliveryMethod 投放方法（必填，用于确定客户数表）
     * @param deliveryEtype 扩展投放类型（必填，用于确定客户数表）
     * @return 计算得出的实际投放量（BigDecimal，保留小数精度）
     * 
     * @throws IllegalArgumentException 当违反计算公式或参数无效时抛出异常
     * 
     * @example
     * calculateActualDeliveryForRegion("房县", [5.0, 3.0, 2.0, ...], "按档位扩展投放", "档位+区县", "两周一访上浮100%")
     * -> 从 region_clientNum_1_2 表获取房县的客户数数据 (双周上浮)
     * -> 计算：5.0×房县D30客户数 + 3.0×房县D29客户数 + ... = 最终实际投放量
     */
    BigDecimal calculateActualDeliveryForRegion(String target, BigDecimal[] allocationRow, String deliveryMethod, String deliveryEtype, String remark);
    
    /**
     * 卷烟总投放量统计计算
     * 
     * 对传入的卷烟分配数据列表按卷烟维度分组统计，计算每种卷烟的总实际投放量。
     * 支持跨区域的投放量汇总，为业务决策提供统计数据支持。
     * 
     * @param rawDataList 原始卷烟分配数据列表（必填，包含各区域的投放记录）
     * @return 卷烟统计Map，key为"卷烟代码_卷烟名称"格式，value为该卷烟的总实际投放量
     * 
     * @example
     * rawDataList包含多个区域的"黄鹤楼（1916中支）"投放记录
     * -> 按卷烟分组：{"42020181_黄鹤楼（1916中支）": 总投放量}
     * -> 汇总该卷烟在所有区域的实际投放量
     */
    Map<String, BigDecimal> calculateTotalActualDeliveryByTobacco(List<CigaretteDistributionPredictionData> rawDataList);
}