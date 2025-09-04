package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.dto.QueryRequestDto;
import org.example.dto.UpdateRequestDto;
import org.example.dto.UpdateCigaretteRequestDto;
import org.example.entity.DemoTestAdvData;
import org.example.entity.DemoTestClientNumData;
import org.example.entity.DemoTestData;
import org.example.repository.DemoTestAdvDataRepository;
import org.example.repository.DemoTestClientNumDataRepository;
import org.example.repository.DemoTestDataRepository;
import org.example.service.CigaretteDistributionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/cigarette")
@Validated
@CrossOrigin(origins = "*")
public class CigaretteDistributionController {
    
    @Autowired
    private CigaretteDistributionService distributionService;
    
    @Autowired
    private DemoTestAdvDataRepository advDataRepository;
    
    @Autowired
    private DemoTestClientNumDataRepository clientNumDataRepository;
    
    @Autowired
    private DemoTestDataRepository testDataRepository;
    
    /**
     * 查询卷烟分配数据 - 合并档位相同但投放区域不同的记录
     */
    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> queryCigaretteDistribution(@Valid @RequestBody QueryRequestDto request) {
        log.info("接收查询请求，年份: {}, 月份: {}, 周序号: {}", request.getYear(), request.getMonth(), request.getWeekSeq());
        
        try {
            // 获取原始数据
            List<DemoTestData> rawDataList = testDataRepository.findByYearAndMonthAndWeekSeq(
                    request.getYear(), request.getMonth(), request.getWeekSeq());
            
            // 合并档位相同但投放区域不同的记录，并添加预投放量和实际投放量
            List<Map<String, Object>> mergedResult = mergeRecordsByAllocationWithAdvAndActual(rawDataList);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "查询成功");
            response.put("data", mergedResult);
            response.put("total", mergedResult.size());
            response.put("originalTotal", rawDataList.size());
            
            log.info("查询成功，原始记录{}条，合并后{}条", rawDataList.size(), mergedResult.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("查询失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 查询原始数据（不合并）
     */
    @PostMapping("/query-raw")
    public ResponseEntity<Map<String, Object>> queryRawData(@Valid @RequestBody QueryRequestDto request) {
        log.info("接收原始数据查询请求，年份: {}, 月份: {}, 周序号: {}", request.getYear(), request.getMonth(), request.getWeekSeq());
        
        try {
            List<DemoTestData> rawDataList = testDataRepository.findByYearAndMonthAndWeekSeq(
                    request.getYear(), request.getMonth(), request.getWeekSeq());
            
            List<Map<String, Object>> result = new ArrayList<>();
            for (DemoTestData data : rawDataList) {
                Map<String, Object> record = new HashMap<>();
                record.put("id", data.getId());
                record.put("cigCode", data.getCigCode());
                record.put("cigName", data.getCigName());
                record.put("deliveryArea", data.getDeliveryArea());
                record.put("year", data.getYear());
                record.put("month", data.getMonth());
                record.put("weekSeq", data.getWeekSeq());
                record.put("remark", data.getBz());
                
                // 添加所有档位数据
                record.put("d30", data.getD30());
                record.put("d29", data.getD29());
                record.put("d28", data.getD28());
                record.put("d27", data.getD27());
                record.put("d26", data.getD26());
                record.put("d25", data.getD25());
                record.put("d24", data.getD24());
                record.put("d23", data.getD23());
                record.put("d22", data.getD22());
                record.put("d21", data.getD21());
                record.put("d20", data.getD20());
                record.put("d19", data.getD19());
                record.put("d18", data.getD18());
                record.put("d17", data.getD17());
                record.put("d16", data.getD16());
                record.put("d15", data.getD15());
                record.put("d14", data.getD14());
                record.put("d13", data.getD13());
                record.put("d12", data.getD12());
                record.put("d11", data.getD11());
                record.put("d10", data.getD10());
                record.put("d9", data.getD9());
                record.put("d8", data.getD8());
                record.put("d7", data.getD7());
                record.put("d6", data.getD6());
                record.put("d5", data.getD5());
                record.put("d4", data.getD4());
                record.put("d3", data.getD3());
                record.put("d2", data.getD2());
                record.put("d1", data.getD1());
                
                result.add(record);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "原始数据查询成功");
            response.put("data", result);
            response.put("total", result.size());
            
            log.info("原始数据查询成功，返回{}条记录", result.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("原始数据查询失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "原始数据查询失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 更新卷烟分配数据 - 将复合区域记录拆分后写回数据库
     */
    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> updateCigaretteDistribution(@Valid @RequestBody UpdateRequestDto request) {
        log.info("接收更新请求，卷烟代码: {}, 卷烟名称: {}, 投放区域: {}", 
                request.getCigCode(), request.getCigName(), request.getDeliveryAreas());
        
        try {
            boolean success = distributionService.updateCigaretteDistributionWithSplit(
                    request.getCigCode(), request.getCigName(), request.getDistribution(), 
                    request.getDeliveryAreas(), request.getYear(), request.getMonth(), request.getWeekSeq());
            
            Map<String, Object> response = new HashMap<>();
            if (success) {
                response.put("success", true);
                response.put("message", "更新成功");
                log.info("更新成功");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "更新失败：未找到对应的卷烟数据");
                log.warn("更新失败：未找到对应的卷烟数据");
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("更新失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 更新卷烟信息
     * 前端传入查询参数{卷烟代码，卷烟名称，年份，月份，周序号}
     * 需要修改的字段为{投放方式，扩展投放方式，投放区域，D30~D1，备注}
     */
    @PostMapping("/update-cigarette")
    @CrossOrigin
    public ResponseEntity<Map<String, Object>> updateCigaretteInfo(@Valid @RequestBody UpdateCigaretteRequestDto request) {
        log.info("收到卷烟更新请求: {}", request);
        
        try {
            Map<String, Object> result = distributionService.updateCigaretteInfo(request);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
            
        } catch (Exception e) {
            log.error("更新卷烟信息时发生错误", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "更新过程中发生错误: " + e.getMessage());
            errorResult.put("error", e.toString());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }
    
    /**
     * 测试接口：检查demo_test_advdata表结构
     */
    @GetMapping("/test-advdata-structure")
    public ResponseEntity<Map<String, Object>> testAdvDataStructure() {
        log.info("检查demo_test_advdata表结构");
        
        try {
            // 使用JdbcTemplate查询表结构
            String sql = "SELECT * FROM demo_test_advdata LIMIT 1";
            List<Map<String, Object>> result = distributionService.getJdbcTemplate().queryForList(sql);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "查询成功");
            response.put("data", result);
            response.put("columns", result.isEmpty() ? new ArrayList<>() : new ArrayList<>(result.get(0).keySet()));
            
            log.info("查询成功，返回{}条记录", result.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("查询失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "卷烟分配服务运行正常");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 调试接口：测试实际投放量计算
     */
    @GetMapping("/debug-actual-amount")
    public ResponseEntity<Map<String, Object>> debugActualAmount() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 测试红金龙的实际投放量计算
            BigDecimal actualAmount = distributionService.calActualDistributionAmount(
                "42010114", "红金龙(硬爱你爆珠)", 2024, 1, 1);
            
            response.put("cigCode", "42010114");
            response.put("cigName", "红金龙(硬爱你爆珠)");
            response.put("actualAmount", actualAmount);
            response.put("success", true);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("调试实际投放量计算失败", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 将算法输出的分配矩阵写回数据库
     */
    @PostMapping("/write-back")
    public ResponseEntity<Map<String, Object>> writeBackAllocationMatrix(@RequestParam Integer year, 
                                                                       @RequestParam Integer month, 
                                                                       @RequestParam Integer weekSeq) {
        log.info("接收写回请求，年份: {}, 月份: {}, 周序号: {}", year, month, weekSeq);
        
        try {
            Map<String, Object> result = distributionService.writeBackAllocationMatrix(year, month, weekSeq);
            
            if ((Boolean) result.get("success")) {
                log.info("分配矩阵写回成功，成功: {}/{}", result.get("successCount"), result.get("totalCount"));
                return ResponseEntity.ok(result);
            } else {
                log.error("分配矩阵写回失败: {}", result.get("message"));
                return ResponseEntity.internalServerError().body(result);
            }
            
        } catch (Exception e) {
            log.error("分配矩阵写回失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "分配矩阵写回失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 测试分配算法接口
     */
    @GetMapping("/test-algorithm")
    public ResponseEntity<Map<String, Object>> testAlgorithm() {
        log.info("开始测试分配算法");
        
        try {
            // 获取预投放量数据
            List<DemoTestAdvData> advDataList = advDataRepository.findAll();
            List<DemoTestClientNumData> clientNumDataList = clientNumDataRepository.findAllByOrderByIdAsc();
            List<DemoTestData> testDataList = testDataRepository.findByYearAndMonthAndWeekSeq(2024, 1, 1);
            
            List<String> allRegions = clientNumDataRepository.findAllByOrderByIdAsc()
                    .stream()
                    .map(DemoTestClientNumData::getUrbanRuralCode)
                    .collect(java.util.stream.Collectors.toList());
            
            List<Map<String, Object>> testResults = new ArrayList<>();
            
            for (DemoTestAdvData advData : advDataList) {
                Map<String, Object> result = new HashMap<>();
                result.put("cigCode", advData.getCigCode());
                result.put("cigName", advData.getCigName());
                result.put("adv", advData.getAdv());
                
                // 直接从advData获取投放区域
                String deliveryArea = advData.getDeliveryArea();
                
                if (deliveryArea != null && !deliveryArea.trim().isEmpty()) {
                    List<String> targetRegions = distributionService.getTargetRegionList(deliveryArea);
                    
                    result.put("deliveryArea", deliveryArea);
                    result.put("targetRegions", targetRegions);
                    
                    if (!targetRegions.isEmpty()) {
                        // 计算分配矩阵
                        BigDecimal[][] allocationMatrix = distributionService.calculateDistributionMatrix(
                            targetRegions, advData.getAdv());
                        
                        // 计算实际投放量
                        BigDecimal actualAmount = calculateActualAmount(allocationMatrix, targetRegions);
                        result.put("actualAmount", actualAmount);
                        result.put("error", advData.getAdv().subtract(actualAmount).abs());
                        
                        // 验证约束
                        result.put("constraintValid", validateConstraints(allocationMatrix));
                    }
                }
                
                testResults.add(result);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "算法测试完成");
            response.put("results", testResults);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("算法测试失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "算法测试失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 测试接口：直接使用JdbcTemplate查询demo_test_clientNumdata表
     */
    @GetMapping("/test-client-numdata-direct")
    public ResponseEntity<Map<String, Object>> testClientNumDataDirect() {
        log.info("直接查询demo_test_clientNumdata表");
        
        try {
            // 使用JdbcTemplate直接查询表结构
            String sql = "SELECT * FROM demo_test_clientNumdata LIMIT 5";
            List<Map<String, Object>> result = distributionService.getJdbcTemplate().queryForList(sql);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "查询成功");
            response.put("data", result);
            response.put("columns", result.isEmpty() ? new ArrayList<>() : new ArrayList<>(result.get(0).keySet()));
            response.put("rowCount", result.size());
            
            log.info("查询成功，返回{}条记录", result.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("查询失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "查询失败: " + e.getMessage());
            response.put("error", e.toString());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 测试接口：查看getRegionCustomerMatrix方法的输出
     */
    @GetMapping("/test-region-customer-matrix")
    public ResponseEntity<Map<String, Object>> testRegionCustomerMatrix() {
        log.info("测试getRegionCustomerMatrix方法输出");
        
        try {
            // 获取区域客户数矩阵
            BigDecimal[][] regionCustomerMatrix = distributionService.getRegionCustomerMatrix();
            
            // 获取所有投放区域列表
            List<String> allRegions = distributionService.getAllRegionList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "查询成功");
            response.put("matrixSize", regionCustomerMatrix.length + "x" + regionCustomerMatrix[0].length);
            response.put("regionCount", allRegions.size());
            response.put("regions", allRegions);
            
            // 构建矩阵数据
            List<Map<String, Object>> matrixData = new ArrayList<>();
            for (int i = 0; i < regionCustomerMatrix.length; i++) {
                Map<String, Object> regionData = new HashMap<>();
                regionData.put("regionIndex", i);
                regionData.put("regionName", allRegions.get(i));
                
                // 添加所有档位数据
                Map<String, Object> gradeData = new HashMap<>();
                for (int j = 0; j < 30; j++) {
                    String columnName = "D" + (30 - j);
                    gradeData.put(columnName, regionCustomerMatrix[i][j]);
                }
                regionData.put("gradeData", gradeData);
                
                matrixData.add(regionData);
            }
            response.put("matrixData", matrixData);
            
            log.info("查询成功，矩阵大小: {}x30, 区域数量: {}", regionCustomerMatrix.length, allRegions.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("查询失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "查询失败: " + e.getMessage());
            response.put("error", e.toString());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 合并档位相同但投放区域不同的记录
     */
    private List<Map<String, Object>> mergeRecordsByAllocation(List<DemoTestData> rawDataList) {
        Map<String, List<DemoTestData>> groupedData = new HashMap<>();
        
        // 按卷烟代码+名称+档位方案分组
        for (DemoTestData data : rawDataList) {
            String key = generateAllocationKey(data);
            groupedData.computeIfAbsent(key, k -> new ArrayList<>()).add(data);
        }
        
        List<Map<String, Object>> mergedResult = new ArrayList<>();
        
        for (Map.Entry<String, List<DemoTestData>> entry : groupedData.entrySet()) {
            List<DemoTestData> groupData = entry.getValue();
            if (groupData.isEmpty()) continue;
            
            DemoTestData firstData = groupData.get(0);
            
            // 合并投放区域
            String mergedDeliveryAreas = groupData.stream()
                    .map(DemoTestData::getDeliveryArea)
                    .filter(area -> area != null && !area.trim().isEmpty())
                    .distinct()
                    .sorted()
                    .collect(java.util.stream.Collectors.joining(","));
            
            // 计算实际投放量
            BigDecimal actualDelivery = calculateActualDelivery(firstData);
            
            // 构建合并后的记录
            Map<String, Object> mergedRecord = new HashMap<>();
            mergedRecord.put("cigCode", firstData.getCigCode());
            mergedRecord.put("cigName", firstData.getCigName());
            mergedRecord.put("deliveryAreas", mergedDeliveryAreas);
            mergedRecord.put("year", firstData.getYear());
            mergedRecord.put("month", firstData.getMonth());
            mergedRecord.put("weekSeq", firstData.getWeekSeq());
            mergedRecord.put("actualDelivery", actualDelivery);
            mergedRecord.put("remark", firstData.getBz());
            mergedRecord.put("mergedCount", groupData.size()); // 合并的记录数
            
            // 添加所有档位数据
            mergedRecord.put("d30", firstData.getD30());
            mergedRecord.put("d29", firstData.getD29());
            mergedRecord.put("d28", firstData.getD28());
            mergedRecord.put("d27", firstData.getD27());
            mergedRecord.put("d26", firstData.getD26());
            mergedRecord.put("d25", firstData.getD25());
            mergedRecord.put("d24", firstData.getD24());
            mergedRecord.put("d23", firstData.getD23());
            mergedRecord.put("d22", firstData.getD22());
            mergedRecord.put("d21", firstData.getD21());
            mergedRecord.put("d20", firstData.getD20());
            mergedRecord.put("d19", firstData.getD19());
            mergedRecord.put("d18", firstData.getD18());
            mergedRecord.put("d17", firstData.getD17());
            mergedRecord.put("d16", firstData.getD16());
            mergedRecord.put("d15", firstData.getD15());
            mergedRecord.put("d14", firstData.getD14());
            mergedRecord.put("d13", firstData.getD13());
            mergedRecord.put("d12", firstData.getD12());
            mergedRecord.put("d11", firstData.getD11());
            mergedRecord.put("d10", firstData.getD10());
            mergedRecord.put("d9", firstData.getD9());
            mergedRecord.put("d8", firstData.getD8());
            mergedRecord.put("d7", firstData.getD7());
            mergedRecord.put("d6", firstData.getD6());
            mergedRecord.put("d5", firstData.getD5());
            mergedRecord.put("d4", firstData.getD4());
            mergedRecord.put("d3", firstData.getD3());
            mergedRecord.put("d2", firstData.getD2());
            mergedRecord.put("d1", firstData.getD1());
            
            mergedResult.add(mergedRecord);
        }
        
        return mergedResult;
    }
    
    /**
     * 合并档位相同但投放区域不同的记录，并添加预投放量和实际投放量
     */
    private List<Map<String, Object>> mergeRecordsByAllocationWithAdvAndActual(List<DemoTestData> rawDataList) {
        Map<String, List<DemoTestData>> groupedData = new HashMap<>();
        
        // 按卷烟代码+名称+档位方案分组
        for (DemoTestData data : rawDataList) {
            String key = generateAllocationKey(data);
            groupedData.computeIfAbsent(key, k -> new ArrayList<>()).add(data);
        }
        
        List<Map<String, Object>> mergedResult = new ArrayList<>();
        
        for (Map.Entry<String, List<DemoTestData>> entry : groupedData.entrySet()) {
            List<DemoTestData> groupData = entry.getValue();
            if (groupData.isEmpty()) continue;
            
            DemoTestData firstData = groupData.get(0);
            
            // 合并投放区域
            String mergedDeliveryAreas = groupData.stream()
                    .map(DemoTestData::getDeliveryArea)
                    .filter(area -> area != null && !area.trim().isEmpty())
                    .distinct()
                    .sorted()
                    .collect(java.util.stream.Collectors.joining(","));
            
            // 获取预投放量（从demo_test_ADVdata表的ADV字段）
            BigDecimal advAmount = getAdvAmount(firstData.getCigCode(), firstData.getCigName());
            
            // 从服务层获取实际投放量
            BigDecimal actualDelivery = distributionService.calculateActualAmountForRecord(firstData);
            
            // 构建合并后的记录
            Map<String, Object> mergedRecord = new HashMap<>();
            mergedRecord.put("cigCode", firstData.getCigCode());
            mergedRecord.put("cigName", firstData.getCigName());
            mergedRecord.put("deliveryAreas", mergedDeliveryAreas);
            mergedRecord.put("year", firstData.getYear());
            mergedRecord.put("month", firstData.getMonth());
            mergedRecord.put("weekSeq", firstData.getWeekSeq());
            mergedRecord.put("advAmount", advAmount); // 预投放量
            mergedRecord.put("actualDelivery", actualDelivery); // 实际投放量
            mergedRecord.put("remark", firstData.getBz());
            mergedRecord.put("mergedCount", groupData.size()); // 合并的记录数
            
            // 添加所有档位数据
            mergedRecord.put("d30", firstData.getD30());
            mergedRecord.put("d29", firstData.getD29());
            mergedRecord.put("d28", firstData.getD28());
            mergedRecord.put("d27", firstData.getD27());
            mergedRecord.put("d26", firstData.getD26());
            mergedRecord.put("d25", firstData.getD25());
            mergedRecord.put("d24", firstData.getD24());
            mergedRecord.put("d23", firstData.getD23());
            mergedRecord.put("d22", firstData.getD22());
            mergedRecord.put("d21", firstData.getD21());
            mergedRecord.put("d20", firstData.getD20());
            mergedRecord.put("d19", firstData.getD19());
            mergedRecord.put("d18", firstData.getD18());
            mergedRecord.put("d17", firstData.getD17());
            mergedRecord.put("d16", firstData.getD16());
            mergedRecord.put("d15", firstData.getD15());
            mergedRecord.put("d14", firstData.getD14());
            mergedRecord.put("d13", firstData.getD13());
            mergedRecord.put("d12", firstData.getD12());
            mergedRecord.put("d11", firstData.getD11());
            mergedRecord.put("d10", firstData.getD10());
            mergedRecord.put("d9", firstData.getD9());
            mergedRecord.put("d8", firstData.getD8());
            mergedRecord.put("d7", firstData.getD7());
            mergedRecord.put("d6", firstData.getD6());
            mergedRecord.put("d5", firstData.getD5());
            mergedRecord.put("d4", firstData.getD4());
            mergedRecord.put("d3", firstData.getD3());
            mergedRecord.put("d2", firstData.getD2());
            mergedRecord.put("d1", firstData.getD1());
            
            mergedResult.add(mergedRecord);
        }
        
        return mergedResult;
    }
    
    /**
     * 生成档位分配方案的唯一键
     */
    private String generateAllocationKey(DemoTestData data) {
        StringBuilder key = new StringBuilder();
        key.append(data.getCigCode()).append("_")
            .append(data.getCigName()).append("_");
        
        // 添加所有档位值
        key.append("D30:").append(data.getD30()).append("_")
           .append("D29:").append(data.getD29()).append("_")
           .append("D28:").append(data.getD28()).append("_")
           .append("D27:").append(data.getD27()).append("_")
           .append("D26:").append(data.getD26()).append("_")
           .append("D25:").append(data.getD25()).append("_")
           .append("D24:").append(data.getD24()).append("_")
           .append("D23:").append(data.getD23()).append("_")
           .append("D22:").append(data.getD22()).append("_")
           .append("D21:").append(data.getD21()).append("_")
           .append("D20:").append(data.getD20()).append("_")
           .append("D19:").append(data.getD19()).append("_")
           .append("D18:").append(data.getD18()).append("_")
           .append("D17:").append(data.getD17()).append("_")
           .append("D16:").append(data.getD16()).append("_")
           .append("D15:").append(data.getD15()).append("_")
           .append("D14:").append(data.getD14()).append("_")
           .append("D13:").append(data.getD13()).append("_")
           .append("D12:").append(data.getD12()).append("_")
           .append("D11:").append(data.getD11()).append("_")
           .append("D10:").append(data.getD10()).append("_")
           .append("D9:").append(data.getD9()).append("_")
           .append("D8:").append(data.getD8()).append("_")
           .append("D7:").append(data.getD7()).append("_")
           .append("D6:").append(data.getD6()).append("_")
           .append("D5:").append(data.getD5()).append("_")
           .append("D4:").append(data.getD4()).append("_")
           .append("D3:").append(data.getD3()).append("_")
           .append("D2:").append(data.getD2()).append("_")
           .append("D1:").append(data.getD1());
        
        return key.toString();
    }
    
    /**
     * 获取预投放量（从demo_test_ADVdata表的ADV字段）
     */
    private BigDecimal getAdvAmount(String cigCode, String cigName) {
        try {
            DemoTestAdvData advData = advDataRepository.findByCigCodeAndCigName(cigCode, cigName);
            if (advData != null && advData.getAdv() != null) {
                return advData.getAdv();
            }
        } catch (Exception e) {
            log.warn("获取预投放量失败，卷烟代码: {}, 卷烟名称: {}, 错误: {}", cigCode, cigName, e.getMessage());
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * 计算实际投放量
     */
    private BigDecimal calculateActualAmount(BigDecimal[][] allocationMatrix, List<String> targetRegions) {
        BigDecimal total = BigDecimal.ZERO;
        // 这里需要根据实际的客户数矩阵计算，简化处理
        return total;
    }
    
    /**
     * 计算实际投放量
     */
    private BigDecimal calculateActualDelivery(DemoTestData data) {
        BigDecimal total = BigDecimal.ZERO;
        
        if (data.getD30() != null) total = total.add(data.getD30());
        if (data.getD29() != null) total = total.add(data.getD29());
        if (data.getD28() != null) total = total.add(data.getD28());
        if (data.getD27() != null) total = total.add(data.getD27());
        if (data.getD26() != null) total = total.add(data.getD26());
        if (data.getD25() != null) total = total.add(data.getD25());
        if (data.getD24() != null) total = total.add(data.getD24());
        if (data.getD23() != null) total = total.add(data.getD23());
        if (data.getD22() != null) total = total.add(data.getD22());
        if (data.getD21() != null) total = total.add(data.getD21());
        if (data.getD20() != null) total = total.add(data.getD20());
        if (data.getD19() != null) total = total.add(data.getD19());
        if (data.getD18() != null) total = total.add(data.getD18());
        if (data.getD17() != null) total = total.add(data.getD17());
        if (data.getD16() != null) total = total.add(data.getD16());
        if (data.getD15() != null) total = total.add(data.getD15());
        if (data.getD14() != null) total = total.add(data.getD14());
        if (data.getD13() != null) total = total.add(data.getD13());
        if (data.getD12() != null) total = total.add(data.getD12());
        if (data.getD11() != null) total = total.add(data.getD11());
        if (data.getD10() != null) total = total.add(data.getD10());
        if (data.getD9() != null) total = total.add(data.getD9());
        if (data.getD8() != null) total = total.add(data.getD8());
        if (data.getD7() != null) total = total.add(data.getD7());
        if (data.getD6() != null) total = total.add(data.getD6());
        if (data.getD5() != null) total = total.add(data.getD5());
        if (data.getD4() != null) total = total.add(data.getD4());
        if (data.getD3() != null) total = total.add(data.getD3());
        if (data.getD2() != null) total = total.add(data.getD2());
        if (data.getD1() != null) total = total.add(data.getD1());
        
        return total;
    }
    
    /**
     * 验证约束条件
     */
    private boolean validateConstraints(BigDecimal[][] matrix) {
        // 验证非递增约束
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 1; j < matrix[i].length; j++) {
                if (matrix[i][j-1] != null && matrix[i][j] != null) {
                    if (matrix[i][j-1].compareTo(matrix[i][j]) < 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    /**
     * 统计非零值数量
     */
    private int countNonZeroValues(BigDecimal[][] matrix) {
        int count = 0;
        for (BigDecimal[] row : matrix) {
            for (BigDecimal value : row) {
                if (value != null && value.compareTo(BigDecimal.ZERO) != 0) {
                    count++;
                }
            }
        }
        return count;
    }
    
    /**
     * 统计零值数量
     */
    private int countZeroValues(BigDecimal[][] matrix) {
        int count = 0;
        for (BigDecimal[] row : matrix) {
            for (BigDecimal value : row) {
                if (value == null || value.compareTo(BigDecimal.ZERO) == 0) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * 测试KMP匹配功能
     */
    @GetMapping("/test-kmp-matching")
    public ResponseEntity<Map<String, Object>> testKmpMatching() {
        log.info("测试KMP匹配功能");
        
        try {
            // 获取所有区域
            String regionSql = "SELECT URBAN_RURAL_CODE FROM demo_test_clientNumdata ORDER BY id ASC";
            List<Map<String, Object>> regionData = distributionService.getJdbcTemplate().queryForList(regionSql);
            List<String> allRegions = regionData.stream()
                .map(row -> (String) row.get("URBAN_RURAL_CODE"))
                .collect(java.util.stream.Collectors.toList());
            
            // 获取投放区域
            String advDataSql = "SELECT cig_code, cig_name, delivery_area FROM demo_test_advdata";
            List<Map<String, Object>> advDataList = distributionService.getJdbcTemplate().queryForList(advDataSql);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "KMP匹配测试完成");
            response.put("allRegions", allRegions);
            
            List<Map<String, Object>> testResults = new ArrayList<>();
            for (Map<String, Object> advData : advDataList) {
                String cigCode = (String) advData.get("cig_code");
                String cigName = (String) advData.get("cig_name");
                String deliveryArea = (String) advData.get("delivery_area");
                
                Map<String, Object> testResult = new HashMap<>();
                testResult.put("cigCode", cigCode);
                testResult.put("cigName", cigName);
                testResult.put("deliveryArea", deliveryArea);
                
                if (deliveryArea != null && !deliveryArea.trim().isEmpty()) {
                    // 使用KMP匹配
                    List<String> matchedRegions = distributionService.getKmpMatcher().matchPatterns(deliveryArea, allRegions);
                    testResult.put("matchedRegions", matchedRegions);
                    testResult.put("matchCount", matchedRegions.size());
                } else {
                    testResult.put("matchedRegions", new ArrayList<>());
                    testResult.put("matchCount", 0);
                }
                
                testResults.add(testResult);
            }
            
            response.put("testResults", testResults);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("KMP匹配测试失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "KMP匹配测试失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
