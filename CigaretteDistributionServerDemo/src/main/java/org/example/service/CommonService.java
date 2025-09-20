package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.entity.DemoTestAdvData;
import org.example.entity.DemoTestData;
import org.example.repository.DemoTestAdvDataRepository;
import org.example.repository.DemoTestDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * 通用服务类
 * 为五种投放类型的开发人员提供统一的数据访问和操作方法
 */
@Slf4j
@Service
public class CommonService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private DemoTestAdvDataRepository advDataRepository;
    
    @Autowired
    private DemoTestDataRepository testDataRepository;

    /**
     * 方法1：根据投放类型和扩展投放类型的组合判断属于哪种组合投放类型
     * 从对应的区域客户数矩阵表获取表中存在的已有投放区域列表AllRegionList作为输出
     * 
     * @param deliveryMethod 投放方法（如：按档位统一投放、按档位扩展投放）
     * @param deliveryEtype 扩展投放类型（如：档位+区县、档位+市场类型等，可为null）
     * @return 该投放类型对应的所有投放区域列表
     */
    public List<String> getAllRegionList(String deliveryMethod, String deliveryEtype) {
        log.info("获取投放区域列表，投放方法: {}, 扩展投放类型: {}", deliveryMethod, deliveryEtype);
        
        try {
            // 根据投放类型组合确定查询的表和字段
            String tableInfo = getTableAndFieldInfo(deliveryMethod, deliveryEtype);
            if (tableInfo == null) {
                log.warn("不支持的投放类型组合，投放方法: {}, 扩展投放类型: {}", deliveryMethod, deliveryEtype);
                return new ArrayList<>();
            }
            
            String[] parts = tableInfo.split("\\|");
            String tableName = parts[0];
            String fieldName = parts[1];
            
            // 构建查询SQL，获取所有不重复的区域名称
            String sql = String.format("SELECT DISTINCT %s FROM %s WHERE %s IS NOT NULL ORDER BY %s", 
                                     fieldName, tableName, fieldName, fieldName);
            
            log.info("执行查询SQL: {}", sql);
            
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            List<String> regionList = new ArrayList<>();
            
            for (Map<String, Object> row : result) {
                Object regionName = row.get(fieldName);
                if (regionName == null) {
                    regionName = row.get(fieldName.toUpperCase()); // 尝试大写字段名
                }
                if (regionName != null) {
                    regionList.add(regionName.toString());
                }
            }
            
            log.info("获取到 {} 个投放区域: {}", regionList.size(), regionList);
            return regionList;
            
        } catch (Exception e) {
            log.error("获取投放区域列表失败，投放方法: {}, 扩展投放类型: {}", deliveryMethod, deliveryEtype, e);
            return new ArrayList<>();
        }
    }

    /**
     * 方法2：根据投放类型从对应的区域客户数表获取区域和档位信息构建成完整的区域客户数矩阵作为输出
     * 输出矩阵类型应当为n*30，n为该投放类型的包含区域数
     * 
     * @param deliveryMethod 投放方法（如：按档位统一投放、按档位扩展投放）
     * @param deliveryEtype 扩展投放类型（如：档位+区县、档位+市场类型等，可为null）
     * @return RegionCustomerMatrix 区域客户数矩阵对象，包含区域名称列表和对应的客户数矩阵
     */
    public RegionCustomerMatrix buildRegionCustomerMatrix(String deliveryMethod, String deliveryEtype) {
        log.info("构建区域客户数矩阵，投放方法: {}, 扩展投放类型: {}", deliveryMethod, deliveryEtype);
        
        try {
            // 根据投放类型组合确定查询的表和字段
            String tableInfo = getTableAndFieldInfo(deliveryMethod, deliveryEtype);
            if (tableInfo == null) {
                log.warn("不支持的投放类型组合，投放方法: {}, 扩展投放类型: {}", deliveryMethod, deliveryEtype);
                return new RegionCustomerMatrix();
            }
            
            String[] parts = tableInfo.split("\\|");
            String tableName = parts[0];
            String fieldName = parts[1];
            
            // 构建查询SQL，获取所有区域及其30个档位的客户数
            String baseColumns = "D30, D29, D28, D27, D26, D25, D24, D23, D22, D21, " +
                               "D20, D19, D18, D17, D16, D15, D14, D13, D12, D11, " +
                               "D10, D9, D8, D7, D6, D5, D4, D3, D2, D1";
            
            String sql;
            if ("city_clientnum_data".equals(tableName)) {
                // 按档位统一投放：只查询全市数据
                sql = String.format("SELECT %s as region_name, %s FROM %s WHERE %s = '全市'", 
                                  fieldName, baseColumns, tableName, fieldName);
            } else {
                // 其他扩展投放类型：查询所有区域数据
                sql = String.format("SELECT %s as region_name, %s FROM %s ORDER BY %s", 
                                  fieldName, baseColumns, tableName, fieldName);
            }
            
            log.info("执行查询SQL: {}", sql);
            
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            
            // 构建矩阵
            List<String> regionNames = new ArrayList<>();
            List<BigDecimal[]> customerMatrix = new ArrayList<>();
            
            for (Map<String, Object> row : result) {
                // 获取区域名称
                Object regionName = row.get("region_name");
                if (regionName == null) {
                    regionName = row.get("REGION_NAME"); // 尝试大写字段名
                }
                
                if (regionName != null) {
                    regionNames.add(regionName.toString());
                    
                    // 提取30个档位的客户数
                    BigDecimal[] customerCounts = extractCustomerCounts(row);
                    customerMatrix.add(customerCounts);
                }
            }
            
            RegionCustomerMatrix matrix = new RegionCustomerMatrix(regionNames, customerMatrix);
            log.info("构建完成，矩阵维度: {}x30，包含区域: {}", matrix.getRegionCount(), regionNames);
            
            return matrix;
            
        } catch (Exception e) {
            log.error("构建区域客户数矩阵失败，投放方法: {}, 扩展投放类型: {}", deliveryMethod, deliveryEtype, e);
            return new RegionCustomerMatrix();
        }
    }

    /**
     * 方法3：根据投放类型从demo_test_ADVdata中获取目标投放类型卷烟的预投放量和投放区域
     * @param deliveryEtype 投放类型
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @return 预投放量数据列表，包含卷烟代码、名称、预投放量(ADV)、投放区域等信息
     */
    public List<DemoTestAdvData> getAdvDataByDeliveryType(String deliveryEtype, Integer year, Integer month, Integer weekSeq) {
        log.info("根据投放类型获取预投放量数据，投放类型: {}, 时间: {}-{}-{}", deliveryEtype, year, month, weekSeq);
        
        try {
            List<DemoTestAdvData> advDataList;
            
            if ("NULL".equals(deliveryEtype) || deliveryEtype == null) {
                // 全市统一投放：获取投放类型为NULL或为空的记录
                advDataList = advDataRepository.findByYearAndMonthAndWeekSeq(year, month, weekSeq)
                    .stream()
                    .filter(data -> data.getDeliveryEtype() == null || "NULL".equals(data.getDeliveryEtype()))
                    .collect(java.util.stream.Collectors.toList());
            } else {
                // 按具体投放类型获取数据
                advDataList = advDataRepository.findByYearAndMonthAndWeekSeq(year, month, weekSeq)
                    .stream()
                    .filter(data -> deliveryEtype.equals(data.getDeliveryEtype()))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            log.info("获取到 {} 条预投放量数据", advDataList.size());
            return advDataList;
            
        } catch (Exception e) {
            log.error("获取预投放量数据失败，投放类型: {}", deliveryEtype, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 方法4：批量写入DemoTestData数据到demo_test_data表
     * @param testDataList DemoTestData对象列表
     * @return 写入结果，包含成功/失败状态和详细信息
     */
    @Transactional
    public Map<String, Object> batchInsertTestData(List<DemoTestData> testDataList) {
        log.info("开始批量写入测试数据，共 {} 条记录", testDataList.size());
        
        Map<String, Object> result = new HashMap<>();
        
        if (testDataList == null || testDataList.isEmpty()) {
            result.put("success", true);
            result.put("message", "没有数据需要写入");
            result.put("totalCount", 0);
            result.put("successCount", 0);
            result.put("failCount", 0);
            return result;
        }
        
        try {
            // 使用JPA Repository进行批量保存
            List<DemoTestData> savedData = testDataRepository.saveAll(testDataList);
            
            int successCount = savedData.size();
            int failCount = testDataList.size() - successCount;
            
            result.put("success", failCount == 0);
            result.put("totalCount", testDataList.size());
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            
            if (failCount == 0) {
                result.put("message", String.format("所有 %d 条测试数据写入成功", successCount));
                log.info("批量写入测试数据完成，成功写入 {} 条记录", successCount);
            } else {
                result.put("message", String.format("部分写入失败，成功: %d，失败: %d", successCount, failCount));
                log.warn("批量写入测试数据部分失败，成功: {}, 失败: {}", successCount, failCount);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("批量写入测试数据失败", e);
            result.put("success", false);
            result.put("totalCount", testDataList.size());
            result.put("successCount", 0);
            result.put("failCount", testDataList.size());
            result.put("message", "批量写入失败: " + e.getMessage());
            result.put("errorDetails", e.getClass().getSimpleName() + ": " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 方法5：删除demo_test_data表中指定卷烟、指定日期、指定投放区域的现有数据
     * 根据卷烟信息、时间信息和投放区域精确删除匹配的投放记录
     * 
     * @param cigCode 卷烟代码
     * @param cigName 卷烟名称
     * @param year 年份
     * @param month 月份
     * @param weekSeq 周序号
     * @param deliveryArea 投放区域
     * @return 删除结果Map，包含成功状态、删除记录数和消息信息
     */
    @Transactional
    public Map<String, Object> deleteSpecificTestData(String cigCode, String cigName, 
                                                     Integer year, Integer month, Integer weekSeq, 
                                                     String deliveryArea) {
        log.info("开始删除指定条件的测试数据，卷烟: {}-{}, 时间: {}-{}-{}, 区域: {}", 
                cigCode, cigName, year, month, weekSeq, deliveryArea);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 参数验证
            if (cigCode == null || cigCode.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "卷烟代码不能为空");
                result.put("deletedCount", 0);
                return result;
            }
            
            if (cigName == null || cigName.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "卷烟名称不能为空");
                result.put("deletedCount", 0);
                return result;
            }
            
            if (year == null || month == null || weekSeq == null) {
                result.put("success", false);
                result.put("message", "时间参数不能为空");
                result.put("deletedCount", 0);
                return result;
            }
            
            if (deliveryArea == null || deliveryArea.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "投放区域不能为空");
                result.put("deletedCount", 0);
                return result;
            }
            
            // 首先查询是否存在匹配的记录
            List<DemoTestData> existingData = testDataRepository.findByYearAndMonthAndWeekSeqAndCigCodeAndDeliveryAreaIn(
                year, month, weekSeq, cigCode, Arrays.asList(deliveryArea));
            
            // 过滤出完全匹配的记录（包括卷烟名称）
            List<DemoTestData> matchingData = existingData.stream()
                .filter(data -> cigName.equals(data.getCigName()))
                .collect(java.util.stream.Collectors.toList());
            
            if (matchingData.isEmpty()) {
                result.put("success", true);
                result.put("message", "未找到匹配的记录，无需删除");
                result.put("deletedCount", 0);
                log.info("未找到匹配的记录: 卷烟{}-{}, 时间{}-{}-{}, 区域{}", 
                        cigCode, cigName, year, month, weekSeq, deliveryArea);
                return result;
            }
            
            // 执行删除操作
            testDataRepository.deleteAll(matchingData);
            int deletedCount = matchingData.size();
            
            result.put("success", true);
            result.put("deletedCount", deletedCount);
            result.put("message", String.format("成功删除 %d 条匹配记录", deletedCount));
            
            log.info("成功删除 {} 条记录: 卷烟{}-{}, 时间{}-{}-{}, 区域{}", 
                    deletedCount, cigCode, cigName, year, month, weekSeq, deliveryArea);
            
            return result;
            
        } catch (Exception e) {
            log.error("删除指定条件的测试数据失败，卷烟: {}-{}, 时间: {}-{}-{}, 区域: {}", 
                     cigCode, cigName, year, month, weekSeq, deliveryArea, e);
            result.put("success", false);
            result.put("deletedCount", 0);
            result.put("message", "删除失败: " + e.getMessage());
            result.put("errorDetails", e.getClass().getSimpleName() + ": " + e.getMessage());
            return result;
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 根据投放类型组合确定对应的表名和字段名
     * @param deliveryMethod 投放方法
     * @param deliveryEtype 扩展投放类型
     * @return 表名|字段名 格式的字符串，如果不支持则返回null
     */
    private String getTableAndFieldInfo(String deliveryMethod, String deliveryEtype) {
        // 按档位统一投放
        if ("按档位统一投放".equals(deliveryMethod) && (deliveryEtype == null || "NULL".equals(deliveryEtype))) {
            return "city_clientnum_data|URBAN_RURAL_CODE";
        }
        
        // 按档位扩展投放的各种类型
        if ("按档位扩展投放".equals(deliveryMethod) && deliveryEtype != null) {
        switch (deliveryEtype) {
            case "档位+区县":
                    return "demo_test_county_client_numdata|COUNTY";
            case "档位+市场类型":
                    return "demo_market_test_clientnumdata|URBAN_RURAL_CODE";
            case "档位+城乡分类代码":
                    return "demo_test_clientNumdata|URBAN_RURAL_CODE";
            case "档位+业态":
                    return "demo_test_businessFormat_clientNumData|BusinessFormat";
            }
        }
        
        return null; // 不支持的组合
    }
    
    /**
     * 从查询结果行中提取30个档位的客户数
     * @param row 查询结果行
     * @return 30个档位的客户数数组（按D30到D1的顺序）
     */
    private BigDecimal[] extractCustomerCounts(Map<String, Object> row) {
        BigDecimal[] customerCounts = new BigDecimal[30];
        
        for (int i = 0; i < 30; i++) {
            String columnName = "D" + (30 - i); // D30对应数组索引0，D1对应数组索引29
            Object value = row.get(columnName);
            if (value == null) {
                value = row.get(columnName.toLowerCase()); // 尝试小写字段名
            }
            
            if (value != null) {
                customerCounts[i] = new BigDecimal(value.toString());
            } else {
                customerCounts[i] = BigDecimal.ZERO;
            }
        }
        
        return customerCounts;
    }
    
    // ==================== 内部数据类 ====================

    /**
     * 区域客户数矩阵数据类
     * 包含区域名称列表和对应的客户数矩阵（n*30）
     */
    public static class RegionCustomerMatrix {
        private List<String> regionNames;           // 区域名称列表
        private List<BigDecimal[]> customerMatrix;  // 客户数矩阵，每行30个档位数据
        
        // 默认构造函数
        public RegionCustomerMatrix() {
            this.regionNames = new ArrayList<>();
            this.customerMatrix = new ArrayList<>();
        }
        
        // 带参数构造函数
        public RegionCustomerMatrix(List<String> regionNames, List<BigDecimal[]> customerMatrix) {
            this.regionNames = regionNames != null ? regionNames : new ArrayList<>();
            this.customerMatrix = customerMatrix != null ? customerMatrix : new ArrayList<>();
        }
        
        // Getters and Setters
        public List<String> getRegionNames() {
            return regionNames;
        }
        
        public void setRegionNames(List<String> regionNames) {
            this.regionNames = regionNames;
        }
        
        public List<BigDecimal[]> getCustomerMatrix() {
            return customerMatrix;
        }
        
        public void setCustomerMatrix(List<BigDecimal[]> customerMatrix) {
            this.customerMatrix = customerMatrix;
        }
        
        /**
         * 获取区域数量
         */
        public int getRegionCount() {
            return regionNames.size();
        }
        
        /**
         * 获取档位数量（固定为30）
         */
        public int getGradeCount() {
            return 30;
        }
        
        /**
         * 根据区域名称获取客户数数组
         */
        public BigDecimal[] getCustomerCountsByRegion(String regionName) {
            int index = regionNames.indexOf(regionName);
            if (index >= 0 && index < customerMatrix.size()) {
                return customerMatrix.get(index);
            }
            return null;
        }
        
        /**
         * 检查矩阵是否为空
         */
        public boolean isEmpty() {
            return regionNames.isEmpty() || customerMatrix.isEmpty();
        }
        
        @Override
        public String toString() {
            return String.format("RegionCustomerMatrix{regionCount=%d, gradeCount=%d, regions=%s}", 
                               getRegionCount(), getGradeCount(), regionNames);
        }
    }
}
