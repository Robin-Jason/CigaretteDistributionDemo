-- ================================================================
-- 档位+城乡分类代码算法测试数据生成脚本
-- ================================================================
-- 功能：生成测试用例，投放量从100到200000，卷烟代码8位数字
-- 区域：从7个区域（主城区、乡中心区、城乡结合区、村庄、特殊区域、镇中心区、镇乡结合区）中随机组合
-- 预投放量规则：100, 500, 1000, 然后每1000区间随机一个数
-- ================================================================

USE marketing;

-- 清空旧数据
TRUNCATE TABLE cigarette_distribution_info_2099_1_1;

-- ================================================================
-- 定义7个城乡分类区域（除全市外）
-- ================================================================
-- 1. 主城区
-- 2. 乡中心区  
-- 3. 城乡结合区
-- 4. 村庄
-- 5. 特殊区域
-- 6. 镇中心区
-- 7. 镇乡结合区

DELIMITER $$

DROP PROCEDURE IF EXISTS generate_urban_rural_test_cases$$

CREATE PROCEDURE generate_urban_rural_test_cases()
BEGIN
    DECLARE v_code VARCHAR(20);
    DECLARE v_name VARCHAR(100);
    DECLARE v_adv DECIMAL(15, 2);
    DECLARE v_areas VARCHAR(500);
    DECLARE v_region_count INT;
    DECLARE v_index INT DEFAULT 1;
    DECLARE v_random_offset INT;
    
    -- 7个区域的所有可能组合（按区域数分组）
    -- 2个区域的组合（21种）
    DECLARE region_2 TEXT DEFAULT '主城区,乡中心区|主城区,城乡结合区|主城区,村庄|主城区,特殊区域|主城区,镇中心区|主城区,镇乡结合区|乡中心区,城乡结合区|乡中心区,村庄|乡中心区,特殊区域|乡中心区,镇中心区|乡中心区,镇乡结合区|城乡结合区,村庄|城乡结合区,特殊区域|城乡结合区,镇中心区|城乡结合区,镇乡结合区|村庄,特殊区域|村庄,镇中心区|村庄,镇乡结合区|特殊区域,镇中心区|特殊区域,镇乡结合区|镇中心区,镇乡结合区';
    
    -- 3个区域的组合（35种）
    DECLARE region_3 TEXT DEFAULT '主城区,乡中心区,城乡结合区|主城区,乡中心区,村庄|主城区,乡中心区,特殊区域|主城区,乡中心区,镇中心区|主城区,乡中心区,镇乡结合区|主城区,城乡结合区,村庄|主城区,城乡结合区,特殊区域|主城区,城乡结合区,镇中心区|主城区,城乡结合区,镇乡结合区|主城区,村庄,特殊区域|主城区,村庄,镇中心区|主城区,村庄,镇乡结合区|主城区,特殊区域,镇中心区|主城区,特殊区域,镇乡结合区|主城区,镇中心区,镇乡结合区|乡中心区,城乡结合区,村庄|乡中心区,城乡结合区,特殊区域|乡中心区,城乡结合区,镇中心区|乡中心区,城乡结合区,镇乡结合区|乡中心区,村庄,特殊区域|乡中心区,村庄,镇中心区|乡中心区,村庄,镇乡结合区|乡中心区,特殊区域,镇中心区|乡中心区,特殊区域,镇乡结合区|乡中心区,镇中心区,镇乡结合区|城乡结合区,村庄,特殊区域|城乡结合区,村庄,镇中心区|城乡结合区,村庄,镇乡结合区|城乡结合区,特殊区域,镇中心区|城乡结合区,特殊区域,镇乡结合区|城乡结合区,镇中心区,镇乡结合区|村庄,特殊区域,镇中心区|村庄,特殊区域,镇乡结合区|村庄,镇中心区,镇乡结合区|特殊区域,镇中心区,镇乡结合区';
    
    -- 4个区域的组合（35种）
    DECLARE region_4 TEXT DEFAULT '主城区,乡中心区,城乡结合区,村庄|主城区,乡中心区,城乡结合区,特殊区域|主城区,乡中心区,城乡结合区,镇中心区|主城区,乡中心区,城乡结合区,镇乡结合区|主城区,乡中心区,村庄,特殊区域|主城区,乡中心区,村庄,镇中心区|主城区,乡中心区,村庄,镇乡结合区|主城区,乡中心区,特殊区域,镇中心区|主城区,乡中心区,特殊区域,镇乡结合区|主城区,乡中心区,镇中心区,镇乡结合区|主城区,城乡结合区,村庄,特殊区域|主城区,城乡结合区,村庄,镇中心区|主城区,城乡结合区,村庄,镇乡结合区|主城区,城乡结合区,特殊区域,镇中心区|主城区,城乡结合区,特殊区域,镇乡结合区|主城区,城乡结合区,镇中心区,镇乡结合区|主城区,村庄,特殊区域,镇中心区|主城区,村庄,特殊区域,镇乡结合区|主城区,村庄,镇中心区,镇乡结合区|主城区,特殊区域,镇中心区,镇乡结合区|乡中心区,城乡结合区,村庄,特殊区域|乡中心区,城乡结合区,村庄,镇中心区|乡中心区,城乡结合区,村庄,镇乡结合区|乡中心区,城乡结合区,特殊区域,镇中心区|乡中心区,城乡结合区,特殊区域,镇乡结合区|乡中心区,城乡结合区,镇中心区,镇乡结合区|乡中心区,村庄,特殊区域,镇中心区|乡中心区,村庄,特殊区域,镇乡结合区|乡中心区,村庄,镇中心区,镇乡结合区|乡中心区,特殊区域,镇中心区,镇乡结合区|城乡结合区,村庄,特殊区域,镇中心区|城乡结合区,村庄,特殊区域,镇乡结合区|城乡结合区,村庄,镇中心区,镇乡结合区|城乡结合区,特殊区域,镇中心区,镇乡结合区|村庄,特殊区域,镇中心区,镇乡结合区';
    
    -- 5个区域的组合（21种）
    DECLARE region_5 TEXT DEFAULT '主城区,乡中心区,城乡结合区,村庄,特殊区域|主城区,乡中心区,城乡结合区,村庄,镇中心区|主城区,乡中心区,城乡结合区,村庄,镇乡结合区|主城区,乡中心区,城乡结合区,特殊区域,镇中心区|主城区,乡中心区,城乡结合区,特殊区域,镇乡结合区|主城区,乡中心区,城乡结合区,镇中心区,镇乡结合区|主城区,乡中心区,村庄,特殊区域,镇中心区|主城区,乡中心区,村庄,特殊区域,镇乡结合区|主城区,乡中心区,村庄,镇中心区,镇乡结合区|主城区,乡中心区,特殊区域,镇中心区,镇乡结合区|主城区,城乡结合区,村庄,特殊区域,镇中心区|主城区,城乡结合区,村庄,特殊区域,镇乡结合区|主城区,城乡结合区,村庄,镇中心区,镇乡结合区|主城区,城乡结合区,特殊区域,镇中心区,镇乡结合区|主城区,村庄,特殊区域,镇中心区,镇乡结合区|乡中心区,城乡结合区,村庄,特殊区域,镇中心区|乡中心区,城乡结合区,村庄,特殊区域,镇乡结合区|乡中心区,城乡结合区,村庄,镇中心区,镇乡结合区|乡中心区,城乡结合区,特殊区域,镇中心区,镇乡结合区|乡中心区,村庄,特殊区域,镇中心区,镇乡结合区|城乡结合区,村庄,特殊区域,镇中心区,镇乡结合区';
    
    -- 6个区域的组合（7种）
    DECLARE region_6 TEXT DEFAULT '主城区,乡中心区,城乡结合区,村庄,特殊区域,镇中心区|主城区,乡中心区,城乡结合区,村庄,特殊区域,镇乡结合区|主城区,乡中心区,城乡结合区,村庄,镇中心区,镇乡结合区|主城区,乡中心区,城乡结合区,特殊区域,镇中心区,镇乡结合区|主城区,乡中心区,村庄,特殊区域,镇中心区,镇乡结合区|主城区,城乡结合区,村庄,特殊区域,镇中心区,镇乡结合区|乡中心区,城乡结合区,村庄,特殊区域,镇中心区,镇乡结合区';
    
    -- 7个区域（全部）
    DECLARE region_7 TEXT DEFAULT '主城区,乡中心区,城乡结合区,村庄,特殊区域,镇中心区,镇乡结合区';
    
    DECLARE combination_index INT;
    DECLARE combination_count INT;
    DECLARE combinations TEXT;
    DECLARE v_current_range_start INT;
    
    -- ============================================================
    -- 1. 插入固定测试用例：100, 500, 1000
    -- ============================================================
    
    -- 测试用例1: 100条
    INSERT INTO cigarette_distribution_info_2099_1_1 
    (CIG_CODE, CIG_NAME, ADV, DELIVERY_METHOD, DELIVERY_ETYPE, DELIVERY_AREA, bz)
    VALUES 
    ('10000001', '测试烟-100条', 100, '按档位扩展投放', '档位+城乡分类代码', '主城区,乡中心区', '固定测试-100条');
    
    -- 测试用例2: 500条
    INSERT INTO cigarette_distribution_info_2099_1_1 
    (CIG_CODE, CIG_NAME, ADV, DELIVERY_METHOD, DELIVERY_ETYPE, DELIVERY_AREA, bz)
    VALUES 
    ('10000002', '测试烟-500条', 500, '按档位扩展投放', '档位+城乡分类代码', '城乡结合区,镇中心区,村庄', '固定测试-500条');
    
    -- 测试用例3: 1000条
    INSERT INTO cigarette_distribution_info_2099_1_1 
    (CIG_CODE, CIG_NAME, ADV, DELIVERY_METHOD, DELIVERY_ETYPE, DELIVERY_AREA, bz)
    VALUES 
    ('10000003', '测试烟-1000条', 1000, '按档位扩展投放', '档位+城乡分类代码', '主城区,城乡结合区,镇中心区,镇乡结合区', '固定测试-1000条');
    
    SET v_index = 4;
    
    -- ============================================================
    -- 2. 生成随机测试用例：从1000-2000, 2000-3000, ..., 199000-200000
    -- ============================================================
    
    SET v_current_range_start = 1000;
    
    WHILE v_current_range_start < 200000 DO
        -- 生成8位数字代码
        SET v_code = LPAD(10000000 + v_index, 8, '0');
        
        -- 在当前1000区间内随机生成一个预投放量
        SET v_random_offset = FLOOR(RAND() * 1000) + 1;
        SET v_adv = v_current_range_start + v_random_offset;
        
        -- 确保不超过200000
        IF v_adv > 200000 THEN
            SET v_adv = 200000;
        END IF;
        
        SET v_name = CONCAT('测试烟-', v_adv, '条');
        
        -- 根据投放量决定区域数（投放量越大，区域越多）
        IF v_adv < 5000 THEN
            SET v_region_count = 2 + FLOOR(RAND() * 2); -- 2-3个区域
        ELSEIF v_adv < 20000 THEN
            SET v_region_count = 3 + FLOOR(RAND() * 2); -- 3-4个区域
        ELSEIF v_adv < 50000 THEN
            SET v_region_count = 4 + FLOOR(RAND() * 2); -- 4-5个区域
        ELSEIF v_adv < 100000 THEN
            SET v_region_count = 5 + FLOOR(RAND() * 2); -- 5-6个区域
        ELSE
            SET v_region_count = 6 + FLOOR(RAND() * 2); -- 6-7个区域
        END IF;
        
        -- 从对应区域数的组合中随机选择一个
        CASE v_region_count
            WHEN 2 THEN 
                SET combinations = region_2;
                SET combination_count = 21;
            WHEN 3 THEN 
                SET combinations = region_3;
                SET combination_count = 35;
            WHEN 4 THEN 
                SET combinations = region_4;
                SET combination_count = 35;
            WHEN 5 THEN 
                SET combinations = region_5;
                SET combination_count = 21;
            WHEN 6 THEN 
                SET combinations = region_6;
                SET combination_count = 7;
            ELSE 
                SET combinations = region_7;
                SET combination_count = 1;
        END CASE;
        
        -- 随机选择一个组合
        IF v_region_count = 7 THEN
            SET v_areas = combinations;
        ELSE
            SET combination_index = FLOOR(RAND() * combination_count) + 1;
            SET v_areas = SUBSTRING_INDEX(SUBSTRING_INDEX(combinations, '|', combination_index), '|', -1);
        END IF;
        
        -- 插入测试用例
        INSERT INTO cigarette_distribution_info_2099_1_1 
        (CIG_CODE, CIG_NAME, ADV, DELIVERY_METHOD, DELIVERY_ETYPE, DELIVERY_AREA, bz)
        VALUES 
        (v_code, v_name, v_adv, '按档位扩展投放', '档位+城乡分类代码', v_areas, 
         CONCAT('随机测试-', v_current_range_start, '-', v_current_range_start + 1000, '区间-', v_region_count, '区域'));
        
        SET v_current_range_start = v_current_range_start + 1000;
        SET v_index = v_index + 1;
    END WHILE;
    
END$$

DELIMITER ;

-- 执行存储过程生成测试用例
CALL generate_urban_rural_test_cases();

-- 清理存储过程
DROP PROCEDURE IF EXISTS generate_urban_rural_test_cases;

-- ================================================================
-- 统计信息
-- ================================================================

SELECT '✅ 测试数据生成完成！' as '状态';

-- 查看生成的测试用例数量
SELECT COUNT(*) as '测试用例总数' FROM cigarette_distribution_info_2099_1_1;

-- 查看测试用例按投放量范围分布
SELECT 
    CASE 
        WHEN ADV <= 100 THEN '≤100'
        WHEN ADV <= 1000 THEN '100-1,000'
        WHEN ADV <= 10000 THEN '1,000-10,000'
        WHEN ADV <= 50000 THEN '10,000-50,000'
        WHEN ADV <= 100000 THEN '50,000-100,000'
        WHEN ADV <= 150000 THEN '100,000-150,000'
        ELSE '150,000-200,000'
    END as '投放量范围',
    COUNT(*) as '用例数',
    MIN(ADV) as '最小值',
    MAX(ADV) as '最大值'
FROM cigarette_distribution_info_2099_1_1
GROUP BY 
    CASE 
        WHEN ADV <= 100 THEN '≤100'
        WHEN ADV <= 1000 THEN '100-1,000'
        WHEN ADV <= 10000 THEN '1,000-10,000'
        WHEN ADV <= 50000 THEN '10,000-50,000'
        WHEN ADV <= 100000 THEN '50,000-100,000'
        WHEN ADV <= 150000 THEN '100,000-150,000'
        ELSE '150,000-200,000'
    END
ORDER BY MIN(ADV);

-- 查看测试用例按区域数分布
SELECT 
    LENGTH(DELIVERY_AREA) - LENGTH(REPLACE(DELIVERY_AREA, ',', '')) + 1 as '区域数',
    COUNT(*) as '用例数',
    MIN(ADV) as '最小投放量',
    MAX(ADV) as '最大投放量'
FROM cigarette_distribution_info_2099_1_1
GROUP BY LENGTH(DELIVERY_AREA) - LENGTH(REPLACE(DELIVERY_AREA, ',', '')) + 1
ORDER BY '区域数';

-- 查看前10个和后10个测试用例
(SELECT CIG_CODE, CIG_NAME, ADV, DELIVERY_AREA, bz 
 FROM cigarette_distribution_info_2099_1_1 
 ORDER BY ADV 
 LIMIT 10)
UNION ALL
(SELECT CIG_CODE, CIG_NAME, ADV, DELIVERY_AREA, bz 
 FROM cigarette_distribution_info_2099_1_1 
 ORDER BY ADV DESC 
 LIMIT 10)
ORDER BY ADV;

-- 验证卷烟代码格式（应该都是8位数字）
SELECT 
    '卷烟代码格式验证' as '检查项',
    COUNT(*) as '总数',
    SUM(CASE WHEN LENGTH(CIG_CODE) = 8 AND CIG_CODE REGEXP '^[0-9]{8}$' THEN 1 ELSE 0 END) as '符合8位数字格式',
    SUM(CASE WHEN LENGTH(CIG_CODE) != 8 OR CIG_CODE NOT REGEXP '^[0-9]{8}$' THEN 1 ELSE 0 END) as '不符合格式'
FROM cigarette_distribution_info_2099_1_1;

-- ================================================================
-- 脚本执行完成
-- ================================================================
SELECT CONCAT('共生成 ', COUNT(*), ' 个测试用例，准备进行算法测试！') as '完成信息' 
FROM cigarette_distribution_info_2099_1_1;

