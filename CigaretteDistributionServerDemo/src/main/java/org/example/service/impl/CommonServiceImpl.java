package org.example.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.entity.CigaretteDistributionInfoData;
import org.example.entity.CigaretteDistributionPredictionData;
import org.example.service.CommonService;
import org.example.util.CigaretteDistributionRowMapper;
import org.example.util.CigaretteDistributionSqlBuilder;
import org.example.util.TableNameGeneratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * 通用服务实现类
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
@Slf4j
@Service
public class CommonServiceImpl implements CommonService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    @Override
    public List<String> getAllRegionList(String deliveryMethod, String deliveryEtype) {
        log.info("获取投放区域列表，投放方法: {}, 扩展投放类型: {}", deliveryMethod, deliveryEtype);
        
        try {
            // 根据投放类型组合确定查询的表和字段
            String tableInfo = getTableAndFieldInfo(deliveryMethod, deliveryEtype);
            if (tableInfo == null) {
                log.warn("不支持的投放类型组合，投放方法: {}, 扩展投放类型: {}", deliveryMethod, deliveryEtype);
                return new ArrayList<>();
            }
            
            String[] parts = tableInfo.split("\\|");
            String tableName = parts[0];
            String fieldName = parts[1];
            
            // 使用SQL构建工具类构建查询语句
            CigaretteDistributionSqlBuilder.validateSqlComponents(tableName, fieldName);
            String sql = CigaretteDistributionSqlBuilder.buildRegionListSql(tableName, fieldName);
            
            log.info("执行查询SQL: {}", sql);
            
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            List<String> regionList = new ArrayList<>();
            
            for (Map<String, Object> row : result) {
                Object regionName = row.get(fieldName);
                if (regionName == null) {
                    regionName = row.get(fieldName.toUpperCase()); // 尝试大写字段名
                }
                if (regionName != null) {
                    regionList.add(regionName.toString());
                }
            }
            
            log.info("获取到 {} 个投放区域: {}", regionList.size(), regionList);
            return regionList;
            
        } catch (Exception e) {
            log.error("获取投放区域列表失败，投放方法: {}, 扩展投放类型: {}", deliveryMethod, deliveryEtype, e);
            return new ArrayList<>();
        }
    }

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
    @Override
    public RegionCustomerMatrix buildRegionCustomerMatrix(String deliveryMethod, String deliveryEtype) {
        log.info("构建区域客户数矩阵，投放方法: {}, 扩展投放类型: {}", deliveryMethod, deliveryEtype);
        
        try {
            // 根据投放类型组合确定查询的表和字段
            String tableInfo = getTableAndFieldInfo(deliveryMethod, deliveryEtype);
            if (tableInfo == null) {
                log.warn("不支持的投放类型组合，投放方法: {}, 扩展投放类型: {}", deliveryMethod, deliveryEtype);
                return new RegionCustomerMatrix();
            }
            
            String[] parts = tableInfo.split("\\|");
            String tableName = parts[0];
            String fieldName = parts[1];
            
            // 使用SQL构建工具类构建查询语句
            CigaretteDistributionSqlBuilder.validateSqlComponents(tableName, fieldName);
            
            String sql;
            if (tableName.startsWith("region_clientNum_0_")) {
                // 按档位统一投放：只查询全市数据
                sql = CigaretteDistributionSqlBuilder.buildCityMatrixSql(tableName, fieldName);
            } else {
                // 其他扩展投放类型：查询所有区域数据
                sql = CigaretteDistributionSqlBuilder.buildRegionMatrixSql(tableName, fieldName);
            }
            
            log.info("执行查询SQL: {}", sql);
            
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            
            // 构建矩阵
            List<String> regionNames = new ArrayList<>();
            List<BigDecimal[]> customerMatrix = new ArrayList<>();
            
            for (Map<String, Object> row : result) {
                // 获取区域名称
                Object regionName = row.get("region_name");
                if (regionName == null) {
                    regionName = row.get("REGION_NAME"); // 尝试大写字段名
                }
                
                if (regionName != null) {
                    regionNames.add(regionName.toString());
                    
                    // 使用RowMapper工具类提取30个档位的客户数
                    BigDecimal[] customerCounts = CigaretteDistributionRowMapper.extractCustomerCounts(row);
                    customerMatrix.add(customerCounts);
                }
            }
            
            RegionCustomerMatrix matrix = new RegionCustomerMatrix(regionNames, customerMatrix);
            log.info("构建完成，矩阵维度: {}x30，包含区域: {}", matrix.getRegionCount(), regionNames);
            
            return matrix;
            
        } catch (Exception e) {
            log.error("构建区域客户数矩阵失败，投放方法: {}, 扩展投放类型: {}", deliveryMethod, deliveryEtype, e);
            return new RegionCustomerMatrix();
        }
    }

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
    @Override
    public List<CigaretteDistributionInfoData> getAdvDataByDeliveryType(String deliveryEtype, Integer year, Integer month, Integer weekSeq) {
        log.info("根据投放类型获取预投放量数据，投放类型: {}, 时间: {}-{}-{}", deliveryEtype, year, month, weekSeq);
        
        try {
            // 使用TableNameGeneratorUtil生成卷烟投放基本信息表名
            String tableName = TableNameGeneratorUtil.generateDistributionInfoTableName(year, month, weekSeq);
            log.debug("生成卷烟投放基本信息表名: {}", tableName);
            
            // 使用SQL构建工具类构建查询语句
            CigaretteDistributionSqlBuilder.validateSqlComponents(tableName, null);
            String sql;
            List<Object> params = new ArrayList<>();
            
            if ("NULL".equals(deliveryEtype) || deliveryEtype == null) {
                // 全市统一投放：获取投放类型为NULL或为空的记录
                sql = CigaretteDistributionSqlBuilder.buildCityDeliveryQuerySql(tableName);
            } else {
                // 按具体投放类型获取数据
                sql = CigaretteDistributionSqlBuilder.buildDeliveryTypeQuerySql(tableName);
                params.add(deliveryEtype);
            }
            
            log.info("执行查询SQL: {}", sql);
            
            // 执行查询
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, params.toArray());
            
            // 使用RowMapper工具类转换查询结果
            List<CigaretteDistributionInfoData> advDataList = new ArrayList<>();
            for (Map<String, Object> row : results) {
                CigaretteDistributionInfoData data = CigaretteDistributionRowMapper.mapToCigaretteDistributionInfoData(row);
                if (data != null) {
                    advDataList.add(data);
                }
            }
            
            log.info("从表 {} 获取到 {} 条预投放量数据", tableName, advDataList.size());
            return advDataList;
            
        } catch (Exception e) {
            log.error("获取预投放量数据失败，投放类型: {}, 时间: {}-{}-{}, 错误: {}", 
                    deliveryEtype, year, month, weekSeq, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
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
    @Override
    @Transactional
    public Map<String, Object> batchInsertTestData(List<CigaretteDistributionPredictionData> testDataList) {
        log.info("开始批量写入预测数据，共 {} 条记录", testDataList.size());
        
        Map<String, Object> result = new HashMap<>();
        
        if (testDataList == null || testDataList.isEmpty()) {
            result.put("success", true);
            result.put("message", "没有数据需要写入");
            result.put("totalCount", 0);
            result.put("successCount", 0);
            result.put("failCount", 0);
            return result;
        }
        
        try {
            // 从第一条记录获取时间参数
            CigaretteDistributionPredictionData firstData = testDataList.get(0);
            Integer year = firstData.getYear();
            Integer month = firstData.getMonth(); 
            Integer weekSeq = firstData.getWeekSeq();
            
            if (year == null || month == null || weekSeq == null) {
                result.put("success", false);
                result.put("message", "数据中缺少必要的时间参数（年/月/周序号）");
                result.put("totalCount", testDataList.size());
                result.put("successCount", 0);
                result.put("failCount", testDataList.size());
                return result;
            }
            
            // 使用TableNameGeneratorUtil生成动态表名
            String tableName = TableNameGeneratorUtil.generatePredictionTableName(year, month, weekSeq);
            log.debug("生成预测数据表名: {}", tableName);
            
            int successCount = 0;
            int failCount = 0;
            
            // 使用SQL构建工具类构建插入SQL
            CigaretteDistributionSqlBuilder.validateSqlComponents(tableName, null);
            String insertSql = CigaretteDistributionSqlBuilder.buildBatchInsertSql(tableName);
            log.debug("执行批量插入SQL模板: {}", insertSql);
            
            // 批量执行插入
            for (CigaretteDistributionPredictionData data : testDataList) {
                try {
                    // 验证数据有效性
                    if (!CigaretteDistributionRowMapper.validatePredictionData(data)) {
                        log.warn("跳过无效数据: {}-{}", data.getCigCode(), data.getCigName());
                        failCount++;
                        continue;
                    }
                    
                    Object[] params = CigaretteDistributionRowMapper.buildInsertParams(data);
                    int affectedRows = jdbcTemplate.update(insertSql, params);
                    if (affectedRows > 0) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (Exception e) {
                    log.error("插入单条记录失败，卷烟: {}-{}, 错误: {}", 
                            data.getCigCode(), data.getCigName(), e.getMessage());
                    failCount++;
                }
            }
            
            result.put("success", failCount == 0);
            result.put("totalCount", testDataList.size());
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            
            if (failCount == 0) {
                result.put("message", String.format("所有 %d 条预测数据写入表 %s 成功", successCount, tableName));
                log.info("批量写入预测数据完成，成功写入 {} 条记录到表 {}", successCount, tableName);
            } else {
                result.put("message", String.format("部分写入失败，成功: %d，失败: %d，目标表: %s", successCount, failCount, tableName));
                log.warn("批量写入预测数据部分失败，成功: {}, 失败: {}, 目标表: {}", successCount, failCount, tableName);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("批量写入预测数据失败", e);
            result.put("success", false);
            result.put("totalCount", testDataList.size());
            result.put("successCount", 0);
            result.put("failCount", testDataList.size());
            result.put("message", "批量写入失败: " + e.getMessage());
            result.put("errorDetails", e.getClass().getSimpleName() + ": " + e.getMessage());
            return result;
        }
    }
    
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
    @Override
    @Transactional
    public Map<String, Object> deleteSpecificTestData(String cigCode, String cigName, 
                                                     Integer year, Integer month, Integer weekSeq, 
                                                     String deliveryArea) {
        log.info("开始删除指定条件的测试数据，卷烟: {}-{}, 时间: {}-{}-{}, 区域: {}", 
                cigCode, cigName, year, month, weekSeq, deliveryArea);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 参数验证
            if (cigCode == null || cigCode.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "卷烟代码不能为空");
                result.put("deletedCount", 0);
                return result;
            }
            
            if (cigName == null || cigName.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "卷烟名称不能为空");
                result.put("deletedCount", 0);
                return result;
            }
            
            if (year == null || month == null || weekSeq == null) {
                result.put("success", false);
                result.put("message", "时间参数不能为空");
                result.put("deletedCount", 0);
                return result;
            }
            
            if (deliveryArea == null || deliveryArea.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "投放区域不能为空");
                result.put("deletedCount", 0);
                return result;
            }
            
            // 使用TableNameGeneratorUtil生成动态表名
            String tableName = TableNameGeneratorUtil.generatePredictionTableName(year, month, weekSeq);
            log.debug("生成预测数据表名: {}", tableName);
            
            // 使用SQL构建工具类构建查询和删除SQL
            CigaretteDistributionSqlBuilder.validateSqlComponents(tableName, null);
            
            // 首先查询是否存在匹配的记录
            String querySql = CigaretteDistributionSqlBuilder.buildExistenceCheckSql(tableName);
            log.debug("执行查询SQL: {}", querySql);
            
            Integer existingCount = jdbcTemplate.queryForObject(querySql, Integer.class, 
                    cigCode, cigName, deliveryArea);
            
            if (existingCount == null || existingCount == 0) {
                result.put("success", true);
                result.put("message", "未找到匹配的记录，无需删除");
                result.put("deletedCount", 0);
                log.info("未找到匹配的记录: 卷烟{}-{}, 时间{}-{}-{}, 区域{}, 表: {}", 
                        cigCode, cigName, year, month, weekSeq, deliveryArea, tableName);
                return result;
            }
            
            // 执行删除操作
            String deleteSql = CigaretteDistributionSqlBuilder.buildPreciseDeleteSql(tableName);
            log.debug("执行删除SQL: {}", deleteSql);
            
            int deletedCount = jdbcTemplate.update(deleteSql, cigCode, cigName, deliveryArea);
            
            result.put("success", true);
            result.put("deletedCount", deletedCount);
            result.put("message", String.format("成功从表 %s 删除 %d 条匹配记录", tableName, deletedCount));
            
            log.info("成功删除 {} 条记录: 卷烟{}-{}, 时间{}-{}-{}, 区域{}, 表: {}", 
                    deletedCount, cigCode, cigName, year, month, weekSeq, deliveryArea, tableName);
            
            return result;
            
        } catch (Exception e) {
            log.error("删除指定条件的测试数据失败，卷烟: {}-{}, 时间: {}-{}-{}, 区域: {}", 
                     cigCode, cigName, year, month, weekSeq, deliveryArea, e);
            result.put("success", false);
            result.put("deletedCount", 0);
            result.put("message", "删除失败: " + e.getMessage());
            result.put("errorDetails", e.getClass().getSimpleName() + ": " + e.getMessage());
            return result;
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 根据投放类型组合确定对应的表名和字段名
     * 
     * 使用TableNameGeneratorUtil生成符合命名规则的region_clientNum表名，支持所有投放类型的动态表名生成。
     * 统一使用"region"作为字段名，简化查询逻辑。
     * 
     * @param deliveryMethod 投放方法（按档位统一投放、按档位扩展投放）
     * @param deliveryEtype 扩展投放类型（档位+区县、档位+市场类型、档位+城乡分类代码、档位+业态，可为null）
     * @return 表名|字段名 格式的字符串，如果不支持则返回null
     * 
     * @example
     * getTableAndFieldInfo("按档位扩展投放", "档位+区县") -> "region_clientNum_1_1|region"
     */
    private String getTableAndFieldInfo(String deliveryMethod, String deliveryEtype) {
        try {
            // 验证投放类型组合是否有效
            if (!TableNameGeneratorUtil.isValidDeliveryTypeCombination(deliveryMethod, deliveryEtype)) {
                log.warn("不支持的投放类型组合，投放方法: {}, 扩展投放类型: {}", deliveryMethod, deliveryEtype);
                return null;
            }
            
            // 使用TableNameGeneratorUtil生成区域客户数表名（默认非双周上浮）
            String tableName = TableNameGeneratorUtil.generateRegionClientTableName(
                deliveryMethod, deliveryEtype, false);
            
            log.debug("生成区域客户数表名: {} (投放方法: {}, 扩展投放类型: {})", 
                    tableName, deliveryMethod, deliveryEtype);
            
            // 所有region_clientNum表统一使用"region"作为字段名
            return tableName + "|region";
            
        } catch (IllegalArgumentException e) {
            log.error("生成表名失败，投放方法: {}, 扩展投放类型: {}, 错误: {}", 
                    deliveryMethod, deliveryEtype, e.getMessage());
            return null;
        }
    }
    
}
