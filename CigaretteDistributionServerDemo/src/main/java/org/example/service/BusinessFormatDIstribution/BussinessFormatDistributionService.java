package org.example.service.BusinessFormatDIstribution;

import lombok.extern.slf4j.Slf4j;

import org.example.service.CommonService;

import org.example.service.algorithm.BussinessFormatDistributionAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 业态类型卷烟分配服务
 * 专门处理"档位+业态"业务类型
 */
@Slf4j
@Service
public class BussinessFormatDistributionService {
    


    @Autowired
    private BussinessFormatDistributionAlgorithm distributionAlgorithm;



    @Autowired
    private org.example.util.KmpMatcher kmpMatcher;
    
    @Autowired
    private CommonService commonService;
    

    
    // 缓存业态类型列表
    private List<String> allBusinessFormatList;
    
//    // 缓存业态类型客户数矩阵
//    private BigDecimal[][] businessFormatCustomerMatrix;
//
    /**
     * 获取所有业态类型列表（按id从小到大排序去重）
     */
    @Cacheable("allBusinessFormatList")
    public List<String> getAllBusinessFormatList() {
        if (allBusinessFormatList == null) {
            log.info("初始化业态类型列表(通用服务)");
            allBusinessFormatList = commonService.getAllRegionList("按档位扩展投放", "档位+业态")
                    .stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            log.info("业态类型列表初始化完成，共{}个业态类型", allBusinessFormatList.size());
        }
        return allBusinessFormatList;
    }

    /**
     * 获取业态类型客户数矩阵
     */
//
//    @Cacheable("businessFormatCustomerMatrix")
//    public BigDecimal[][] getBusinessFormatCustomerMatrix() {
//        if (businessFormatCustomerMatrix == null) {
//            log.info("初始化业态类型客户数矩阵(通用服务)");
//            CommonService.RegionCustomerMatrix matrix = commonService.buildRegionCustomerMatrix("按档位扩展投放", "档位+业态");
//            List<BigDecimal[]> rows = matrix.getCustomerMatrix();
//            businessFormatCustomerMatrix = new BigDecimal[rows.size()][30];
//            for (int i = 0; i < rows.size(); i++) {
//                BigDecimal[] row = rows.get(i);
//                System.arraycopy(row, 0, businessFormatCustomerMatrix[i], 0, 30);
//            }
//            // 同步缓存的名称列表
//            this.allBusinessFormatList = matrix.getRegionNames();
//            log.info("业态类型客户数矩阵初始化完成，矩阵大小: {}x30", rows.size());
//        }
//        return businessFormatCustomerMatrix;
//    }
    /**
     * 获取卷烟的待投放业态类型列表
     * @param deliveryArea 投放区域字段值
     * @return 匹配成功的业态类型列表
     */
    //这里可以看下调用getAdvDataByDeliveryType
    public List<String> getTargetBusinessFormatList(String deliveryArea) {
        log.info("获取卷烟投放业态类型，投放区域字段值: {}", deliveryArea);
        List<String> allBusinessFormats = getAllBusinessFormatList();
        List<String> targetBusinessFormats = kmpMatcher.matchPatterns(deliveryArea, allBusinessFormats);
        log.info("匹配成功的业态类型: {}", targetBusinessFormats);
        return targetBusinessFormats;
    }
    
    /**
     * 计算卷烟分配矩阵（业态类型）
     * @param targetBusinessFormats 目标业态类型列表
     * @param targetAmount 预投放量
     * @return 分配矩阵 [业态类型数][档位数]
     */
    public BigDecimal[][] calculateBusinessFormatDistributionMatrix(List<String> targetBusinessFormats, BigDecimal targetAmount) {
        log.info("计算卷烟业态类型分配矩阵，目标业态类型数: {}, 预投放量: {}", targetBusinessFormats.size(), targetAmount);
        // 使用通用服务获取完整矩阵和区域名，用于筛选
        CommonService.RegionCustomerMatrix fullMatrix = commonService.buildRegionCustomerMatrix("按档位扩展投放", "档位+业态");
        List<String> allFormats = fullMatrix.getRegionNames();
        List<BigDecimal[]> rows = fullMatrix.getCustomerMatrix();
        
        BigDecimal[][] targetMatrix = new BigDecimal[targetBusinessFormats.size()][30];
        for (int i = 0; i < targetBusinessFormats.size(); i++) {
            String fmt = targetBusinessFormats.get(i);
            int idx = allFormats.indexOf(fmt);
            if (idx >= 0) {
                BigDecimal[] row = rows.get(idx);
                System.arraycopy(row, 0, targetMatrix[i], 0, 30);
            } else {
                targetMatrix[i] = new BigDecimal[30];
                for (int g = 0; g < 30; g++) targetMatrix[i][g] = BigDecimal.ZERO;
            }
        }
        return distributionAlgorithm.calculateDistribution(targetBusinessFormats, targetMatrix, targetAmount);
    }

}
