#!/bin/bash

# 卷烟投放管理系统 - 自动部署脚本
# 用途：在服务器上一键部署整个系统

set -e  # 遇到错误立即退出

# 定义颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # 无颜色

echo -e "${BLUE}=========================================="
echo -e "  卷烟投放管理系统 - 自动部署"
echo -e "==========================================${NC}"
echo ""

# 步骤1: 检查Docker环境
echo -e "${BLUE}[1/6]${NC} 检查Docker环境..."

if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker 未安装，请先安装 Docker${NC}"
    echo -e "安装命令: curl -fsSL https://get.docker.com | sh"
    exit 1
fi
echo -e "${GREEN}✅ Docker 已安装${NC}"

# 检查docker-compose或docker compose
if command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE="docker-compose"
    echo -e "${GREEN}✅ Docker Compose 已安装 (docker-compose)${NC}"
elif docker compose version &> /dev/null; then
    DOCKER_COMPOSE="docker compose"
    echo -e "${GREEN}✅ Docker Compose 已安装 (docker compose)${NC}"
else
    echo -e "${RED}❌ Docker Compose 未安装，请先安装 Docker Compose${NC}"
    exit 1
fi

# 检查Docker服务是否运行
if ! docker info &> /dev/null; then
    echo -e "${RED}❌ Docker 服务未运行，请启动 Docker${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Docker 服务正在运行${NC}"
echo ""

# 步骤2: 检查必要文件
echo -e "${BLUE}[2/6]${NC} 检查部署文件..."

# 2.1 检查前端构建产物
echo -e "检查前端构建产物..."
if [ -d "vue-web/dist" ]; then
    if [ -f "vue-web/dist/index.html" ]; then
        DIST_SIZE=$(du -sh vue-web/dist 2>/dev/null | cut -f1)
        echo -e "   ${GREEN}✅ vue-web/dist/ 存在 (大小: $DIST_SIZE)${NC}"
    else
        echo -e "   ${RED}❌ vue-web/dist/index.html 不存在${NC}"
        echo -e "   ${YELLOW}请先构建前端: cd ../CigaretteDistributionSystem-vue-web && npm run build${NC}"
        exit 1
    fi
else
    echo -e "   ${RED}❌ vue-web/dist/ 文件夹不存在${NC}"
    echo -e "   ${YELLOW}请先构建前端: cd ../CigaretteDistributionSystem-vue-web && npm run build${NC}"
    exit 1
fi

# 2.2 检查后端jar包
echo -e "检查后端jar包..."
if [ -f "Springboot-server/app.jar" ]; then
    JAR_SIZE=$(du -sh Springboot-server/app.jar 2>/dev/null | cut -f1)
    echo -e "   ${GREEN}✅ Springboot-server/app.jar 存在 (大小: $JAR_SIZE)${NC}"
else
    echo -e "   ${RED}❌ Springboot-server/app.jar 不存在${NC}"
    echo -e "   ${YELLOW}请先构建后端: cd ../CigaretteDistributionSystem-Springboot-server && mvn clean package${NC}"
    exit 1
fi

# 2.3 检查所有配置文件
echo -e "检查配置文件..."
REQUIRED_FILES=(
    "vue-web/Dockerfile"
    "vue-web/docker-compose.yml"
    "vue-web/ngx.conf"
    "Springboot-server/Dockerfile"
    "Springboot-server/docker-compose.yml"
    "Springboot-server/application-prod.yml"
    "mysql/docker-compose.yml"
    "docker-compose.yml"
)

MISSING_FILES=0
for file in "${REQUIRED_FILES[@]}"; do
    if [ -f "$file" ]; then
        echo -e "   ${GREEN}✅ $file${NC}"
    else
        echo -e "   ${RED}❌ $file 缺失${NC}"
        MISSING_FILES=$((MISSING_FILES + 1))
    fi
done

if [ $MISSING_FILES -gt 0 ]; then
    echo -e "${RED}发现 $MISSING_FILES 个配置文件缺失，请检查部署包完整性${NC}"
    exit 1
fi

echo -e "${GREEN}✅ 所有必要文件已就绪 (前端+后端+8个配置文件)${NC}"
echo ""

# 步骤3: 创建必要的目录并复制初始化文件
echo -e "${BLUE}[3/6]${NC} 创建数据目录..."

mkdir -p /data/mysql/data
mkdir -p /data/mysql/conf
mkdir -p /data/mysql/init
mkdir -p /data/springboot/logs
mkdir -p /data/vue-web/logs

echo -e "${GREEN}✅ 数据目录创建完成${NC}"

# 复制初始化SQL到数据目录（仅在首次部署或文件不存在时）
if [ -f "mysql/init/init.sql" ]; then
    echo -e "复制数据库初始化SQL..."
    cp mysql/init/init.sql /data/mysql/init/
    echo -e "${GREEN}✅ 初始化SQL已复制${NC}"
else
    echo -e "${YELLOW}⚠️  未找到 mysql/init/init.sql，跳过初始化SQL复制${NC}"
fi
echo ""

# 步骤4: 创建Docker网络
echo -e "${BLUE}[4/6]${NC} 配置Docker网络..."

if docker network inspect cigarette-network &> /dev/null; then
    echo -e "${YELLOW}⚠️  网络 cigarette-network 已存在${NC}"
else
    docker network create cigarette-network
    echo -e "${GREEN}✅ 网络 cigarette-network 创建成功${NC}"
fi
echo ""

# 步骤5: 停止旧服务（如果存在）
echo -e "${BLUE}[5/6]${NC} 检查现有服务..."

RUNNING_CONTAINERS=$($DOCKER_COMPOSE ps -q 2>/dev/null | wc -l)
if [ "$RUNNING_CONTAINERS" -gt 0 ]; then
    echo -e "${YELLOW}⚠️  发现正在运行的服务，准备停止...${NC}"
    $DOCKER_COMPOSE down
    echo -e "${GREEN}✅ 旧服务已停止${NC}"
else
    echo -e "${GREEN}✅ 没有运行中的服务${NC}"
fi
echo ""

# 步骤6: 构建镜像并启动服务
echo -e "${BLUE}[6/6]${NC} 构建镜像并启动服务..."

echo -e "构建Docker镜像..."
$DOCKER_COMPOSE build --no-cache || {
    echo -e "${RED}❌ 构建镜像失败${NC}"
    exit 1
}
echo -e "${GREEN}✅ 镜像构建成功${NC}"

echo -e "启动服务..."
$DOCKER_COMPOSE up -d || {
    echo -e "${RED}❌ 启动服务失败${NC}"
    exit 1
}
echo -e "${GREEN}✅ 服务启动成功${NC}"
echo ""

# 等待服务启动
echo -e "${BLUE}等待服务完全启动...${NC}"
sleep 5

# 检查服务状态
echo -e "${BLUE}检查服务状态...${NC}"
$DOCKER_COMPOSE ps

echo ""
echo -e "${GREEN}=========================================="
echo -e "  🎉 部署成功！"
echo -e "==========================================${NC}"
echo ""
echo -e "📊 服务访问地址："
echo -e "  前端界面: ${GREEN}http://$(hostname -I | awk '{print $1}')${NC}"
echo -e "  或访问:   ${GREEN}http://服务器IP${NC}"
echo ""
echo -e "🔍 管理命令："
echo -e "  查看状态: ${YELLOW}$DOCKER_COMPOSE ps${NC}"
echo -e "  查看日志: ${YELLOW}$DOCKER_COMPOSE logs -f${NC}"
echo -e "  重启服务: ${YELLOW}$DOCKER_COMPOSE restart${NC}"
echo -e "  停止服务: ${YELLOW}$DOCKER_COMPOSE down${NC}"
echo ""
echo -e "📝 健康检查："
echo -e "  ${YELLOW}curl http://localhost:28080/api/common/health${NC}"
echo ""