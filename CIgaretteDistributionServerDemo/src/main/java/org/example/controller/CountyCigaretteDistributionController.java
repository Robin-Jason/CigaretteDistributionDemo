package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.dto.CountyQueryRequestDto;
import org.example.service.CountyCigaretteDistributionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/county-cigarette")
@Validated
@CrossOrigin(origins = "*")
public class CountyCigaretteDistributionController {

    @Autowired
    private CountyCigaretteDistributionService countyCigaretteDistributionService;

    /**
     * 接口: 运行并保存“档位+区县”的预测结果到 demo_test_data 表
     */
    @PostMapping("/predict")
    public ResponseEntity<Map<String, Object>> runAndSavePrediction(@Valid @RequestBody CountyQueryRequestDto request) {

        log.info("接收“档位+区县”预测执行请求，年份: {}, 月份: {}, 周序号: {}", request.getYear(), request.getMonth(), request.getWeekSeq());
        try {
            Map<String, Object> result = countyCigaretteDistributionService.runAndSavePrediction(
                    request.getYear(), request.getMonth(), request.getWeekSeq());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // 关键：添加日志打印，输出完整异常信息
            e.printStackTrace(); // 简单打印，或用日志框架（如log.info/log.error）
            // 原有catch逻辑（如事务回滚、返回错误信息）
            log.error("执行“档位+区县”预测失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "预测执行失败: " + e.getMessage());
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
}