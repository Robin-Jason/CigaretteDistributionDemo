package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.dto.CigaretteImportRequestDto;
import org.example.dto.RegionClientNumImportRequestDto;
import org.example.service.ExcelImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据导入控制器
 * 负责Excel文件的导入功能
 */
@Slf4j
@RestController
@RequestMapping("/api/import")
@Validated
@CrossOrigin(origins = "*")
public class ExcelImportController {
    
    @Autowired
    private ExcelImportService excelImportService;

    /**
     * 导入卷烟投放基础信息Excel
     */
    @PostMapping("/cigarette-info")
    public ResponseEntity<Map<String, Object>> importCigaretteDistributionInfo(@Valid CigaretteImportRequestDto request) {
        log.info("接收卷烟投放基础信息导入请求，年份: {}, 月份: {}, 周序号: {}",
                request.getYear(), request.getMonth(), request.getWeekSeq());

        Map<String, Object> response = new HashMap<>();

        try {
            // 1. 验证文件
            if (request.getFile() == null || request.getFile().isEmpty()) {
                response.put("success", false);
                response.put("message", "请选择要上传的Excel文件");
                response.put("error", "FILE_EMPTY");
                return ResponseEntity.badRequest().body(response);
            }

            // 2. 检查文件大小（限制10MB）
            if (request.getFile().getSize() > 10 * 1024 * 1024) {
                response.put("success", false);
                response.put("message", "文件大小超过限制（最大10MB）");
                response.put("error", "FILE_TOO_LARGE");
                return ResponseEntity.badRequest().body(response);
            }

            // 3. 执行导入
            Map<String, Object> importResult = excelImportService.importCigaretteDistributionInfo(request);

            if ((Boolean) importResult.get("success")) {
                log.info("卷烟投放基础信息导入成功，表名: {}, 插入记录数: {}",
                        importResult.get("tableName"), importResult.get("insertedCount"));
                return ResponseEntity.ok(importResult);
            } else {
                log.warn("卷烟投放基础信息导入失败: {}", importResult.get("message"));
                return ResponseEntity.badRequest().body(importResult);
            }

        } catch (Exception e) {
            log.error("卷烟投放基础信息导入失败", e);
            response.put("success", false);
            response.put("message", "导入失败: " + e.getMessage());
            response.put("error", "IMPORT_FAILED");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 导入区域客户数表Excel
     */
    @PostMapping("/region-clientnum")
    public ResponseEntity<Map<String, Object>> importRegionClientNumData(@Valid RegionClientNumImportRequestDto request) {
        log.info("接收区域客户数表导入请求，年份: {}, 月份: {}, 投放类型: {}, 扩展投放类型: {}",
                request.getYear(), request.getMonth(), request.getDeliveryMethod(), request.getDeliveryEtype());

        Map<String, Object> response = new HashMap<>();

        try {
            // 1. 验证文件
            if (request.getFile() == null || request.getFile().isEmpty()) {
                response.put("success", false);
                response.put("message", "请选择要上传的Excel文件");
                response.put("error", "FILE_EMPTY");
                return ResponseEntity.badRequest().body(response);
            }

            // 2. 检查文件大小（限制10MB）
            if (request.getFile().getSize() > 10 * 1024 * 1024) {
                response.put("success", false);
                response.put("message", "文件大小超过限制（最大10MB）");
                response.put("error", "FILE_TOO_LARGE");
                return ResponseEntity.badRequest().body(response);
            }

            // 3. 验证投放类型映射
            Integer sequenceNumber = request.getSequenceNumber();
            if (sequenceNumber < 0 || sequenceNumber > 4) {
                response.put("success", false);
                response.put("message", "投放类型和扩展投放类型组合无效");
                response.put("error", "INVALID_DELIVERY_TYPE");
                response.put("deliveryMethod", request.getDeliveryMethod());
                response.put("deliveryEtype", request.getDeliveryEtype());
                return ResponseEntity.badRequest().body(response);
            }

            // 4. 执行导入
            Map<String, Object> importResult = excelImportService.importRegionClientNumData(request);

            if ((Boolean) importResult.get("success")) {
                log.info("区域客户数表导入成功，表名: {}, 插入记录数: {}, 序号: {}",
                        importResult.get("tableName"), importResult.get("insertedCount"), sequenceNumber);
                return ResponseEntity.ok(importResult);
            } else {
                log.warn("区域客户数表导入失败: {}", importResult.get("message"));
                return ResponseEntity.badRequest().body(importResult);
            }

        } catch (Exception e) {
            log.error("区域客户数表导入失败", e);
            response.put("success", false);
            response.put("message", "导入失败: " + e.getMessage());
            response.put("error", "IMPORT_FAILED");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
