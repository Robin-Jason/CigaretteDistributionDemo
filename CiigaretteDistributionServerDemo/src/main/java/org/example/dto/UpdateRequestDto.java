package org.example.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdateRequestDto {
    @NotBlank(message = "卷烟代码不能为空")
    private String cigCode;
    
    @NotBlank(message = "卷烟名称不能为空")
    private String cigName;
    
    @NotEmpty(message = "分配值不能为空")
    private List<BigDecimal> distribution; // D30到D1的分配值
    
    @NotBlank(message = "投放区域不能为空")
    private String deliveryAreas; // 投放区域，多个区域用逗号分隔
    
    @NotNull(message = "年份不能为空")
    private Integer year;
    
    @NotNull(message = "月份不能为空")
    private Integer month;
    
    @NotNull(message = "周序号不能为空")
    private Integer weekSeq;
}
