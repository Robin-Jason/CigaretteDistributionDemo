package org.example.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.entity.CigaretteDistributionPredictionData;
import org.example.service.DistributionCalculateService;
import org.example.service.EncodeDecodeService;
import org.example.service.RegionClientNumDataService;
import org.example.service.strategy.DistributionStrategy;
import org.example.service.strategy.DistributionStrategyManager;
import org.example.util.*;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * 分配计算服务实现类
 * 负责算法计算和分配矩阵写回等核心计算功能
 */
@Slf4j
@Service
public class DistributionCalculateServiceImpl implements DistributionCalculateService {
    
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private DistributionStrategyManager strategyManager;
    
    @Autowired
    private EncodeDecodeService encodeDecodeService;
    
    @Autowired
    private RegionClientNumDataService regionClientNumDataService;

    
    // ==================== 一键生成分配方案并写回数据库服务 ====================
    
    /**
     * 方法1：获取算法输出的分配矩阵并写回数据库
     * 根据年月周序号从对应的cigarette_distribution_info表获取卷烟信息，协调各投放类型算法计算分配矩阵，计算实际投放量并写入对应的预测表
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @return 写回结果Map，包含成功状态、统计信息和详细的分配结果列表
     */
    @Override
    public Map<String, Object> getAndwriteBackAllocationMatrix(Integer year, Integer month, Integer weekSeq) {
        // 调用带比例参数的方法，传null表示使用默认比例
        return getAndwriteBackAllocationMatrix(year, month, weekSeq, null);
    }
    
    @Override
    public Map<String, Object> getAndwriteBackAllocationMatrix(Integer year, Integer month, Integer weekSeq, 
                                                              Map<String, BigDecimal> marketRatios) {
        log.info("协调器：开始将分配矩阵写回数据库，年份: {}, 月份: {}, 周序号: {}", year, month, weekSeq);
        if (marketRatios != null && !marketRatios.isEmpty()) {
            log.info("接收市场类型比例参数 - 城网: {}, 农网: {}", 
                    marketRatios.get("urbanRatio"), marketRatios.get("ruralRatio"));
        }
        
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> writeBackResults = new ArrayList<>();
        
        try {
            // 生成动态表名
            String tableName = TableNameGeneratorUtil.generateDistributionInfoTableName(year, month, weekSeq);
            log.debug("查询卷烟投放基本信息表: {}", tableName);
            
            // 检查表是否存在
            String checkTableSql = CigaretteDistributionSqlBuilder.buildCheckTableExistsSql();
            Integer tableExists = jdbcTemplate.queryForObject(checkTableSql, Integer.class, tableName);
            
            if (tableExists == null || tableExists == 0) {
                log.warn("表 {} 不存在，无法获取预投放量数据", tableName);
                result.put("success", false);
                result.put("message", "指定时间的卷烟投放基本信息表不存在: " + tableName);
                result.put("totalCount", 0);
                result.put("successCount", 0);
                result.put("writeBackResults", new ArrayList<>());
                return result;
            }
            
            // 获取指定周期的预投放量数据
            String advDataSql = CigaretteDistributionSqlBuilder.buildAdvDataQuerySql(tableName, year, month, weekSeq);
            List<Map<String, Object>> advDataList = jdbcTemplate.queryForList(advDataSql);
            log.info("从表 {} 获取{}年{}月第{}周的预投放量数据数量: {}", tableName, year, month, weekSeq, advDataList.size());
            
            // 调试日志：检查cigarette_distribution_info表的第一条数据，查看字段情况
            if (!advDataList.isEmpty()) {
                Map<String, Object> firstRecord = advDataList.get(0);
                log.debug("卷烟投放基本信息表第一条记录字段检查:");
                log.debug("  cig_code: {}", firstRecord.get("cig_code"));
                log.debug("  cig_name: {}", firstRecord.get("cig_name"));
                log.debug("  delivery_method: {}", firstRecord.get("delivery_method"));
                log.debug("  delivery_etype: {}", firstRecord.get("delivery_etype"));
                log.debug("  所有字段: {}", firstRecord.keySet());
            }
            
            int successCount = 0;
            int totalCount = 0;
            
            // 处理每个卷烟
            for (Map<String, Object> advData : advDataList) {
                totalCount++;
                // 初始化结果对象
                Map<String, Object> cigResult = new HashMap<>();
                
                // 清洗和验证卷烟代码，处理格式不规范的数据
                String rawCigCode = (String) advData.get("cig_code");
                String cigName = (String) advData.get("cig_name");
                String cigCode;
                
                // 设置基本信息到结果中（先用原始数据）
                cigResult.put("cigCode", rawCigCode);
                cigResult.put("cigName", cigName);
                
                try {
                    cigCode = DistributionValidationUtils.sanitizeAndValidateCigaretteCode(rawCigCode, cigName);
                    // 更新清洗后的代码到原数据中，确保后续使用的都是清洗后的代码
                    advData.put("cig_code", cigCode);
                    // 更新结果中的代码为清洗后的代码
                    cigResult.put("cigCode", cigCode);
                } catch (IllegalArgumentException e) {
                    log.error("卷烟数据验证失败: 代码[{}] 名称[{}], 错误: {}", rawCigCode, cigName, e.getMessage());
                    cigResult.put("writeBackStatus", "跳过");
                    cigResult.put("writeBackMessage", "卷烟数据格式错误: " + e.getMessage());
                    writeBackResults.add(cigResult);
                    continue;
                }
                BigDecimal adv = (BigDecimal) advData.get("adv");
                String deliveryArea = (String) advData.get("delivery_area");
                String deliveryEtype = (String) advData.get("delivery_etype");
                
                // 调试日志：检查从cigarette_distribution_info表读取的关键字段值
                log.debug("处理卷烟: {} - {}, delivery_etype: {}", cigCode, cigName, deliveryEtype);
                // 从cigarette_distribution_info表中获取对应的日期信息
                // 处理year字段可能是Date类型的情况
                // 使用RowMapper工具类提取数据，避免重复的类型转换代码
                Integer cigYear = CigaretteDistributionRowMapper.extractYear(advData, "year");
                Integer cigMonth = CigaretteDistributionRowMapper.extractInteger(advData, "month");
                Integer cigWeekSeq = CigaretteDistributionRowMapper.extractInteger(advData, "week_seq");
                
                // 设置其他信息到结果中
                cigResult.put("adv", adv);
                cigResult.put("deliveryArea", deliveryArea);
                cigResult.put("deliveryEtype", deliveryEtype);
                cigResult.put("advYear", cigYear);
                cigResult.put("advMonth", cigMonth);
                cigResult.put("advWeekSeq", cigWeekSeq);
                    
                try {
                    if (deliveryArea != null && !deliveryArea.trim().isEmpty()) {
                        // 根据投放方式和扩展投放类型委托给对应的服务处理
                        List<String> targetList;
                        BigDecimal[][] allocationMatrix;
                        String deliveryMethod = (String) advData.get("delivery_method");
                        String remark = (String) advData.get("remark");  // 获取备注字段
                        
                        // 调试日志：检查关键字段值
                        log.debug("卷烟: {} - {}, deliveryMethod: {}, deliveryEtype: {}, remark: {}", 
                                 cigCode, cigName, deliveryMethod, deliveryEtype, remark);
                        
                        // 使用策略模式处理不同的投放类型
                        try {
                            DistributionStrategy strategy = strategyManager.getStrategy(deliveryMethod, deliveryEtype);
                            
                            // 获取目标列表
                            targetList = strategy.getTargetList(deliveryArea);
                            
                            // 构建额外参数（用于档位+市场类型的比例参数）
                            java.util.Map<String, Object> extraParams = new java.util.HashMap<>();
                            if ("档位+市场类型".equals(deliveryEtype) && marketRatios != null) {
                                // 从方法参数中读取城网和农网比例（前端传入）
                                BigDecimal urbanRatioParam = marketRatios.get("urbanRatio");
                                BigDecimal ruralRatioParam = marketRatios.get("ruralRatio");
                                
                                if (urbanRatioParam != null && ruralRatioParam != null) {
                                    extraParams.put("urbanRatio", urbanRatioParam);
                                    extraParams.put("ruralRatio", ruralRatioParam);
                                    log.debug("卷烟: {} - {}, 使用前端传入的市场类型比例 - 城网: {}, 农网: {}", 
                                             cigCode, cigName, urbanRatioParam, ruralRatioParam);
                                }
                            }
                            
                            // 计算分配矩阵（传递额外参数）
                            allocationMatrix = strategy.calculateMatrix(targetList, adv, extraParams);
                            
                            // 设置结果信息
                            cigResult.put("targetType", strategy.getTargetTypeDescription());
                            cigResult.put("algorithm", strategy.getAlgorithmName());
                            
                        } catch (IllegalArgumentException e) {
                            log.warn("不支持的投放类型组合: 投放方法={}, 投放类型={}, 错误: {}", 
                                   deliveryMethod, deliveryEtype, e.getMessage());
                            targetList = new ArrayList<>();
                            allocationMatrix = null;
                            cigResult.put("targetType", "不支持的类型");
                            cigResult.put("algorithm", "无");
                        } catch (RuntimeException e) {
                            log.error("算法计算失败: 投放方法={}, 投放类型={}, 错误: {}", 
                                    deliveryMethod, deliveryEtype, e.getMessage(), e);
                            targetList = new ArrayList<>();
                            allocationMatrix = null;
                            cigResult.put("targetType", "算法错误");
                            cigResult.put("algorithm", "N/A");
                        }
                        
                        if (!targetList.isEmpty() && allocationMatrix != null) {
                            // 写回数据库，使用cigarette_distribution_info表中的日期信息
                            boolean writeBackSuccess = writeBackToDatabase(allocationMatrix, targetList, 
                                cigCode, cigName, cigYear, cigMonth, cigWeekSeq, deliveryMethod, deliveryEtype, remark);
                            
                            if (writeBackSuccess) {
                                successCount++;
                                cigResult.put("writeBackStatus", "成功");
                                cigResult.put("writeBackMessage", "分配矩阵已成功写回数据库");
                            } else {
                                cigResult.put("writeBackStatus", "失败");
                                cigResult.put("writeBackMessage", "分配矩阵写回数据库失败");
                            }
                        } else {
                            cigResult.put("writeBackStatus", "跳过");
                            cigResult.put("writeBackMessage", "未找到匹配的投放目标");
                        }
                    } else {
                        cigResult.put("writeBackStatus", "跳过");
                        cigResult.put("writeBackMessage", "投放区域为空");
                    }
                    
                } catch (Exception e) {
                    log.error("处理卷烟 {} 时发生错误", cigCode, e);
                    cigResult.put("writeBackStatus", "错误");
                    cigResult.put("writeBackMessage", "处理过程中发生错误: " + e.getMessage());
                }
                
                writeBackResults.add(cigResult);
            }
            
            result.put("success", true);
            result.put("message", String.format("分配矩阵写回完成，成功: %d/%d", successCount, totalCount));
            result.put("totalCount", totalCount);
            result.put("successCount", successCount);
            result.put("results", writeBackResults);
            
            log.info("分配矩阵写回完成，成功: {}/{}", successCount, totalCount);
            
        } catch (Exception e) {
            log.error("分配矩阵写回过程中发生错误", e);
            result.put("success", false);
            result.put("message", "分配矩阵写回失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 方法2：计算卷烟在指定区域的实际投放量
     * 严格遵循公式：实际投放量 = ∑（档位分配值 × 对应区域客户数档位值）
     * 不允许使用其他计算规则，违反时抛出异常
     * 
     * @param target 目标区域名称
     * @param allocationRow 档位分配数组，包含30个档位的分配值
     * @param deliveryMethod 投放方法
     * @param deliveryEtype 投放类型，用于确定客户数表
     * @param remark 备注字段（用于判断是否双周上浮）
     * @return 计算得出的实际投放量
     */
    @Override
    public BigDecimal calculateActualDeliveryForRegion(String target, BigDecimal[] allocationRow, String deliveryMethod, String deliveryEtype, String remark) {
        if (target == null || target.trim().isEmpty()) {
            throw new IllegalArgumentException("目标区域不能为空");
        }
        
        if (allocationRow == null || allocationRow.length != 30) {
            throw new IllegalArgumentException("档位分配数组必须包含30个档位(D30-D1)");
        }
        
        // 对于统一投放（按档位统一投放或按档位投放），deliveryEtype可以为null
        if ("按档位扩展投放".equals(deliveryMethod) && deliveryEtype == null) {
            throw new IllegalArgumentException("扩展投放类型不能为空");
        }
        
        try {
            // 获取目标区域的客户数档位值，使用备注判断是否双周上浮
            BigDecimal[] customerCounts = getCustomerCountsForTarget(target, deliveryMethod, deliveryEtype, remark);
            
            if (customerCounts == null || customerCounts.length != 30) {
                throw new RuntimeException(String.format("无法获取区域 '%s' 的客户数档位数据", target));
            }
            
            BigDecimal actualDelivery = BigDecimal.ZERO;
            StringBuilder calculationDetails = new StringBuilder();
            calculationDetails.append(String.format("区域 '%s' 实际投放量计算: ", target));
            
            // 严格按照公式计算：∑（档位分配值 × 对应区域客户数档位值）
            for (int i = 0; i < 30; i++) {
                BigDecimal allocation = allocationRow[i] != null ? allocationRow[i] : BigDecimal.ZERO;
                BigDecimal customerCount = customerCounts[i] != null ? customerCounts[i] : BigDecimal.ZERO;
                
                BigDecimal gradeContribution = allocation.multiply(customerCount);
                actualDelivery = actualDelivery.add(gradeContribution);
                
                // 记录非零贡献的档位计算详情
                if (gradeContribution.compareTo(BigDecimal.ZERO) > 0) {
                    calculationDetails.append(String.format("D%d(%s×%s=%.4f) ", 
                        30-i, allocation, customerCount, gradeContribution));
                }
            }
            
            log.debug("{} = {}", calculationDetails.toString(), actualDelivery);
            
            // 验证计算结果的合理性
            if (actualDelivery.compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException(String.format("区域 '%s' 的实际投放量计算结果为负数: %s", target, actualDelivery));
            }
            
            log.info("区域 '{}' 实际投放量计算完成: {} (投放类型: {})", target, actualDelivery, deliveryEtype);
            return actualDelivery;
            
        } catch (IllegalArgumentException e) {
            // 重新抛出参数异常
            throw e;
        } catch (RuntimeException e) {
            // 重新抛出运行时异常
            throw e;
        } catch (Exception e) {
            String errorMessage = String.format("计算区域 '%s' (投放类型: %s) 的实际投放量时发生系统错误: %s", 
                target, deliveryEtype, e.getMessage());
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    // ==================== 私有辅助方法 ====================
    /**
     * 将分配矩阵写回数据库（按卷烟覆盖逻辑）
     * 新逻辑：如果表存在，先删除该卷烟的所有记录再插入新数据；如果表不存在，先创建表再插入数据
     */
    private boolean writeBackToDatabase(BigDecimal[][] allocationMatrix, 
                                      List<String> targetList,
                                      String cigCode, 
                                      String cigName,
                                      Integer year, 
                                      Integer month, 
                                      Integer weekSeq,
                                      String deliveryMethod,
                                      String deliveryEtype,
                                      String remark) {
        try {
            // 使用验证工具统一验证所有参数
            DistributionValidationUtils.validateWriteBackParams(
                allocationMatrix, targetList, cigCode, cigName, 
                year, month, weekSeq, deliveryMethod, deliveryEtype);
            
            // 调试日志：检查writeBackToDatabase接收到的参数
            log.debug("writeBackToDatabase - 卷烟: {} - {}, deliveryMethod: {}, deliveryEtype: {}", 
                     cigCode, cigName, deliveryMethod, deliveryEtype);
            
            // 生成动态表名
            String tableName = TableNameGeneratorUtil.generatePredictionTableName(year, month, weekSeq);
            log.debug("写回卷烟预测数据表: {}", tableName);
            
            // 验证表名安全性
            CigaretteDistributionSqlBuilder.validateSqlComponents(tableName, null);
            
            // 第1步：检查表是否存在，不存在则创建
            ensurePredictionTableExists(tableName);
            
            // 第2步：删除该卷烟的所有现有记录（按卷烟覆盖逻辑）
            deleteExistingCigaretteRecords(tableName, cigCode, cigName);
            
            // 第3步：使用GradeMatrixUtils构建所有区域的预测数据记录，用于编码表达式生成
            List<CigaretteDistributionPredictionData> allCigaretteRecords = GradeMatrixUtils.buildPredictionRecords(
                cigCode, cigName, deliveryMethod, deliveryEtype, allocationMatrix, targetList);
            
            // 第4步：使用简单插入SQL语句（不使用ON DUPLICATE KEY UPDATE）
            String insertSql = CigaretteDistributionSqlBuilder.buildSimpleInsertSql(tableName);
            
            // 第5步：为每个目标（区域或业态类型）执行插入
            for (int i = 0; i < targetList.size(); i++) {
                String target = targetList.get(i);
                
                // 严格按照公式计算该区域的实际投放量：∑（档位分配值 × 对应区域客户数档位值）
                BigDecimal actualDelivery;
                try {
                    actualDelivery = calculateActualDeliveryForRegion(target, allocationMatrix[i], deliveryMethod, deliveryEtype, remark);
                } catch (Exception e) {
                    String errorMessage = String.format("卷烟 '%s' 在区域 '%s' (投放类型: %s) 的实际投放量计算失败: %s", 
                        cigName, target, deliveryEtype, e.getMessage());
                    log.error(errorMessage, e);
                    throw new RuntimeException(errorMessage, e);
                }
                
                // 为当前区域生成对应的编码表达式
                String currentAreaEncodedExpression = encodeDecodeService.encodeForSpecificArea(
                    cigCode, cigName, deliveryMethod, deliveryEtype, target, allCigaretteRecords);
                
                // 构建预测数据对象并使用RowMapper生成参数
                CigaretteDistributionPredictionData predictionData = new CigaretteDistributionPredictionData();
                predictionData.setCigCode(cigCode);
                predictionData.setCigName(cigName);
                predictionData.setDeliveryArea(target);
                predictionData.setDeliveryMethod(deliveryMethod);
                predictionData.setDeliveryEtype(deliveryEtype);
                predictionData.setYear(year);
                predictionData.setMonth(month);
                predictionData.setWeekSeq(weekSeq);
                predictionData.setActualDelivery(actualDelivery);
                predictionData.setDeployinfoCode(currentAreaEncodedExpression);
                
                // 对于包含双周上浮关键词的卷烟，保留原始备注；否则使用"算法自动生成"
                if (TableNameGeneratorUtil.checkBiWeeklyFloatFromRemark(remark)) {
                    predictionData.setBz(remark);  // 保留原始的双周上浮备注
                } else {
                    predictionData.setBz("算法自动生成");  // 默认备注
                }
                
                // 使用GradeMatrixUtils设置30个档位值
                GradeMatrixUtils.setGradesToEntity(predictionData, allocationMatrix[i]);
                
                // 使用RowMapper构建参数数组
                Object[] params = CigaretteDistributionRowMapper.buildInsertParams(predictionData);
                
                // 调试日志：检查SQL执行前的关键参数值
                log.debug("SQL执行参数 - target: {}, deliveryMethod: {}, deliveryEtype: {}, encodedExpression: {}", 
                         target, deliveryMethod, deliveryEtype, currentAreaEncodedExpression);
                
                int insertedRows = jdbcTemplate.update(insertSql, params);
                log.debug("目标 {} 的分配矩阵已写入数据库，影响行数: {}", target, insertedRows);
            }
            
            log.info("卷烟 {} 的分配矩阵已成功写回数据库（按卷烟覆盖模式）", cigName);
            return true;
            
        } catch (Exception e) {
            log.error("写回数据库失败，卷烟: {} - {}, deliveryMethod: {}, deliveryEtype: {}, 错误类型: {}, 错误信息: {}", 
                     cigCode, cigName, deliveryMethod, deliveryEtype, e.getClass().getSimpleName(), e.getMessage());
            log.error("详细堆栈信息:", e);
            return false;
        }
    }

    /**
     * 根据投放类型和目标区域获取客户数数组
     * 使用TableNameGeneratorUtil生成动态表名，从region_clientNum表获取数据
     * 严格验证数据获取过程，确保计算基础数据的准确性
     * 
     * @param target 目标区域
     * @param deliveryMethod 投放方法  
     * @param deliveryEtype 扩展投放类型
     * @param remark 备注字段（用于判断是否双周上浮）
     */
    private BigDecimal[] getCustomerCountsForTarget(String target, String deliveryMethod, String deliveryEtype, String remark) {
        if (target == null || target.trim().isEmpty()) {
            throw new IllegalArgumentException("目标区域不能为空");
        }
        
        if (deliveryMethod == null) {
            throw new IllegalArgumentException("投放方法不能为空");
        }
        
        // 对于统一投放（按档位统一投放或按档位投放），deliveryEtype可以为null
        if ("按档位扩展投放".equals(deliveryMethod) && deliveryEtype == null) {
            throw new IllegalArgumentException("扩展投放类型不能为空");
        }
        
        try {
            // 使用TableNameGeneratorUtil生成动态表名，根据备注判断是否双周上浮
            String tableName = TableNameGeneratorUtil.generateRegionClientTableName(deliveryMethod, deliveryEtype, remark);
            log.debug("查询区域客户数表: {} 目标区域: {} (备注: {})", tableName, target, remark);
            
            // 使用RegionClientNumDataService查询数据
            List<org.example.entity.RegionClientNumData> dataList = regionClientNumDataService.findByTableNameAndRegion(tableName, target);
            
            if (dataList.isEmpty()) {
                String errorMessage = String.format("在表 '%s' 中未找到目标区域 '%s' (投放方法: %s, 投放类型: %s) 的客户数数据", 
                    tableName, target, deliveryMethod, deliveryEtype);
                log.error(errorMessage);
                throw new RuntimeException(errorMessage);
            }
            
            if (dataList.size() > 1) {
                log.warn("目标区域 '{}' (投放方法: {}, 投放类型: {}) 在表 '{}' 中存在多条记录，使用第一条", 
                        target, deliveryMethod, deliveryEtype, tableName);
            }
            
            // 获取第一条记录的档位数据
            org.example.entity.RegionClientNumData regionData = dataList.get(0);
            BigDecimal[] customerCounts = regionData.getGradeArray();
            
            // 使用GradeMatrixUtils验证客户数数据的完整性
            GradeMatrixUtils.validateGradeArray(customerCounts, "获取目标区域客户数");
            
            log.debug("成功获取目标区域 '{}' (投放方法: {}, 投放类型: {}) 的客户数数据", target, deliveryMethod, deliveryEtype);
            return customerCounts;
            
        } catch (IllegalArgumentException e) {
            // 重新抛出参数异常
            throw e;
        } catch (RuntimeException e) {
            // 重新抛出运行时异常
            throw e;
        } catch (Exception e) {
            String errorMessage = String.format("获取目标区域 '%s' (投放方法: %s, 投放类型: %s) 的客户数数据时发生系统错误: %s", 
                target, deliveryMethod, deliveryEtype, e.getMessage());
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }
    
    
    // 已使用GradeMatrixUtils.buildPredictionRecords代替原有的buildAllCigaretteRecords方法
    // 已使用RegionClientNumDataService和RegionClientNumData.getGradeArray()代替原有的硬编码表查询
    
    /**
     * 确保预测数据表存在，如果不存在则创建
     * 使用SQL工具类检查表存在性并创建表
     * 
     * @param tableName 预测数据表名
     */
    private void ensurePredictionTableExists(String tableName) {
        try {
            // 检查表是否存在
            String checkTableSql = CigaretteDistributionSqlBuilder.buildCheckTableExistsSql();
            Integer tableExists = jdbcTemplate.queryForObject(checkTableSql, Integer.class, tableName);
            
            if (tableExists == null || tableExists == 0) {
                // 表不存在，创建新表
                String createTableSql = CigaretteDistributionSqlBuilder.buildCreatePredictionTableSql(tableName);
                jdbcTemplate.execute(createTableSql);
                log.info("成功创建预测数据表: {}", tableName);
            } else {
                log.debug("预测数据表已存在: {}", tableName);
            }
            
        } catch (Exception e) {
            String errorMessage = String.format("检查/创建预测数据表 '%s' 时发生错误: %s", tableName, e.getMessage());
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }
    
    /**
     * 删除指定卷烟的所有现有记录（按卷烟覆盖逻辑的第一步）
     * 使用SQL工具类构建删除语句，删除该卷烟在所有区域的分配记录
     * 
     * @param tableName 预测数据表名
     * @param cigCode 卷烟代码
     * @param cigName 卷烟名称
     */
    private void deleteExistingCigaretteRecords(String tableName, String cigCode, String cigName) {
        try {
            // 使用SQL工具类构建按卷烟删除的SQL语句
            String deleteSql = CigaretteDistributionSqlBuilder.buildDeleteCigaretteAllRecordsSql(tableName);
            int deletedCount = jdbcTemplate.update(deleteSql, cigCode, cigName);
            
            if (deletedCount > 0) {
                log.info("删除卷烟 {} - {} 的 {} 条现有记录", cigCode, cigName, deletedCount);
            } else {
                log.debug("卷烟 {} - {} 没有现有记录需要删除", cigCode, cigName);
            }
            
        } catch (Exception e) {
            String errorMessage = String.format("删除卷烟 '%s - %s' 现有记录时发生错误: %s", cigCode, cigName, e.getMessage());
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }
    
    /**
     * 按卷烟代码+名称分组计算总实际投放量
     */
    @Override
    public Map<String, BigDecimal> calculateTotalActualDeliveryByTobacco(List<CigaretteDistributionPredictionData> rawDataList) {
        Map<String, BigDecimal> totalActualDeliveryMap = new HashMap<>();
        
        try {
            // 按卷烟代码+名称分组
            Map<String, List<CigaretteDistributionPredictionData>> groupedByTobacco = new HashMap<>();
            for (CigaretteDistributionPredictionData data : rawDataList) {
                String tobaccoKey = data.getCigCode() + "_" + data.getCigName();
                groupedByTobacco.computeIfAbsent(tobaccoKey, k -> new ArrayList<>()).add(data);
            }
            
            // 计算每个卷烟的总实际投放量
            for (Map.Entry<String, List<CigaretteDistributionPredictionData>> entry : groupedByTobacco.entrySet()) {
                String tobaccoKey = entry.getKey();
                List<CigaretteDistributionPredictionData> tobaccoRecords = entry.getValue();
                
                BigDecimal totalActualDelivery = BigDecimal.ZERO;
                
                // 直接从数据库记录中累加各区域的ACTUAL_DELIVERY字段
                for (CigaretteDistributionPredictionData data : tobaccoRecords) {
                    if (data.getActualDelivery() != null) {
                        totalActualDelivery = totalActualDelivery.add(data.getActualDelivery());
                        log.debug("卷烟 {} 区域 {} 实际投放量: {}", data.getCigName(), data.getDeliveryArea(), data.getActualDelivery());
                    } else {
                        log.warn("卷烟 {} 区域 {} 的ACTUAL_DELIVERY字段为null", data.getCigName(), data.getDeliveryArea());
                    }
                }
                
                totalActualDeliveryMap.put(tobaccoKey, totalActualDelivery);
                log.debug("卷烟 {} 总实际投放量: {} (包含 {} 个区域)", 
                         tobaccoKey.split("_")[1], totalActualDelivery, tobaccoRecords.size());
            }
            
            log.debug("完成总实际投放量计算，共处理 {} 种卷烟", totalActualDeliveryMap.size());
            
        } catch (Exception e) {
            log.error("计算总实际投放量失败", e);
        }
        
        return totalActualDeliveryMap;
    }
}
