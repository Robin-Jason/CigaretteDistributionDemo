package org.example.service.algorithm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

/**
 * 业态类型卷烟分配算法
 * 改为委托通用基础算法，避免重复实现
 */
@Slf4j
@Service
public class BussinessFormatDistributionAlgorithm {
    
    @Autowired
    private BaseCigaretteDistributionAlgorithm baseAlgorithm;
    
    /**
     * 卷烟分配算法（委托给基础算法实现）
     * @param targetBusinessFormats 目标业态类型列表
     * @param businessFormatCustomerMatrix 业态类型客户数矩阵 [业态类型数][档位数]
     * @param targetAmount 预投放量
     * @return 分配矩阵 [业态类型数][档位数]
     */
    public BigDecimal[][] calculateDistribution(List<String> targetBusinessFormats, 
                                             BigDecimal[][] businessFormatCustomerMatrix, 
                                             BigDecimal targetAmount) {
        return baseAlgorithm.calculateDistribution(targetBusinessFormats, businessFormatCustomerMatrix, targetAmount);
    }
}
