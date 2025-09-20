package org.example.controller.DebugOrTest;

import lombok.extern.slf4j.Slf4j;

import org.example.dto.CityUnifiedDistribution.CityPredictionRequestDto;
import org.example.service.CityUnifiedDistribution.CityPredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/prediction")
@Validated
@CrossOrigin(origins = "*")
public class CityPredictionController {

    @Autowired

    private CityPredictionService predictionService;


    @PostMapping("/predict")
    public ResponseEntity<Map<String, Object>> predict(@Valid @RequestBody CityPredictionRequestDto request) {
        log.info("接收到全市投放量预测请求，年份: {}, 月份: {}, 周序号: {}",
                request.getYear(), request.getMonth(), request.getWeekSeq());
        Map<String, Object> response = new HashMap<>();
        try {
            predictionService.predictAndSave(request.getYear(), request.getMonth(), request.getWeekSeq());
            response.put("success", true);
            response.put("message", "全市投放量预测并保存成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("全市投放量预测失败", e);
            response.put("success", false);
            response.put("message", "全市投放量预测失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}