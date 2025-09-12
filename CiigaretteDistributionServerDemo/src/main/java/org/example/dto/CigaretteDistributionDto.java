package org.example.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CigaretteDistributionDto {
    private String cigCode;
    private String cigName;
    private BigDecimal adv; // 预投放量
    private BigDecimal actualDelivery; // 实际投放量
    private List<BigDecimal> distribution; // D30到D1的分配值
    private String deliveryAreas; // 投放区域，格式：区域1，区域2，...
    private String remark; // 备注
}
