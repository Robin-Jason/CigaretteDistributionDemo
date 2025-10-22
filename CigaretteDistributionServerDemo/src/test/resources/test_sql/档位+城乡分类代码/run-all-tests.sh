#!/bin/bash

# ================================================================
# 档位+城乡分类代码算法完整测试脚本
# ================================================================
# 功能：一键执行创建表、生成数据、运行算法、生成报告的完整流程
# 使用：bash run-all-tests.sh
# ================================================================

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 数据库配置
DB_USER="root"
DB_PASSWORD="LuvuubyRK*Jason1258"
DB_NAME="marketing"
MYSQL_CMD="mysql -u${DB_USER} -p'${DB_PASSWORD}' -D${DB_NAME}"

# 测试参数
TEST_YEAR=2099
TEST_MONTH=1
TEST_WEEK=1

echo -e "${BLUE}================================${NC}"
echo -e "${BLUE}档位+城乡分类代码算法测试${NC}"
echo -e "${BLUE}================================${NC}"
echo ""

# ================================================================
# 步骤1: 创建测试表
# ================================================================
echo -e "${YELLOW}[步骤 1/4] 创建测试表...${NC}"
mysql -u${DB_USER} -p"${DB_PASSWORD}" < 01-create-test-tables.sql
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ 测试表创建成功${NC}"
else
    echo -e "${RED}❌ 测试表创建失败${NC}"
    exit 1
fi
echo ""

# ================================================================
# 步骤2: 生成测试数据
# ================================================================
echo -e "${YELLOW}[步骤 2/4] 生成测试数据...${NC}"
mysql -u${DB_USER} -p"${DB_PASSWORD}" < 02-generate-test-data.sql 2>&1 | grep -v "Warning"
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ 测试数据生成成功${NC}"
    
    # 显示数据统计
    TEST_COUNT=$(${MYSQL_CMD} -N -e "SELECT COUNT(*) FROM cigarette_distribution_info_2099_1_1;")
    echo -e "${GREEN}   共生成 ${TEST_COUNT} 个测试用例${NC}"
else
    echo -e "${RED}❌ 测试数据生成失败${NC}"
    exit 1
fi
echo ""

# ================================================================
# 步骤3: 执行算法计算
# ================================================================
echo -e "${YELLOW}[步骤 3/4] 执行算法计算...${NC}"
echo -e "${BLUE}   调用API: POST /api/calculate/write-back${NC}"
echo -e "${BLUE}   参数: year=${TEST_YEAR}, month=${TEST_MONTH}, weekSeq=${TEST_WEEK}${NC}"

# 检查Spring Boot是否运行
if ! curl -s http://localhost:8080/api/health > /dev/null; then
    echo -e "${RED}❌ Spring Boot应用未运行，请先启动应用${NC}"
    echo -e "${YELLOW}   启动命令: mvn spring-boot:run${NC}"
    exit 1
fi

# 调用算法API
API_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/calculate/write-back?year=${TEST_YEAR}&month=${TEST_MONTH}&weekSeq=${TEST_WEEK}")
echo -e "${GREEN}✅ 算法执行完成${NC}"
echo -e "${BLUE}   API响应: ${API_RESPONSE}${NC}"
echo ""

# ================================================================
# 步骤4: 生成测试报告
# ================================================================
echo -e "${YELLOW}[步骤 4/4] 生成测试报告...${NC}"

# 查询统计数据
RESULT_COUNT=$(${MYSQL_CMD} -N -e "SELECT COUNT(*) FROM cigarette_distribution_prediction_2099_1_1;")
echo -e "${GREEN}   算法输出记录数: ${RESULT_COUNT}${NC}"

# 生成误差统计
echo -e "${BLUE}   生成误差分析...${NC}"
${MYSQL_CMD} -e "
SELECT 
    '误差统计' as '指标类型',
    CONCAT(FORMAT(AVG(ABS(actual - pre)), 2), ' 条') as '平均绝对误差',
    CONCAT(FORMAT(AVG(ABS(actual - pre) / pre * 100), 4), ' %') as '平均误差率',
    CONCAT(FORMAT(MAX(ABS(actual - pre) / pre * 100), 4), ' %') as '最大误差率'
FROM (
    SELECT 
        i.ADV as pre,
        SUM(p.ACTUAL_DELIVERY) as actual
    FROM cigarette_distribution_info_2099_1_1 i
    LEFT JOIN cigarette_distribution_prediction_2099_1_1 p 
        ON i.CIG_CODE = p.CIG_CODE AND i.CIG_NAME = p.CIG_NAME
    GROUP BY i.CIG_CODE, i.CIG_NAME, i.ADV
) AS stats;
"

# 按投放量范围统计误差
echo ""
echo -e "${BLUE}   按投放量范围统计误差...${NC}"
${MYSQL_CMD} -e "
SELECT 
    CASE 
        WHEN i.ADV <= 1000 THEN '≤1,000'
        WHEN i.ADV <= 10000 THEN '1,000-10,000'
        WHEN i.ADV <= 50000 THEN '10,000-50,000'
        WHEN i.ADV <= 100000 THEN '50,000-100,000'
        ELSE '>100,000'
    END as '投放量范围',
    COUNT(*) as '用例数',
    CONCAT(FORMAT(AVG(ABS(actual - pre) / pre * 100), 4), ' %') as '平均误差率',
    CONCAT(FORMAT(MAX(ABS(actual - pre) / pre * 100), 4), ' %') as '最大误差率'
FROM (
    SELECT 
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
        WHEN ADV <= 1000 THEN '≤1,000'
        WHEN ADV <= 10000 THEN '1,000-10,000'
        WHEN ADV <= 50000 THEN '10,000-50,000'
        WHEN ADV <= 100000 THEN '50,000-100,000'
        ELSE '>100,000'
    END
ORDER BY MIN(ADV);
"

echo ""
echo -e "${GREEN}✅ 测试报告生成完成${NC}"
echo ""

# ================================================================
# 测试完成总结
# ================================================================
echo -e "${BLUE}================================${NC}"
echo -e "${GREEN}✅ 全部测试完成！${NC}"
echo -e "${BLUE}================================${NC}"
echo ""
echo -e "${YELLOW}测试摘要:${NC}"
echo -e "  • 测试用例数: ${TEST_COUNT}"
echo -e "  • 输出记录数: ${RESULT_COUNT}"
echo -e "  • 测试时间: ${TEST_YEAR}年${TEST_MONTH}月第${TEST_WEEK}周"
echo ""
echo -e "${YELLOW}查看详细结果:${NC}"
echo -e "  ${BLUE}mysql -u${DB_USER} -p'${DB_PASSWORD}' -D${DB_NAME}${NC}"
echo -e "  ${BLUE}SELECT * FROM cigarette_distribution_prediction_2099_1_1 LIMIT 10;${NC}"
echo ""

