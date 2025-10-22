#!/bin/bash

# ... (脚本开头部分) ...

echo -e "${BLUE}================================${NC}"
echo -e "${BLUE}  档位+市场类型算法自动化测试  ${NC}"
echo -e "${BLUE}================================${NC}"
echo ""

# 数据库配置
DB_USER="root"
DB_PASSWORD="root" # <--- 将密码直接写在这里 (或直接在 MYSQL_CMD 中使用)
DB_NAME="marketing"
# MYSQL_CMD="mysql -u${DB_USER} -p'${DB_PASSWORD}' -D${DB_NAME}" # 使用变量方式
MYSQL_CMD="mysql -u${DB_USER} -proot -D${DB_NAME}" # 直接写入密码的方式 (注意 -proot)

# 提示用户输入密码（更安全的方式）
# echo -n -e "${YELLOW}请输入 MySQL 数据库 '${DB_NAME}' 用户 '${DB_USER}' 的密码: ${NC}"
# read -s DB_PASSWORD # <--- 注释掉或删除
# echo ""
# echo ""
# 更新 MYSQL_CMD 以包含密码 (如果上面定义了 DB_PASSWORD 变量)
# MYSQL_CMD="mysql -u${DB_USER} -p'${DB_PASSWORD}' -D${DB_NAME}"

# 测试参数 (与 SQL 脚本中的表名保持一致)
TEST_YEAR=2099
TEST_MONTH=1
TEST_WEEK=2

# 后端 API 地址
API_BASE_URL="http://localhost:28080/api"
API_ENDPOINT="${API_BASE_URL}/calculate/write-back"
HEALTH_CHECK_URL="${API_BASE_URL}/common/health" # 使用 /api/common/health

# 脚本文件名
CREATE_TABLES_SQL="01-create-test-tables.sql"
GENERATE_DATA_SQL="02-generate-test-data.sql"
QUERY_RESULTS_SQL="03-query-test-results.sql"
OUTPUT_FILE="market_type_test_results.txt" # 修改输出文件名

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color
# -------------

# 检查脚本文件是否存在
if [ ! -f "$CREATE_TABLES_SQL" ] || [ ! -f "$GENERATE_DATA_SQL" ] || [ ! -f "$QUERY_RESULTS_SQL" ]; then
    echo -e "${RED}❌ 错误: 缺少必要的 SQL 脚本文件 (01, 02, 或 03)。请确保它们在此目录下。${NC}"
    exit 1
fi

# ================================================================
# 步骤1: 创建测试表
# ================================================================
echo -e "${YELLOW}[步骤 1/5] 创建测试表 (${CREATE_TABLES_SQL})...${NC}"
# 使用修改后的 MYSQL_CMD
if ${MYSQL_CMD} < "$CREATE_TABLES_SQL"; then
    echo -e "${GREEN}✅ 测试表创建成功${NC}"
else
    echo -e "${RED}❌ 测试表创建失败${NC}"
    exit 1
fi
echo ""

# ================================================================
# 步骤2: 生成测试数据
# ================================================================
echo -e "${YELLOW}[步骤 2/5] 生成测试数据 (${GENERATE_DATA_SQL})...${NC}"
# 隐藏 Warning 输出
if ${MYSQL_CMD} < "$GENERATE_DATA_SQL" 2>&1 | grep -v "Warning"; then
    echo -e "${GREEN}✅ 测试数据生成成功${NC}"
    TEST_COUNT=$(${MYSQL_CMD} -N -e "SELECT COUNT(*) FROM cigarette_distribution_info_${TEST_YEAR}_${TEST_MONTH}_${TEST_WEEK};")
    echo -e "${GREEN}   共生成 ${TEST_COUNT} 个测试用例${NC}"
else
    echo -e "${RED}❌ 测试数据生成失败${NC}"
    exit 1
fi
echo ""

# ================================================================
# 步骤3: 检查后端服务
# ================================================================
echo -e "${YELLOW}[步骤 3/5] 检查后端服务状态 (${HEALTH_CHECK_URL})...${NC}"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" ${HEALTH_CHECK_URL})

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✅ 后端服务运行正常 (HTTP ${HTTP_CODE})${NC}"
else
    echo -e "${RED}❌ 无法连接到后端服务 (HTTP ${HTTP_CODE})，请确保 Spring Boot 服务在端口 28080 运行。${NC}"
    echo -e "${YELLOW}   启动命令: cd /path/to/project && mvn spring-boot:run${NC}"
    exit 1
fi
echo ""

# ================================================================
# 步骤4: 调用后端API执行算法
# ================================================================
echo -e "${YELLOW}[步骤 4/5] 调用后端API执行算法...${NC}"
API_URL="${API_ENDPOINT}?year=${TEST_YEAR}&month=${TEST_MONTH}&weekSeq=${TEST_WEEK}"
echo -e "${BLUE}   API: ${API_URL}${NC}"
echo -e "${BLUE}   注意: 此处未传递城网/农网比例参数，算法将使用默认值或根据情况自动调整。${NC}"
echo ""

# 执行 API 调用
API_RESPONSE=$(curl -s -X POST "${API_URL}")

# 检查 API 响应是否包含成功标志
if echo "$API_RESPONSE" | grep -q '"success":true'; then
    echo -e "${GREEN}✅ 算法执行成功${NC}"
    SUCCESS_COUNT=$(echo "$API_RESPONSE" | grep -o '"successCount":[0-9]*' | grep -o '[0-9]*')
    TOTAL_COUNT=$(echo "$API_RESPONSE" | grep -o '"totalCount":[0-9]*' | grep -o '[0-9]*')
    if [ -n "$SUCCESS_COUNT" ] && [ -n "$TOTAL_COUNT" ]; then
        echo -e "${GREEN}   成功计算卷烟数: ${SUCCESS_COUNT}/${TOTAL_COUNT}${NC}"
    else
        echo -e "${YELLOW}   未能从响应中提取成功数量。${NC}"
    fi
    echo -e "${BLUE}   API响应: ${API_RESPONSE}${NC}"
else
    echo -e "${RED}❌ 算法执行失败${NC}"
    echo -e "${RED}   API响应: ${API_RESPONSE}${NC}"
fi
echo ""

# ================================================================
# 步骤5: 查询测试结果并保存
# ================================================================
echo -e "${YELLOW}[步骤 5/5] 查询测试结果 (${QUERY_RESULTS_SQL}) 并保存到 ${OUTPUT_FILE}...${NC}"
if ${MYSQL_CMD} < "$QUERY_RESULTS_SQL" > "$OUTPUT_FILE"; then
    echo -e "${GREEN}✅ 测试结果查询并保存成功${NC}"
    echo ""
    echo -e "${BLUE}--- 测试报告关键统计 ---${NC}"
    grep -A 15 '===== 报告部分 =====' "$OUTPUT_FILE" | grep -v '====='
    echo -e "${BLUE}-----------------------${NC}"
else
    echo -e "${RED}❌ 测试结果查询失败${NC}"
    exit 1
fi
echo ""

# ================================================================
# 结束
# ================================================================
echo -e "${BLUE}================================${NC}"
echo -e "${GREEN}  ✅ 档位+市场类型测试完成！  ${NC}"
echo -e "${BLUE}================================${NC}"
echo ""
echo -e "${YELLOW}📊 查看详细结果:${NC}"
echo -e "   ${BLUE}cat ${OUTPUT_FILE}${NC}"
echo ""
echo -e "${YELLOW}📝 如需生成详细测试报告, 请参考:${NC}"
echo -e "   ${BLUE}test docs/档位+市场类型/测试报告模板.md (请手动创建)${NC}"
echo ""