package org.example.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.entity.RegionClientNumData;
import org.example.service.RegionClientNumDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 区域客户数查询服务实现类
 * 
 * 【核心功能】
 * 通过EntityManager实现动态表名的区域客户数精确查询
 * 
 * 【技术特性】
 * - 使用EntityManager原生SQL查询
 * - 支持动态表名
 * - 自动映射到RegionClientNumData实体
 * - 为实际投放量计算提供数据支持
 * 
 * @author Robin
 * @version 4.0 - 精简版，专注核心查询功能
 * @since 2025-10-19
 */
@Slf4j
@Service
public class RegionClientNumDataServiceImpl implements RegionClientNumDataService {
    
    @Autowired
    private EntityManager entityManager;
    
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
