-- ================================================================
-- 档位+城乡分类代码算法测试结果查询脚本
-- ================================================================
-- 功能：提供各种预定义查询，用于分析算法测试结果
-- 使用：mysql -uroot -p'YOUR_PASSWORD' < 03-query-test-results.sql
-- ================================================================

USE marketing;

SET @test_year = 2099;
SET @test_month = 1;
SET @test_week = 1;

-- ================================================================
-- 1. 基础统计信息
-- ================================================================
SELECT '📊 基础统计信息' as '===== 报告部分 =====';

SELECT 
    '测试用例总数' as '指标',
    COUNT(*) as '数值'
FROM cigarette_distribution_info_2099_1_1

UNION ALL

SELECT 
    '算法输出记录数' as '指标',
    COUNT(*) as '数值'
FROM cigarette_distribution_prediction_2099_1_1

UNION ALL

SELECT 
    '投放量总和' as '指标',
    FORMAT(SUM(ADV), 2) as '数值'
FROM cigarette_distribution_info_2099_1_1

UNION ALL

SELECT 
    '实际投放总和' as '指标',
    FORMAT(SUM(ACTUAL_DELIVERY), 2) as '数值'
FROM cigarette_distribution_prediction_2099_1_1;

-- ================================================================
-- 2. 误差分析（按卷烟汇总）
-- ================================================================
SELECT '' as '';
SELECT '📈 误差分析（前20个测试用例）' as '===== 报告部分 =====';

SELECT 
    i.CIG_CODE as '卷烟代码',
    i.CIG_NAME as '卷烟名称',
    i.ADV as '预投放量',
    SUM(p.ACTUAL_DELIVERY) as '实际投放量',
    (SUM(p.ACTUAL_DELIVERY) - i.ADV) as '误差',
    CONCAT(FORMAT(ABS(SUM(p.ACTUAL_DELIVERY) - i.ADV) / i.ADV * 100, 4), ' %') as '误差率',
    COUNT(p.id) as '区域数'
FROM cigarette_distribution_info_2099_1_1 i
LEFT JOIN cigarette_distribution_prediction_2099_1_1 p 
    ON i.CIG_CODE = p.CIG_CODE AND i.CIG_NAME = p.CIG_NAME
GROUP BY i.CIG_CODE, i.CIG_NAME, i.ADV
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
    CONCAT(FORMAT(AVG(ABS(actual - pre) / pre * 100), 6), ' %') as '平均误差率',
    CONCAT(FORMAT(MAX(ABS(actual - pre) / pre * 100), 6), ' %') as '最大误差率',
    CONCAT(FORMAT(MIN(ABS(actual - pre) / pre * 100), 6), ' %') as '最小误差率'
FROM (
    SELECT 
        i.ADV as pre,
        SUM(p.ACTUAL_DELIVERY) as actual
    FROM cigarette_distribution_info_2099_1_1 i
    LEFT JOIN cigarette_distribution_prediction_2099_1_1 p 
        ON i.CIG_CODE = p.CIG_CODE AND i.CIG_NAME = p.CIG_NAME
    GROUP BY i.CIG_CODE, i.CIG_NAME, i.ADV
) AS stats;

-- ================================================================
-- 4. 按投放量范围统计误差
-- ================================================================
SELECT '' as '';
SELECT '📊 按投放量范围统计误差' as '===== 报告部分 =====';

SELECT 
    CASE 
        WHEN i.ADV <= 100 THEN '1. ≤100'
        WHEN i.ADV <= 1000 THEN '2. 100-1,000'
        WHEN i.ADV <= 10000 THEN '3. 1,000-10,000'
        WHEN i.ADV <= 50000 THEN '4. 10,000-50,000'
        WHEN i.ADV <= 100000 THEN '5. 50,000-100,000'
        WHEN i.ADV <= 150000 THEN '6. 100,000-150,000'
        ELSE '7. 150,000-200,000'
    END as '投放量范围',
    COUNT(*) as '用例数',
    CONCAT(FORMAT(MIN(i.ADV), 0), ' - ', FORMAT(MAX(i.ADV), 0)) as '实际范围',
    CONCAT(FORMAT(AVG(ABS(actual - pre) / pre * 100), 4), ' %') as '平均误差率',
    CONCAT(FORMAT(MAX(ABS(actual - pre) / pre * 100), 4), ' %') as '最大误差率'
FROM (
    SELECT 
        i.CIG_CODE,
        i.ADV,
        i.ADV as pre,
        SUM(p.ACTUAL_DELIVERY) as actual
    FROM cigarette_distribution_info_2099_1_1 i
    LEFT JOIN cigarette_distribution_prediction_2099_1_1 p 
        ON i.CIG_CODE = p.CIG_CODE AND i.CIG_NAME = p.CIG_NAME
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
-- 5. 按区域数统计误差
-- ================================================================
SELECT '' as '';
SELECT '📊 按区域数统计误差' as '===== 报告部分 =====';

SELECT 
    region_count as '区域数',
    COUNT(*) as '用例数',
    CONCAT(FORMAT(MIN(ADV), 0), ' - ', FORMAT(MAX(ADV), 0)) as '投放量范围',
    CONCAT(FORMAT(AVG(ABS(actual - pre) / pre * 100), 4), ' %') as '平均误差率',
    CONCAT(FORMAT(MAX(ABS(actual - pre) / pre * 100), 4), ' %') as '最大误差率'
FROM (
    SELECT 
        i.ADV,
        LENGTH(i.DELIVERY_AREA) - LENGTH(REPLACE(i.DELIVERY_AREA, ',', '')) + 1 as region_count,
        i.ADV as pre,
        SUM(p.ACTUAL_DELIVERY) as actual
    FROM cigarette_distribution_info_2099_1_1 i
    LEFT JOIN cigarette_distribution_prediction_2099_1_1 p 
        ON i.CIG_CODE = p.CIG_CODE AND i.CIG_NAME = p.CIG_NAME
    GROUP BY i.CIG_CODE, i.CIG_NAME, i.ADV, i.DELIVERY_AREA
) AS stats
GROUP BY region_count
ORDER BY region_count;

-- ================================================================
-- 6. 区域级别详细分配（前5个测试用例）
-- ================================================================
SELECT '' as '';
SELECT '📋 区域级别详细分配（前5个测试用例）' as '===== 报告部分 =====';

SELECT 
    p.CIG_CODE as '卷烟代码',
    p.CIG_NAME as '卷烟名称',
    i.ADV as '预投放量',
    p.DELIVERY_AREA as '投放区域',
    p.ACTUAL_DELIVERY as '实际投放量',
    CONCAT(FORMAT((p.ACTUAL_DELIVERY / i.ADV * 100), 2), ' %') as '占比',
    p.D30 + p.D29 + p.D28 + p.D27 + p.D26 + p.D25 + p.D24 + p.D23 + p.D22 + p.D21 as '高档位(30-21)',
    p.D20 + p.D19 + p.D18 + p.D17 + p.D16 + p.D15 + p.D14 + p.D13 + p.D12 + p.D11 as '中档位(20-11)',
    p.D10 + p.D9 + p.D8 + p.D7 + p.D6 + p.D5 + p.D4 + p.D3 + p.D2 + p.D1 as '低档位(10-1)'
FROM cigarette_distribution_prediction_2099_1_1 p
JOIN cigarette_distribution_info_2099_1_1 i 
    ON p.CIG_CODE = i.CIG_CODE AND p.CIG_NAME = i.CIG_NAME
WHERE p.CIG_CODE IN ('10000001', '10000002', '10000003', '10000004', '10000005')
ORDER BY p.CIG_CODE, p.DELIVERY_AREA;

-- ================================================================
-- 7. 档位分配统计（第一个测试用例详细展示）
-- ================================================================
SELECT '' as '';
SELECT '📊 档位分配详细统计（第一个测试用例）' as '===== 报告部分 =====';

SELECT 
    DELIVERY_AREA as '区域',
    D30, D29, D28, D27, D26, D25, D24, D23, D22, D21,
    D20, D19, D18, D17, D16, D15, D14, D13, D12, D11,
    D10, D9, D8, D7, D6, D5, D4, D3, D2, D1,
    ACTUAL_DELIVERY as '区域总量'
FROM cigarette_distribution_prediction_2099_1_1
WHERE CIG_CODE = '10000001'
ORDER BY DELIVERY_AREA;

-- ================================================================
-- 8. 最佳和最差表现的测试用例
-- ================================================================
SELECT '' as '';
SELECT '🏆 误差率最小的10个测试用例' as '===== 报告部分 =====';

SELECT 
    i.CIG_CODE as '卷烟代码',
    i.CIG_NAME as '卷烟名称',
    i.ADV as '预投放量',
    SUM(p.ACTUAL_DELIVERY) as '实际投放量',
    (SUM(p.ACTUAL_DELIVERY) - i.ADV) as '误差',
    CONCAT(FORMAT(ABS(SUM(p.ACTUAL_DELIVERY) - i.ADV) / i.ADV * 100, 6), ' %') as '误差率',
    COUNT(p.id) as '区域数'
FROM cigarette_distribution_info_2099_1_1 i
LEFT JOIN cigarette_distribution_prediction_2099_1_1 p 
    ON i.CIG_CODE = p.CIG_CODE AND i.CIG_NAME = p.CIG_NAME
GROUP BY i.CIG_CODE, i.CIG_NAME, i.ADV
ORDER BY ABS(SUM(p.ACTUAL_DELIVERY) - i.ADV) / i.ADV
LIMIT 10;

SELECT '' as '';
SELECT '⚠️ 误差率最大的10个测试用例' as '===== 报告部分 =====';

SELECT 
    i.CIG_CODE as '卷烟代码',
    i.CIG_NAME as '卷烟名称',
    i.ADV as '预投放量',
    SUM(p.ACTUAL_DELIVERY) as '实际投放量',
    (SUM(p.ACTUAL_DELIVERY) - i.ADV) as '误差',
    CONCAT(FORMAT(ABS(SUM(p.ACTUAL_DELIVERY) - i.ADV) / i.ADV * 100, 6), ' %') as '误差率',
    COUNT(p.id) as '区域数'
FROM cigarette_distribution_info_2099_1_1 i
LEFT JOIN cigarette_distribution_prediction_2099_1_1 p 
    ON i.CIG_CODE = p.CIG_CODE AND i.CIG_NAME = p.CIG_NAME
GROUP BY i.CIG_CODE, i.CIG_NAME, i.ADV
ORDER BY ABS(SUM(p.ACTUAL_DELIVERY) - i.ADV) / i.ADV DESC
LIMIT 10;

-- ================================================================
-- 9. 区域投放量排名（TOP区域分析）
-- ================================================================
SELECT '' as '';
SELECT '🗺️ 各区域接收投放量排名' as '===== 报告部分 =====';

SELECT 
    DELIVERY_AREA as '区域名称',
    COUNT(*) as '分配次数',
    FORMAT(SUM(ACTUAL_DELIVERY), 2) as '总投放量',
    FORMAT(AVG(ACTUAL_DELIVERY), 2) as '平均投放量',
    FORMAT(MAX(ACTUAL_DELIVERY), 2) as '最大投放量',
    FORMAT(MIN(ACTUAL_DELIVERY), 2) as '最小投放量'
FROM cigarette_distribution_prediction_2099_1_1
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
        WHEN AVG(ABS(actual - pre) / pre * 100) <= 0.1 THEN '优秀 (≤0.1%)'
        WHEN AVG(ABS(actual - pre) / pre * 100) <= 1 THEN '良好 (≤1%)'
        WHEN AVG(ABS(actual - pre) / pre * 100) <= 5 THEN '合格 (≤5%)'
        ELSE '需优化 (>5%)'
    END as '评级',
    CONCAT(FORMAT(AVG(ABS(actual - pre) / pre * 100), 6), ' %') as '平均误差率'
FROM (
    SELECT 
        i.ADV as pre,
        SUM(p.ACTUAL_DELIVERY) as actual
    FROM cigarette_distribution_info_2099_1_1 i
    LEFT JOIN cigarette_distribution_prediction_2099_1_1 p 
        ON i.CIG_CODE = p.CIG_CODE AND i.CIG_NAME = p.CIG_NAME
    GROUP BY i.CIG_CODE, i.CIG_NAME, i.ADV
) AS stats;

-- ================================================================
-- 脚本执行完成
-- ================================================================
SELECT '' as '';
SELECT '✅ 查询报告生成完成！' as '状态';
SELECT CONCAT('测试时间: ', @test_year, '年', @test_month, '月第', @test_week, '周') as '测试信息';

