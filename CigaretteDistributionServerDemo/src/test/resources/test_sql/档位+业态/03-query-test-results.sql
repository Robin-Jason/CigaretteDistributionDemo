-- ================================================================
-- 档位+业态算法测试结果查询脚本
-- ================================================================
-- 功能：查询测试结果并进行统计分析
-- ================================================================

USE marketing;

-- ================================================================
-- 查询1：测试用例总览
-- ================================================================
SELECT 
    '测试用例总览' AS query_name,
    COUNT(*) AS total_cases,
    MIN(ADV) AS min_adv,
    MAX(ADV) AS max_adv,
    AVG(ADV) AS avg_adv
FROM cigarette_distribution_info_2099_1_2;

-- ================================================================
-- 查询2：测试结果总览
-- ================================================================
SELECT 
    '测试结果总览' AS query_name,
    COUNT(DISTINCT CIG_CODE) AS total_cigarettes,
    COUNT(*) AS total_records
FROM cigarette_distribution_prediction_2099_1_2;

-- ================================================================
-- 查询3：误差分析
-- ================================================================
SELECT 
    i.CIG_CODE,
    i.CIG_NAME,
    i.ADV AS predicted_delivery,
    i.DELIVERY_AREA,
    SUM(p.ACTUAL_DELIVERY) AS actual_delivery,
    (SUM(p.ACTUAL_DELIVERY) - i.ADV) AS error_amount,
    ROUND(ABS(SUM(p.ACTUAL_DELIVERY) - i.ADV) / i.ADV * 100, 4) AS error_rate_percent
FROM cigarette_distribution_info_2099_1_2 i
LEFT JOIN cigarette_distribution_prediction_2099_1_2 p 
    ON i.CIG_CODE = p.CIG_CODE AND i.CIG_NAME = p.CIG_NAME
GROUP BY i.CIG_CODE, i.CIG_NAME, i.ADV, i.DELIVERY_AREA
ORDER BY i.ADV;

-- ================================================================
-- 查询4：误差统计
-- ================================================================
SELECT 
    '误差统计' AS query_name,
    COUNT(*) AS total_cases,
    ROUND(AVG(ABS(SUM(p.ACTUAL_DELIVERY) - i.ADV)), 2) AS avg_abs_error,
    MAX(ABS(SUM(p.ACTUAL_DELIVERY) - i.ADV)) AS max_abs_error,
    ROUND(AVG(ABS(SUM(p.ACTUAL_DELIVERY) - i.ADV) / i.ADV * 100), 4) AS avg_error_rate_percent,
    MAX(ABS(SUM(p.ACTUAL_DELIVERY) - i.ADV) / i.ADV * 100) AS max_error_rate_percent,
    SUM(CASE WHEN ABS(SUM(p.ACTUAL_DELIVERY) - i.ADV) = 0 THEN 1 ELSE 0 END) AS perfect_count,
    SUM(CASE WHEN ABS(SUM(p.ACTUAL_DELIVERY) - i.ADV) <= 5 THEN 1 ELSE 0 END) AS excellent_count
FROM cigarette_distribution_info_2099_1_2 i
LEFT JOIN cigarette_distribution_prediction_2099_1_2 p 
    ON i.CIG_CODE = p.CIG_CODE
GROUP BY i.CIG_CODE, i.ADV;

-- ================================================================
-- 查询5：按预投放量范围统计
-- ================================================================
SELECT 
    CASE 
        WHEN i.ADV < 1000 THEN '小量级(<1000)'
        WHEN i.ADV < 10000 THEN '中量级(1000-10000)'
        WHEN i.ADV < 50000 THEN '大量级(10000-50000)'
        ELSE '超大量级(>=50000)'
    END AS adv_range,
    COUNT(*) AS case_count,
    ROUND(AVG(ABS(SUM(p.ACTUAL_DELIVERY) - i.ADV)), 2) AS avg_abs_error,
    ROUND(AVG(ABS(SUM(p.ACTUAL_DELIVERY) - i.ADV) / i.ADV * 100), 4) AS avg_error_rate_percent,
    SUM(CASE WHEN ABS(SUM(p.ACTUAL_DELIVERY) - i.ADV) = 0 THEN 1 ELSE 0 END) AS perfect_count
FROM cigarette_distribution_info_2099_1_2 i
LEFT JOIN cigarette_distribution_prediction_2099_1_2 p 
    ON i.CIG_CODE = p.CIG_CODE
GROUP BY i.CIG_CODE, i.ADV, 
    CASE 
        WHEN i.ADV < 1000 THEN '小量级(<1000)'
        WHEN i.ADV < 10000 THEN '中量级(1000-10000)'
        WHEN i.ADV < 50000 THEN '大量级(10000-50000)'
        ELSE '超大量级(>=50000)'
    END
ORDER BY MIN(i.ADV);

-- ================================================================
-- 查询6：按业态数量统计
-- ================================================================
SELECT 
    LENGTH(i.DELIVERY_AREA) - LENGTH(REPLACE(i.DELIVERY_AREA, ',', '')) + 1 AS business_format_count,
    COUNT(*) AS case_count,
    ROUND(AVG(ABS(SUM(p.ACTUAL_DELIVERY) - i.ADV)), 2) AS avg_abs_error,
    ROUND(AVG(ABS(SUM(p.ACTUAL_DELIVERY) - i.ADV) / i.ADV * 100), 4) AS avg_error_rate_percent
FROM cigarette_distribution_info_2099_1_2 i
LEFT JOIN cigarette_distribution_prediction_2099_1_2 p 
    ON i.CIG_CODE = p.CIG_CODE
GROUP BY i.CIG_CODE, i.DELIVERY_AREA,
    LENGTH(i.DELIVERY_AREA) - LENGTH(REPLACE(i.DELIVERY_AREA, ',', '')) + 1
ORDER BY business_format_count;

-- ================================================================
-- 查询7：最大误差案例（Top 10）
-- ================================================================
SELECT 
    '最大误差Top10' AS query_name,
    i.CIG_CODE,
    i.CIG_NAME,
    i.ADV AS predicted,
    SUM(p.ACTUAL_DELIVERY) AS actual,
    (SUM(p.ACTUAL_DELIVERY) - i.ADV) AS error_amount,
    ROUND(ABS(SUM(p.ACTUAL_DELIVERY) - i.ADV) / i.ADV * 100, 4) AS error_rate_percent,
    i.DELIVERY_AREA AS business_formats,
    LENGTH(i.DELIVERY_AREA) - LENGTH(REPLACE(i.DELIVERY_AREA, ',', '')) + 1 AS format_count
FROM cigarette_distribution_info_2099_1_2 i
LEFT JOIN cigarette_distribution_prediction_2099_1_2 p 
    ON i.CIG_CODE = p.CIG_CODE
GROUP BY i.CIG_CODE, i.CIG_NAME, i.ADV, i.DELIVERY_AREA
ORDER BY ABS(SUM(p.ACTUAL_DELIVERY) - i.ADV) DESC
LIMIT 10;

-- ================================================================
-- 查询8：最小误差率案例（Top 10）
-- ================================================================
SELECT 
    '最小误差率Top10' AS query_name,
    i.CIG_CODE,
    i.CIG_NAME,
    i.ADV AS predicted,
    SUM(p.ACTUAL_DELIVERY) AS actual,
    (SUM(p.ACTUAL_DELIVERY) - i.ADV) AS error_amount,
    ROUND(ABS(SUM(p.ACTUAL_DELIVERY) - i.ADV) / i.ADV * 100, 4) AS error_rate_percent,
    i.DELIVERY_AREA AS business_formats
FROM cigarette_distribution_info_2099_1_2 i
LEFT JOIN cigarette_distribution_prediction_2099_1_2 p 
    ON i.CIG_CODE = p.CIG_CODE
GROUP BY i.CIG_CODE, i.CIG_NAME, i.ADV, i.DELIVERY_AREA
ORDER BY ABS(SUM(p.ACTUAL_DELIVERY) - i.ADV) / i.ADV
LIMIT 10;

-- ================================================================
-- 查询9：完美案例（误差=0）
-- ================================================================
SELECT 
    '完美案例' AS query_name,
    i.CIG_CODE,
    i.CIG_NAME,
    i.ADV AS predicted,
    SUM(p.ACTUAL_DELIVERY) AS actual,
    i.DELIVERY_AREA AS business_formats
FROM cigarette_distribution_info_2099_1_2 i
LEFT JOIN cigarette_distribution_prediction_2099_1_2 p 
    ON i.CIG_CODE = p.CIG_CODE
GROUP BY i.CIG_CODE, i.CIG_NAME, i.ADV, i.DELIVERY_AREA
HAVING ABS(SUM(p.ACTUAL_DELIVERY) - i.ADV) = 0
ORDER BY i.ADV;

-- ================================================================
-- 查询10：按业态查看分配详情（示例：便利店）
-- ================================================================
SELECT 
    p.CIG_CODE,
    p.CIG_NAME,
    p.DELIVERY_AREA AS business_format,
    p.D30, p.D29, p.D28, p.D27, p.D26, p.D25,
    p.ACTUAL_DELIVERY
FROM cigarette_distribution_prediction_2099_1_2 p
WHERE p.DELIVERY_AREA = '便利店'
ORDER BY p.CIG_CODE
LIMIT 5;

