package org.example.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;

/**
 * 卷烟分配预测数据模型
 * 注意：实际使用动态表名格式：cigarette_distribution_prediction_{year}_{month}_{weekSeq}
 * 这里的@Table注解主要用于兼容JPA Repository，实际表名在服务层动态生成
 */
@Data
@Entity
@Table(name = "cigarette_distribution_prediction_dynamic") // 占位表名，实际使用动态表名
public class CigaretteDistributionPredictionData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "CIG_CODE")
    private String cigCode;
    
    @Column(name = "CIG_NAME")
    private String cigName;
    
    @Column(name = "YEAR")
    private Integer year;
    
    @Column(name = "MONTH")
    private Integer month;
    
    @Column(name = "WEEK_SEQ")
    private Integer weekSeq;
    
    @Column(name = "DELIVERY_AREA")
    private String deliveryArea;
    
    @Column(name = "DELIVERY_METHOD")
    private String deliveryMethod;
    
    @Column(name = "DELIVERY_ETYPE")
    private String deliveryEtype;
    
    // 30个档位字段（D30-D1）
    @Column(name = "D30")
    private BigDecimal d30;
    @Column(name = "D29")
    private BigDecimal d29;
    @Column(name = "D28")
    private BigDecimal d28;
    @Column(name = "D27")
    private BigDecimal d27;
    @Column(name = "D26")
    private BigDecimal d26;
    @Column(name = "D25")
    private BigDecimal d25;
    @Column(name = "D24")
    private BigDecimal d24;
    @Column(name = "D23")
    private BigDecimal d23;
    @Column(name = "D22")
    private BigDecimal d22;
    @Column(name = "D21")
    private BigDecimal d21;
    @Column(name = "D20")
    private BigDecimal d20;
    @Column(name = "D19")
    private BigDecimal d19;
    @Column(name = "D18")
    private BigDecimal d18;
    @Column(name = "D17")
    private BigDecimal d17;
    @Column(name = "D16")
    private BigDecimal d16;
    @Column(name = "D15")
    private BigDecimal d15;
    @Column(name = "D14")
    private BigDecimal d14;
    @Column(name = "D13")
    private BigDecimal d13;
    @Column(name = "D12")
    private BigDecimal d12;
    @Column(name = "D11")
    private BigDecimal d11;
    @Column(name = "D10")
    private BigDecimal d10;
    @Column(name = "D9")
    private BigDecimal d9;
    @Column(name = "D8")
    private BigDecimal d8;
    @Column(name = "D7")
    private BigDecimal d7;
    @Column(name = "D6")
    private BigDecimal d6;
    @Column(name = "D5")
    private BigDecimal d5;
    @Column(name = "D4")
    private BigDecimal d4;
    @Column(name = "D3")
    private BigDecimal d3;
    @Column(name = "D2")
    private BigDecimal d2;
    @Column(name = "D1")
    private BigDecimal d1;
    
    @Column(name = "bz")
    private String bz; // 备注
    
    @Column(name = "ACTUAL_DELIVERY")
    private BigDecimal actualDelivery; // 实际投放量
    
    @Column(name = "DEPLOYINFO_CODE")
    private String deployinfoCode; // 投放信息编码
}
