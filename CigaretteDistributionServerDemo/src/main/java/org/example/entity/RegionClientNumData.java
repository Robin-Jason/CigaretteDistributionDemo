package org.example.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;

/**
 * 区域客户数统一实体类
 * 
 * 适用于所有投放类型的区域客户数数据：
 * - 按档位统一投放（region_clientNum_0_1/2）
 * - 档位+区县（region_clientNum_1_1/2）
 * - 档位+市场类型（region_clientNum_2_1/2）
 * - 档位+城乡分类代码（region_clientNum_3_1/2）
 * - 档位+业态（region_clientNum_4_1/2）
 * 
 * 表名通过 @Table 注解动态指定，支持双周上浮的子序号区分
 */
@Data
@Entity
public class RegionClientNumData {
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    /**
     * 区域字段 - 统一字段名，适用于所有投放类型
     * 根据不同投放类型存储不同内容：
     * - 按档位统一投放: 投放区域
     * - 档位+区县: 区县名称  
     * - 档位+市场类型: 市场类型（城网/农网）
     * - 档位+城乡分类代码: 城乡分类代码
     * - 档位+业态: 业态类型
     */
    @Column(name = "region")
    private String region;
    
    // 30个档位字段 - 从D30到D1
    
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
    
    /**
     * 总计字段
     */
    @Column(name = "TOTAL")
    private BigDecimal total;
    
    /**
     * 获取所有档位数据的数组形式
     * @return 档位数据数组，从D30到D1的顺序
     */
    public BigDecimal[] getGradeArray() {
        return new BigDecimal[]{
            d30, d29, d28, d27, d26, d25, d24, d23, d22, d21,
            d20, d19, d18, d17, d16, d15, d14, d13, d12, d11,
            d10, d9, d8, d7, d6, d5, d4, d3, d2, d1
        };
    }
    
    /**
     * 设置所有档位数据
     * @param gradeArray 档位数据数组，从D30到D1的顺序
     */
    public void setGradeArray(BigDecimal[] gradeArray) {
        if (gradeArray != null && gradeArray.length == 30) {
            this.d30 = gradeArray[0]; this.d29 = gradeArray[1]; this.d28 = gradeArray[2];
            this.d27 = gradeArray[3]; this.d26 = gradeArray[4]; this.d25 = gradeArray[5];
            this.d24 = gradeArray[6]; this.d23 = gradeArray[7]; this.d22 = gradeArray[8];
            this.d21 = gradeArray[9]; this.d20 = gradeArray[10]; this.d19 = gradeArray[11];
            this.d18 = gradeArray[12]; this.d17 = gradeArray[13]; this.d16 = gradeArray[14];
            this.d15 = gradeArray[15]; this.d14 = gradeArray[16]; this.d13 = gradeArray[17];
            this.d12 = gradeArray[18]; this.d11 = gradeArray[19]; this.d10 = gradeArray[20];
            this.d9 = gradeArray[21]; this.d8 = gradeArray[22]; this.d7 = gradeArray[23];
            this.d6 = gradeArray[24]; this.d5 = gradeArray[25]; this.d4 = gradeArray[26];
            this.d3 = gradeArray[27]; this.d2 = gradeArray[28]; this.d1 = gradeArray[29];
        }
    }
}
