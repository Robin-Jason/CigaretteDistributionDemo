#!/bin/bash

echo "=== 卷烟分配算法测试 ==="
echo "启动Spring Boot应用..."

# 编译项目
echo "1. 编译项目..."
mvn clean compile

# 启动应用
echo "2. 启动应用..."
mvn spring-boot:run &
APP_PID=$!

# 等待应用启动
echo "3. 等待应用启动..."
sleep 30

# 测试算法
echo "4. 测试分配算法..."
curl -X GET "http://localhost:28080/api/cigarette/test-algorithm" \
     -H "Content-Type: application/json" \
     -w "\nHTTP状态码: %{http_code}\n总耗时: %{time_total}秒\n"

# 停止应用
echo "5. 停止应用..."
kill $APP_PID

echo "测试完成！"
