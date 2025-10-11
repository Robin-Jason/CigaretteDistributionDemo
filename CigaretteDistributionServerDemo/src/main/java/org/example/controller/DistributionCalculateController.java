package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.entity.CigaretteDistributionPredictionData;
import org.example.service.DataManagementService;
import org.example.service.DistributionCalculateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分配计算控制器
 * 负责卷烟分配算法计算和分配矩阵写回等功能
 */
@Slf4j
@RestController
@RequestMapping("/api/calculate")
@CrossOrigin(origins = "*")
public class DistributionCalculateController {
    
    @Autowired
    private DistributionCalculateService distributionService;
    
    @Autowired
    private DataManagementService dataManagementService;
    
    /**
     * 获取算法输出的分配矩阵并写回数据库
     */
    @PostMapping("/write-back")
    public ResponseEntity<Map<String, Object>> getAndwriteBackAllocationMatrix(@RequestParam Integer year, 
                                                                       @RequestParam Integer month, 
                                                                       @RequestParam Integer weekSeq) {
        log.info("接收写回请求，年份: {}, 月份: {}, 周序号: {}", year, month, weekSeq);
        
        try {
            Map<String, Object> result = distributionService.getAndwriteBackAllocationMatrix(year, month, weekSeq);
            
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
     * 一键生成分配方案
     * 前端调用接口：generate-distribution-plan
     * 功能：删除指定日期的所有分配数据，重新执行各投放类型的算法分配并写回数据库
     */
    @PostMapping("/generate-distribution-plan")
    @Transactional
    public ResponseEntity<Map<String, Object>> generateDistributionPlan(@RequestParam Integer year,
                                                                        @RequestParam Integer month,
                                                                        @RequestParam Integer weekSeq) {
        log.info("接收一键生成分配方案请求，年份: {}, 月份: {}, 周序号: {}", year, month, weekSeq);
        
        try {
            // 1. 检查指定日期是否存在分配数据（通过DataManagementService）
            List<CigaretteDistributionPredictionData> existingData = dataManagementService.queryTestDataByTime(year, month, weekSeq);
            
            Map<String, Object> response = new HashMap<>();
            response.put("year", year);
            response.put("month", month);
            response.put("weekSeq", weekSeq);
            response.put("startTime", System.currentTimeMillis());
            
            if (!existingData.isEmpty()) {
                log.info("发现指定日期已存在{}条分配数据，将先删除后重新分配", existingData.size());
                
                // 2. 删除现有分配数据（通过DataManagementService）
                Map<String, Object> deleteResult = dataManagementService.deleteDistributionDataByTime(year, month, weekSeq);
                
                if ((Boolean) deleteResult.get("success")) {
                    log.info("成功删除{}年{}月第{}周的{}条现有分配数据", year, month, weekSeq, deleteResult.get("deletedCount"));
                    
                    response.put("deletedExistingData", true);
                    response.put("deletedRecords", deleteResult.get("deletedCount"));
                    
                } else {
                    log.error("删除现有分配数据失败: {}", deleteResult.get("message"));
                    response.put("success", false);
                    response.put("message", "删除现有分配数据失败: " + deleteResult.get("message"));
                    response.put("error", "DELETE_FAILED");
                    return ResponseEntity.internalServerError().body(response);
                }
            } else {
                log.info("指定日期暂无分配数据，将直接进行新分配");
                response.put("deletedExistingData", false);
                response.put("deletedRecords", 0);
            }
            
            // 3. 执行算法分配并写回数据库
            log.info("开始执行各投放类型的算法分配...");
            Map<String, Object> allocationResult = distributionService.getAndwriteBackAllocationMatrix(year, month, weekSeq);
            
            if ((Boolean) allocationResult.get("success")) {
                // 4. 分配成功，查询生成的分配记录数（通过DataManagementService）
                List<CigaretteDistributionPredictionData> generatedData = dataManagementService.queryTestDataByTime(year, month, weekSeq);
                int processedCount = generatedData.size();
                
                // 5. 合并结果
                response.put("success", true);
                response.put("message", "一键分配方案生成成功");
                response.put("operation", "一键生成分配方案");
                response.put("endTime", System.currentTimeMillis());
                response.put("processingTime", (Long) response.get("endTime") - (Long) response.get("startTime") + "ms");
                
                // 合并分配结果信息
                response.put("allocationResult", allocationResult);
                response.put("totalCigarettes", allocationResult.get("totalCount"));
                response.put("successfulAllocations", allocationResult.get("successCount"));
                response.put("processedCount", processedCount);  // 新增：生成的分配记录数
                response.put("allocationDetails", allocationResult.get("results"));
                
                log.info("一键分配方案生成完成，成功分配: {}/{} 种卷烟，生成 {} 条分配记录", 
                        allocationResult.get("successCount"), allocationResult.get("totalCount"), processedCount);
                        
                return ResponseEntity.ok(response);
                
            } else {
                // 5. 分配失败，但仍需统计可能已生成的记录数（通过DataManagementService）
                List<CigaretteDistributionPredictionData> partialData = dataManagementService.queryTestDataByTime(year, month, weekSeq);
                int processedCount = partialData.size();
                
                response.put("success", false);
                response.put("message", "算法分配失败: " + allocationResult.get("message"));
                response.put("error", "ALLOCATION_FAILED");
                response.put("processedCount", processedCount);  // 已生成的分配记录数（可能部分成功）
                response.put("allocationResult", allocationResult);
                
                log.error("一键分配方案生成失败: {}，已生成 {} 条分配记录", allocationResult.get("message"), processedCount);
                return ResponseEntity.internalServerError().body(response);
            }
            
        } catch (Exception e) {
            log.error("一键生成分配方案失败", e);
            
            // 即使发生异常，也尝试统计已生成的记录数（通过DataManagementService）
            int processedCount = 0;
            try {
                List<CigaretteDistributionPredictionData> existingRecords = dataManagementService.queryTestDataByTime(year, month, weekSeq);
                processedCount = existingRecords.size();
            } catch (Exception countException) {
                log.warn("统计已生成记录数时发生异常: {}", countException.getMessage());
            }
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "一键生成分配方案失败: " + e.getMessage());
            errorResponse.put("error", "GENERATION_FAILED");
            errorResponse.put("year", year);
            errorResponse.put("month", month);
            errorResponse.put("weekSeq", weekSeq);
            errorResponse.put("processedCount", processedCount);  // 异常情况下的记录数
            errorResponse.put("exception", e.getClass().getSimpleName());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 计算指定时间范围内所有卷烟的总实际投放量
     */
    @PostMapping("/total-actual-delivery")
    public ResponseEntity<Map<String, Object>> calculateTotalActualDelivery(@RequestParam Integer year,
                                                                           @RequestParam Integer month,
                                                                           @RequestParam Integer weekSeq) {
        log.info("接收总实际投放量计算请求，年份: {}, 月份: {}, 周序号: {}", year, month, weekSeq);
        
        try {
            // 获取指定时间的数据（通过DataManagementService）
            List<CigaretteDistributionPredictionData> rawDataList = dataManagementService.queryTestDataByTime(year, month, weekSeq);
            
            if (rawDataList.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "未找到指定时间的数据");
                response.put("data", new HashMap<>());
                return ResponseEntity.ok(response);
            }
            
            // 计算总实际投放量（通过DistributionCalculateService）
            Map<String, BigDecimal> totalActualDeliveryMap = distributionService.calculateTotalActualDeliveryByTobacco(rawDataList);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "总实际投放量计算成功");
            response.put("data", totalActualDeliveryMap);
            response.put("year", year);
            response.put("month", month);
            response.put("weekSeq", weekSeq);
            response.put("totalRecords", rawDataList.size());
            response.put("cigaretteCount", totalActualDeliveryMap.size());
            
            log.info("总实际投放量计算成功，返回{}种卷烟的数据", totalActualDeliveryMap.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("总实际投放量计算失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "总实际投放量计算失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
