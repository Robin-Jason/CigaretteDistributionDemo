package org.example.controller;

import org.example.entity.DemoTestAdvData;
import org.example.entity.DemoTestData;
import org.example.repository.DemoTestAdvDataRepository;
import org.example.repository.DemoTestDataRepository;
import org.example.service.algorithm.CigaretteDistributionAlgorithm;
import org.example.util.KmpMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/optimized-algorithm")
public class OptimizedAlgorithmTestController {

    @Autowired
    private DemoTestAdvDataRepository advDataRepository;

    @Autowired
    private DemoTestDataRepository testDataRepository;

    @Autowired
    private CigaretteDistributionAlgorithm distributionAlgorithm;

    @Autowired
    private KmpMatcher kmpMatcher;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 测试优化后的卷烟分配算法
     */
    @GetMapping("/test-optimized-algorithm")
    public Map<String, Object> testOptimizedAlgorithm() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 1. 获取预投放量数据
            List<DemoTestAdvData> advDataList = advDataRepository.findAll();
            response.put("advDataCount", advDataList.size());
            response.put("advData", advDataList);
            
            // 2. 获取卷烟测试数据
            List<DemoTestData> testDataList = testDataRepository.findAll();
            response.put("testDataCount", testDataList.size());
            
            // 3. 使用JdbcTemplate直接获取客户数量数据
            String clientDataSql = "SELECT URBAN_RURAL_CODE, D30, D29, D28, D27, D26, D25, D24, D23, D22, D21, D20, " +
                "D19, D18, D17, D16, D15, D14, D13, D12, D11, D10, D9, D8, D7, D6, D5, D4, D3, D2, D1, TOTAL " +
                "FROM demo_test_clientNumdata ORDER BY id ASC";
            
            List<Map<String, Object>> clientDataList = jdbcTemplate.queryForList(clientDataSql);
            response.put("clientDataCount", clientDataList.size());
            
            // 4. 构建区域客户数矩阵
            List<String> regions = new ArrayList<>();
            BigDecimal[][] regionCustomerMatrix = new BigDecimal[clientDataList.size()][30];
            
            for (int i = 0; i < clientDataList.size(); i++) {
                Map<String, Object> row = clientDataList.get(i);
                regions.add((String) row.get("URBAN_RURAL_CODE"));
                
                // 填充D30到D1的数据
                for (int j = 0; j < 30; j++) {
                    String columnName = "D" + (30 - j);
                    Object value = row.get(columnName);
                    if (value instanceof BigDecimal) {
                        regionCustomerMatrix[i][j] = (BigDecimal) value;
                    } else if (value instanceof Number) {
                        regionCustomerMatrix[i][j] = new BigDecimal(value.toString());
                    } else {
                        regionCustomerMatrix[i][j] = BigDecimal.ZERO;
                    }
                }
            }
            
            response.put("regions", regions);
            response.put("regionCustomerMatrix", regionCustomerMatrix);
            
            // 5. 测试每个卷烟的分配
            List<Map<String, Object>> algorithmResults = new ArrayList<>();
            
            for (DemoTestAdvData advData : advDataList) {
                Map<String, Object> result = new HashMap<>();
                result.put("cigCode", advData.getCigCode());
                result.put("cigName", advData.getCigName());
                result.put("adv", advData.getAdv());
                result.put("deliveryMethod", advData.getDeliveryMethod());
                result.put("deliveryEtype", advData.getDeliveryEtype());
                
                // 直接从advData获取投放区域
                String deliveryArea = advData.getDeliveryArea();
                result.put("deliveryArea", deliveryArea);
                
                if (deliveryArea != null && !deliveryArea.trim().isEmpty()) {
                    // 使用KMP匹配投放区域
                    List<String> targetRegions = kmpMatcher.matchPatterns(deliveryArea, regions);
                    result.put("matchedRegions", targetRegions);
                    
                    if (!targetRegions.isEmpty()) {
                        // 根据匹配的区域筛选客户数矩阵
                        BigDecimal[][] targetRegionCustomerMatrix = new BigDecimal[targetRegions.size()][30];
                        for (int i = 0; i < targetRegions.size(); i++) {
                            String targetRegion = targetRegions.get(i);
                            int regionIndex = regions.indexOf(targetRegion);
                            if (regionIndex >= 0) {
                                System.arraycopy(regionCustomerMatrix[regionIndex], 0, targetRegionCustomerMatrix[i], 0, 30);
                            }
                        }
                        
                        // 执行优化后的分配算法
                        BigDecimal[][] allocationMatrix = distributionAlgorithm.calculateDistribution(
                            targetRegions, targetRegionCustomerMatrix, advData.getAdv());
                        
                        // 计算实际投放量
                        BigDecimal actualAmount = calculateActualAmount(allocationMatrix, targetRegionCustomerMatrix);
                        result.put("actualAmount", actualAmount);
                        result.put("error", advData.getAdv().subtract(actualAmount).abs());
                        result.put("errorPercentage", advData.getAdv().subtract(actualAmount).abs()
                            .divide(advData.getAdv(), 4, BigDecimal.ROUND_HALF_UP)
                            .multiply(new BigDecimal("100")));
                        
                        // 验证约束
                        result.put("constraintValid", validateConstraints(allocationMatrix));
                        
                        // 输出完整的分配矩阵（所有30个档位）
                        Map<String, Object> allocationResult = new HashMap<>();
                        for (int i = 0; i < targetRegions.size(); i++) {
                            Map<String, Object> regionAllocation = new HashMap<>();
                            regionAllocation.put("region", targetRegions.get(i));
                            
                            // 添加所有30个档位的分配值
                            for (int j = 0; j < 30; j++) {
                                String columnName = "d" + (30 - j);
                                regionAllocation.put(columnName, allocationMatrix[i][j]);
                            }
                            
                            allocationResult.put("region_" + i, regionAllocation);
                        }
                        result.put("allocationMatrix", allocationResult);
                        
                        // 添加约束验证详情
                        result.put("constraintDetails", getConstraintDetails(allocationMatrix, targetRegions));
                    } else {
                        result.put("error", "未找到匹配的投放区域");
                    }
                } else {
                    result.put("error", "投放区域为空");
                }
                
                algorithmResults.add(result);
            }
            
            response.put("algorithmResults", algorithmResults);
            response.put("success", true);
            response.put("message", "优化算法测试完成");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "优化算法测试失败: " + e.getMessage());
            response.put("error", e.toString());
        }
        
        return response;
    }

    /**
     * 计算实际投放量
     */
    private BigDecimal calculateActualAmount(BigDecimal[][] allocationMatrix, BigDecimal[][] regionCustomerMatrix) {
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < allocationMatrix.length; i++) {
            for (int j = 0; j < 30; j++) {
                if (allocationMatrix[i][j] != null && regionCustomerMatrix[i][j] != null) {
                    total = total.add(allocationMatrix[i][j].multiply(regionCustomerMatrix[i][j]));
                }
            }
        }
        return total;
    }

    /**
     * 验证约束条件
     */
    private boolean validateConstraints(BigDecimal[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 1; j < matrix[i].length; j++) {
                if (matrix[i][j-1] != null && matrix[i][j] != null) {
                    if (matrix[i][j-1].compareTo(matrix[i][j]) < 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    /**
     * 获取约束验证详情
     */
    private Map<String, Object> getConstraintDetails(BigDecimal[][] matrix, List<String> regions) {
        Map<String, Object> details = new HashMap<>();
        List<Map<String, Object>> violations = new ArrayList<>();
        
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 1; j < matrix[i].length; j++) {
                if (matrix[i][j-1] != null && matrix[i][j] != null) {
                    if (matrix[i][j-1].compareTo(matrix[i][j]) < 0) {
                        Map<String, Object> violation = new HashMap<>();
                        violation.put("region", regions.get(i));
                        violation.put("higherGrade", "D" + (30 - (j-1)));
                        violation.put("higherValue", matrix[i][j-1]);
                        violation.put("lowerGrade", "D" + (30 - j));
                        violation.put("lowerValue", matrix[i][j]);
                        violations.add(violation);
                    }
                }
            }
        }
        
        details.put("violationCount", violations.size());
        details.put("violations", violations);
        details.put("isValid", violations.isEmpty());
        
        return details;
    }
}
