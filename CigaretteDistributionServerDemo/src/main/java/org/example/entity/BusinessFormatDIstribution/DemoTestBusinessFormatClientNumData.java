package org.example.entity.BusinessFormatDIstribution;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "demo_test_businessFormat_clientNumData")
public class DemoTestBusinessFormatClientNumData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "BusinessFormat")
    private String businessFormatCode;
    
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
    
    @Column(name = "TOTAL")
    private BigDecimal total;
}
