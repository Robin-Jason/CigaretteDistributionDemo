package org.example.service;

import org.example.entity.CigaretteDistributionInfoData;
import org.example.entity.CigaretteDistributionPredictionData;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 通用服务接口
 * 
 * 【核心功能】
 * 为五种卷烟投放类型提供统一的数据访问和操作方法，支持动态表名策略
 * 
 * 【支持的投放类型】
 * - 按档位统一投放（全市统一）
 * - 档位+区县（区县级精准投放）
 * - 档位+市场类型（城网/农网分类投放）
 * - 档位+城乡分类代码（城乡属性分类投放）
 * - 档位+业态（业态类型分类投放）
 * 
 * 【动态表名策略】
 * - 卷烟投放基本信息表：cigarette_distribution_info_{year}_{month}_{weekSeq}
 * - 卷烟投放预测数据表：cigarette_distribution_prediction_{year}_{month}_{weekSeq}
 * - 区域客户数表：region_clientNum_{主序号}_{子序号}
 * 
 * @author Robin
 * @version 3.0 - 动态表名版本
 * @since 2025-10-10
 */
public interface CommonService {

    /**
     * 方法1：获取投放区域列表
     * 
     * 根据投放类型组合从对应的region_clientNum表获取所有可用的投放区域列表。
     * 使用TableNameGeneratorUtil动态生成表名，支持非双周上浮和双周上浮两种表格。
     * 
     * @param deliveryMethod 投放方法（按档位统一投放、按档位扩展投放）
     * @param deliveryEtype 扩展投放类型（档位+区县、档位+市场类型、档位+城乡分类代码、档位+业态，可为null）
     * @return 该投放类型对应的所有投放区域列表，按区域名称排序
     * 
     * @example
     * getAllRegionList("按档位扩展投放", "档位+区县") 
     * -> 查询 region_clientNum_1_1 表
     * -> 返回 ["丹江", "房县", "郧西", "郧阳", "竹山", "竹溪", "城区"]
     */
    List<String> getAllRegionList(String deliveryMethod, String deliveryEtype);

    /**
     * 方法2：构建区域客户数矩阵
     * 
     * 根据投放类型从对应的region_clientNum表获取区域和档位信息，构建完整的区域客户数矩阵。
     * 使用TableNameGeneratorUtil动态生成表名，支持所有投放类型的矩阵构建。
     * 
     * @param deliveryMethod 投放方法（按档位统一投放、按档位扩展投放）
     * @param deliveryEtype 扩展投放类型（档位+区县、档位+市场类型、档位+城乡分类代码、档位+业态，可为null）
     * @return RegionCustomerMatrix 区域客户数矩阵对象，包含区域名称列表和对应的30×n客户数矩阵
     * 
     * @example
     * buildRegionCustomerMatrix("按档位扩展投放", "档位+区县")
     * -> 查询 region_clientNum_1_1 表
     * -> 返回 RegionCustomerMatrix{regionNames: [...], customerMatrix: [[D30值, D29值, ...], ...]}
     */
    RegionCustomerMatrix buildRegionCustomerMatrix(String deliveryMethod, String deliveryEtype);

    /**
     * 方法3：获取预投放量数据
     * 
     * 根据投放类型从cigarette_distribution_info_{year}_{month}_{weekSeq}表中获取指定投放类型的卷烟预投放量数据。
     * 使用TableNameGeneratorUtil动态生成表名，支持时间维度的数据隔离。
     * 
     * @param deliveryEtype 投放类型（NULL表示按档位统一投放，其他值表示具体的扩展投放类型）
     * @param year 年份（2020-2099）
     * @param month 月份（1-12）
     * @param weekSeq 周序号（1-5）
     * @return 预投放量数据列表，包含卷烟代码、名称、预投放量(ADV)、投放区域等完整信息
     * 
     * @example
     * getAdvDataByDeliveryType("档位+区县", 2025, 9, 3)
     * -> 查询 cigarette_distribution_info_2025_9_3 表
     * -> WHERE DELIVERY_ETYPE = '档位+区县'
     * -> 返回该投放类型的所有卷烟基础信息
     */
    List<CigaretteDistributionInfoData> getAdvDataByDeliveryType(String deliveryEtype, Integer year, Integer month, Integer weekSeq);

    /**
     * 方法4：批量写入预测数据
     * 
     * 将CigaretteDistributionPredictionData对象列表批量写入到cigarette_distribution_prediction_{year}_{month}_{weekSeq}表中。
     * 使用TableNameGeneratorUtil动态生成表名，从数据中自动提取时间参数，支持完整的事务管理。
     * 
     * @param testDataList CigaretteDistributionPredictionData对象列表，每个对象包含完整的预测数据
     * @return 写入结果Map，包含以下字段：
     *         - success: 写入是否完全成功
     *         - totalCount: 总记录数
     *         - successCount: 成功写入数
     *         - failCount: 失败记录数
     *         - message: 操作结果描述
     * 
     * @example
     * 数据中包含年月周序号(2025,9,3) -> 自动生成表名 cigarette_distribution_prediction_2025_9_3
     * 执行批量INSERT操作，包含所有字段（CIG_CODE, CIG_NAME, D30-D1, BZ等）
     */
    Map<String, Object> batchInsertTestData(List<CigaretteDistributionPredictionData> testDataList);

    /**
     * 方法5：精确删除预测数据
     * 
     * 根据卷烟信息、时间信息和投放区域从cigarette_distribution_prediction_{year}_{month}_{weekSeq}表中精确删除匹配的投放记录。
     * 使用TableNameGeneratorUtil动态生成表名，先查询后删除，确保操作安全性。
     * 
     * @param cigCode 卷烟代码（必填）
     * @param cigName 卷烟名称（必填）
     * @param year 年份（必填，2020-2099）
     * @param month 月份（必填，1-12）
     * @param weekSeq 周序号（必填，1-5）
     * @param deliveryArea 投放区域（必填）
     * @return 删除结果Map，包含以下字段：
     *         - success: 删除操作是否成功
     *         - deletedCount: 实际删除的记录数
     *         - message: 操作结果描述
     * 
     * @example
     * deleteSpecificTestData("42020181", "黄鹤楼（1916中支）", 2025, 9, 3, "全市")
     * -> 生成表名 cigarette_distribution_prediction_2025_9_3
     * -> DELETE WHERE CIG_CODE='42020181' AND CIG_NAME='黄鹤楼（1916中支）' AND DELIVERY_AREA='全市'
     */
    Map<String, Object> deleteSpecificTestData(String cigCode, String cigName, 
                                              Integer year, Integer month, Integer weekSeq, 
                                              String deliveryArea);

    /**
     * 区域客户数矩阵数据类
     * 
     * 用于封装区域客户数矩阵的完整数据结构，支持动态投放类型的区域和档位信息管理。
     * 包含区域名称列表和对应的客户数矩阵（n×30），其中n为区域数量，30为档位数量（D30-D1）。
     * 
     * 【核心功能】
     * - 区域名称管理：维护投放类型对应的所有区域列表
     * - 客户数矩阵：每个区域30个档位的客户数数据
     * - 便捷查询：支持按区域名称快速查询客户数数组
     * - 数据验证：提供矩阵完整性检查方法
     * 
     * 【使用场景】
     * - 算法计算：为各种分配算法提供基础数据
     * - 数据传输：在服务层之间传递完整的矩阵信息
     * - 业务逻辑：支持复杂的区域和档位数据操作
     */
    class RegionCustomerMatrix {
        private List<String> regionNames;           // 区域名称列表：存储投放类型对应的所有区域
        private List<BigDecimal[]> customerMatrix;  // 客户数矩阵：每行对应一个区域的30个档位客户数数据（D30-D1）
        
        /**
         * 默认构造函数
         * 初始化空的区域列表和客户数矩阵
         */
        public RegionCustomerMatrix() {
            this.regionNames = new java.util.ArrayList<>();
            this.customerMatrix = new java.util.ArrayList<>();
        }
        
        /**
         * 带参数构造函数
         * 使用提供的区域列表和客户数矩阵初始化对象，自动处理null值情况
         * 
         * @param regionNames 区域名称列表
         * @param customerMatrix 客户数矩阵，每行30个档位数据
         */
        public RegionCustomerMatrix(List<String> regionNames, List<BigDecimal[]> customerMatrix) {
            this.regionNames = regionNames != null ? regionNames : new java.util.ArrayList<>();
            this.customerMatrix = customerMatrix != null ? customerMatrix : new java.util.ArrayList<>();
        }
        
        // ==================== Getters and Setters ====================
        
        /**
         * 获取区域名称列表
         * @return 所有区域的名称列表
         */
        public List<String> getRegionNames() {
            return regionNames;
        }
        
        /**
         * 设置区域名称列表
         * @param regionNames 区域名称列表
         */
        public void setRegionNames(List<String> regionNames) {
            this.regionNames = regionNames;
        }
        
        /**
         * 获取客户数矩阵
         * @return 客户数矩阵，每行对应一个区域的30个档位数据
         */
        public List<BigDecimal[]> getCustomerMatrix() {
            return customerMatrix;
        }
        
        /**
         * 设置客户数矩阵
         * @param customerMatrix 客户数矩阵
         */
        public void setCustomerMatrix(List<BigDecimal[]> customerMatrix) {
            this.customerMatrix = customerMatrix;
        }
        
        // ==================== 便捷查询方法 ====================
        
        /**
         * 获取区域数量
         * @return 当前矩阵包含的区域数量
         */
        public int getRegionCount() {
            return regionNames.size();
        }
        
        /**
         * 获取档位数量（固定为30）
         * @return 档位数量，从D30到D1共30个档位
         */
        public int getGradeCount() {
            return 30;
        }
        
        /**
         * 根据区域名称获取客户数数组
         * 提供便捷的按区域查询功能，用于算法计算和业务逻辑处理
         * 
         * @param regionName 区域名称
         * @return 该区域的30个档位客户数数组，如果区域不存在返回null
         */
        public BigDecimal[] getCustomerCountsByRegion(String regionName) {
            int index = regionNames.indexOf(regionName);
            if (index >= 0 && index < customerMatrix.size()) {
                return customerMatrix.get(index);
            }
            return null;
        }
        
        /**
         * 检查矩阵是否为空
         * @return true表示矩阵为空（无区域或无数据），false表示包含有效数据
         */
        public boolean isEmpty() {
            return regionNames.isEmpty() || customerMatrix.isEmpty();
        }
        
        /**
         * 返回矩阵的字符串表示
         * 便于调试和日志输出
         * @return 包含区域数量、档位数量和区域列表的描述字符串
         */
        public String toString() {
            return String.format("RegionCustomerMatrix{regionCount=%d, gradeCount=%d, regions=%s}", 
                               getRegionCount(), getGradeCount(), regionNames);
        }
    }
}