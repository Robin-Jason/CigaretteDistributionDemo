-- ================================================================
-- 档位+市场类型算法测试结果查询脚本
-- ================================================================
-- 功能：提供各种预定义查询，用于分析算法测试结果
-- 表名后缀：_2099_1_2
-- ** 注意：修改查询5，明确使用 stats.DELIVERY_AREA **
-- ================================================================

USE marketing;

SET @test_year = 2099;
SET @test_month = 1;
SET @test_week = 2; -- 注意周序号是 2

-- ================================================================
-- 1. 基础统计信息
-- ================================================================
SELECT '📊 基础统计信息' as '===== 报告部分 =====';

SELECT
    '测试用例总数' as '指标',
        COUNT(*) as '数值'
FROM cigarette_distribution_info_2099_1_2

UNION ALL

SELECT
    '算法输出记录数' as '指标',
        COUNT(*) as '数值'
FROM cigarette_distribution_prediction_2099_1_2

UNION ALL

SELECT
    '预投放量总和' as '指标',
        FORMAT(SUM(ADV), 2) as '数值'
FROM cigarette_distribution_info_2099_1_2

UNION ALL

SELECT
    '实际投放总和' as '指标',
        FORMAT(SUM(actual_sum), 2) as '数值'
FROM (
         SELECT SUM(p.ACTUAL_DELIVERY) as actual_sum
         FROM cigarette_distribution_prediction_2099_1_2 p
         WHERE p.YEAR = @test_year AND p.MONTH = @test_month AND p.WEEK_SEQ = @test_week
         GROUP BY p.CIG_CODE, p.CIG_NAME
     ) AS agg_actual;


-- ================================================================
-- 2. 误差分析（按卷烟汇总）
-- ================================================================
SELECT '' as '';
SELECT '📈 误差分析（前20个测试用例）' as '===== 报告部分 =====';

SELECT
    i.CIG_CODE as '卷烟代码',
        i.CIG_NAME as '卷烟名称',
        i.ADV as '预投放量',
        COALESCE(SUM(p.ACTUAL_DELIVERY), 0) as '实际投放量',
        (COALESCE(SUM(p.ACTUAL_DELIVERY), 0) - i.ADV) as '误差',
        CASE
            WHEN i.ADV = 0 THEN 'N/A'
            ELSE CONCAT(FORMAT(ABS(COALESCE(SUM(p.ACTUAL_DELIVERY), 0) - i.ADV) / i.ADV * 100, 4), ' %')
            END as '误差率',
        i.DELIVERY_AREA as '投放市场'
FROM cigarette_distribution_info_2099_1_2 i
         LEFT JOIN cigarette_distribution_prediction_2099_1_2 p
                   ON i.CIG_CODE = p.CIG_CODE AND i.CIG_NAME = p.CIG_NAME
                       AND p.YEAR = @test_year AND p.MONTH = @test_month AND p.WEEK_SEQ = @test_week
GROUP BY i.CIG_CODE, i.CIG_NAME, i.ADV, i.DELIVERY_AREA
ORDER BY i.ADV
    LIMIT 20;

-- ================================================================
-- 3. 误差统计汇总
-- ================================================================
SELECT '' as '';
SELECT '📊 误差统计汇总' as '===== 报告部分 =====';

SELECT
    '误差统计' as '指标类型',
        CONCAT(FORMAT(AVG(ABS(actual - pre)), 2), ' 条') as '平均绝对误差',
        CONCAT(FORMAT(AVG(CASE WHEN pre = 0 THEN 0 ELSE ABS(actual - pre) / pre * 100 END), 6), ' %') as '平均误差率',
        CONCAT(FORMAT(MAX(CASE WHEN pre = 0 THEN 0 ELSE ABS(actual - pre) / pre * 100 END), 6), ' %') as '最大误差率',
        CONCAT(FORMAT(MIN(CASE WHEN pre = 0 THEN 0 ELSE ABS(actual - pre) / pre * 100 END), 6), ' %') as '最小误差率'
FROM (
         SELECT
             i.ADV as pre,
             COALESCE(SUM(p.ACTUAL_DELIVERY), 0) as actual
         FROM cigarette_distribution_info_2099_1_2 i
                  LEFT JOIN cigarette_distribution_prediction_2099_1_2 p
                            ON i.CIG_CODE = p.CIG_CODE AND i.CIG_NAME = p.CIG_NAME
                                AND p.YEAR = @test_year AND p.MONTH = @test_month AND p.WEEK_SEQ = @test_week
         GROUP BY i.CIG_CODE, i.CIG_NAME, i.ADV
     ) AS stats;

-- ================================================================
-- 4. 按投放量范围统计误差
-- ================================================================
SELECT '' as '';
SELECT '📊 按投放量范围统计误差' as '===== 报告部分 =====';

SELECT
    CASE
        WHEN ADV <= 100 THEN '1. ≤100'
        WHEN ADV <= 1000 THEN '2. 100-1,000'
        WHEN ADV <= 10000 THEN '3. 1,000-10,000'
        WHEN ADV <= 50000 THEN '4. 10,000-50,000'
        WHEN ADV <= 100000 THEN '5. 50,000-100,000'
        WHEN ADV <= 150000 THEN '6. 100,000-150,000'
        ELSE '7. 150,000-200,000'
        END as '投放量范围',
        COUNT(*) as '用例数',
        CONCAT(FORMAT(MIN(pre), 0), ' - ', FORMAT(MAX(pre), 0)) as '实际范围',
        CONCAT(FORMAT(AVG(CASE WHEN pre = 0 THEN 0 ELSE ABS(actual - pre) / pre * 100 END), 4), ' %') as '平均误差率',
        CONCAT(FORMAT(MAX(CASE WHEN pre = 0 THEN 0 ELSE ABS(actual - pre) / pre * 100 END), 4), ' %') as '最大误差率'
FROM (
         SELECT
             i.CIG_CODE,
             i.ADV,
             i.ADV as pre,
             COALESCE(SUM(p.ACTUAL_DELIVERY), 0) as actual
         FROM cigarette_distribution_info_2099_1_2 i
                  LEFT JOIN cigarette_distribution_prediction_2099_1_2 p
                            ON i.CIG_CODE = p.CIG_CODE AND i.CIG_NAME = p.CIG_NAME
                                AND p.YEAR = @test_year AND p.MONTH = @test_month AND p.WEEK_SEQ = @test_week
         GROUP BY i.CIG_CODE, i.CIG_NAME, i.ADV
     ) AS stats
GROUP BY
    CASE
        WHEN ADV <= 100 THEN '1. ≤100'
        WHEN ADV <= 1000 THEN '2. 100-1,000'
        WHEN ADV <= 10000 THEN '3. 1,000-10,000'
        WHEN ADV <= 50000 THEN '4. 10,000-50,000'
        WHEN ADV <= 100000 THEN '5. 50,000-100,000'
        WHEN ADV <= 150000 THEN '6. 100,000-150,000'
        ELSE '7. 150,000-200,000'
        END
ORDER BY MIN(ADV);

-- ================================================================
-- 5. 按投放场景（市场类型）统计误差 -- **修改部分**
-- ================================================================
SELECT '' as '';
SELECT '📊 按投放场景统计误差' as '===== 报告部分 =====';

SELECT
    CASE
        WHEN stats.DELIVERY_AREA = '城网' THEN '仅城网'       -- 使用 stats.DELIVERY_AREA
        WHEN stats.DELIVERY_AREA = '农网' THEN '仅农网'       -- 使用 stats.DELIVERY_AREA
        WHEN stats.DELIVERY_AREA = '城网,农网' THEN '城网+农网' -- 使用 stats.DELIVERY_AREA
        ELSE '其他'
        END as '投放场景',
        COUNT(*) as '用例数',
        CONCAT(FORMAT(MIN(pre), 0), ' - ', FORMAT(MAX(pre), 0)) as '投放量范围',
        CONCAT(FORMAT(AVG(CASE WHEN pre = 0 THEN 0 ELSE ABS(actual - pre) / pre * 100 END), 4), ' %') as '平均误差率',
        CONCAT(FORMAT(MAX(CASE WHEN pre = 0 THEN 0 ELSE ABS(actual - pre) / pre * 100 END), 4), ' %') as '最大误差率'
FROM (
         SELECT
             i.ADV,
             i.DELIVERY_AREA, -- 从 i 表选择 DELIVERY_AREA
             i.ADV as pre,
             COALESCE(SUM(p.ACTUAL_DELIVERY), 0) as actual
         FROM cigarette_distribution_info_2099_1_2 i -- 定义别名 i
                  LEFT JOIN cigarette_distribution_prediction_2099_1_2 p -- 定义别名 p
                            ON i.CIG_CODE = p.CIG_CODE AND i.CIG_NAME = p.CIG_NAME
                                AND p.YEAR = @test_year AND p.MONTH = @test_month AND p.WEEK_SEQ = @test_week
         GROUP BY i.CIG_CODE, i.CIG_NAME, i.ADV, i.DELIVERY_AREA -- 按 i.DELIVERY_AREA 分组
     ) AS stats -- 子查询别名为 stats
GROUP BY `投放场景` -- 按外部 CASE 表达式的别名分组
ORDER BY `投放场景`;

-- ================================================================
-- 6. 市场类型级别详细分配（前5个测试用例）
-- ================================================================
SELECT '' as '';
SELECT '📋 市场类型级别详细分配（前5个测试用例）' as '===== 报告部分 =====';

SELECT
    p.CIG_CODE as '卷烟代码',
        p.CIG_NAME as '卷烟名称',
        i.ADV as '预投放量',
        p.DELIVERY_AREA as '市场类型',
        p.ACTUAL_DELIVERY as '实际投放量',
        CASE
            WHEN i.ADV = 0 THEN 'N/A'
            ELSE CONCAT(FORMAT((p.ACTUAL_DELIVERY / i.ADV * 100), 2), ' %')
            END as '占预投放量比例',
        p.D30 + p.D29 + p.D28 + p.D27 + p.D26 + p.D25 + p.D24 + p.D23 + p.D22 + p.D21 as '高档位(30-21)值和',
        p.D20 + p.D19 + p.D18 + p.D17 + p.D16 + p.D15 + p.D14 + p.D13 + p.D12 + p.D11 as '中档位(20-11)值和',
        p.D10 + p.D9 + p.D8 + p.D7 + p.D6 + p.D5 + p.D4 + p.D3 + p.D2 + p.D1 as '低档位(10-1)值和'
FROM cigarette_distribution_prediction_2099_1_2 p
         JOIN cigarette_distribution_info_2099_1_2 i
              ON p.CIG_CODE = i.CIG_CODE AND p.CIG_NAME = i.CIG_NAME
WHERE p.CIG_CODE IN ('30000001', '30000002', '30000003', '30000004', '30000005')
  AND p.YEAR = @test_year AND p.MONTH = @test_month AND p.WEEK_SEQ = @test_week
ORDER BY p.CIG_CODE, p.DELIVERY_AREA;

-- ================================================================
-- 7. 档位分配统计（第一个 "城网+农网" 测试用例详细展示）
-- ================================================================
SELECT '' as '';
SELECT '📊 档位分配详细统计（第一个城网+农网用例）' as '===== 报告部分 =====';

SELECT
    DELIVERY_AREA as '市场类型',
        D30, D29, D28, D27, D26, D25, D24, D23, D22, D21,
    D20, D19, D18, D17, D16, D15, D14, D13, D12, D11,
    D10, D9, D8, D7, D6, D5, D4, D3, D2, D1,
    ACTUAL_DELIVERY as '区域总量'
FROM cigarette_distribution_prediction_2099_1_2
WHERE CIG_CODE = '30000003'
          AND YEAR = @test_year AND MONTH = @test_month AND WEEK_SEQ = @test_week
ORDER BY DELIVERY_AREA;

-- ================================================================
-- 8. 最佳和最差表现的测试用例（按卷烟汇总误差率）
-- ================================================================
SELECT '' as '';
SELECT '🏆 误差率最小的10个测试用例' as '===== 报告部分 =====';

SELECT
    i.CIG_CODE as '卷烟代码',
        i.CIG_NAME as '卷烟名称',
        i.ADV as '预投放量',
        COALESCE(SUM(p.ACTUAL_DELIVERY), 0) as '实际投放量',
        (COALESCE(SUM(p.ACTUAL_DELIVERY), 0) - i.ADV) as '误差',
        CASE
            WHEN i.ADV = 0 THEN 'N/A'
            ELSE CONCAT(FORMAT(ABS(COALESCE(SUM(p.ACTUAL_DELIVERY), 0) - i.ADV) / i.ADV * 100, 6), ' %')
            END as '误差率',
        i.DELIVERY_AREA as '投放市场'
FROM cigarette_distribution_info_2099_1_2 i
         LEFT JOIN cigarette_distribution_prediction_2099_1_2 p
                   ON i.CIG_CODE = p.CIG_CODE AND i.CIG_NAME = p.CIG_NAME
                       AND p.YEAR = @test_year AND p.MONTH = @test_month AND p.WEEK_SEQ = @test_week
GROUP BY i.CIG_CODE, i.CIG_NAME, i.ADV, i.DELIVERY_AREA
ORDER BY CASE WHEN i.ADV = 0 THEN 999 ELSE ABS(COALESCE(SUM(p.ACTUAL_DELIVERY), 0) - i.ADV) / i.ADV END
    LIMIT 10;

SELECT '' as '';
SELECT '⚠️ 误差率最大的10个测试用例' as '===== 报告部分 =====';

SELECT
    i.CIG_CODE as '卷烟代码',
        i.CIG_NAME as '卷烟名称',
        i.ADV as '预投放量',
        COALESCE(SUM(p.ACTUAL_DELIVERY), 0) as '实际投放量',
        (COALESCE(SUM(p.ACTUAL_DELIVERY), 0) - i.ADV) as '误差',
        CASE
            WHEN i.ADV = 0 THEN 'N/A'
            ELSE CONCAT(FORMAT(ABS(COALESCE(SUM(p.ACTUAL_DELIVERY), 0) - i.ADV) / i.ADV * 100, 6), ' %')
            END as '误差率',
        i.DELIVERY_AREA as '投放市场'
FROM cigarette_distribution_info_2099_1_2 i
         LEFT JOIN cigarette_distribution_prediction_2099_1_2 p
                   ON i.CIG_CODE = p.CIG_CODE AND i.CIG_NAME = p.CIG_NAME
                       AND p.YEAR = @test_year AND p.MONTH = @test_month AND p.WEEK_SEQ = @test_week
GROUP BY i.CIG_CODE, i.CIG_NAME, i.ADV, i.DELIVERY_AREA
ORDER BY CASE WHEN i.ADV = 0 THEN 0 ELSE ABS(COALESCE(SUM(p.ACTUAL_DELIVERY), 0) - i.ADV) / i.ADV END DESC
    LIMIT 10;

-- ================================================================
-- 9. 市场类型投放量排名
-- ================================================================
SELECT '' as '';
SELECT '🗺️ 各市场类型接收投放量排名' as '===== 报告部分 =====';

SELECT
    DELIVERY_AREA as '市场类型',
        COUNT(DISTINCT CIG_CODE, CIG_NAME) as '分配卷烟数',
        FORMAT(SUM(ACTUAL_DELIVERY), 2) as '总投放量',
        FORMAT(AVG(ACTUAL_DELIVERY), 2) as '平均投放量（按记录）',
        FORMAT(MAX(ACTUAL_DELIVERY), 2) as '最大单次投放量',
        FORMAT(MIN(ACTUAL_DELIVERY), 2) as '最小单次投放量'
FROM cigarette_distribution_prediction_2099_1_2
WHERE YEAR = @test_year AND MONTH = @test_month AND WEEK_SEQ = @test_week
GROUP BY DELIVERY_AREA
ORDER BY SUM(ACTUAL_DELIVERY) DESC;

-- ================================================================
-- 10. 算法性能总结
-- ================================================================
SELECT '' as '';
SELECT '✅ 算法性能总结' as '===== 报告部分 =====';

SELECT
    '总体性能' as '评估维度',
        CASE
            WHEN AVG(CASE WHEN pre = 0 THEN 0 ELSE ABS(actual - pre) / pre * 100 END) <= 0.1 THEN '优秀 (≤0.1%)'
            WHEN AVG(CASE WHEN pre = 0 THEN 0 ELSE ABS(actual - pre) / pre * 100 END) <= 1 THEN '良好 (≤1%)'
            WHEN AVG(CASE WHEN pre = 0 THEN 0 ELSE ABS(actual - pre) / pre * 100 END) <= 5 THEN '合格 (≤5%)'
            ELSE '需优化 (>5%)'
            END as '评级',
        CONCAT(FORMAT(AVG(CASE WHEN pre = 0 THEN 0 ELSE ABS(actual - pre) / pre * 100 END), 6), ' %') as '平均误差率'
FROM (
         SELECT
             i.ADV as pre,
             COALESCE(SUM(p.ACTUAL_DELIVERY), 0) as actual
         FROM cigarette_distribution_info_2099_1_2 i
                  LEFT JOIN cigarette_distribution_prediction_2099_1_2 p
                            ON i.CIG_CODE = p.CIG_CODE AND i.CIG_NAME = p.CIG_NAME
                                AND p.YEAR = @test_year AND p.MONTH = @test_month AND p.WEEK_SEQ = @test_week
         GROUP BY i.CIG_CODE, i.CIG_NAME, i.ADV
     ) AS stats;

-- ================================================================
-- 脚本执行完成
-- ================================================================
SELECT '' as '';
SELECT '✅ 市场类型查询报告生成完成！' as '状态';
SELECT CONCAT('测试时间: ', @test_year, '年', @test_month, '月第', @test_week, '周') as '测试信息';