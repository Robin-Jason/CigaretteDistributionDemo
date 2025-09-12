package org.example.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;

/**
 * 用于查询“档位+区县”预测数据的DTO
 */
@Data
public class CountyQueryRequestDto {
    @NotNull(message = "年份不能为空")
    private Integer year;

    @NotNull(message = "月份不能为空")
    private Integer month;

    @NotNull(message = "周序号不能为空")
    private Integer weekSeq;
}