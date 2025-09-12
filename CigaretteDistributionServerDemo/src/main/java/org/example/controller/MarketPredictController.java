package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.dto.MarketPredictionRequestDto;
import org.example.service.MarketPredictService; // 新增导入
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/predict")
@Validated
@CrossOrigin(origins = "*")
public class MarketPredictController {

    //@Autowired
    //private PredictService predictService;

    @Autowired
    private MarketPredictService marketPredictService; // 新增注入

    /**
     * 预测卷烟投放量并写回数据库
     */
//    @PostMapping("/run-market-prediction")
//    public ResponseEntity<Map<String, Object>> runMarketPrediction(@Valid @RequestBody PredictionRequestDto request) {
//        log.info("接收到卷烟投放量预测请求，年份: {}, 月份: {}, 周序号: {}",
//                request.getYear(), request.getMonth(), request.getWeekSeq());
//
//        Map<String, Object> response = new HashMap<>();
//
//        try {
//            predictService.predictAndWriteBack(request.getYear(), request.getMonth(), request.getWeekSeq());
//
//            response.put("success", true);
//            response.put("message", "卷烟投放量预测成功，数据已写入demo_test_data表");
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            log.error("卷烟投放量预测失败", e);
//            response.put("success", false);
//            response.put("message", "预测失败: " + e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
//        }
//    }

    /**
     * 预测卷烟投放量并写回数据库 (市场类型)
     * 此接口专门用于“档位+市场类型”的投放，并使用 demo_market_test_clientnumdata 表作为客户数数据源。
     */
    @PostMapping("/run-market-prediction-new")
    public ResponseEntity<Map<String, Object>> runMarketPredictionNew(@Valid @RequestBody MarketPredictionRequestDto request) {
        log.info("接收到卷烟投放量预测请求 (市场类型)，年份: {}, 月份: {}, 周序号: {}",
                request.getYear(), request.getMonth(), request.getWeekSeq());

        Map<String, Object> response = new HashMap<>();

        try {
            marketPredictService.predictAndWriteBackForMarket(request.getYear(), request.getMonth(), request.getWeekSeq());

            response.put("success", true);
            response.put("message", "卷烟投放量预测成功，数据已写入demo_test_data表");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("卷烟投放量预测失败 (市场类型)", e);
            response.put("success", false);
            response.put("message", "预测失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
