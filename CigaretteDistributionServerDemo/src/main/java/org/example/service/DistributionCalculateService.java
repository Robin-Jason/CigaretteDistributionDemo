package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.service.BusinessFormatDIstribution.BussinessFormatDistributionService;
import org.example.service.CityUnifiedDistribution.CityPredictionService;
import org.example.service.CountyDistribution.CountyCigaretteDistributionService;
import org.example.service.MarketTypeDistribution.MarketPredictService;
import org.example.service.UrbanRuralClassificationCodeDIstribution.UrbanRuralClassificationCodeDistributionService;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * 分配计算服务
 * 负责算法计算和分配矩阵写回等核心计算功能
 */
@Slf4j
@Service
public class DistributionCalculateService {
    
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private UrbanRuralClassificationCodeDistributionService urbanRuralService;
    
    @Autowired
    private BussinessFormatDistributionService businessFormatService;
    
    @Autowired
    private CountyCigaretteDistributionService countyService;
    
    @Autowired
    private MarketPredictService marketService;
    
    @Autowired
    private CityPredictionService cityService;

    
    // ==================== 一键生成分配方案并写回数据库服务 ====================
    
    /**
     * 方法1：获取算法输出的分配矩阵并写回数据库
     * 协调各投放类型算法计算分配矩阵，计算实际投放量并写入demo_test_data表
     * 
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @return 写回结果Map，包含成功状态、统计信息和详细的分配结果列表
     */
    public Map<String, Object> getAndwriteBackAllocationMatrix(Integer year, Integer month, Integer weekSeq) {
        log.info("协调器：开始将分配矩阵写回数据库，年份: {}, 月份: {}, 周序号: {}", year, month, weekSeq);
        
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> writeBackResults = new ArrayList<>();
        
        try {
            // 获取指定周期的预投放量数据
            String advDataSql = "SELECT cig_code, cig_name, adv, delivery_area, delivery_method, delivery_etype, year, month, week_seq " +
                               "FROM demo_test_advdata WHERE year = ? AND month = ? AND week_seq = ?";
            List<Map<String, Object>> advDataList = jdbcTemplate.queryForList(advDataSql, year, month, weekSeq);
            log.info("获取{}年{}月第{}周的预投放量数据数量: {}", year, month, weekSeq, advDataList.size());
            
            int successCount = 0;
            int totalCount = 0;
            
            // 处理每个卷烟
            for (Map<String, Object> advData : advDataList) {
                totalCount++;
                String cigCode = (String) advData.get("cig_code");
                String cigName = (String) advData.get("cig_name");
                BigDecimal adv = (BigDecimal) advData.get("adv");
                String deliveryArea = (String) advData.get("delivery_area");
                String deliveryEtype = (String) advData.get("delivery_etype");
                // 从demo_test_ADVdata表中获取对应的日期信息
                // 处理year字段可能是Date类型的情况
                Integer advYear = extractYearFromData(advData.get("year"));
                Integer advMonth = (Integer) advData.get("month");
                Integer advWeekSeq = (Integer) advData.get("week_seq");
                
                Map<String, Object> cigResult = new HashMap<>();
                cigResult.put("cigCode", cigCode);
                cigResult.put("cigName", cigName);
                cigResult.put("adv", adv);
                cigResult.put("deliveryArea", deliveryArea);
                cigResult.put("deliveryEtype", deliveryEtype);
                cigResult.put("advYear", advYear);
                cigResult.put("advMonth", advMonth);
                cigResult.put("advWeekSeq", advWeekSeq);
                    
                try {
                    if (deliveryArea != null && !deliveryArea.trim().isEmpty()) {
                        // 根据投放方式和扩展投放类型委托给对应的服务处理
                        List<String> targetList;
                        BigDecimal[][] allocationMatrix;
                        String deliveryMethod = (String) advData.get("delivery_method");
                        
                        // 按投放方式分类处理
                        if ("按档位统一投放".equals(deliveryMethod)) {
                            // 按档位统一投放：使用城市算法，投放区域固定为"全市"
                            targetList = Arrays.asList("全市");
                            allocationMatrix = cityService.calculateCityDistributionMatrix(targetList, adv);
                            cigResult.put("targetType", "全市统一投放");
                            cigResult.put("algorithm", "CityCigaretteDistributionAlgorithm");
                            
                        } else if ("按档位扩展投放".equals(deliveryMethod)) {
                            // 按档位扩展投放：根据扩展投放类型选择对应算法
                            switch (deliveryEtype) {
                                case "档位+区县":
                                    targetList = countyService.getTargetCountyList(deliveryArea);
                                    allocationMatrix = countyService.calculateDistributionMatrix(targetList, adv);
                                    cigResult.put("targetType", "区县分配");
                                    cigResult.put("algorithm", "countyCigaretteDistributionAlgorithm");
                                    break;
                                    
                                case "档位+市场类型":
                                    targetList = marketService.getTargetMarketList(deliveryArea);
                                    allocationMatrix = marketService.calculateMarketDistributionMatrix(targetList, adv);
                                    cigResult.put("targetType", "市场类型分配");
                                    cigResult.put("algorithm", "MarketProportionalCigaretteDistributionAlgorithm");
                                    break;
                                    
                                case "档位+城乡分类代码":
                                    targetList = urbanRuralService.getTargetRegionList(deliveryArea);
                                    allocationMatrix = urbanRuralService.calculateDistributionMatrix(targetList, adv);
                                    cigResult.put("targetType", "城乡分类代码分配");
                                    cigResult.put("algorithm", "UrbanRuralClassificationCodeDistributionAlgorithm");
                                    break;
                                    
                                case "档位+业态":
                                    targetList = businessFormatService.getTargetBusinessFormatList(deliveryArea);
                                    allocationMatrix = businessFormatService.calculateBusinessFormatDistributionMatrix(targetList, adv);
                                    cigResult.put("targetType", "业态类型分配");
                                    cigResult.put("algorithm", "BussinessFormatDistributionAlgorithm");
                                    break;
                                    
                                default:
                                    log.warn("未知的扩展投放类型: {}", deliveryEtype);
                                    targetList = new ArrayList<>();
                                    allocationMatrix = null;
                                    cigResult.put("targetType", "未知类型");
                                    cigResult.put("algorithm", "无");
                                    break;
                            }
                        } else {
                            log.warn("未知的投放方式: {}", deliveryMethod);
                            targetList = new ArrayList<>();
                            allocationMatrix = null;
                            cigResult.put("targetType", "未知投放方式");
                            cigResult.put("algorithm", "无");
                        }
                        
                        if (!targetList.isEmpty() && allocationMatrix != null) {
                            // 写回数据库，使用demo_test_ADVdata表中的日期信息
                            boolean writeBackSuccess = writeBackToDatabase(allocationMatrix, targetList, 
                                cigCode, cigName, advYear, advMonth, advWeekSeq, deliveryEtype);
                            
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
     * @param deliveryEtype 投放类型，用于确定客户数表
     * @return 计算得出的实际投放量
     */
    public BigDecimal calculateActualDeliveryForRegion(String target, BigDecimal[] allocationRow, String deliveryEtype) {
        if (target == null || target.trim().isEmpty()) {
            throw new IllegalArgumentException("目标区域不能为空");
        }
        
        if (allocationRow == null || allocationRow.length != 30) {
            throw new IllegalArgumentException("档位分配数组必须包含30个档位(D30-D1)");
        }
        
        if (deliveryEtype == null) {
            throw new IllegalArgumentException("投放类型不能为空");
        }
        
        try {
            // 获取目标区域的客户数档位值
            BigDecimal[] customerCounts = getCustomerCountsForTarget(target, deliveryEtype);
            
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
     * 从数据中提取年份（处理Date和Integer类型）
     */
    private Integer extractYearFromData(Object yearData) {
        if (yearData == null) {
            return null;
        }
        
        if (yearData instanceof Integer) {
            return (Integer) yearData;
        } else if (yearData instanceof java.sql.Date) {
            java.sql.Date date = (java.sql.Date) yearData;
            java.time.LocalDate localDate = date.toLocalDate();
            return localDate.getYear();
        } else if (yearData instanceof java.util.Date) {
            java.util.Date date = (java.util.Date) yearData;
            java.time.LocalDate localDate = new java.sql.Date(date.getTime()).toLocalDate();
            return localDate.getYear();
        } else {
            // 尝试转换为字符串再解析
            try {
                return Integer.parseInt(yearData.toString().substring(0, 4));
            } catch (Exception e) {
                log.warn("无法解析年份数据: {}", yearData);
                return null;
            }
        }
    }
    
    /**
     * 将分配矩阵写回数据库（包含实际投放量计算）
     */
    private boolean writeBackToDatabase(BigDecimal[][] allocationMatrix, 
                                      List<String> targetList,
                                      String cigCode, 
                                      String cigName,
                                      Integer year, 
                                      Integer month, 
                                      Integer weekSeq,
                                      String deliveryEtype) {
        try {
            String insertSql = "INSERT INTO demo_test_data (cig_code, cig_name, delivery_area, year, month, week_seq, " +
                "d30, d29, d28, d27, d26, d25, d24, d23, d22, d21, d20, d19, d18, d17, d16, d15, d14, d13, d12, d11, d10, d9, d8, d7, d6, d5, d4, d3, d2, d1, actual_delivery, bz) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "d30=VALUES(d30), d29=VALUES(d29), d28=VALUES(d28), d27=VALUES(d27), d26=VALUES(d26), " +
                "d25=VALUES(d25), d24=VALUES(d24), d23=VALUES(d23), d22=VALUES(d22), d21=VALUES(d21), " +
                "d20=VALUES(d20), d19=VALUES(d19), d18=VALUES(d18), d17=VALUES(d17), d16=VALUES(d16), " +
                "d15=VALUES(d15), d14=VALUES(d14), d13=VALUES(d13), d12=VALUES(d12), d11=VALUES(d11), " +
                "d10=VALUES(d10), d9=VALUES(d9), d8=VALUES(d8), d7=VALUES(d7), d6=VALUES(d6), " +
                "d5=VALUES(d5), d4=VALUES(d4), d3=VALUES(d3), d2=VALUES(d2), d1=VALUES(d1), " +
                "actual_delivery=VALUES(actual_delivery), bz=VALUES(bz)";
            
            // 为每个目标（区域或业态类型）执行插入或更新
            for (int i = 0; i < targetList.size(); i++) {
                String target = targetList.get(i);
                
                // 严格按照公式计算该区域的实际投放量：∑（档位分配值 × 对应区域客户数档位值）
                BigDecimal actualDelivery;
                try {
                    actualDelivery = calculateActualDeliveryForRegion(target, allocationMatrix[i], deliveryEtype);
                } catch (Exception e) {
                    String errorMessage = String.format("卷烟 '%s' 在区域 '%s' (投放类型: %s) 的实际投放量计算失败: %s", 
                        cigName, target, deliveryEtype, e.getMessage());
                    log.error(errorMessage, e);
                    throw new RuntimeException(errorMessage, e);
                }
                
                Object[] params = {
                    cigCode, cigName, target, year, month, weekSeq,
                    allocationMatrix[i][0],  // D30
                    allocationMatrix[i][1],  // D29
                    allocationMatrix[i][2],  // D28
                    allocationMatrix[i][3],  // D27
                    allocationMatrix[i][4],  // D26
                    allocationMatrix[i][5],  // D25
                    allocationMatrix[i][6],  // D24
                    allocationMatrix[i][7],  // D23
                    allocationMatrix[i][8],  // D22
                    allocationMatrix[i][9],  // D21
                    allocationMatrix[i][10], // D20
                    allocationMatrix[i][11], // D19
                    allocationMatrix[i][12], // D18
                    allocationMatrix[i][13], // D17
                    allocationMatrix[i][14], // D16
                    allocationMatrix[i][15], // D15
                    allocationMatrix[i][16], // D14
                    allocationMatrix[i][17], // D13
                    allocationMatrix[i][18], // D12
                    allocationMatrix[i][19], // D11
                    allocationMatrix[i][20], // D10
                    allocationMatrix[i][21], // D9
                    allocationMatrix[i][22], // D8
                    allocationMatrix[i][23], // D7
                    allocationMatrix[i][24], // D6
                    allocationMatrix[i][25], // D5
                    allocationMatrix[i][26], // D4
                    allocationMatrix[i][27], // D3
                    allocationMatrix[i][28], // D2
                    allocationMatrix[i][29], // D1
                    actualDelivery,          // ACTUAL_DELIVERY
                    "算法自动生成"
                };
                
                jdbcTemplate.update(insertSql, params);
                log.debug("目标 {} 的分配矩阵已写入数据库", target);
            }
            
            log.info("卷烟 {} 的分配矩阵已成功写回数据库", cigName);
            return true;
            
        } catch (Exception e) {
            log.error("写回数据库失败，卷烟: {}, 错误: {}", cigName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 根据投放类型和目标区域获取客户数数组
     * 严格验证数据获取过程，确保计算基础数据的准确性
     */
    private BigDecimal[] getCustomerCountsForTarget(String target, String deliveryEtype) {
        if (target == null || target.trim().isEmpty()) {
            throw new IllegalArgumentException("目标区域不能为空");
        }
        
        if (deliveryEtype == null) {
            throw new IllegalArgumentException("投放类型不能为空");
        }
        
        BigDecimal[] customerCounts = new BigDecimal[30];
        String tableName = "";
        String whereClause = "";
        String queryParam = target;
        
        try {
            // 根据投放类型确定客户数表和查询条件
            if ("NULL".equals(deliveryEtype) || deliveryEtype.trim().isEmpty()) {
                // 按档位统一投放：使用城市客户数表
                tableName = "city_clientnum_data";
                whereClause = "urban_rural_code = ?";
                queryParam = "全市";
                
            } else if ("档位+区县".equals(deliveryEtype)) {
                // 档位+区县：使用区县客户数表
                tableName = "demo_test_county_client_numdata";
                whereClause = "county = ?";
                
            } else if ("档位+市场类型".equals(deliveryEtype)) {
                // 档位+市场类型：使用市场客户数表
                tableName = "demo_market_test_clientnumdata";
                whereClause = "urban_rural_code = ?";
                
            } else if ("档位+城乡分类代码".equals(deliveryEtype)) {
                // 档位+城乡分类代码：使用城乡分类客户数表
                tableName = "demo_test_clientNumdata";
                whereClause = "urban_rural_code = ?";
                
            } else if ("档位+业态".equals(deliveryEtype)) {
                // 档位+业态：使用业态客户数表
                tableName = "demo_test_businessFormat_clientNumData";
                whereClause = "BusinessFormat = ?";
                
            } else {
                throw new IllegalArgumentException(String.format("不支持的投放类型: %s", deliveryEtype));
            }
            
            // 构建30档位查询SQL（业态表字段名为大写）
            String sql;
            if ("demo_test_businessFormat_clientNumData".equals(tableName)) {
                sql = String.format(
                    "SELECT D30,D29,D28,D27,D26,D25,D24,D23,D22,D21,D20,D19,D18,D17,D16,D15,D14,D13,D12,D11,D10,D9,D8,D7,D6,D5,D4,D3,D2,D1 " +
                    "FROM %s WHERE %s", tableName, whereClause);
            } else {
                sql = String.format(
                    "SELECT d30,d29,d28,d27,d26,d25,d24,d23,d22,d21,d20,d19,d18,d17,d16,d15,d14,d13,d12,d11,d10,d9,d8,d7,d6,d5,d4,d3,d2,d1 " +
                    "FROM %s WHERE %s", tableName, whereClause);
            }
            
            log.debug("查询客户数SQL: {} 参数: {}", sql, queryParam);
            
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, queryParam);
            
            if (result.isEmpty()) {
                String errorMessage = String.format("在表 '%s' 中未找到目标区域 '%s' (投放类型: %s) 的客户数数据", 
                    tableName, target, deliveryEtype);
                log.error(errorMessage);
                throw new RuntimeException(errorMessage);
            }
            
            if (result.size() > 1) {
                log.warn("目标区域 '{}' 在表 '{}' 中有多条记录，使用第一条", target, tableName);
            }
            
            Map<String, Object> row = result.get(0);
            populateCustomerCounts(customerCounts, row);
            
            // 验证客户数数据的完整性
            validateCustomerCounts(customerCounts, target, deliveryEtype);
            
            log.debug("成功获取目标区域 '{}' (投放类型: {}) 的客户数数据", target, deliveryEtype);
            
        } catch (IllegalArgumentException e) {
            // 重新抛出参数异常
            throw e;
        } catch (RuntimeException e) {
            // 重新抛出运行时异常
            throw e;
        } catch (Exception e) {
            String errorMessage = String.format("获取目标区域 '%s' (投放类型: %s) 的客户数数据时发生系统错误: %s", 
                target, deliveryEtype, e.getMessage());
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
        
        return customerCounts;
    }
    
    /**
     * 验证客户数数据的完整性
     */
    private void validateCustomerCounts(BigDecimal[] customerCounts, String target, String deliveryEtype) {
        if (customerCounts == null || customerCounts.length != 30) {
            throw new RuntimeException(String.format("区域 '%s' (投放类型: %s) 的客户数数组长度不正确", target, deliveryEtype));
        }
        
        boolean hasValidData = false;
        for (BigDecimal count : customerCounts) {
            if (count != null && count.compareTo(BigDecimal.ZERO) > 0) {
                hasValidData = true;
                break;
            }
        }
        
        if (!hasValidData) {
            log.warn("区域 '{}' (投放类型: {}) 的所有档位客户数均为0或null", target, deliveryEtype);
        }
    }
    
    /**
     * 从查询结果填充客户数数组（支持大小写字段名）
     */
    private void populateCustomerCounts(BigDecimal[] customerCounts, Map<String, Object> row) {
        // 尝试小写字段名，如果不存在则尝试大写字段名（用于业态表）
        customerCounts[0] = getBigDecimalFromMap(row, "d30") != null ? getBigDecimalFromMap(row, "d30") : getBigDecimalFromMap(row, "D30");
        customerCounts[1] = getBigDecimalFromMap(row, "d29") != null ? getBigDecimalFromMap(row, "d29") : getBigDecimalFromMap(row, "D29");
        customerCounts[2] = getBigDecimalFromMap(row, "d28") != null ? getBigDecimalFromMap(row, "d28") : getBigDecimalFromMap(row, "D28");
        customerCounts[3] = getBigDecimalFromMap(row, "d27") != null ? getBigDecimalFromMap(row, "d27") : getBigDecimalFromMap(row, "D27");
        customerCounts[4] = getBigDecimalFromMap(row, "d26") != null ? getBigDecimalFromMap(row, "d26") : getBigDecimalFromMap(row, "D26");
        customerCounts[5] = getBigDecimalFromMap(row, "d25") != null ? getBigDecimalFromMap(row, "d25") : getBigDecimalFromMap(row, "D25");
        customerCounts[6] = getBigDecimalFromMap(row, "d24") != null ? getBigDecimalFromMap(row, "d24") : getBigDecimalFromMap(row, "D24");
        customerCounts[7] = getBigDecimalFromMap(row, "d23") != null ? getBigDecimalFromMap(row, "d23") : getBigDecimalFromMap(row, "D23");
        customerCounts[8] = getBigDecimalFromMap(row, "d22") != null ? getBigDecimalFromMap(row, "d22") : getBigDecimalFromMap(row, "D22");
        customerCounts[9] = getBigDecimalFromMap(row, "d21") != null ? getBigDecimalFromMap(row, "d21") : getBigDecimalFromMap(row, "D21");
        customerCounts[10] = getBigDecimalFromMap(row, "d20") != null ? getBigDecimalFromMap(row, "d20") : getBigDecimalFromMap(row, "D20");
        customerCounts[11] = getBigDecimalFromMap(row, "d19") != null ? getBigDecimalFromMap(row, "d19") : getBigDecimalFromMap(row, "D19");
        customerCounts[12] = getBigDecimalFromMap(row, "d18") != null ? getBigDecimalFromMap(row, "d18") : getBigDecimalFromMap(row, "D18");
        customerCounts[13] = getBigDecimalFromMap(row, "d17") != null ? getBigDecimalFromMap(row, "d17") : getBigDecimalFromMap(row, "D17");
        customerCounts[14] = getBigDecimalFromMap(row, "d16") != null ? getBigDecimalFromMap(row, "d16") : getBigDecimalFromMap(row, "D16");
        customerCounts[15] = getBigDecimalFromMap(row, "d15") != null ? getBigDecimalFromMap(row, "d15") : getBigDecimalFromMap(row, "D15");
        customerCounts[16] = getBigDecimalFromMap(row, "d14") != null ? getBigDecimalFromMap(row, "d14") : getBigDecimalFromMap(row, "D14");
        customerCounts[17] = getBigDecimalFromMap(row, "d13") != null ? getBigDecimalFromMap(row, "d13") : getBigDecimalFromMap(row, "D13");
        customerCounts[18] = getBigDecimalFromMap(row, "d12") != null ? getBigDecimalFromMap(row, "d12") : getBigDecimalFromMap(row, "D12");
        customerCounts[19] = getBigDecimalFromMap(row, "d11") != null ? getBigDecimalFromMap(row, "d11") : getBigDecimalFromMap(row, "D11");
        customerCounts[20] = getBigDecimalFromMap(row, "d10") != null ? getBigDecimalFromMap(row, "d10") : getBigDecimalFromMap(row, "D10");
        customerCounts[21] = getBigDecimalFromMap(row, "d9") != null ? getBigDecimalFromMap(row, "d9") : getBigDecimalFromMap(row, "D9");
        customerCounts[22] = getBigDecimalFromMap(row, "d8") != null ? getBigDecimalFromMap(row, "d8") : getBigDecimalFromMap(row, "D8");
        customerCounts[23] = getBigDecimalFromMap(row, "d7") != null ? getBigDecimalFromMap(row, "d7") : getBigDecimalFromMap(row, "D7");
        customerCounts[24] = getBigDecimalFromMap(row, "d6") != null ? getBigDecimalFromMap(row, "d6") : getBigDecimalFromMap(row, "D6");
        customerCounts[25] = getBigDecimalFromMap(row, "d5") != null ? getBigDecimalFromMap(row, "d5") : getBigDecimalFromMap(row, "D5");
        customerCounts[26] = getBigDecimalFromMap(row, "d4") != null ? getBigDecimalFromMap(row, "d4") : getBigDecimalFromMap(row, "D4");
        customerCounts[27] = getBigDecimalFromMap(row, "d3") != null ? getBigDecimalFromMap(row, "d3") : getBigDecimalFromMap(row, "D3");
        customerCounts[28] = getBigDecimalFromMap(row, "d2") != null ? getBigDecimalFromMap(row, "d2") : getBigDecimalFromMap(row, "D2");
        customerCounts[29] = getBigDecimalFromMap(row, "d1") != null ? getBigDecimalFromMap(row, "d1") : getBigDecimalFromMap(row, "D1");
    }
    
    /**
     * 从Map中安全获取BigDecimal值
     */
    private BigDecimal getBigDecimalFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
