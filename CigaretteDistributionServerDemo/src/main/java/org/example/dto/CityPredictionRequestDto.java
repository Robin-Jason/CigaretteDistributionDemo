package org.example.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class CityPredictionRequestDto {
    @NotNull(message = "年份不能为空")
    private Integer year;

    @NotNull(message = "月份不能为空")
    private Integer month;

    @NotNull(message = "周序号不能为空")
    private Integer weekSeq;
}