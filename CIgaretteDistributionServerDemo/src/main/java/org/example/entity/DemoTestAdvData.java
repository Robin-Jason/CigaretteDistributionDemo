package org.example.entity;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "demo_test_ADVdata")
public class DemoTestAdvData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "CIG_CODE")
    private String cigCode;
    
    @Column(name = "CIG_NAME")
    private String cigName;
    
    @Column(name = "DELIVERY_AREA")
    private String deliveryArea;
    
    @Column(name = "DELIVERY_METHOD")
    private String deliveryMethod;
    
    @Column(name = "DELIVERY_ETYPE")
    private String deliveryEtype;
    
    @Column(name = "URS")
    private BigDecimal urs;
    
    @Column(name = "ADV")
    private BigDecimal adv;
    
    @Column(name = "YEAR")
    private Integer year;
    
    @Column(name = "MONTH")
    private Integer month;
    
    @Column(name = "WEEK_SEQ")
    private Integer weekSeq;
    
    // 手动实现所有getter和setter方法
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public String getCigCode() {
        return cigCode;
    }
    
    public void setCigCode(String cigCode) {
        this.cigCode = cigCode;
    }
    
    public String getCigName() {
        return cigName;
    }
    
    public void setCigName(String cigName) {
        this.cigName = cigName;
    }
    
    public String getDeliveryArea() {
        return deliveryArea;
    }
    
    public void setDeliveryArea(String deliveryArea) {
        this.deliveryArea = deliveryArea;
    }
    
    public String getDeliveryMethod() {
        return deliveryMethod;
    }
    
    public void setDeliveryMethod(String deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }
    
    public String getDeliveryEtype() {
        return deliveryEtype;
    }
    
    public void setDeliveryEtype(String deliveryEtype) {
        this.deliveryEtype = deliveryEtype;
    }
    
    public BigDecimal getUrs() {
        return urs;
    }
    
    public void setUrs(BigDecimal urs) {
        this.urs = urs;
    }
    
    public BigDecimal getAdv() {
        return adv;
    }
    
    public void setAdv(BigDecimal adv) {
        this.adv = adv;
    }
    
    public Integer getYear() {
        return year;
    }
    
    public void setYear(Integer year) {
        this.year = year;
    }
    
    public Integer getMonth() {
        return month;
    }
    
    public void setMonth(Integer month) {
        this.month = month;
    }
    
    public Integer getWeekSeq() {
        return weekSeq;
    }
    
    public void setWeekSeq(Integer weekSeq) {
        this.weekSeq = weekSeq;
    }
}
