package org.example.repository;

import org.example.entity.RegionClientNumData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 区域客户数统一Repository接口
 * 
 * 支持所有投放类型的区域客户数数据访问：
 * - 按档位统一投放（region_clientNum_0_1/2）
 * - 档位+区县（region_clientNum_1_1/2）  
 * - 档位+市场类型（region_clientNum_2_1/2）
 * - 档位+城乡分类代码（region_clientNum_3_1/2）
 * - 档位+业态（region_clientNum_4_1/2）
 * 
 * 注意：由于JPA不支持动态表名，实际的动态表操作通过Service层的EntityManager实现
 */
@Repository
public interface RegionClientNumDataRepository extends JpaRepository<RegionClientNumData, Integer> {
    
    /**
     * 根据区域查询数据
     * @param region 区域标识
     * @return 匹配的区域客户数数据列表
     */
    List<RegionClientNumData> findByRegion(String region);
    
    /**
     * 根据区域查询数据（按ID升序）
     * @param region 区域标识
     * @return 匹配的区域客户数数据列表
     */
    List<RegionClientNumData> findByRegionOrderByIdAsc(String region);
    
    /**
     * 查询所有数据（按ID升序）
     * @return 所有区域客户数数据
     */
    List<RegionClientNumData> findAllByOrderByIdAsc();
}
