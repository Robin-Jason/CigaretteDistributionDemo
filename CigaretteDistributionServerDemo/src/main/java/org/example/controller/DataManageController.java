package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.dto.BatchUpdateFromExpressionsRequestDto;
import org.example.dto.QueryRequestDto;
import org.example.dto.UpdateCigaretteRequestDto;
import org.example.dto.DeleteAreasRequestDto;
import org.example.entity.CigaretteDistributionPredictionData;
import org.example.service.DataManagementService;
import org.example.service.DistributionCalculateService;
import org.example.service.EncodeDecodeService;
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

/**
 * 数据管理控制器
 * 负责卷烟分配数据的查询、更新、删除等管理操作
 */
@Slf4j
@RestController
@RequestMapping("/api/data")
@Validated
@CrossOrigin(origins = "*")
public class DataManageController {
    
    @Autowired
    private DataManagementService dataManagementService;
    
    @Autowired
    private DistributionCalculateService distributionCalculateService;
    
    @Autowired
    private EncodeDecodeService encodeDecodeService;

    /**
     * 查询卷烟分配数据 - 返回原始数据并添加预投放量和实际投放量
     */
    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> queryCigaretteDistribution(@Valid @RequestBody QueryRequestDto request) {
        log.info("接收查询请求，年份: {}, 月份: {}, 周序号: {}", request.getYear(), request.getMonth(), request.getWeekSeq());
        
        try {
            // 获取原始数据（通过服务层）
            List<CigaretteDistributionPredictionData> rawDataList = dataManagementService.queryTestDataByTime(
                    request.getYear(), request.getMonth(), request.getWeekSeq());
            
            // 按卷烟代码+名称分组计算总实际投放量（通过DistributionCalculateService）
            Map<String, BigDecimal> totalActualDeliveryMap = distributionCalculateService.calculateTotalActualDeliveryByTobacco(rawDataList);
            
            // 按卷烟代码+名称分组，用于编码解码处理
            Map<String, List<CigaretteDistributionPredictionData>> cigaretteGroupMap = rawDataList.stream()
                .collect(java.util.stream.Collectors.groupingBy(data -> data.getCigCode() + "_" + data.getCigName()));
            
            // 返回原始数据，添加预投放量、实际投放量、编码表达和解码表达
            List<Map<String, Object>> result = new ArrayList<>();
            for (CigaretteDistributionPredictionData data : rawDataList) {
                Map<String, Object> record = new HashMap<>();
                record.put("id", data.getId());
                record.put("cigCode", data.getCigCode());
                record.put("cigName", data.getCigName());
                record.put("deliveryArea", data.getDeliveryArea());
                record.put("year", data.getYear());
                record.put("month", data.getMonth());
                record.put("weekSeq", data.getWeekSeq());
                record.put("remark", data.getBz());

                // 获取预投放量（从对应的cigarette_distribution_info表）和投放类型（通过服务层）
                Map<String, Object> advInfo = dataManagementService.getAdvDataInfo(data.getCigCode(), data.getCigName(), 
                        data.getYear(), data.getMonth(), data.getWeekSeq());
                record.put("advAmount", advInfo.get("advAmount")); // 预投放量
                record.put("deliveryMethod", data.getDeliveryMethod()); // 投放类型（从demo_test_data表）
                record.put("deliveryEtype", data.getDeliveryEtype()); // 扩展投放类型（从demo_test_data表）

                // 获取该卷烟所有区域的总实际投放量
                String tobaccoKey = data.getCigCode() + "_" + data.getCigName();
                BigDecimal totalActualDelivery = totalActualDeliveryMap.getOrDefault(tobaccoKey, BigDecimal.ZERO);
                record.put("actualDelivery", totalActualDelivery);

                // 为当前记录生成对应的编码表达和解码表达
                List<CigaretteDistributionPredictionData> cigaretteRecords = cigaretteGroupMap.get(tobaccoKey);
                
                String encodedExpression = "";
                String decodedExpression = "";
                
                if (cigaretteRecords != null && !cigaretteRecords.isEmpty()) {
                    // 为该特定区域生成编码表达式
                    encodedExpression = encodeDecodeService.encodeForSpecificArea(
                        data.getCigCode(),
                        data.getCigName(),
                        data.getDeliveryMethod(),
                        data.getDeliveryEtype(),
                        data.getDeliveryArea(),
                        cigaretteRecords
                    );
                    
                    // 生成解码表达
                    decodedExpression = encodeDecodeService.decode(encodedExpression);
                }
                
                record.put("encodedExpression", encodedExpression);
                record.put("decodedExpression", decodedExpression);

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
            response.put("message", "查询成功");
            response.put("data", result);
            response.put("total", result.size());
            
            log.info("查询成功，返回{}条原始记录", result.size());
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
     * 更新卷烟信息
     * 前端传入查询参数{卷烟代码，卷烟名称，年份，月份，周序号}
     * 需要修改的字段为{投放方式，扩展投放方式，投放区域，D30~D1，备注}
     */
    @PostMapping("/update-cigarette")
    @CrossOrigin
    public ResponseEntity<Map<String, Object>> updateCigaretteInfo(@Valid @RequestBody UpdateCigaretteRequestDto request) {
        log.info("收到卷烟更新请求: {}", request);
        
        try {
            Map<String, Object> result = dataManagementService.updateCigaretteInfo(request);
            
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
     * 删除指定卷烟的投放区域记录
     */
    @PostMapping("/delete-delivery-areas")
    public ResponseEntity<Map<String, Object>> deleteDeliveryAreas(@Valid @RequestBody DeleteAreasRequestDto request) {
        log.info("接收删除投放区域请求，卷烟代码: {}, 卷烟名称: {}, 要删除的区域: {}",
                request.getCigCode(), request.getCigName(), request.getAreasToDelete());

        Map<String, Object> response = new HashMap<>();

        try {
            // 1. 验证请求参数
            if (request.getAreasToDelete() == null || request.getAreasToDelete().isEmpty()) {
                response.put("success", false);
                response.put("message", "要删除的投放区域列表不能为空");
                response.put("error", "INVALID_PARAMETERS");
                return ResponseEntity.badRequest().body(response);
            }

            // 2. 查询当前该卷烟的所有投放区域（通过服务层）
            List<CigaretteDistributionPredictionData> currentRecords = dataManagementService.queryTestDataByTime(
                    request.getYear(), request.getMonth(), request.getWeekSeq());

            List<String> currentAreas = currentRecords.stream()
                    .filter(record -> request.getCigCode().equals(record.getCigCode()) &&
                                    request.getCigName().equals(record.getCigName()))
                    .map(CigaretteDistributionPredictionData::getDeliveryArea)
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());

            if (currentAreas.isEmpty()) {
                response.put("success", false);
                response.put("message", "未找到要删除的卷烟记录");
                response.put("error", "RECORD_NOT_FOUND");
                return ResponseEntity.badRequest().body(response);
            }

            // 3. 安全检查：确保删除后至少还有一个区域
            List<String> remainingAreas = currentAreas.stream()
                    .filter(area -> !request.getAreasToDelete().contains(area))
                    .collect(java.util.stream.Collectors.toList());

            if (remainingAreas.isEmpty()) {
                response.put("success", false);
                response.put("message", "删除失败：不能删除所有投放区域，至少需要保留一个");
                response.put("error", "CANNOT_DELETE_ALL_AREAS");
                response.put("currentAreas", currentAreas);
                response.put("areasToDelete", request.getAreasToDelete());
                return ResponseEntity.badRequest().body(response);
            }

            // 4. 执行删除操作
            Map<String, Object> deleteResult = dataManagementService.deleteDeliveryAreas(request);

            if ((Boolean) deleteResult.get("success")) {
                // 5. 记录操作日志
                log.info("成功删除投放区域，卷烟: {} - {}, 删除区域: {}, 删除记录数: {}",
                        request.getCigName(), request.getCigCode(),
                        request.getAreasToDelete(), deleteResult.get("deletedCount"));

                // 6. 返回成功响应
                response.put("success", true);
                response.put("message", "删除成功");
                response.put("deletedCount", deleteResult.get("deletedCount"));
                response.put("deletedAreas", request.getAreasToDelete());
                response.put("remainingAreas", remainingAreas);
                response.put("remainingCount", remainingAreas.size());

                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "删除失败: " + deleteResult.get("message"));
                response.put("error", "DELETE_FAILED");
                return ResponseEntity.internalServerError().body(response);
            }

        } catch (Exception e) {
            log.error("删除投放区域失败", e);
            response.put("success", false);
            response.put("message", "删除失败: " + e.getMessage());
            response.put("error", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    

    /**
     * 根据编码表达式批量更新卷烟信息
     * 前端传入某卷烟的多条编码表达式，控制层解码后调用DataManagementService进行更新
     */
    @PostMapping("/batch-update-from-expressions")
    @CrossOrigin
    public ResponseEntity<Map<String, Object>> batchUpdateFromExpressions(@Valid @RequestBody BatchUpdateFromExpressionsRequestDto request) {
        log.info("接收编码表达式批量更新请求，卷烟: {} - {}, 编码表达式数量: {}", 
                request.getCigCode(), request.getCigName(), request.getEncodedExpressions().size());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 1. 验证请求参数
            if (request.getEncodedExpressions() == null || request.getEncodedExpressions().isEmpty()) {
                response.put("success", false);
                response.put("message", "编码表达式列表不能为空");
                response.put("error", "INVALID_PARAMETERS");
                return ResponseEntity.badRequest().body(response);
            }

            // 2. 调用DataManagementService的新批量更新方法
            Map<String, Object> batchResult = dataManagementService.batchUpdateFromEncodedExpressions(
                request.getCigCode(), 
                request.getCigName(),
                request.getYear(),
                request.getMonth(),
                request.getWeekSeq(),
                request.getEncodedExpressions(),
                request.getRemark()
            );

            // 3. 构建响应结果
            boolean overallSuccess = (Boolean) batchResult.get("success");
            response.put("success", overallSuccess);
            response.put("message", batchResult.get("message"));
            response.put("operation", batchResult.get("operation"));
            response.put("totalExpressions", request.getEncodedExpressions().size());
            
            // 根据操作类型添加不同的统计信息
            if (overallSuccess) {
                if ("投放类型变更".equals(batchResult.get("operation"))) {
                    response.put("deletedRecords", batchResult.get("deletedRecords"));
                    response.put("createdRecords", batchResult.get("createdRecords"));
                } else if ("增量更新".equals(batchResult.get("operation"))) {
                    response.put("newAreas", batchResult.get("newAreas"));
                    response.put("updatedAreas", batchResult.get("updatedAreas"));
                    response.put("deletedAreas", batchResult.get("deletedAreas"));
                }
            }

            log.info("编码表达式批量更新完成，卷烟: {} - {}, 操作类型: {}, 结果: {}", 
                    request.getCigCode(), request.getCigName(), batchResult.get("operation"), 
                    overallSuccess ? "成功" : "失败");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("编码表达式批量更新过程中发生系统错误", e);
            response.put("success", false);
            response.put("message", "系统内部错误: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
