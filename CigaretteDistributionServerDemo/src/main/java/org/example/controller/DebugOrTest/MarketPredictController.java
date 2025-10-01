package org.example.controller.DebugOrTest;

import lombok.extern.slf4j.Slf4j;
import org.example.dto.MarketTypeDistribution.MarketPredictionRequestDto;
import org.example.service.MarketTypeDistribution.MarketPredictService;
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

    @Autowired
    private MarketPredictService marketPredictService;

    /**
     * 预测卷烟投放量并写回数据库 (市场类型)
     * 此接口专门用于“档位+市场类型”的投放，并使用 demo_market_test_clientnumdata 表作为客户数数据源。
     */
    @PostMapping("/run-market-prediction-new")
    public ResponseEntity<Map<String, Object>> runMarketPredictionNew(@Valid @RequestBody MarketPredictionRequestDto request) {
        log.info("接收到卷烟投放量预测请求 (市场类型)，年份: {}, 月份: {}, 周序号: {}, 目标市场: {}, 城网比例: {}, 农网比例: {}",
                request.getYear(), request.getMonth(), request.getWeekSeq(),
                request.getTargetMarkets(), request.getUrbanRatio(), request.getRuralRatio());

        Map<String, Object> response = new HashMap<>();

        try {
            // --- 修改此处，传递整个request对象 ---
            marketPredictService.predictAndWriteBackForMarket(request);

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