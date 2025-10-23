#!/bin/bash

# ================================================================
# 档位+业态算法一键测试脚本
# ================================================================
# 功能：自动化执行完整的测试流程
# 1. 创建测试表
# 2. 生成测试数据
# 3. 调用后端API执行算法
# 4. 查询并分析结果
# ================================================================

echo "=================================="
echo "  档位+业态算法自动化测试"
echo "=================================="
echo ""

# 数据库配置
DB_HOST="localhost"
DB_USER="root"
DB_NAME="marketing"

# 提示用户输入密码（更安全的方式）
echo "请输入MySQL密码:"
read -s DB_PASSWORD
echo ""

# 后端API配置
API_URL="http://localhost:28080/api/calculate/write-back"
YEAR=2099
MONTH=1
WEEK_SEQ=2

# 脚本目录
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# ================================================================
# 步骤1：创建测试表
# ================================================================
echo "步骤1：创建测试表..."
mysql -h "$DB_HOST" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < "$SCRIPT_DIR/01-create-test-tables.sql"

if [ $? -eq 0 ]; then
    echo "✅ 测试表创建成功"
else
    echo "❌ 测试表创建失败"
    exit 1
fi
echo ""

# ================================================================
# 步骤2：生成测试数据
# ================================================================
echo "步骤2：生成测试数据..."
mysql -h "$DB_HOST" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < "$SCRIPT_DIR/02-generate-test-data.sql"

if [ $? -eq 0 ]; then
    echo "✅ 测试数据生成成功"
    
    # 查询生成的数据量
    TEST_CASE_COUNT=$(mysql -h "$DB_HOST" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -N -e "SELECT COUNT(*) FROM cigarette_distribution_info_2099_1_2;")
    echo "   生成测试用例数: $TEST_CASE_COUNT"
else
    echo "❌ 测试数据生成失败"
    exit 1
fi
echo ""

# ================================================================
# 步骤3：检查后端服务
# ================================================================
echo "步骤3：检查后端服务状态..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:28080/actuator/health 2>/dev/null)

if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ 后端服务运行正常"
else
    echo "⚠️  警告：无法连接到后端服务 (HTTP $HTTP_CODE)"
    echo "   请确保Spring Boot服务在端口28080运行"
    echo "   可以手动启动: mvn spring-boot:run"
    echo ""
    read -p "是否继续？(y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi
echo ""

# ================================================================
# 步骤4：调用后端API执行算法
# ================================================================
echo "步骤4：调用后端API执行档位+业态算法..."
echo "   API: $API_URL"
echo "   参数: year=$YEAR, month=$MONTH, weekSeq=$WEEK_SEQ"
echo ""

API_RESPONSE=$(curl -s -X POST "$API_URL?year=$YEAR&month=$MONTH&weekSeq=$WEEK_SEQ")
echo "   API响应: $API_RESPONSE"

# 检查API响应
if echo "$API_RESPONSE" | grep -q '"success":true'; then
    echo "✅ 算法执行成功"
    
    # 提取成功数量
    SUCCESS_COUNT=$(echo "$API_RESPONSE" | grep -o '"successCount":[0-9]*' | grep -o '[0-9]*')
    echo "   成功计算卷烟数: $SUCCESS_COUNT"
else
    echo "❌ 算法执行失败"
    echo "   响应: $API_RESPONSE"
    exit 1
fi
echo ""

# ================================================================
# 步骤5：查询测试结果
# ================================================================
echo "步骤5：查询并分析测试结果..."
mysql -h "$DB_HOST" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < "$SCRIPT_DIR/03-query-test-results.sql" > "$SCRIPT_DIR/test_results_output.txt"

if [ $? -eq 0 ]; then
    echo "✅ 测试结果查询成功"
    echo "   结果已保存到: test_results_output.txt"
else
    echo "❌ 测试结果查询失败"
    exit 1
fi
echo ""

# ================================================================
# 步骤6：显示关键统计
# ================================================================
echo "步骤6：显示关键统计信息..."
echo ""

# 查询关键统计数据
mysql -h "$DB_HOST" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -N -e "
SELECT 
    CONCAT('测试用例数: ', COUNT(*)) AS stat
FROM cigarette_distribution_info_2099_1_2
UNION ALL
SELECT 
    CONCAT('完美案例数: ', COUNT(*)) AS stat
FROM (
    SELECT i.CIG_CODE
    FROM cigarette_distribution_info_2099_1_2 i
    LEFT JOIN cigarette_distribution_prediction_2099_1_2 p ON i.CIG_CODE = p.CIG_CODE
    GROUP BY i.CIG_CODE, i.ADV
    HAVING ABS(SUM(p.ACTUAL_DELIVERY) - i.ADV) = 0
) AS perfect_cases
UNION ALL
SELECT 
    CONCAT('平均误差: ', ROUND(AVG(ABS(actual - predicted)), 2), '条') AS stat
FROM (
    SELECT 
        i.ADV AS predicted,
        SUM(p.ACTUAL_DELIVERY) AS actual
    FROM cigarette_distribution_info_2099_1_2 i
    LEFT JOIN cigarette_distribution_prediction_2099_1_2 p ON i.CIG_CODE = p.CIG_CODE
    GROUP BY i.CIG_CODE, i.ADV
) AS errors;
"

echo ""
echo "=================================="
echo "  ✅ 测试完成！"
echo "=================================="
echo ""
echo "📊 查看详细结果:"
echo "   cat test_results_output.txt"
echo ""
echo "📝 生成测试报告:"
echo "   请根据test_results_output.txt的数据生成详细测试报告"
echo "   报告位置: test docs/档位+业态/测试报告_202用例_详细分析.md"
echo ""

