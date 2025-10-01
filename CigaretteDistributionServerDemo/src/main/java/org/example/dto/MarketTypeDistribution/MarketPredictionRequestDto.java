package org.example.dto.MarketTypeDistribution;

import lombok.Data;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.math.BigDecimal;
import java.util.List;

@Data
public class MarketPredictionRequestDto {
    @NotNull(message = "年份不能为空")
    @Min(value = 2020, message = "年份不能小于2020")
    @Max(value = 2099, message = "年份不能大于2099")
    private Integer year;

    @NotNull(message = "月份不能为空")
    @Min(value = 1, message = "月份不能小于1")
    @Max(value = 12, message = "月份不能大于12")
    private Integer month;

    @NotNull(message = "周序号不能为空")
    @Min(value = 1, message = "周序号不能小于1")
    @Max(value = 5, message = "周序号不能大于5")
    private Integer weekSeq;

    // --- 新增字段 ---
    @NotNull(message = "目标市场列表不能为空")
    private List<String> targetMarkets; // e.g., ["城网", "农网"] or just ["城网"]

    private BigDecimal urbanRatio; // 城网比例，例如 0.4

    private BigDecimal ruralRatio; // 农网比例，例如 0.6
}