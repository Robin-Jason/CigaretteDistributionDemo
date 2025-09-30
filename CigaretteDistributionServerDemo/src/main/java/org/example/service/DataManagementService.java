package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.dto.DeleteAreasRequestDto;
import org.example.dto.UpdateCigaretteRequestDto;
import org.example.entity.DemoTestAdvData;
import org.example.entity.DemoTestData;
import org.example.repository.DemoTestAdvDataRepository;
import org.example.repository.DemoTestDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * 数据管理服务类
 * 负责卷烟分配数据的更新、删除、查询等管理操作
 */
@Slf4j
@Service
public class DataManagementService {
    
    @Autowired
    private DemoTestAdvDataRepository advDataRepository;
    
    @Autowired
    private DemoTestDataRepository testDataRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private DistributionCalculateService distributionCalculateService;
    
    @Autowired
    private EncodeDecodeService encodeDecodeService;

    // ==================== 更新档位投放设置和新增区域投放档位记录已经删除卷烟指定区域投放记录的服务 ====================
    
    /**
     * 根据编码表达式批量更新卷烟信息
     * 统一处理多条编码表达式，按照原有的DataManagementService更新逻辑进行处理
     * 
     * @param cigCode 卷烟代码
     * @param cigName 卷烟名称
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @param encodedExpressions 编码表达式列表
     * @param remark 备注
     * @return 操作结果Map，包含成功状态、消息和相关统计信息
     */
    @Transactional
    public Map<String, Object> batchUpdateFromEncodedExpressions(String cigCode, String cigName, 
                                                                Integer year, Integer month, Integer weekSeq,
                                                                List<String> encodedExpressions, String remark) {
        log.info("数据管理：开始根据编码表达式批量更新卷烟信息，卷烟: {} - {}, 表达式数量: {}", 
                cigCode, cigName, encodedExpressions.size());
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 解析所有编码表达式
            List<EncodeDecodeService.ParsedExpressionData> allParsedData = new ArrayList<>();
            for (String expression : encodedExpressions) {
                EncodeDecodeService.ParsedExpressionData parsed = encodeDecodeService.parseEncodedExpression(expression);
                allParsedData.add(parsed);
            }
            
            // 2. 验证所有表达式的投放类型组合是否一致
            if (!validateConsistentDeliveryTypes(allParsedData)) {
                result.put("success", false);
                result.put("message", "编码表达式中的投放类型不一致，无法批量更新");
                return result;
            }
            
            // 3. 获取第一个表达式的投放类型作为目标投放类型
            EncodeDecodeService.ParsedExpressionData firstParsed = allParsedData.get(0);
            String targetDeliveryMethod = firstParsed.getDeliveryMethod();
            String targetDeliveryEtype = firstParsed.getDeliveryEtype();
            
            // 4. 检查demo_test_ADVdata表中的现有记录
            DemoTestAdvData existingAdvData = advDataRepository.findByCigCodeAndCigName(cigCode, cigName);
            if (existingAdvData == null) {
                result.put("success", false);
                result.put("message", "卷烟在预投放量表中不存在");
                return result;
            }
            
            // 5. 检查投放类型是否发生变化
            String currentDeliveryMethod = existingAdvData.getDeliveryMethod();
            String currentDeliveryEtype = existingAdvData.getDeliveryEtype();
            
            boolean deliveryTypeChanged = !Objects.equals(currentDeliveryMethod, targetDeliveryMethod) || 
                                        !Objects.equals(currentDeliveryEtype, targetDeliveryEtype);
            
            log.info("投放类型变更检查 - 原: [{}, {}], 新: [{}, {}], 是否变更: {}", 
                    currentDeliveryMethod, currentDeliveryEtype, 
                    targetDeliveryMethod, targetDeliveryEtype, deliveryTypeChanged);
            
            if (deliveryTypeChanged) {
                // 6a. 投放类型改变：删除旧记录，重建新记录
                result = handleBatchDeliveryTypeChange(cigCode, cigName, year, month, weekSeq, 
                                                     allParsedData, remark);
            } else {
                // 6b. 投放类型未改变：增量更新
                result = handleBatchIncrementalUpdate(cigCode, cigName, year, month, weekSeq, 
                                                    allParsedData, remark);
            }
            
        } catch (Exception e) {
            log.error("根据编码表达式批量更新卷烟信息时发生错误", e);
            result.put("success", false);
            result.put("message", "批量更新过程中发生错误: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 方法1：更新卷烟信息（新逻辑：根据投放类型是否改变采用不同策略）
     * 根据投放类型是否变更采用删除重建或增量更新策略，确保数据一致性
     * 
     * @param request 更新卷烟请求DTO，包含卷烟代码、名称、投放类型、档位分配等信息
     * @return 操作结果Map，包含成功状态、消息和相关统计信息
     */
    @Transactional
    public Map<String, Object> updateCigaretteInfo(UpdateCigaretteRequestDto request) {
        log.info("数据管理：开始修改卷烟信息，卷烟代码: {}, 卷烟名称: {}", request.getCigCode(), request.getCigName());
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 检查demo_test_ADVdata表中的现有记录
            DemoTestAdvData existingAdvData = advDataRepository.findByCigCodeAndCigName(
                request.getCigCode(), request.getCigName());
            
            if (existingAdvData == null) {
                result.put("success", false);
                result.put("message", "卷烟在预投放量表中不存在");
                return result;
            }
            
            // 2. 检查投放类型是否发生变化
            String currentDeliveryMethod = existingAdvData.getDeliveryMethod();
            String currentDeliveryEtype = existingAdvData.getDeliveryEtype();
            String newDeliveryMethod = request.getDeliveryMethod();
            String newDeliveryEtype = request.getDeliveryEtype();
            
            boolean deliveryTypeChanged = !Objects.equals(currentDeliveryMethod, newDeliveryMethod) || 
                                        !Objects.equals(currentDeliveryEtype, newDeliveryEtype);
            
            log.info("投放类型变更检查 - 原: [{}, {}], 新: [{}, {}], 是否变更: {}", 
                    currentDeliveryMethod, currentDeliveryEtype, 
                    newDeliveryMethod, newDeliveryEtype, deliveryTypeChanged);
            
            if (deliveryTypeChanged) {
                // 3a. 投放类型改变：删除旧记录，重建新记录（新记录包含更新后的投放类型）
                result = handleDeliveryTypeChange(request, existingAdvData);
            } else {
                // 3b. 投放类型未改变：增量更新（新增+更新已存在区域）
                result = handleIncrementalUpdate(request, existingAdvData);
            }
            
        } catch (Exception e) {
            log.error("修改卷烟信息时发生错误", e);
            result.put("success", false);
            result.put("message", "修改过程中发生错误: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 方法2：获取指定卷烟在指定时间的所有投放区域
     * 从demo_test_data表中查询指定卷烟在特定时间周期的所有投放区域列表
     * 
     * @param cigCode 卷烟代码
     * @param cigName 卷烟名称
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @return 投放区域名称列表，去重排序后返回
     */
    public List<String> getCurrentDeliveryAreas(String cigCode, String cigName, 
                                               Integer year, Integer month, Integer weekSeq) {
        try {
            List<DemoTestData> records = testDataRepository.findByCigCodeAndCigNameAndYearAndMonthAndWeekSeq(
                cigCode, cigName, year, month, weekSeq);
            
            return records.stream()
                    .map(DemoTestData::getDeliveryArea)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());
            
        } catch (Exception e) {
            log.error("获取当前投放区域失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 方法3：删除指定卷烟的投放区域记录
     * 从demo_test_data表中删除指定卷烟在特定时间周期的指定区域投放记录
     * 
     * @param request 删除区域请求DTO，包含卷烟信息、时间信息和待删除区域列表
     * @return 操作结果Map，包含成功状态、删除记录数和消息信息
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> deleteDeliveryAreas(DeleteAreasRequestDto request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("开始删除投放区域，卷烟: {}-{}, 删除区域: {}", 
                    request.getCigName(), request.getCigCode(), request.getAreasToDelete());
            
            int deletedCount = 0;
            
            // 删除指定区域的记录
            for (String areaToDelete : request.getAreasToDelete()) {
                List<DemoTestData> recordsToDelete = testDataRepository.findByYearAndMonthAndWeekSeqAndCigCodeAndDeliveryAreaIn(
                        request.getYear(), request.getMonth(), request.getWeekSeq(), 
                        request.getCigCode(), Collections.singletonList(areaToDelete));
                
                if (!recordsToDelete.isEmpty()) {
                    testDataRepository.deleteAll(recordsToDelete);
                    deletedCount += recordsToDelete.size();
                    log.info("删除区域 {} 的{}条记录", areaToDelete, recordsToDelete.size());
                }
            }
            
            result.put("success", true);
            result.put("deletedCount", deletedCount);
            result.put("message", String.format("成功删除%d条记录", deletedCount));
            
        } catch (Exception e) {
            log.error("删除投放区域失败", e);
            result.put("success", false);
            result.put("message", "删除失败: " + e.getMessage());
        }
        
        return result;
    }

    // ==================== 私有辅助方法 ====================
    
    /**
     * 处理投放类型改变的情况：删除旧记录，重建新记录（新记录包含更新后的投放类型）
     */
    private Map<String, Object> handleDeliveryTypeChange(UpdateCigaretteRequestDto request, DemoTestAdvData existingAdvData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("投放类型发生变更，执行删除重建操作");
            
            // 1. 删除demo_test_data表中该卷烟该日期的所有投放记录
            int deletedCount = testDataRepository.deleteByCigCodeAndCigNameAndYearAndMonthAndWeekSeq(
                request.getCigCode(), request.getCigName(), 
                request.getYear(), request.getMonth(), request.getWeekSeq());
            
            log.info("已删除 {} 条旧投放记录", deletedCount);
            
            // 2. 根据新的投放类型重建投放记录
            boolean rebuildSuccess = rebuildDeliveryRecords(request);
            
            log.info("已重建投放记录，新投放类型: [{}, {}]", request.getDeliveryMethod(), request.getDeliveryEtype());
            
            if (rebuildSuccess) {
                result.put("success", true);
                result.put("message", "投放类型变更完成，已重建投放记录");
                result.put("operation", "投放类型变更");
                result.put("deletedRecords", deletedCount);
            } else {
                result.put("success", false);
                result.put("message", "重建投放记录失败");
            }
            
        } catch (Exception e) {
            log.error("处理投放类型变更时发生错误", e);
            result.put("success", false);
            result.put("message", "投放类型变更失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 根据新的投放类型重建投放记录
     */
    private boolean rebuildDeliveryRecords(UpdateCigaretteRequestDto request) {
        try {
            // 解析投放区域
            List<String> areas = parseDeliveryAreas(request.getDeliveryArea(), request.getDeliveryEtype());
            
            // 为每个区域创建新记录
            for (String area : areas) {
                boolean success = addNewAreaRecord(request, area);
                if (!success) {
                    log.error("重建区域 {} 的记录失败", area);
                    return false;
                }
            }
            
            log.info("成功重建 {} 个区域的投放记录", areas.size());
            return true;
            
        } catch (Exception e) {
            log.error("重建投放记录时发生错误", e);
            return false;
        }
    }
    
    /**
     * 处理增量更新的情况：投放类型未改变，新增+更新已存在区域
     */
    private Map<String, Object> handleIncrementalUpdate(UpdateCigaretteRequestDto request, DemoTestAdvData existingAdvData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("投放类型未变更，执行增量更新操作");
            
            // 1. 获取当前该卷烟该日期的所有投放区域
            List<String> currentAreas = getCurrentDeliveryAreas(
                request.getCigCode(), request.getCigName(), 
                request.getYear(), request.getMonth(), request.getWeekSeq());
            
            // 2. 解析新的投放区域
            List<String> newAreas = parseDeliveryAreas(request.getDeliveryArea(), request.getDeliveryEtype());
            
            // 3. 分别处理新增和更新
            int newCount = 0;
            int updateCount = 0;
            
            for (String area : newAreas) {
                if (currentAreas.contains(area)) {
                    // 更新已存在的区域
                    boolean updateSuccess = updateExistingAreaRecord(request, area);
                    if (updateSuccess) updateCount++;
                } else {
                    // 新增区域记录
                    boolean addSuccess = addNewAreaRecord(request, area);
                    if (addSuccess) newCount++;
                }
            }
            
            result.put("success", true);
            result.put("message", String.format("增量更新完成，新增 %d 个区域，更新 %d 个区域", newCount, updateCount));
            result.put("operation", "增量更新");
            result.put("newAreas", newCount);
            result.put("updatedAreas", updateCount);
            
        } catch (Exception e) {
            log.error("处理增量更新时发生错误", e);
            result.put("success", false);
            result.put("message", "增量更新失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 解析投放区域字符串，根据投放类型返回区域列表（解析前端更新时返回的区域列表，格式会被逗号分隔）
     */
    private List<String> parseDeliveryAreas(String deliveryArea, String deliveryEtype) {
        if (deliveryArea == null || deliveryArea.trim().isEmpty()) {
            return Arrays.asList();
        }
        
        // 根据投放类型决定解析方式
        if ("NULL".equals(deliveryEtype) || deliveryEtype == null) {
            // 全市统一投放
            return Arrays.asList("全市");
        } else if (deliveryArea.contains(",")) {
            // 多个区域，用逗号分隔
            return Arrays.asList(deliveryArea.split(","));
        } else {
            // 单个区域
            return Arrays.asList(deliveryArea);
        }
    }
    
    /**
     * 更新已存在区域的记录
     */
    private boolean updateExistingAreaRecord(UpdateCigaretteRequestDto request, String area) {
        try {
            // 构建档位数据
            Map<String, BigDecimal> gradeData = buildGradeData(request.getDistribution());
            
            // 计算该区域的实际投放量
            BigDecimal actualDelivery = distributionCalculateService.calculateActualDeliveryForRegion(area, 
                request.getDistribution().toArray(new BigDecimal[0]), request.getDeliveryEtype());
            
            // 生成编码表达 - 基于当前区域的数据生成简化编码  
            String encodedExpression = generateSingleAreaEncodedExpression(request, area);
            
            // 更新记录
            String updateSql = "UPDATE demo_test_data SET " +
                "delivery_method=?, delivery_etype=?, " +
                "d30=?, d29=?, d28=?, d27=?, d26=?, d25=?, d24=?, d23=?, d22=?, d21=?, " +
                "d20=?, d19=?, d18=?, d17=?, d16=?, d15=?, d14=?, d13=?, d12=?, d11=?, " +
                "d10=?, d9=?, d8=?, d7=?, d6=?, d5=?, d4=?, d3=?, d2=?, d1=?, " +
                "bz=?, actual_delivery=?, deployinfo_code=? " +
                "WHERE cig_code=? AND cig_name=? AND delivery_area=? AND year=? AND month=? AND week_seq=?";
            
            List<Object> params = new ArrayList<>();
            // 添加投放类型参数
            params.add(request.getDeliveryMethod());
            params.add(request.getDeliveryEtype());
            // 添加30个档位参数
            for (int i = 30; i >= 1; i--) {
                params.add(gradeData.get("d" + i));
            }
            params.add(request.getRemark());
            params.add(actualDelivery);
            params.add(encodedExpression);
            params.add(request.getCigCode());
            params.add(request.getCigName());
            params.add(area);
            params.add(request.getYear());
            params.add(request.getMonth());
            params.add(request.getWeekSeq());
            
            int updatedRows = jdbcTemplate.update(updateSql, params.toArray());
            
            log.debug("更新区域 {} 记录，影响行数: {}", area, updatedRows);
            return updatedRows > 0;
            
        } catch (Exception e) {
            log.error("更新区域 {} 记录时发生错误", area, e);
            return false;
        }
    }
    
    /**
     * 新增区域记录
     */
    private boolean addNewAreaRecord(UpdateCigaretteRequestDto request, String area) {
        try {
            // 构建档位数据
            Map<String, BigDecimal> gradeData = buildGradeData(request.getDistribution());
            
            // 计算该区域的实际投放量
            BigDecimal actualDelivery = distributionCalculateService.calculateActualDeliveryForRegion(area, 
                request.getDistribution().toArray(new BigDecimal[0]), request.getDeliveryEtype());
            
            // 生成编码表达 - 基于当前区域的数据生成简化编码
            String encodedExpression = generateSingleAreaEncodedExpression(request, area);
            
            // 插入新记录
            String insertSql = "INSERT INTO demo_test_data (" +
                "cig_code, cig_name, delivery_area, delivery_method, delivery_etype, year, month, week_seq, " +
                "d30, d29, d28, d27, d26, d25, d24, d23, d22, d21, " +
                "d20, d19, d18, d17, d16, d15, d14, d13, d12, d11, " +
                "d10, d9, d8, d7, d6, d5, d4, d3, d2, d1, " +
                "bz, actual_delivery, deployinfo_code) VALUES (" +
                "?, ?, ?, ?, ?, ?, ?, ?, " +
                "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                "?, ?, ?)";
            
            List<Object> params = new ArrayList<>();
            params.add(request.getCigCode());
            params.add(request.getCigName());
            params.add(area);
            params.add(request.getDeliveryMethod());
            params.add(request.getDeliveryEtype());
            params.add(request.getYear());
            params.add(request.getMonth());
            params.add(request.getWeekSeq());
            
            // 添加30个档位参数
            for (int i = 30; i >= 1; i--) {
                params.add(gradeData.get("d" + i));
            }
            params.add(request.getRemark());
            params.add(actualDelivery);
            params.add(encodedExpression);
            
            int insertedRows = jdbcTemplate.update(insertSql, params.toArray());
            
            log.debug("新增区域 {} 记录，影响行数: {}", area, insertedRows);
            return insertedRows > 0;
            
        } catch (Exception e) {
            log.error("新增区域 {} 记录时发生错误", area, e);
            return false;
        }
    }
    
    /**
     * 构建档位数据Map
     * 修正档位映射：前端传入distribution[0] = D30, distribution[29] = D1
     */
    private Map<String, BigDecimal> buildGradeData(List<BigDecimal> distribution) {
        Map<String, BigDecimal> gradeData = new HashMap<>();
        
        // 确保有30个档位的数据，正确映射D30到D1
        for (int i = 1; i <= 30; i++) {
            BigDecimal value = BigDecimal.ZERO;
            if (distribution != null && distribution.size() >= 30) {
                // 正确的映射：D30在索引0，D1在索引29
                // d30 = distribution.get(0), d29 = distribution.get(1), ..., d1 = distribution.get(29)
                value = distribution.get(30 - i);
                if (value == null) value = BigDecimal.ZERO;
            }
            gradeData.put("d" + i, value);
        }
        
        return gradeData;
    }
    
    /**
     * 为单个区域生成编码表达
     * 注意：这是简化版本，仅基于当前区域的数据，完整的编码应该考虑所有区域
     */
    private String generateSingleAreaEncodedExpression(UpdateCigaretteRequestDto request, String area) {
        try {
            // 构建单个区域的DemoTestData对象
            DemoTestData record = new DemoTestData();
            record.setCigCode(request.getCigCode());
            record.setCigName(request.getCigName());
            record.setDeliveryArea(area);
            record.setDeliveryMethod(request.getDeliveryMethod());
            record.setDeliveryEtype(request.getDeliveryEtype());
            
            // 设置30个档位的分配值
            List<BigDecimal> distribution = request.getDistribution();
            if (distribution != null && distribution.size() >= 30) {
                record.setD30(distribution.get(0));
                record.setD29(distribution.get(1));
                record.setD28(distribution.get(2));
                record.setD27(distribution.get(3));
                record.setD26(distribution.get(4));
                record.setD25(distribution.get(5));
                record.setD24(distribution.get(6));
                record.setD23(distribution.get(7));
                record.setD22(distribution.get(8));
                record.setD21(distribution.get(9));
                record.setD20(distribution.get(10));
                record.setD19(distribution.get(11));
                record.setD18(distribution.get(12));
                record.setD17(distribution.get(13));
                record.setD16(distribution.get(14));
                record.setD15(distribution.get(15));
                record.setD14(distribution.get(16));
                record.setD13(distribution.get(17));
                record.setD12(distribution.get(18));
                record.setD11(distribution.get(19));
                record.setD10(distribution.get(20));
                record.setD9(distribution.get(21));
                record.setD8(distribution.get(22));
                record.setD7(distribution.get(23));
                record.setD6(distribution.get(24));
                record.setD5(distribution.get(25));
                record.setD4(distribution.get(26));
                record.setD3(distribution.get(27));
                record.setD2(distribution.get(28));
                record.setD1(distribution.get(29));
            }
            
            // 调用EncodeDecodeService生成编码表达（单区域版本）
            List<DemoTestData> cigaretteRecords = Arrays.asList(record);
            String encodedExpression = encodeDecodeService.encodeToString(
                request.getCigCode(), 
                request.getCigName(),
                request.getDeliveryMethod(),
                request.getDeliveryEtype(),
                cigaretteRecords
            );
            
            log.debug("单区域编码生成：{} - {} - {} -> {}", request.getCigCode(), request.getCigName(), area, encodedExpression);
            
            return encodedExpression;
            
        } catch (Exception e) {
            log.error("生成单区域编码表达失败，卷烟: {} - {}, 区域: {}", 
                     request.getCigCode(), request.getCigName(), area, e);
            return ""; // 编码失败时返回空字符串
        }
    }
    
    // ==================== 批量更新辅助方法 ====================
    
    /**
     * 验证所有解析的表达式是否具有一致的投放类型组合
     */
    private boolean validateConsistentDeliveryTypes(List<EncodeDecodeService.ParsedExpressionData> allParsedData) {
        if (allParsedData.isEmpty()) {
            return false;
        }
        
        String firstDeliveryMethod = allParsedData.get(0).getDeliveryMethod();
        String firstDeliveryEtype = allParsedData.get(0).getDeliveryEtype();
        
        for (EncodeDecodeService.ParsedExpressionData parsed : allParsedData) {
            if (!Objects.equals(firstDeliveryMethod, parsed.getDeliveryMethod()) ||
                !Objects.equals(firstDeliveryEtype, parsed.getDeliveryEtype())) {
                log.warn("编码表达式投放类型不一致：期望[{}, {}]，实际[{}, {}]", 
                        firstDeliveryMethod, firstDeliveryEtype,
                        parsed.getDeliveryMethod(), parsed.getDeliveryEtype());
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 处理投放类型改变的批量更新：删除旧记录，重建新记录
     */
    private Map<String, Object> handleBatchDeliveryTypeChange(String cigCode, String cigName, 
                                                             Integer year, Integer month, Integer weekSeq,
                                                             List<EncodeDecodeService.ParsedExpressionData> allParsedData, 
                                                             String remark) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("投放类型发生变更，执行批量删除重建操作");
            
            // 1. 删除demo_test_data表中该卷烟该日期的所有投放记录
            int deletedCount = testDataRepository.deleteByCigCodeAndCigNameAndYearAndMonthAndWeekSeq(
                cigCode, cigName, year, month, weekSeq);
            
            log.info("已删除 {} 条旧投放记录", deletedCount);
            
            // 2. 根据新的编码表达式重建投放记录
            int totalCreatedRecords = 0;
            for (EncodeDecodeService.ParsedExpressionData parsedData : allParsedData) {
                for (String deliveryArea : parsedData.getDeliveryAreas()) {
                    boolean success = createNewRecordFromParsedData(cigCode, cigName, year, month, weekSeq,
                                                                   parsedData, deliveryArea, remark);
                    if (success) {
                        totalCreatedRecords++;
                    } else {
                        log.error("重建区域 {} 的记录失败", deliveryArea);
                    }
                }
            }
            
            log.info("已重建 {} 条投放记录，新投放类型: [{}, {}]", 
                    totalCreatedRecords, allParsedData.get(0).getDeliveryMethod(), allParsedData.get(0).getDeliveryEtype());
            
            result.put("success", true);
            result.put("message", "投放类型变更完成，已重建投放记录");
            result.put("operation", "投放类型变更");
            result.put("deletedRecords", deletedCount);
            result.put("createdRecords", totalCreatedRecords);
            
        } catch (Exception e) {
            log.error("处理投放类型变更时发生错误", e);
            result.put("success", false);
            result.put("message", "投放类型变更失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 处理投放类型不变的批量增量更新：新增、更新、删除区域记录
     */
    private Map<String, Object> handleBatchIncrementalUpdate(String cigCode, String cigName, 
                                                           Integer year, Integer month, Integer weekSeq,
                                                           List<EncodeDecodeService.ParsedExpressionData> allParsedData, 
                                                           String remark) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("投放类型未变更，执行批量增量更新操作");
            
            // 1. 获取当前该卷烟该日期的所有投放区域
            List<String> currentAreas = getCurrentDeliveryAreas(cigCode, cigName, year, month, weekSeq);
            
            // 2. 收集所有编码表达式中的区域
            Set<String> newAreasSet = new HashSet<>();
            Map<String, EncodeDecodeService.ParsedExpressionData> areaToDataMap = new HashMap<>();
            
            for (EncodeDecodeService.ParsedExpressionData parsedData : allParsedData) {
                for (String deliveryArea : parsedData.getDeliveryAreas()) {
                    newAreasSet.add(deliveryArea);
                    areaToDataMap.put(deliveryArea, parsedData);
                }
            }
            
            List<String> newAreas = new ArrayList<>(newAreasSet);
            
            // 3. 分别处理新增、更新和删除
            int newCount = 0;
            int updateCount = 0;
            int deleteCount = 0;
            
            // 3.1 处理新增和更新
            for (String area : newAreas) {
                EncodeDecodeService.ParsedExpressionData parsedData = areaToDataMap.get(area);
                
                if (currentAreas.contains(area)) {
                    // 更新已存在的区域
                    boolean updateSuccess = updateExistingRecordFromParsedData(cigCode, cigName, year, month, weekSeq,
                                                                              parsedData, area, remark);
                    if (updateSuccess) updateCount++;
                } else {
                    // 新增区域记录
                    boolean addSuccess = createNewRecordFromParsedData(cigCode, cigName, year, month, weekSeq,
                                                                      parsedData, area, remark);
                    if (addSuccess) newCount++;
                }
            }
            
            // 3.2 删除表达式不存在而数据库存在的旧区域记录
            List<String> areasToDelete = currentAreas.stream()
                    .filter(area -> !newAreas.contains(area))
                    .collect(java.util.stream.Collectors.toList());
            
            for (String areaToDelete : areasToDelete) {
                List<DemoTestData> recordsToDelete = testDataRepository.findByYearAndMonthAndWeekSeqAndCigCodeAndDeliveryAreaIn(
                        year, month, weekSeq, cigCode, Collections.singletonList(areaToDelete));
                
                if (!recordsToDelete.isEmpty()) {
                    testDataRepository.deleteAll(recordsToDelete);
                    deleteCount += recordsToDelete.size();
                    log.info("删除旧区域 {} 的{}条记录", areaToDelete, recordsToDelete.size());
                }
            }
            
            result.put("success", true);
            result.put("message", String.format("增量更新完成，新增 %d 个区域，更新 %d 个区域，删除 %d 个区域", newCount, updateCount, deleteCount));
            result.put("operation", "增量更新");
            result.put("newAreas", newCount);
            result.put("updatedAreas", updateCount);
            result.put("deletedAreas", deleteCount);
            
        } catch (Exception e) {
            log.error("处理增量更新时发生错误", e);
            result.put("success", false);
            result.put("message", "增量更新失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 根据解析的表达式数据创建新的投放记录
     */
    private boolean createNewRecordFromParsedData(String cigCode, String cigName, 
                                                 Integer year, Integer month, Integer weekSeq,
                                                 EncodeDecodeService.ParsedExpressionData parsedData, 
                                                 String deliveryArea, String remark) {
        try {
            // 构建档位数据
            Map<String, BigDecimal> gradeData = buildGradeDataFromArray(parsedData.getGradeAllocations());
            
            // 计算该区域的实际投放量
            BigDecimal actualDelivery = distributionCalculateService.calculateActualDeliveryForRegion(deliveryArea, 
                parsedData.getGradeAllocations(), parsedData.getDeliveryEtype());
            
            // 生成编码表达
            String encodedExpression = generateEncodedExpressionFromParsedData(cigCode, cigName, parsedData, deliveryArea);
            
            // 插入新记录
            String insertSql = "INSERT INTO demo_test_data (" +
                "cig_code, cig_name, delivery_area, delivery_method, delivery_etype, year, month, week_seq, " +
                "d30, d29, d28, d27, d26, d25, d24, d23, d22, d21, " +
                "d20, d19, d18, d17, d16, d15, d14, d13, d12, d11, " +
                "d10, d9, d8, d7, d6, d5, d4, d3, d2, d1, " +
                "bz, actual_delivery, deployinfo_code) VALUES (" +
                "?, ?, ?, ?, ?, ?, ?, ?, " +
                "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                "?, ?, ?)";
            
            List<Object> params = new ArrayList<>();
            params.add(cigCode);
            params.add(cigName);
            params.add(deliveryArea);
            params.add(parsedData.getDeliveryMethod());
            params.add(parsedData.getDeliveryEtype());
            params.add(year);
            params.add(month);
            params.add(weekSeq);
            
            // 添加30个档位参数
            for (int i = 30; i >= 1; i--) {
                params.add(gradeData.get("d" + i));
            }
            params.add(remark);
            params.add(actualDelivery);
            params.add(encodedExpression);
            
            int insertedRows = jdbcTemplate.update(insertSql, params.toArray());
            
            log.debug("新增区域 {} 记录，影响行数: {}", deliveryArea, insertedRows);
            return insertedRows > 0;
            
        } catch (Exception e) {
            log.error("新增区域 {} 记录时发生错误", deliveryArea, e);
            return false;
        }
    }
    
    /**
     * 根据解析的表达式数据更新已存在的投放记录
     */
    private boolean updateExistingRecordFromParsedData(String cigCode, String cigName, 
                                                      Integer year, Integer month, Integer weekSeq,
                                                      EncodeDecodeService.ParsedExpressionData parsedData, 
                                                      String deliveryArea, String remark) {
        try {
            // 构建档位数据
            Map<String, BigDecimal> gradeData = buildGradeDataFromArray(parsedData.getGradeAllocations());
            
            // 计算该区域的实际投放量
            BigDecimal actualDelivery = distributionCalculateService.calculateActualDeliveryForRegion(deliveryArea, 
                parsedData.getGradeAllocations(), parsedData.getDeliveryEtype());
            
            // 生成编码表达
            String encodedExpression = generateEncodedExpressionFromParsedData(cigCode, cigName, parsedData, deliveryArea);
            
            // 更新记录
            String updateSql = "UPDATE demo_test_data SET " +
                "delivery_method=?, delivery_etype=?, " +
                "d30=?, d29=?, d28=?, d27=?, d26=?, d25=?, d24=?, d23=?, d22=?, d21=?, " +
                "d20=?, d19=?, d18=?, d17=?, d16=?, d15=?, d14=?, d13=?, d12=?, d11=?, " +
                "d10=?, d9=?, d8=?, d7=?, d6=?, d5=?, d4=?, d3=?, d2=?, d1=?, " +
                "bz=?, actual_delivery=?, deployinfo_code=? " +
                "WHERE cig_code=? AND cig_name=? AND delivery_area=? AND year=? AND month=? AND week_seq=?";
            
            List<Object> params = new ArrayList<>();
            // 添加投放类型参数
            params.add(parsedData.getDeliveryMethod());
            params.add(parsedData.getDeliveryEtype());
            // 添加30个档位参数
            for (int i = 30; i >= 1; i--) {
                params.add(gradeData.get("d" + i));
            }
            params.add(remark);
            params.add(actualDelivery);
            params.add(encodedExpression);
            params.add(cigCode);
            params.add(cigName);
            params.add(deliveryArea);
            params.add(year);
            params.add(month);
            params.add(weekSeq);
            
            int updatedRows = jdbcTemplate.update(updateSql, params.toArray());
            
            log.debug("更新区域 {} 记录，影响行数: {}", deliveryArea, updatedRows);
            return updatedRows > 0;
            
        } catch (Exception e) {
            log.error("更新区域 {} 记录时发生错误", deliveryArea, e);
            return false;
        }
    }
    
    /**
     * 根据档位分配数组构建档位数据Map
     */
    private Map<String, BigDecimal> buildGradeDataFromArray(BigDecimal[] gradeAllocations) {
        Map<String, BigDecimal> gradeData = new HashMap<>();
        
        // 档位分配数组从D30到D1（索引0到29）
        for (int i = 1; i <= 30; i++) {
            BigDecimal value = BigDecimal.ZERO;
            if (gradeAllocations != null && gradeAllocations.length >= 30) {
                // D30在索引0，D1在索引29
                value = gradeAllocations[30 - i];
                if (value == null) value = BigDecimal.ZERO;
            }
            gradeData.put("d" + i, value);
        }
        
        return gradeData;
    }
    
    /**
     * 根据解析数据生成编码表达
     */
    private String generateEncodedExpressionFromParsedData(String cigCode, String cigName, 
                                                          EncodeDecodeService.ParsedExpressionData parsedData, 
                                                          String deliveryArea) {
        try {
            // 构建单个区域的DemoTestData对象
            DemoTestData record = new DemoTestData();
            record.setCigCode(cigCode);
            record.setCigName(cigName);
            record.setDeliveryArea(deliveryArea);
            record.setDeliveryMethod(parsedData.getDeliveryMethod());
            record.setDeliveryEtype(parsedData.getDeliveryEtype());
            
            // 设置30个档位的分配值
            BigDecimal[] gradeAllocations = parsedData.getGradeAllocations();
            if (gradeAllocations != null && gradeAllocations.length >= 30) {
                record.setD30(gradeAllocations[0]);
                record.setD29(gradeAllocations[1]);
                record.setD28(gradeAllocations[2]);
                record.setD27(gradeAllocations[3]);
                record.setD26(gradeAllocations[4]);
                record.setD25(gradeAllocations[5]);
                record.setD24(gradeAllocations[6]);
                record.setD23(gradeAllocations[7]);
                record.setD22(gradeAllocations[8]);
                record.setD21(gradeAllocations[9]);
                record.setD20(gradeAllocations[10]);
                record.setD19(gradeAllocations[11]);
                record.setD18(gradeAllocations[12]);
                record.setD17(gradeAllocations[13]);
                record.setD16(gradeAllocations[14]);
                record.setD15(gradeAllocations[15]);
                record.setD14(gradeAllocations[16]);
                record.setD13(gradeAllocations[17]);
                record.setD12(gradeAllocations[18]);
                record.setD11(gradeAllocations[19]);
                record.setD10(gradeAllocations[20]);
                record.setD9(gradeAllocations[21]);
                record.setD8(gradeAllocations[22]);
                record.setD7(gradeAllocations[23]);
                record.setD6(gradeAllocations[24]);
                record.setD5(gradeAllocations[25]);
                record.setD4(gradeAllocations[26]);
                record.setD3(gradeAllocations[27]);
                record.setD2(gradeAllocations[28]);
                record.setD1(gradeAllocations[29]);
            }
            
            // 调用EncodeDecodeService生成编码表达
            List<DemoTestData> cigaretteRecords = Arrays.asList(record);
            String encodedExpression = encodeDecodeService.encodeToString(
                cigCode, 
                cigName,
                parsedData.getDeliveryMethod(),
                parsedData.getDeliveryEtype(),
                cigaretteRecords
            );
            
            log.debug("生成编码表达：{} - {} - {} -> {}", cigCode, cigName, deliveryArea, encodedExpression);
            
            return encodedExpression;
            
        } catch (Exception e) {
            log.error("生成编码表达失败，卷烟: {} - {}, 区域: {}", cigCode, cigName, deliveryArea, e);
            return ""; // 编码失败时返回空字符串
        }
    }

}
