package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.service.impl.DistributionCalculateServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

/**
 * 分配计算服务调试测试
 * 
 * 用于分析一键生成分配方案失败的具体原因
 */
@Slf4j
@SpringBootTest
public class DistributionCalculateServiceDebugTest {
    
    @Autowired
    private DistributionCalculateServiceImpl distributionCalculateService;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * 分析数据库中实际的投放方法和类型数据
     */
    @Test
    public void analyzeDeliveryMethodsAndTypes() {
        log.info("=== 分析数据库中的投放方法和类型 ===");
        
        try {
            // 查找最近的投放信息表
            String findTablesSql = "SELECT table_name FROM information_schema.tables " +
                                  "WHERE table_schema = DATABASE() AND table_name LIKE 'cigarette_distribution_info_%' " +
                                  "ORDER BY table_name DESC LIMIT 5";
            List<Map<String, Object>> tables = jdbcTemplate.queryForList(findTablesSql);
            
            log.info("找到 {} 个投放信息表:", tables.size());
            for (Map<String, Object> table : tables) {
                String tableName = (String) table.get("table_name");
                log.info("  - {}", tableName);
                
                // 分析每个表中的投放方法和类型
                analyzeTableData(tableName);
            }
            
        } catch (Exception e) {
            log.error("分析数据库数据失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 分析特定表中的数据
     */
    private void analyzeTableData(String tableName) {
        try {
            // 统计投放方法的分布
            String deliveryMethodSql = String.format(
                "SELECT DELIVERY_METHOD, COUNT(*) as count FROM `%s` GROUP BY DELIVERY_METHOD", tableName);
            List<Map<String, Object>> methodStats = jdbcTemplate.queryForList(deliveryMethodSql);
            
            log.info("表 {} 中的投放方法分布:", tableName);
            for (Map<String, Object> stat : methodStats) {
                log.info("  - {}: {} 条记录", stat.get("DELIVERY_METHOD"), stat.get("count"));
            }
            
            // 统计投放类型的分布
            String deliveryEtypeSql = String.format(
                "SELECT DELIVERY_ETYPE, COUNT(*) as count FROM `%s` GROUP BY DELIVERY_ETYPE", tableName);
            List<Map<String, Object>> typeStats = jdbcTemplate.queryForList(deliveryEtypeSql);
            
            log.info("表 {} 中的投放类型分布:", tableName);
            for (Map<String, Object> stat : typeStats) {
                log.info("  - {}: {} 条记录", stat.get("DELIVERY_ETYPE"), stat.get("count"));
            }
            
            // 获取具体的数据样本
            String sampleSql = String.format(
                "SELECT CIG_CODE, CIG_NAME, DELIVERY_METHOD, DELIVERY_ETYPE, DELIVERY_AREA " +
                "FROM `%s` LIMIT 5", tableName);
            List<Map<String, Object>> samples = jdbcTemplate.queryForList(sampleSql);
            
            log.info("表 {} 中的数据样本:", tableName);
            for (Map<String, Object> sample : samples) {
                log.info("  卷烟: {} - {}, 投放方法: {}, 投放类型: {}, 投放区域: {}", 
                        sample.get("CIG_CODE"), sample.get("CIG_NAME"), 
                        sample.get("DELIVERY_METHOD"), sample.get("DELIVERY_ETYPE"), 
                        sample.get("DELIVERY_AREA"));
            }
            
        } catch (Exception e) {
            log.error("分析表 {} 数据失败: {}", tableName, e.getMessage());
        }
    }
    
    /**
     * 测试实际的一键生成分配方案，捕获详细错误
     */
    @Test
    public void testActualDistributionGeneration() {
        log.info("=== 测试实际的一键生成分配方案 ===");
        
        try {
            // 使用实际存在的表对应的时间参数进行测试
            Map<String, Object> result = distributionCalculateService.getAndwriteBackAllocationMatrix(2025, 9, 3);
            
            log.info("一键生成分配方案结果:");
            log.info("  成功状态: {}", result.get("success"));
            log.info("  总数量: {}", result.get("totalCount"));
            log.info("  成功数量: {}", result.get("successCount"));
            log.info("  消息: {}", result.get("message"));
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) result.get("results");
            
            if (results != null) {
                log.info("详细结果:");
                for (int i = 0; i < Math.min(results.size(), 10); i++) {
                    Map<String, Object> cigResult = results.get(i);
                    log.info("  卷烟[{}]: {} - {}", i+1, cigResult.get("cigCode"), cigResult.get("cigName"));
                    log.info("    投放方法: {}, 投放类型: {}", cigResult.get("deliveryMethod"), cigResult.get("deliveryEtype"));
                    log.info("    写回状态: {}, 消息: {}", cigResult.get("writeBackStatus"), cigResult.get("writeBackMessage"));
                    log.info("    目标类型: {}, 算法: {}", cigResult.get("targetType"), cigResult.get("algorithm"));
                }
                
                // 统计失败的原因
                Map<String, Integer> failureReasons = new java.util.HashMap<>();
                for (Map<String, Object> cigResult : results) {
                    String status = (String) cigResult.get("writeBackStatus");
                    if (!"成功".equals(status)) {
                        String message = (String) cigResult.get("writeBackMessage");
                        failureReasons.merge(message, 1, Integer::sum);
                    }
                }
                
                log.info("失败原因统计:");
                for (Map.Entry<String, Integer> entry : failureReasons.entrySet()) {
                    log.info("  {}: {} 次", entry.getKey(), entry.getValue());
                }
            }
            
        } catch (Exception e) {
            log.error("测试一键生成分配方案失败: {}", e.getMessage(), e);
        }
    }
}
