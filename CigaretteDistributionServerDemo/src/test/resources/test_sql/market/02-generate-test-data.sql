-- ================================================================
-- 档位+市场类型算法测试数据生成脚本
-- ================================================================
-- 功能：生成测试用例，覆盖仅城网、仅农网、城网+农网三种情况
-- 投放量范围：100 到 200,000
-- 卷烟代码规则：8位数字，从 30000001 开始
-- 目标表：cigarette_distribution_info_2099_1_2
-- ================================================================

USE marketing;

-- 清空旧数据
TRUNCATE TABLE cigarette_distribution_info_2099_1_2;

DELIMITER $$

DROP PROCEDURE IF EXISTS generate_market_test_cases$$

CREATE PROCEDURE generate_market_test_cases()
BEGIN
    DECLARE v_code VARCHAR(20);
    DECLARE v_name VARCHAR(100);
    DECLARE v_adv DECIMAL(15, 2);
    DECLARE v_areas VARCHAR(50); -- 市场类型区域较短
    DECLARE v_scenario INT;
    DECLARE v_index INT DEFAULT 1;
    DECLARE v_random_offset INT;
    DECLARE v_current_range_start INT;

    -- ============================================================
    -- 1. 插入固定测试用例：100, 500, 1000 (分布到三种场景)
    -- ============================================================

    -- 测试用例1: 100条 (仅城网)
INSERT INTO cigarette_distribution_info_2099_1_2
(CIG_CODE, CIG_NAME, ADV, DELIVERY_METHOD, DELIVERY_ETYPE, DELIVERY_AREA, bz, YEAR, MONTH, WEEK_SEQ)
VALUES
    ('30000001', '测试烟-100条-城网', 100, '按档位扩展投放', '档位+市场类型', '城网', '固定测试-100条-仅城网', 2099, 1, 2);

-- 测试用例2: 500条 (仅农网)
INSERT INTO cigarette_distribution_info_2099_1_2
(CIG_CODE, CIG_NAME, ADV, DELIVERY_METHOD, DELIVERY_ETYPE, DELIVERY_AREA, bz, YEAR, MONTH, WEEK_SEQ)
VALUES
    ('30000002', '测试烟-500条-农网', 500, '按档位扩展投放', '档位+市场类型', '农网', '固定测试-500条-仅农网', 2099, 1, 2);

-- 测试用例3: 1000条 (城网+农网)
INSERT INTO cigarette_distribution_info_2099_1_2
(CIG_CODE, CIG_NAME, ADV, DELIVERY_METHOD, DELIVERY_ETYPE, DELIVERY_AREA, bz, YEAR, MONTH, WEEK_SEQ)
VALUES
    ('30000003', '测试烟-1000条-城网农网', 1000, '按档位扩展投放', '档位+市场类型', '城网,农网', '固定测试-1000条-城网+农网', 2099, 1, 2);

SET v_index = 4; -- 从第4个用例开始循环

    -- ============================================================
    -- 2. 生成随机测试用例：从1000-2000, ..., 199000-200000
    -- 均匀分布到三种场景
    -- ============================================================

    SET v_current_range_start = 1000;

    WHILE v_current_range_start < 200000 DO
        -- 生成8位数字代码 (从 30000004 开始)
        SET v_code = LPAD(30000000 + v_index, 8, '0');

        -- 在当前1000区间内随机生成一个预投放量
        SET v_random_offset = FLOOR(RAND() * 1000) + 1;
        SET v_adv = v_current_range_start + v_random_offset;

        -- 确保不超过200000
        IF v_adv > 200000 THEN
            SET v_adv = 200000;
END IF;

        -- 根据场景确定区域和名称
        SET v_scenario = (v_index - 1) % 3; -- 0, 1, 2 循环

        IF v_scenario = 0 THEN
            SET v_areas = '城网';
            SET v_name = CONCAT('测试烟-', FORMAT(v_adv, 2), '条-城网'); -- 统一格式
            SET @remark = CONCAT('随机测试-', v_current_range_start, '区间-仅城网');
        ELSEIF v_scenario = 1 THEN
            SET v_areas = '农网';
            SET v_name = CONCAT('测试烟-', FORMAT(v_adv, 2), '条-农网'); -- 统一格式
            SET @remark = CONCAT('随机测试-', v_current_range_start, '区间-仅农网');
ELSE -- v_scenario = 2
            SET v_areas = '城网,农网';
            SET v_name = CONCAT('测试烟-', FORMAT(v_adv, 2), '条-城网农网'); -- 统一格式
            SET @remark = CONCAT('随机测试-', v_current_range_start, '区间-城网+农网');
END IF;

        -- 插入测试用例
INSERT INTO cigarette_distribution_info_2099_1_2
(CIG_CODE, CIG_NAME, ADV, DELIVERY_METHOD, DELIVERY_ETYPE, DELIVERY_AREA, bz, YEAR, MONTH, WEEK_SEQ)
VALUES
    (v_code, v_name, v_adv, '按档位扩展投放', '档位+市场类型', v_areas, @remark, 2099, 1, 2);

SET v_current_range_start = v_current_range_start + 1000;
        SET v_index = v_index + 1;
END WHILE;

END$$

DELIMITER ;

-- 执行存储过程生成测试用例
CALL generate_market_test_cases();

-- 清理存储过程
DROP PROCEDURE IF EXISTS generate_market_test_cases;

-- ================================================================
-- 统计信息
-- ================================================================

SELECT '✅ 市场类型测试数据生成完成！' as '状态';

-- 查看生成的测试用例数量
SELECT COUNT(*) as '测试用例总数' FROM cigarette_distribution_info_2099_1_2;

-- 按场景统计
SELECT
    CASE
        WHEN DELIVERY_AREA = '城网' THEN '仅城网'
        WHEN DELIVERY_AREA = '农网' THEN '仅农网'
        WHEN DELIVERY_AREA = '城网,农网' THEN '城网+农网'
        ELSE '其他'
        END as '投放场景',
        COUNT(*) as '用例数',
        FORMAT(MIN(ADV), 0) as '最小投放量',
        FORMAT(MAX(ADV), 0) as '最大投放量',
        FORMAT(AVG(ADV), 0) as '平均投放量'
FROM cigarette_distribution_info_2099_1_2
GROUP BY `投放场景`
ORDER BY `投放场景`;

-- 查看前10个测试用例
SELECT CIG_CODE, CIG_NAME, ADV, DELIVERY_AREA, bz
FROM cigarette_distribution_info_2099_1_2
ORDER BY ADV
    LIMIT 10;

-- 验证卷烟代码格式（应该都是8位数字）
SELECT
    '卷烟代码格式验证' as '检查项',
        COUNT(*) as '总数',
        SUM(CASE WHEN LENGTH(CIG_CODE) = 8 AND CIG_CODE REGEXP '^[0-9]{8}$' THEN 1 ELSE 0 END) as '符合8位数字格式',
        SUM(CASE WHEN LENGTH(CIG_CODE) != 8 OR CIG_CODE NOT REGEXP '^[0-9]{8}$' THEN 1 ELSE 0 END) as '不符合格式'
FROM cigarette_distribution_info_2099_1_2;

SELECT CONCAT('共生成 ', COUNT(*), ' 个市场类型测试用例！') as '完成信息'
FROM cigarette_distribution_info_2099_1_2;