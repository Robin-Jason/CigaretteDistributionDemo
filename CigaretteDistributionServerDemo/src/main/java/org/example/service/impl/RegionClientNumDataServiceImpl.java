package org.example.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.entity.RegionClientNumData;
import org.example.service.RegionClientNumDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 区域客户数统一数据服务实现类
 * 
 * 通过EntityManager实现动态表名的数据库操作
 * 支持所有投放类型的区域客户数数据管理
 */
@Slf4j
@Service
public class RegionClientNumDataServiceImpl implements RegionClientNumDataService {
    
    @Autowired
    private EntityManager entityManager;
    
    @Override
    public List<RegionClientNumData> findAllByTableName(String tableName) {
        try {
            String sql = String.format("SELECT * FROM `%s` ORDER BY id ASC", tableName);
            Query query = entityManager.createNativeQuery(sql);
            List<Object[]> results = query.getResultList();
            
            return convertToRegionClientNumDataList(results);
        } catch (Exception e) {
            log.error("查询表 {} 的所有数据失败", tableName, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<RegionClientNumData> findByTableNameAndRegion(String tableName, String region) {
        try {
            String sql = String.format("SELECT * FROM `%s` WHERE region = ? ORDER BY id ASC", tableName);
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, region);
            List<Object[]> results = query.getResultList();
            
            return convertToRegionClientNumDataList(results);
        } catch (Exception e) {
            log.error("查询表 {} 中区域 {} 的数据失败", tableName, region, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public boolean tableExists(String tableName) {
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?";
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, tableName);
            Object result = query.getSingleResult();
            return ((Number) result).intValue() > 0;
        } catch (Exception e) {
            log.error("检查表 {} 是否存在失败", tableName, e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public void createTable(String tableName) {
        try {
            String sql = String.format(
                "CREATE TABLE `%s` (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "region VARCHAR(100) NOT NULL COMMENT '区域标识', " +
                "D30 DECIMAL(10,2) DEFAULT 0.00 COMMENT '档位30', " +
                "D29 DECIMAL(10,2) DEFAULT 0.00 COMMENT '档位29', " +
                "D28 DECIMAL(10,2) DEFAULT 0.00 COMMENT '档位28', " +
                "D27 DECIMAL(10,2) DEFAULT 0.00 COMMENT '档位27', " +
                "D26 DECIMAL(10,2) DEFAULT 0.00 COMMENT '档位26', " +
                "D25 DECIMAL(10,2) DEFAULT 0.00 COMMENT '档位25', " +
                "D24 DECIMAL(10,2) DEFAULT 0.00 COMMENT '档位24', " +
                "D23 DECIMAL(10,2) DEFAULT 0.00 COMMENT '档位23', " +
                "D22 DECIMAL(10,2) DEFAULT 0.00 COMMENT '档位22', " +
                "D21 DECIMAL(10,2) DEFAULT 0.00 COMMENT '档位21', " +
                "D20 DECIMAL(10,2) DEFAULT 0.00 COMMENT '档位20', " +
                "D19 DECIMAL(10,2) DEFAULT 0.00 COMMENT '档位19', " +
                "D18 DECIMAL(10,2) DEFAULT 0.00 COMMENT '档位18', " +
                "D17 DECIMAL(10,2) DEFAULT 0.00 COMMENT '档位17', " +
                "D16 DECIMAL(10,2) DEFAULT 0.00 COMMENT '档位16', " +
                "D15 DECIMAL(10,2) DEFAULT 0.00 COMMENT '档位15', " +
                "D14 DECIMAL(10,2) DEFAULT 0.00 COMMENT '档位14', " +
                "D13 DECIMAL(10,2) DEFAULT 0.00 COMMENT '档位13', " +
                "D12 DECIMAL(10,2) DEFAULT 0.00 COMMENT '档位12', " +
                "D11 DECIMAL(10,2) DEFAULT 0.00 COMMENT '档位11', " +
                "D10 DECIMAL(10,2) DEFAULT 0.00 COMMENT '档位10', " +
                "D9 DECIMAL(10,2) DEFAULT 0.00 COMMENT '档位9', " +
                "D8 DECIMAL(10,2) DEFAULT 0.00 COMMENT '档位8', " +
                "D7 DECIMAL(10,2) DEFAULT 0.00 COMMENT '档位7', " +
                "D6 DECIMAL(10,2) DEFAULT 0.00 COMMENT '档位6', " +
                "D5 DECIMAL(10,2) DEFAULT 0.00 COMMENT '档位5', " +
                "D4 DECIMAL(10,2) DEFAULT 0.00 COMMENT '档位4', " +
                "D3 DECIMAL(10,2) DEFAULT 0.00 COMMENT '档位3', " +
                "D2 DECIMAL(10,2) DEFAULT 0.00 COMMENT '档位2', " +
                "D1 DECIMAL(10,2) DEFAULT 0.00 COMMENT '档位1', " +
                "TOTAL DECIMAL(15,2) DEFAULT 0.00 COMMENT '总计'" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区域客户数数据表'", tableName);
            
            Query query = entityManager.createNativeQuery(sql);
            query.executeUpdate();
            log.info("成功创建表: {}", tableName);
        } catch (Exception e) {
            log.error("创建表 {} 失败", tableName, e);
            throw new RuntimeException("创建表失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public void clearTableData(String tableName) {
        try {
            String sql = String.format("DELETE FROM `%s`", tableName);
            Query query = entityManager.createNativeQuery(sql);
            int deletedCount = query.executeUpdate();
            log.info("成功清空表 {} 的数据，删除 {} 条记录", tableName, deletedCount);
        } catch (Exception e) {
            log.error("清空表 {} 的数据失败", tableName, e);
            throw new RuntimeException("清空表数据失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public void resetAutoIncrement(String tableName) {
        try {
            String sql = String.format("ALTER TABLE `%s` AUTO_INCREMENT = 1", tableName);
            Query query = entityManager.createNativeQuery(sql);
            query.executeUpdate();
            log.debug("成功重置表 {} 的自增ID", tableName);
        } catch (Exception e) {
            log.error("重置表 {} 的自增ID失败", tableName, e);
            throw new RuntimeException("重置自增ID失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public int batchInsertOrReplace(String tableName, List<Map<String, Object>> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return 0;
        }
        
        try {
            // 先清空数据并重置自增ID
            clearTableData(tableName);
            resetAutoIncrement(tableName);
            
            // 构建批量插入SQL
            StringBuilder sql = new StringBuilder();
            sql.append(String.format("INSERT INTO `%s` (`region`, ", tableName));
            
            // 添加档位列名
            for (int i = 30; i >= 1; i--) {
                sql.append("`D").append(i).append("`, ");
            }
            sql.append("`TOTAL`) VALUES ");
            
            // 构建VALUES部分
            for (int i = 0; i < dataList.size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append("(?, ");
                // 30个档位参数占位符
                for (int j = 0; j < 30; j++) {
                    sql.append("?, ");
                }
                sql.append("?)"); // TOTAL字段
            }
            
            Query query = entityManager.createNativeQuery(sql.toString());
            
            // 设置参数
            int paramIndex = 1;
            for (Map<String, Object> row : dataList) {
                // 区域字段
                query.setParameter(paramIndex++, row.get("region"));
                
                // 档位字段D30到D1
                for (int i = 30; i >= 1; i--) {
                    Object value = row.get("D" + i);
                    query.setParameter(paramIndex++, value != null ? value : BigDecimal.ZERO);
                }
                
                // TOTAL字段
                Object total = row.get("TOTAL");
                query.setParameter(paramIndex++, total != null ? total : BigDecimal.ZERO);
            }
            
            int insertedCount = query.executeUpdate();
            log.info("成功向表 {} 批量插入 {} 条数据", tableName, insertedCount);
            return insertedCount;
            
        } catch (Exception e) {
            log.error("向表 {} 批量插入数据失败", tableName, e);
            throw new RuntimeException("批量插入数据失败: " + e.getMessage());
        }
    }
    
    @Override
    public String generateTableName(String deliveryMethod, String deliveryEtype, Boolean isBiWeeklyFloat) {
        Integer mainSeq = getSequenceNumber(deliveryMethod, deliveryEtype);
        Integer subSeq = getSubSequenceNumber(isBiWeeklyFloat);
        return String.format("region_clientNum_%d_%d", mainSeq, subSeq);
    }
    
    @Override
    public Integer getSequenceNumber(String deliveryMethod, String deliveryEtype) {
        if ("按档位统一投放".equals(deliveryMethod)) {
            return 0;  // 全市统一投放
        } else if ("按档位扩展投放".equals(deliveryMethod)) {
            switch (deliveryEtype != null ? deliveryEtype : "") {
                case "档位+区县": return 1;
                case "档位+市场类型": return 2;
                case "档位+城乡分类代码": return 3;
                case "档位+业态": return 4;
                default: return 0;
            }
        }
        return 0; // 默认值
    }
    
    @Override
    public Integer getSubSequenceNumber(Boolean isBiWeeklyFloat) {
        // 所有投放类型均支持双周上浮区分
        return (isBiWeeklyFloat != null && isBiWeeklyFloat) ? 2 : 1;
    }
    
    /**
     * 将查询结果转换为RegionClientNumData对象列表
     * @param results 原生查询结果
     * @return RegionClientNumData对象列表
     */
    private List<RegionClientNumData> convertToRegionClientNumDataList(List<Object[]> results) {
        List<RegionClientNumData> dataList = new ArrayList<>();
        
        for (Object[] row : results) {
            RegionClientNumData data = new RegionClientNumData();
            
            // 设置基本字段
            data.setId((Integer) row[0]);
            data.setRegion((String) row[1]);
            
            // 设置档位字段 D30-D1 (索引2-31)
            data.setD30((BigDecimal) row[2]);
            data.setD29((BigDecimal) row[3]);
            data.setD28((BigDecimal) row[4]);
            data.setD27((BigDecimal) row[5]);
            data.setD26((BigDecimal) row[6]);
            data.setD25((BigDecimal) row[7]);
            data.setD24((BigDecimal) row[8]);
            data.setD23((BigDecimal) row[9]);
            data.setD22((BigDecimal) row[10]);
            data.setD21((BigDecimal) row[11]);
            data.setD20((BigDecimal) row[12]);
            data.setD19((BigDecimal) row[13]);
            data.setD18((BigDecimal) row[14]);
            data.setD17((BigDecimal) row[15]);
            data.setD16((BigDecimal) row[16]);
            data.setD15((BigDecimal) row[17]);
            data.setD14((BigDecimal) row[18]);
            data.setD13((BigDecimal) row[19]);
            data.setD12((BigDecimal) row[20]);
            data.setD11((BigDecimal) row[21]);
            data.setD10((BigDecimal) row[22]);
            data.setD9((BigDecimal) row[23]);
            data.setD8((BigDecimal) row[24]);
            data.setD7((BigDecimal) row[25]);
            data.setD6((BigDecimal) row[26]);
            data.setD5((BigDecimal) row[27]);
            data.setD4((BigDecimal) row[28]);
            data.setD3((BigDecimal) row[29]);
            data.setD2((BigDecimal) row[30]);
            data.setD1((BigDecimal) row[31]);
            
            // 设置总计字段 (索引32)
            data.setTotal((BigDecimal) row[32]);
            
            dataList.add(data);
        }
        
        return dataList;
    }
}
