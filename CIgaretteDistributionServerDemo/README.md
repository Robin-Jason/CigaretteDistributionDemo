# 卷烟分配服务器

## 项目简介

这是一个基于Spring Boot的卷烟分配后端服务，实现了卷烟投放的智能分配算法。系统能够根据预投放量、投放区域和客户档位信息，自动计算最优的卷烟分配方案。

## 技术栈

- **后端框架**: Spring Boot 2.7.18
- **数据库**: MySQL 8.0
- **ORM框架**: Spring Data JPA
- **缓存**: Spring Cache
- **构建工具**: Maven
- **Java版本**: Java 8

## 项目结构

```
src/main/java/org/example/
├── CigaretteDistributionApplication.java    # 主启动类
├── controller/                              # 控制器层
│   └── CigaretteDistributionController.java
├── service/                                 # 服务层
│   ├── CigaretteDistributionService.java
│   └── algorithm/                          # 算法服务
│       └── CigaretteDistributionAlgorithm.java
├── repository/                              # 数据访问层
│   ├── DemoTestDataRepository.java
│   ├── DemoTestClientNumDataRepository.java
│   └── DemoTestAdvDataRepository.java
├── entity/                                  # 实体类
│   ├── DemoTestData.java
│   ├── DemoTestClientNumData.java
│   └── DemoTestAdvData.java
├── dto/                                     # 数据传输对象
│   ├── CigaretteDistributionDto.java
│   ├── QueryRequestDto.java
│   └── UpdateRequestDto.java
├── util/                                    # 工具类
│   └── KmpMatcher.java
└── exception/                               # 异常处理
    └── GlobalExceptionHandler.java
```

## 数据库设计

### 主要数据表

1. **demo_test_data**: 卷烟测试数据表
   - 包含卷烟代码、名称、年份、月份、周序号、投放区域等基本信息
   - D30-D1字段表示30个档位的分配值

2. **demo_test_clientNumdata**: 客户数量数据表
   - 记录各投放区域在不同档位的客户数量
   - 用于计算实际投放量

3. **demo_test_ADVdata**: 预投放量数据表
   - 记录卷烟的预投放量（ADV）和URS值

## 核心功能

### 1. 卷烟分配算法

实现了基于数学优化的卷烟分配算法：

- **非递增约束**: 确保每个区域从高档位到低档位的分配值非递增
- **整体分布均匀**: 通过整列调整和局部调整实现分配均匀性
- **误差最小化**: 通过多种候选方案比较，选择误差最小的分配方案

### 2. KMP字符串匹配

使用KMP算法实现投放区域的高效匹配：

- 将卷烟的投放区域字段与城乡分类代码集合进行匹配
- 返回匹配成功的投放区域列表

### 3. 数据缓存

服务层实现了关键数据的缓存机制：

- 投放区域列表缓存
- 区域客户数矩阵缓存
- 预投放量数据缓存

## API接口

### 1. 查询卷烟分配数据

```
POST /api/cigarette/query
Content-Type: application/json

{
    "year": 2024,
    "month": 1,
    "weekSeq": 1
}
```

**响应示例**:
```json
{
    "success": true,
    "message": "查询成功",
    "data": [
        {
            "cigCode": "001",
            "cigName": "测试卷烟",
            "adv": 1000.00,
            "actualDelivery": 950.00,
            "d30": 10.00,
            "d29": 9.00,
            // ... 其他档位数据
            "deliveryAreas": "区域1，区域2",
            "remark": "测试备注"
        }
    ],
    "total": 1
}
```

### 2. 更新卷烟分配数据

```
POST /api/cigarette/update
Content-Type: application/json

{
    "cigCode": "001",
    "cigName": "测试卷烟",
    "distribution": [10.0, 9.0, 8.0, ...] // D30到D1的分配值
}
```

### 3. 健康检查

```
GET /api/cigarette/health
```

## 配置说明

### 数据库配置

在 `application.yml` 中配置数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/marketing?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=GMT%2B8
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### 服务器配置

```yaml
server:
  port: 28080
```

## 运行说明

### 环境要求

- JDK 8+
- Maven 3.6+
- MySQL 8.0+

### 启动步骤

1. **克隆项目**
   ```bash
   git clone <project-url>
   cd cigarette-distribution-server
   ```

2. **配置数据库**
   - 创建数据库 `marketing`
   - 执行建表SQL脚本
   - 修改 `application.yml` 中的数据库连接信息

3. **编译运行**
   ```bash
   mvn clean compile
   mvn spring-boot:run
   ```

4. **访问服务**
   - 服务地址: http://localhost:28080
   - 健康检查: http://localhost:28080/api/cigarette/health

## 算法原理

### 数学模型

卷烟分配问题建模为优化问题：

- 目标：最小化误差 |S - T|，其中S为实际投放量，T为预投放量
- 约束：非递增约束 x_{i1} ≥ x_{i2} ≥ ... ≥ x_{iB}
- 变量：x_{ij} 表示区域i档位j的分配值

### 算法流程

1. **粗调过程**: 从最高档位开始，逐列增加所有区域的分配值
2. **候选方案生成**: 生成多种调整方案
3. **最优方案选择**: 比较各方案误差，选择最优解

## 注意事项

1. 确保数据库连接正常，表结构完整
2. 算法计算可能需要一定时间，建议在生产环境中设置合理的超时时间
3. 缓存机制会自动管理数据更新，无需手动清理

## 扩展功能

- 支持批量更新操作
- 添加数据导入导出功能
- 实现更复杂的分配策略
- 增加性能监控和日志分析

