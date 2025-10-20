-- ================================================================
-- 档位+业态算法测试数据生成脚本（优化版）
-- ================================================================
-- 功能：生成测试用例，投放量从100到200000，卷烟代码8位数字
-- 区域：从6个业态（便利店、超市、商场、烟草专业店、娱乐服务类、其他）中组合
-- 区域数规则：根据投放量决定业态数（投放量越大，业态越多）
-- 特殊规则：投放量>5000时，投放区域必须包含便利店
-- ================================================================

USE marketing;

-- 清空旧数据
TRUNCATE TABLE cigarette_distribution_info_2099_1_2;

DELIMITER $$

DROP PROCEDURE IF EXISTS generate_business_format_test_cases$$

CREATE PROCEDURE generate_business_format_test_cases()
BEGIN
    DECLARE v_code VARCHAR(20);
    DECLARE v_name VARCHAR(100);
    DECLARE v_adv DECIMAL(15, 2);
    DECLARE v_areas VARCHAR(500);
    DECLARE v_region_count INT;
    DECLARE v_index INT DEFAULT 1;
    DECLARE v_random_offset INT;
    DECLARE v_current_range_start INT;
    DECLARE combination_index INT;
    DECLARE combination_count INT;
    DECLARE combinations TEXT;
    DECLARE must_include_convenience BOOLEAN;
    DECLARE selected_combination TEXT;
    
    -- ============================================================
    -- 定义业态组合（分为包含便利店和不包含便利店两类）
    -- ============================================================
    
    -- 2个业态的组合（包含便利店）
    DECLARE region_2_with_conv TEXT DEFAULT '便利店,超市|便利店,商场|便利店,烟草专业店|便利店,娱乐服务类|便利店,其他';
    
    -- 2个业态的组合（不包含便利店）
    DECLARE region_2_without_conv TEXT DEFAULT '超市,商场|超市,烟草专业店|超市,娱乐服务类|超市,其他|商场,烟草专业店|商场,娱乐服务类|商场,其他|烟草专业店,娱乐服务类|烟草专业店,其他|娱乐服务类,其他';
    
    -- 3个业态的组合（包含便利店）
    DECLARE region_3_with_conv TEXT DEFAULT '便利店,超市,商场|便利店,超市,烟草专业店|便利店,超市,娱乐服务类|便利店,超市,其他|便利店,商场,烟草专业店|便利店,商场,娱乐服务类|便利店,商场,其他|便利店,烟草专业店,娱乐服务类|便利店,烟草专业店,其他|便利店,娱乐服务类,其他';
    
    -- 3个业态的组合（不包含便利店）
    DECLARE region_3_without_conv TEXT DEFAULT '超市,商场,烟草专业店|超市,商场,娱乐服务类|超市,商场,其他|超市,烟草专业店,娱乐服务类|超市,烟草专业店,其他|超市,娱乐服务类,其他|商场,烟草专业店,娱乐服务类|商场,烟草专业店,其他|商场,娱乐服务类,其他|烟草专业店,娱乐服务类,其他';
    
    -- 4个业态的组合（包含便利店）
    DECLARE region_4_with_conv TEXT DEFAULT '便利店,超市,商场,烟草专业店|便利店,超市,商场,娱乐服务类|便利店,超市,商场,其他|便利店,超市,烟草专业店,娱乐服务类|便利店,超市,烟草专业店,其他|便利店,超市,娱乐服务类,其他|便利店,商场,烟草专业店,娱乐服务类|便利店,商场,烟草专业店,其他|便利店,商场,娱乐服务类,其他|便利店,烟草专业店,娱乐服务类,其他';
    
    -- 4个业态的组合（不包含便利店）
    DECLARE region_4_without_conv TEXT DEFAULT '超市,商场,烟草专业店,娱乐服务类|超市,商场,烟草专业店,其他|超市,商场,娱乐服务类,其他|超市,烟草专业店,娱乐服务类,其他|商场,烟草专业店,娱乐服务类,其他';
    
    -- 5个业态的组合（包含便利店）
    DECLARE region_5_with_conv TEXT DEFAULT '便利店,超市,商场,烟草专业店,娱乐服务类|便利店,超市,商场,烟草专业店,其他|便利店,超市,商场,娱乐服务类,其他|便利店,超市,烟草专业店,娱乐服务类,其他|便利店,商场,烟草专业店,娱乐服务类,其他';
    
    -- 5个业态的组合（不包含便利店）
    DECLARE region_5_without_conv TEXT DEFAULT '超市,商场,烟草专业店,娱乐服务类,其他';
    
    -- 6个业态（全部，必然包含便利店）
    DECLARE region_6_all TEXT DEFAULT '便利店,超市,商场,烟草专业店,娱乐服务类,其他';
    
    -- ============================================================
    -- 1. 插入固定测试用例：100, 500, 1000
    -- ============================================================
    
    -- 测试用例1: 100条（小量级，2个业态）
    INSERT INTO cigarette_distribution_info_2099_1_2 
    (CIG_CODE, CIG_NAME, ADV, DELIVERY_METHOD, DELIVERY_ETYPE, DELIVERY_AREA, bz)
    VALUES 
    ('20000001', '测试烟-100条', 100, '按档位扩展投放', '档位+业态', '烟草专业店,其他', '固定测试-100条-2业态');
    
    -- 测试用例2: 500条（小量级，2-3个业态）
    INSERT INTO cigarette_distribution_info_2099_1_2 
    (CIG_CODE, CIG_NAME, ADV, DELIVERY_METHOD, DELIVERY_ETYPE, DELIVERY_AREA, bz)
    VALUES 
    ('20000002', '测试烟-500条', 500, '按档位扩展投放', '档位+业态', '烟草专业店,娱乐服务类,其他', '固定测试-500条-3业态');
    
    -- 测试用例3: 1000条（小量级，2-3个业态）
    INSERT INTO cigarette_distribution_info_2099_1_2 
    (CIG_CODE, CIG_NAME, ADV, DELIVERY_METHOD, DELIVERY_ETYPE, DELIVERY_AREA, bz)
    VALUES 
    ('20000003', '测试烟-1000条', 1000, '按档位扩展投放', '档位+业态', '烟草专业店,娱乐服务类,其他', '固定测试-1000条-3业态');
    
    SET v_index = 4;
    
    -- ============================================================
    -- 2. 生成1000-200000区间的测试用例
    -- 根据投放量决定业态数，>5000时必须包含便利店
    -- ============================================================
    
    SET v_current_range_start = 1000;
    
    WHILE v_current_range_start < 200000 DO
        -- 生成8位数字代码
        SET v_code = LPAD(20000000 + v_index, 8, '0');
        
        -- 在当前1000区间内随机生成一个预投放量
        SET v_random_offset = FLOOR(RAND() * 1000) + 1;
        SET v_adv = v_current_range_start + v_random_offset;
        
        -- 确保不超过200000
        IF v_adv > 200000 THEN
            SET v_adv = 200000;
        END IF;
        
        SET v_name = CONCAT('测试烟-', v_adv, '条');
        
        -- 根据投放量决定业态数（投放量越大，业态越多）
        IF v_adv < 5000 THEN
            SET v_region_count = 2 + FLOOR(RAND() * 2); -- 2-3个业态
            SET must_include_convenience = FALSE;  -- 小量级不强制包含便利店
        ELSEIF v_adv < 20000 THEN
            SET v_region_count = 3 + FLOOR(RAND() * 2); -- 3-4个业态
            SET must_include_convenience = TRUE;   -- >5000必须包含便利店
        ELSEIF v_adv < 50000 THEN
            SET v_region_count = 4 + FLOOR(RAND() * 2); -- 4-5个业态
            SET must_include_convenience = TRUE;
        ELSEIF v_adv < 100000 THEN
            SET v_region_count = 5 + FLOOR(RAND() * 2); -- 5-6个业态
            SET must_include_convenience = TRUE;
        ELSE
            SET v_region_count = 6; -- 6个业态（全部）
            SET must_include_convenience = TRUE;
        END IF;
        
        -- 根据业态数和是否必须包含便利店，选择对应的组合集合
        IF v_region_count = 6 THEN
            -- 6个业态：使用全部业态（必然包含便利店）
            SET v_areas = region_6_all;
        ELSE
            -- 2-5个业态：根据是否必须包含便利店选择组合
            CASE v_region_count
                WHEN 2 THEN 
                    IF must_include_convenience THEN
                        SET combinations = region_2_with_conv;
                        SET combination_count = 5;
                    ELSE
                        SET combinations = region_2_without_conv;
                        SET combination_count = 10;
                    END IF;
                WHEN 3 THEN 
                    IF must_include_convenience THEN
                        SET combinations = region_3_with_conv;
                        SET combination_count = 10;
                    ELSE
                        SET combinations = region_3_without_conv;
                        SET combination_count = 10;
                    END IF;
                WHEN 4 THEN 
                    IF must_include_convenience THEN
                        SET combinations = region_4_with_conv;
                        SET combination_count = 10;
                    ELSE
                        SET combinations = region_4_without_conv;
                        SET combination_count = 5;
                    END IF;
                WHEN 5 THEN 
                    IF must_include_convenience THEN
                        SET combinations = region_5_with_conv;
                        SET combination_count = 5;
                    ELSE
                        SET combinations = region_5_without_conv;
                        SET combination_count = 1;
                    END IF;
                ELSE
                    SET combinations = region_6_all;
                    SET combination_count = 1;
            END CASE;
            
            -- 随机选择一个组合
            SET combination_index = FLOOR(RAND() * combination_count) + 1;
            SET selected_combination = SUBSTRING_INDEX(SUBSTRING_INDEX(combinations, '|', combination_index), '|', -1);
            SET v_areas = selected_combination;
        END IF;
        
        -- 插入测试用例
        INSERT INTO cigarette_distribution_info_2099_1_2 
        (CIG_CODE, CIG_NAME, ADV, DELIVERY_METHOD, DELIVERY_ETYPE, DELIVERY_AREA, bz)
        VALUES 
        (v_code, v_name, v_adv, '按档位扩展投放', '档位+业态', v_areas, 
         CONCAT('范围', v_current_range_start, '-', v_current_range_start + 1000, '-', v_region_count, '业态', 
                IF(must_include_convenience, '-含便利店', '')));
        
        SET v_current_range_start = v_current_range_start + 1000;
        SET v_index = v_index + 1;
    END WHILE;
    
END$$

DELIMITER ;

-- 执行存储过程生成测试用例
CALL generate_business_format_test_cases();

-- 清理存储过程
DROP PROCEDURE IF EXISTS generate_business_format_test_cases;

-- ================================================================
-- 统计信息
-- ================================================================

SELECT '✅ 测试数据生成完成！' as '状态';

-- 查看生成的测试用例数量
SELECT COUNT(*) as '测试用例总数' FROM cigarette_distribution_info_2099_1_2;

-- 统计预投放量范围
SELECT 
    MIN(ADV) as '最小预投放量',
    MAX(ADV) as '最大预投放量',
    ROUND(AVG(ADV), 2) as '平均预投放量'
FROM cigarette_distribution_info_2099_1_2;

-- 统计业态数分布
SELECT 
    (LENGTH(DELIVERY_AREA) - LENGTH(REPLACE(DELIVERY_AREA, ',', '')) + 1) as '业态数',
    COUNT(*) as '用例数'
FROM cigarette_distribution_info_2099_1_2
GROUP BY (LENGTH(DELIVERY_AREA) - LENGTH(REPLACE(DELIVERY_AREA, ',', '')) + 1)
ORDER BY (LENGTH(DELIVERY_AREA) - LENGTH(REPLACE(DELIVERY_AREA, ',', '')) + 1);

-- 统计包含便利店的用例数
SELECT 
    SUM(CASE WHEN DELIVERY_AREA LIKE '%便利店%' THEN 1 ELSE 0 END) as '包含便利店',
    SUM(CASE WHEN DELIVERY_AREA NOT LIKE '%便利店%' THEN 1 ELSE 0 END) as '不包含便利店',
    COUNT(*) as '总计'
FROM cigarette_distribution_info_2099_1_2;

-- 统计>5000的用例是否都包含便利店
SELECT 
    '验证>5000都包含便利店' as '检查项',
    SUM(CASE WHEN ADV > 5000 AND DELIVERY_AREA NOT LIKE '%便利店%' THEN 1 ELSE 0 END) as '违反规则的用例数'
FROM cigarette_distribution_info_2099_1_2;

-- 查看前10个测试用例
SELECT CIG_CODE, CIG_NAME, ADV, DELIVERY_AREA, bz 
FROM cigarette_distribution_info_2099_1_2 
ORDER BY ADV 
LIMIT 10;

SELECT '✅ 所有统计完成，可以执行算法测试！' as '提示';
