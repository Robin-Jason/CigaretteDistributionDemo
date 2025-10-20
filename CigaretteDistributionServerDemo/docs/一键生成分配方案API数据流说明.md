# 一键生成分配方案API - 数据流详解

## 📋 文档概要

**API名称**: 一键生成分配方案  
**接口地址**: `/api/calculate/generate-distribution-plan`  
**请求方式**: POST  
**核心功能**: 删除旧数据 → 执行算法计算 → 写回新数据  
**最后更新**: 2025-10-19

---

## 🎯 API概述

### 两个核心接口

| 接口 | 功能 | 区别 |
|-----|------|------|
| `/api/calculate/write-back` | 执行计算并写回 | 直接写回，可能覆盖 |
| `/api/calculate/generate-distribution-plan` | 一键生成分配方案 | 先删除后写回，完全重建 |

**本文档重点**: `/api/calculate/generate-distribution-plan`

---

## 📡 请求参数说明

### 基本参数（必填）

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|-------|------|------|------|------|
| `year` | Integer | ✅ | 年份 (2020-2099) | 2025 |
| `month` | Integer | ✅ | 月份 (1-12) | 10 |
| `weekSeq` | Integer | ✅ | 周序号 (1-5) | 1 |

### 扩展参数（可选）

| 参数名 | 类型 | 必填 | 说明 | 默认值 | 适用业务类型 |
|-------|------|------|------|--------|------------|
| `urbanRatio` | BigDecimal | ❌ | 城网分配比例 | 0.4 (40%) | 仅档位+市场类型 |
| `ruralRatio` | BigDecimal | ❌ | 农网分配比例 | 0.6 (60%) | 仅档位+市场类型 |

**特别说明**: 
- 比例参数只对"档位+市场类型"生效
- 且仅当待投放区域同时包含城网和农网时才使用

---

## 🔄 完整数据流（端到端）

### 数据流总览

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. 前端发起请求                                                    │
│    POST /api/calculate/generate-distribution-plan               │
│    参数: year=2025, month=10, weekSeq=1                         │
│          urbanRatio=0.45, ruralRatio=0.55 (可选)               │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. Controller层接收（DistributionCalculateController）           │
│    - 接收并验证参数                                                │
│    - 构建marketRatios Map（如果有比例参数）                        │
│    - 调用Service层                                                │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. Service层检查（DistributionCalculateServiceImpl）              │
│    - 检查是否存在旧数据                                             │
│    - 如果存在，调用DataManagementService删除                       │
│    - 调用getAndwriteBackAllocationMatrix()                      │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. 查询卷烟基础数据（JdbcTemplate）                                │
│    - 表名: cigarette_distribution_info_{year}_{month}_{weekSeq}│
│    - SQL: SELECT CIG_CODE, CIG_NAME, ADV, DELIVERY_AREA, ...   │
│    - 结果: List<Map<String, Object>> advDataList               │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│ 5. 遍历每个卷烟，按业务类型分别处理                                  │
│    for each cigarette in advDataList:                          │
│        根据 deliveryMethod + deliveryEtype 确定业务类型           │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 ├─────────────────────────────────────────────────┐
                 │                                                 │
                 ▼                                                 ▼
        [档位+城乡分类代码]                                   [档位+业态]
        [档位+区县]                                          [档位+市场类型]
        [按档位统一投放]                                       ...

（以下详细展开各业务类型的数据流）
```

---

## 📊 各业务类型详细数据流

### 类型1: 档位+城乡分类代码

#### 数据流程

```
卷烟数据
├─ deliveryMethod: "按档位扩展投放"
├─ deliveryEtype: "档位+城乡分类代码"
├─ deliveryArea: "主城区,乡中心区,村庄"
└─ adv: 10000

        ↓

DistributionStrategyManager.getStrategy()
        ↓
返回: UrbanRuralDistributionStrategy

        ↓

UrbanRuralDistributionStrategy.getTargetList()
├─ 调用: CommonService.getAllRegionList("按档位扩展投放", "档位+城乡分类代码")
│   └─ 查询: region_clientNum_3_1 表
│   └─ 返回: ["主城区", "乡中心区", "城乡结合区", "村庄", ...]
├─ 调用: KmpMatcher.matchPatterns("主城区,乡中心区,村庄", allRegions)
└─ 返回: ["主城区", "乡中心区", "村庄"]

        ↓

UrbanRuralDistributionStrategy.calculateMatrix()
├─ 获取客户数矩阵:
│   └─ CommonService.buildRegionCustomerMatrix("按档位扩展投放", "档位+城乡分类代码")
│       └─ 查询: region_clientNum_3_1
│       └─ 返回: RegionCustomerMatrix {
│             regionNames: ["主城区", "乡中心区", ...],
│             customerMatrix: [[D30, D29, ..., D1], [...], ...]
│          }
│
├─ 筛选目标区域的客户数:
│   ├─ 主城区的30个档位客户数: [100, 95, 90, ...]
│   ├─ 乡中心区的30个档位客户数: [50, 48, 45, ...]
│   └─ 村庄的30个档位客户数: [30, 28, 26, ...]
│
└─ 调用算法计算:
    └─ UrbanRuralClassificationCodeDistributionAlgorithm.calculateDistribution()
        ├─ 输入: targetList=["主城区","乡中心区","村庄"]
        │        customerMatrix=[3][30]
        │        targetAmount=10000
        │
        ├─ 执行多轮粗调（100轮上限）
        ├─ 生成3个候选方案
        ├─ 选择误差最小的方案
        ├─ 强制非递增约束
        │
        └─ 输出: allocationMatrix = [
              [5, 5, 5, 4, 4, ...],  // 主城区各档位分配
              [5, 5, 5, 4, 4, ...],  // 乡中心区各档位分配
              [5, 5, 5, 4, 4, ...]   // 村庄各档位分配
           ]

        ↓

计算实际投放量（每个区域）
for each region in targetList:
    actualDelivery = Σ(allocationMatrix[i][j] × customerMatrix[i][j])
    
示例:
├─ 主城区实际投放 = 5×100 + 5×95 + 5×90 + ... = 4500条
├─ 乡中心区实际投放 = 5×50 + 5×48 + 5×45 + ... = 2500条
└─ 村庄实际投放 = 5×30 + 5×28 + 5×26 + ... = 1500条

        ↓

生成编码表达式
├─ 调用: EncodeDecodeService.encodeForSpecificArea()
├─ 主城区编码示例: "B4①(5×9+5×8+10×7+10×6)"
├─ 乡中心区编码示例: "B4⑥(5×9+5×8+10×7+10×6)"
└─ 村庄编码示例: "B4⑦(5×9+5×8+10×7+10×6)"

**编码规则**: 详见 `docs/编码规则表.md`

**编码格式说明**:
- `B`: 投放类型编码（B=按档位扩展投放）
- `4`: 扩展类型编码（4=档位+城乡分类代码）
- `①⑥⑦`: 区域编码（①=主城区，⑥=乡中心区，⑦=村庄）
- `(5×9+5×8+10×7+10×6)`: 档位压缩编码
  - 5×9: D30-D26共5个档位，每档分配9
  - 5×8: D25-D21共5个档位，每档分配8
  - 10×7: D20-D11共10个档位，每档分配7
  - 10×6: D10-D1共10个档位，每档分配6

        ↓

写回数据库
表: cigarette_distribution_prediction_{year}_{month}_{weekSeq}
记录1: {cigCode, cigName, deliveryArea:"主城区", D30:5, D29:5, ..., actualDelivery:4500}
记录2: {cigCode, cigName, deliveryArea:"乡中心区", D30:5, D29:5, ..., actualDelivery:2500}
记录3: {cigCode, cigName, deliveryArea:"村庄", D30:5, D29:5, ..., actualDelivery:1500}
```

---

### 类型2: 档位+业态

#### 数据流程

```
卷烟数据
├─ deliveryMethod: "按档位扩展投放"
├─ deliveryEtype: "档位+业态"
├─ deliveryArea: "便利店,超市,商场"
└─ adv: 20000

        ↓

DistributionStrategyManager.getStrategy()
        ↓
返回: BusinessFormatDistributionStrategy

        ↓

BusinessFormatDistributionStrategy.getTargetList()
├─ 调用: CommonService.getAllRegionList("按档位扩展投放", "档位+业态")
│   └─ 查询: region_clientNum_4_1 表
│   └─ 返回: ["便利店", "超市", "商场", "烟草专业店", ...]
├─ 调用: KmpMatcher.matchPatterns("便利店,超市,商场", allFormats)
└─ 返回: ["便利店", "超市", "商场"]

        ↓

BusinessFormatDistributionStrategy.calculateMatrix()
├─ 获取客户数矩阵: region_clientNum_4_1
├─ 筛选目标业态的客户数
└─ 调用: BussinessFormatDistributionAlgorithm.calculateDistribution()
    └─ 输出: allocationMatrix = [
          [8, 8, 7, 7, ...],  // 便利店
          [8, 8, 7, 7, ...],  // 超市
          [8, 8, 7, 7, ...]   // 商场
       ]

        ↓

计算实际投放量 + 生成编码 + 写回数据库
（与档位+城乡分类代码相同流程）
```

---

### 类型3: 档位+市场类型 ⭐

#### 数据流程（含比例参数）

```
卷烟数据
├─ deliveryMethod: "按档位扩展投放"
├─ deliveryEtype: "档位+市场类型"
├─ deliveryArea: "城网,农网"
└─ adv: 15000

前端传入的比例参数
├─ urbanRatio: 0.45 (45%)
└─ ruralRatio: 0.55 (55%)

        ↓

Controller层
├─ 接收参数: year, month, weekSeq, urbanRatio, ruralRatio
├─ 构建marketRatios Map:
│   marketRatios = {
│       "urbanRatio": 0.45,
│       "ruralRatio": 0.55
│   }
└─ 调用: distributionService.getAndwriteBackAllocationMatrix(
        year, month, weekSeq, marketRatios)

        ↓

Service层（DistributionCalculateServiceImpl）
├─ 从数据库查询卷烟数据
├─ 遍历每个卷烟
└─ 当 deliveryEtype = "档位+市场类型" 时:
    ├─ 从marketRatios中提取比例参数
    └─ 构建extraParams Map:
        extraParams = {
            "urbanRatio": 0.45,
            "ruralRatio": 0.55
        }

        ↓

策略层（MarketDistributionStrategy）
├─ getTargetList("城网,农网")
│   └─ 返回: ["城网", "农网"]
│
└─ calculateMatrix(targetList, 15000, extraParams)
    ├─ 检查是否同时包含城网和农网:
    │   hasUrban = true (包含"城")
    │   hasRural = true (包含"农")
    │   hasBoth = true ✅
    │
    ├─ 确定比例:
    │   if (hasBoth && extraParams有比例) {
    │       urbanRatio = 0.45  // 使用前端传入的
    │       ruralRatio = 0.55
    │   } else {
    │       urbanRatio = 0.4   // 使用默认值
    │       ruralRatio = 0.6
    │   }
    │
    ├─ 获取客户数矩阵: region_clientNum_2_1
    │   ├─ 城网客户数: [150, 145, 140, ...]
    │   └─ 农网客户数: [80, 78, 75, ...]
    │
    └─ 调用算法:
        MarketProportionalCigaretteDistributionAlgorithm.calculateDistribution(
            targetList, customerMatrix, 15000, 0.45, 0.55)
        
        ├─ 按比例计算各网络目标投放量:
        │   urbanTarget = 15000 × 0.45 = 6750条
        │   ruralTarget = 15000 × 0.55 = 8250条
        │
        ├─ 分别对城网和农网执行核心算法
        │   ├─ 城网分配: runCoreAlgorithm(["城网"], ..., 6750)
        │   │   └─ 输出: [[7, 7, 6, 6, ...]]
        │   └─ 农网分配: runCoreAlgorithm(["农网"], ..., 8250)
        │       └─ 输出: [[9, 9, 8, 8, ...]]
        │
        └─ 合并结果:
            allocationMatrix = [
                [7, 7, 6, 6, ...],  // 城网
                [9, 9, 8, 8, ...]   // 农网
            ]

        ↓

计算实际投放量
├─ 城网实际投放 = Σ(7×150 + 7×145 + ...) ≈ 6750条
└─ 农网实际投放 = Σ(9×80 + 9×78 + ...) ≈ 8250条
总计: 15000条 ✅

        ↓

生成编码表达式 + 写回数据库
记录1: {deliveryArea:"城网", D30:7, ..., actualDelivery:6750}
记录2: {deliveryArea:"农网", D30:9, ..., actualDelivery:8250}
```

---

### 类型4: 档位+区县

#### 数据流程

```
卷烟数据
├─ deliveryEtype: "档位+区县"
├─ deliveryArea: "房县,郧西,竹山"
└─ adv: 8000

        ↓

CountyDistributionStrategy
├─ getTargetList() → ["房县", "郧西", "竹山"]
├─ 获取客户数矩阵: region_clientNum_1_1
└─ 调用: countyCigaretteDistributionAlgorithm.calculateDistribution()
    └─ 输出: allocationMatrix (与城乡分类代码相同算法)

        ↓

写回3条记录（房县、郧西、竹山各1条）
```

---

### 类型5: 按档位统一投放

#### 数据流程

```
卷烟数据
├─ deliveryMethod: "按档位统一投放"
├─ deliveryEtype: null
├─ deliveryArea: 任意（会被忽略）
└─ adv: 50000

        ↓

CityDistributionStrategy
├─ getTargetList() → ["全市"] (固定)
├─ 获取客户数矩阵: region_clientNum_0_1
└─ 调用: CityCigaretteDistributionAlgorithm.calculateDistribution()
    └─ 输出: allocationMatrix (单行，全市汇总)

        ↓

写回1条记录（全市）
```

---

## 🔍 数据流关键节点详解

### 节点1: 策略选择（DistributionStrategyManager）

```java
// 根据投放方法和投放类型选择策略
DistributionStrategy strategy = strategyManager.getStrategy(deliveryMethod, deliveryEtype);

// 映射关系
按档位统一投放 + null          → CityDistributionStrategy
按档位扩展投放 + "档位+区县"    → CountyDistributionStrategy
按档位扩展投放 + "档位+市场类型" → MarketDistributionStrategy
按档位扩展投放 + "档位+城乡分类代码" → UrbanRuralDistributionStrategy
按档位扩展投放 + "档位+业态"    → BusinessFormatDistributionStrategy
```

---

### 节点2: 目标列表解析

```java
// 统一的解析流程
List<String> targetList = strategy.getTargetList(deliveryArea);

// 内部流程（以档位+城乡分类代码为例）
1. 调用CommonService获取所有可用区域
   allRegions = ["主城区", "乡中心区", "城乡结合区", "村庄", ...]
   
2. 使用KmpMatcher进行模式匹配
   pattern = "主城区,乡中心区,村庄"
   result = ["主城区", "乡中心区", "村庄"]
   
3. 返回匹配结果
```

**说明**: 
- CommonService统一管理区域列表
- KmpMatcher统一处理模式匹配
- 各策略类使用相同的解析机制

---

### 节点3: 客户数矩阵获取

```java
// 统一的获取流程
BigDecimal[][] customerMatrix = strategy.getCustomerMatrix();

// 内部流程
1. 调用CommonService.buildRegionCustomerMatrix()
   ├─ 根据投放类型确定表名
   │   档位+城乡分类代码 → region_clientNum_3_1
   │   档位+业态 → region_clientNum_4_1
   │   档位+市场类型 → region_clientNum_2_1
   │   ...
   │
   ├─ 查询该表的所有数据
   │   SELECT region, D30, D29, ..., D1 FROM region_clientNum_X_X
   │
   └─ 构建矩阵
       regionNames = ["区域1", "区域2", ...]
       customerMatrix = [
           [D30, D29, ..., D1],  // 区域1
           [D30, D29, ..., D1],  // 区域2
           ...
       ]

2. 筛选目标区域的数据
   targetCustomerMatrix = 从完整矩阵中筛选targetList对应的行

3. 缓存（@Cacheable）
   相同投放类型的后续请求直接从缓存读取
```

---

### 节点4: 算法计算

```java
// 算法调用
BigDecimal[][] allocationMatrix = distributionAlgorithm.calculateDistribution(
    targetList, targetCustomerMatrix, targetAmount);

// 档位+城乡分类代码、档位+业态、档位+区县使用相同算法逻辑:
// V3 多轮粗调优化算法

1. 初始化分配矩阵（全0）
2. 多轮粗调
   ├─ 从D30到D1逐列尝试增加
   ├─ 整列+1操作
   ├─ 最多100轮
   └─ 接近目标（<5%）时停止
3. 生成候选方案（3个）
   ├─ 候选1: 粗调结果
   ├─ 候选2: 较低档位增加
   └─ 候选3: 分段调整
4. 选择误差最小的方案
5. 强制非递增约束

// 档位+市场类型使用特殊算法:
// MarketProportionalCigaretteDistributionAlgorithm

1. 按比例计算城网和农网目标投放量
2. 分别对城网和农网执行核心算法
3. 合并结果
4. 强制非递增约束
```

---

### 节点5: 实际投放量计算

```java
// 对每个区域计算实际投放量
BigDecimal actualDelivery = calculateActualDeliveryForRegion(
    target, allocationRow, deliveryMethod, deliveryEtype, remark);

// 内部流程
1. 获取该区域的客户数
   customerCounts = RegionClientNumDataService.findByTableNameAndRegion(tableName, target)
   
2. 按公式计算
   actualDelivery = Σ(i=0 to 29) allocationRow[i] × customerCounts[i]
   
示例:
   主城区实际投放 = 5×100 + 5×95 + 5×90 + 4×85 + ... = 4500条
```

---

### 节点6: 编码表达式生成

```java
// 生成编码表达式
String encodedExpression = encodeDecodeService.encodeForSpecificArea(
    cigCode, cigName, deliveryMethod, deliveryEtype, target, allRecords);

// 编码示例:
├─ 档位+城乡分类代码，主城区: "B4①(5×9+5×8+10×7+10×6)"
├─ 档位+业态，便利店: "B5a(6×10+6×9+10×8+8×7)"
└─ 档位+市场类型，城网: "B2C(7×10+7×9+8×8+8×7)"
```

**详细编码规则**: 请参考 `docs/编码规则表.md`

**编码格式说明**:
- 第一部分: 投放类型编码
  - `A`: 按档位统一投放
  - `B`: 按档位扩展投放
  - `C`: 按需投放
  
- 第二部分: 扩展类型编码（仅B类型）
  - `1`: 档位+区县
  - `2`: 档位+市场类型
  - `3`: 档位+区县+市场类型
  - `4`: 档位+城乡分类代码
  - `5`: 档位+业态
  
- 第三部分: 区域编码
  - 档位+区县: 城区(1)、丹江(2)、房县(3)、郧西(4)、郧阳(5)、竹山(6)、竹溪(7)
  - 档位+市场类型: 城网(C)、农网(N)
  - 档位+城乡分类代码: 主城区(①)、城乡结合区(②)、镇中心区(③)、镇乡接合区(④)、特殊区域(⑤)、乡中心区(⑥)、村庄(⑦)
  - 档位+业态: 便利店(a)、超市(b)、商场(c)、烟草专业店(d)、娱乐服务类(e)、其他(f)
  
- 第四部分: 档位压缩编码
  - 格式: `(档位跨度×投放量+档位跨度×投放量+...)`
  - 示例: `(5×9+5×8+10×7+10×6)` 表示D30-D26=9, D25-D21=8, D20-D11=7, D10-D1=6

---

### 节点7: 批量写回数据库

```java
// 批量插入（使用JdbcTemplate）
String tableName = "cigarette_distribution_prediction_{year}_{month}_{weekSeq}";
String sql = CigaretteDistributionSqlBuilder.buildBatchInsertSql(tableName);

jdbcTemplate.batchUpdate(sql, batchArgs);

// 每条记录包含
{
    CIG_CODE: "12345678",
    CIG_NAME: "黄鹤楼(1916中支)",
    YEAR: 2025,
    MONTH: 10,
    WEEK_SEQ: 1,
    DELIVERY_AREA: "主城区",  // 具体区域
    DELIVERY_METHOD: "按档位扩展投放",
    DELIVERY_ETYPE: "档位+城乡分类代码",
    D30: 5, D29: 5, ..., D1: 2,
    ACTUAL_DELIVERY: 4500,
    DEPLOYINFO_CODE: "DM1E3R1G5,5,5,4,4,...",
    BZ: "算法自动生成"
}
```

---

## 📊 完整数据流图（分层视图）

### Layer 1: Controller层（HTTP处理）

```
┌─────────────────────────────────────────────────────────────────┐
│ DistributionCalculateController                                 │
│                                                                 │
│ @PostMapping("/generate-distribution-plan")                    │
│                                                                 │
│ 输入参数:                                                        │
│ ├─ year: 2025                                                  │
│ ├─ month: 10                                                   │
│ ├─ weekSeq: 1                                                  │
│ ├─ urbanRatio: 0.45 (可选)                                     │
│ └─ ruralRatio: 0.55 (可选)                                     │
│                                                                 │
│ 处理逻辑:                                                        │
│ ├─ 验证参数                                                      │
│ ├─ 构建marketRatios Map                                        │
│ └─ 调用Service层                                                │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 ▼
```

### Layer 2: Service层（业务协调）

```
┌─────────────────────────────────────────────────────────────────┐
│ DistributionCalculateServiceImpl                                │
│                                                                 │
│ 核心方法:                                                        │
│ getAndwriteBackAllocationMatrix(year, month, weekSeq, ratios)  │
│                                                                 │
│ 处理流程:                                                        │
│ ├─ 1. 查询卷烟基础数据（JdbcTemplate）                           │
│ │   └─ SELECT * FROM cigarette_distribution_info_2025_10_1    │
│ │                                                              │
│ ├─ 2. 遍历每个卷烟                                              │
│ │   for each cigarette:                                       │
│ │       ├─ 提取: deliveryMethod, deliveryEtype, adv, area     │
│ │       ├─ 选择策略: strategyManager.getStrategy(method, type)│
│ │       ├─ 构建extraParams（如果是市场类型）                    │
│ │       ├─ 解析目标: strategy.getTargetList(area)             │
│ │       ├─ 计算分配: strategy.calculateMatrix(targets, adv, extra)│
│ │       ├─ 计算实际投放: 每个区域的Σ(分配×客户数)                │
│ │       ├─ 生成编码: encodeDecodeService.encode(...)          │
│ │       └─ 准备写回数据                                         │
│ │                                                              │
│ └─ 3. 批量写回数据库（JdbcTemplate.batchUpdate）                │
│     └─ INSERT INTO cigarette_distribution_prediction_...       │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 ▼
```

### Layer 3: Strategy层（策略模式）

```
┌─────────────────────────────────────────────────────────────────┐
│ DistributionStrategy（策略接口）                                  │
│                                                                 │
│ 5个具体策略:                                                     │
│ ├─ UrbanRuralDistributionStrategy（城乡分类代码）                │
│ ├─ BusinessFormatDistributionStrategy（业态）                   │
│ ├─ CountyDistributionStrategy（区县）                           │
│ ├─ MarketDistributionStrategy（市场类型）⭐                      │
│ └─ CityDistributionStrategy（统一投放）                          │
│                                                                 │
│ 统一接口方法:                                                    │
│ ├─ getTargetList(deliveryArea) → 解析目标区域列表                │
│ └─ calculateMatrix(targets, amount, extraParams) → 计算分配矩阵  │
│                                                                 │
│ 共同依赖:                                                        │
│ ├─ CommonService（统一数据服务）                                 │
│ ├─ KmpMatcher（模式匹配工具）                                    │
│ └─ 对应的算法类                                                  │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 ▼
```

### Layer 4: Algorithm层（核心算法）

```
┌─────────────────────────────────────────────────────────────────┐
│ 分配算法                                                         │
│                                                                 │
│ V3多轮粗调算法（用于3个业务类型）:                                 │
│ ├─ UrbanRuralClassificationCodeDistributionAlgorithm            │
│ ├─ BussinessFormatDistributionAlgorithm                        │
│ └─ countyCigaretteDistributionAlgorithm                        │
│                                                                 │
│ 市场比例分配算法:                                                 │
│ └─ MarketProportionalCigaretteDistributionAlgorithm             │
│                                                                 │
│ 统一投放算法:                                                     │
│ └─ CityCigaretteDistributionAlgorithm                           │
│                                                                 │
│ 核心逻辑:                                                        │
│ ├─ 初始化分配矩阵                                                │
│ ├─ 多轮粗调（整列+1）                                            │
│ ├─ 生成候选方案                                                  │
│ ├─ 选择最优方案                                                  │
│ └─ 强制非递增约束                                                │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 ▼
```

### Layer 5: Data Access层（数据访问）

```
┌─────────────────────────────────────────────────────────────────┐
│ 数据访问层                                                        │
│                                                                 │
│ CommonService（通用数据服务）:                                    │
│ ├─ getAllRegionList() - 获取区域列表                             │
│ └─ buildRegionCustomerMatrix() - 构建客户数矩阵                  │
│                                                                 │
│ RegionClientNumDataService（区域客户数查询）:                     │
│ └─ findByTableNameAndRegion() - 查询单个区域客户数                │
│                                                                 │
│ 数据访问技术:                                                     │
│ ├─ JdbcTemplate - 主要方式（动态表名）                           │
│ └─ EntityManager - 辅助方式（实体映射）                          │
│                                                                 │
│ SQL工具:                                                         │
│ └─ CigaretteDistributionSqlBuilder - 统一SQL构建                │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 ▼
```

### Layer 6: Database层（数据存储）

```
┌─────────────────────────────────────────────────────────────────┐
│ MySQL数据库（marketing）                                         │
│                                                                 │
│ 输入表（卷烟投放基础信息）:                                        │
│ └─ cigarette_distribution_info_{year}_{month}_{weekSeq}        │
│     ├─ CIG_CODE, CIG_NAME, ADV                                │
│     ├─ DELIVERY_METHOD, DELIVERY_ETYPE, DELIVERY_AREA         │
│     └─ bz, YEAR, MONTH, WEEK_SEQ                              │
│                                                                 │
│ 输出表（卷烟分配预测数据）:                                        │
│ └─ cigarette_distribution_prediction_{year}_{month}_{weekSeq}  │
│     ├─ CIG_CODE, CIG_NAME, DELIVERY_AREA                      │
│     ├─ D30, D29, D28, ..., D2, D1（30个档位）                  │
│     ├─ ACTUAL_DELIVERY（实际投放量）                            │
│     └─ DEPLOYINFO_CODE（编码表达式）                            │
│                                                                 │
│ 客户数基础表（区域客户数）:                                        │
│ ├─ region_clientNum_0_1/2 - 统一投放                           │
│ ├─ region_clientNum_1_1/2 - 档位+区县                          │
│ ├─ region_clientNum_2_1/2 - 档位+市场类型                       │
│ ├─ region_clientNum_3_1/2 - 档位+城乡分类代码                   │
│ └─ region_clientNum_4_1/2 - 档位+业态                          │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🎨 特殊业务类型：档位+市场类型比例参数流

### 完整数据流（带比例参数）

```
前端请求
POST /api/calculate/generate-distribution-plan?
    year=2025&month=10&weekSeq=1&
    urbanRatio=0.45&ruralRatio=0.55
                ↓
Controller层
├─ 接收参数
├─ 构建marketRatios = {"urbanRatio": 0.45, "ruralRatio": 0.55}
└─ 调用Service: getAndwriteBackAllocationMatrix(2025, 10, 1, marketRatios)
                ↓
Service层
├─ 查询cigarette_distribution_info_2025_10_1表
├─ 遍历每个卷烟
└─ 当发现投放类型="档位+市场类型":
    ├─ 从marketRatios提取比例参数
    └─ 构建extraParams = {"urbanRatio": 0.45, "ruralRatio": 0.55}
                ↓
Strategy层（MarketDistributionStrategy）
├─ getTargetList("城网,农网") → ["城网", "农网"]
├─ calculateMatrix(["城网","农网"], 15000, extraParams)
│   ├─ 检查同时包含城网和农网: ✅ 是
│   ├─ 检查extraParams有比例: ✅ 有
│   ├─ 使用比例: urbanRatio=0.45, ruralRatio=0.55
│   └─ 记录日志: "使用前端传入的市场类型比例 - 城网: 0.45, 农网: 0.55"
                ↓
Algorithm层
├─ 计算城网目标: 15000 × 0.45 = 6750条
├─ 计算农网目标: 15000 × 0.55 = 8250条
├─ 分别执行核心算法
├─ 合并结果
└─ 返回: allocationMatrix = [[城网分配], [农网分配]]
                ↓
写回数据库
├─ 记录1: 城网, D30-D1, actualDelivery≈6750
└─ 记录2: 农网, D30-D1, actualDelivery≈8250
```

### 比例参数的三种场景

#### 场景1: 传入比例 + 同时包含城网和农网 ✅

```
待投放区域: "城网,农网"
前端参数: urbanRatio=0.45, ruralRatio=0.55

        ↓
检查: hasUrban=true, hasRural=true, hasBoth=true ✅
使用比例: 0.45 / 0.55（前端传入）
日志: "使用前端传入的市场类型比例"
```

#### 场景2: 不传比例 + 同时包含城网和农网 ✅

```
待投放区域: "城网,农网"
前端参数: 无比例参数

        ↓
检查: hasUrban=true, hasRural=true, hasBoth=true ✅
使用比例: 0.4 / 0.6（默认值）
日志: "使用默认市场类型比例 - 城网: 40%, 农网: 60%"
```

#### 场景3: 传入比例 + 仅包含一个市场 ❌

```
待投放区域: "城网"（仅城网）
前端参数: urbanRatio=0.45, ruralRatio=0.55

        ↓
检查: hasUrban=true, hasRural=false, hasBoth=false ❌
使用比例: 1.0 / 0.0（忽略前端参数，100%给城网）
日志: "待投放区域仅包含城网，比例参数无效，100%分配给城网"
警告: "待投放区域不同时包含城网和农网，传入的比例参数无效，已忽略"
```

---

## 📝 五种业务类型对比

| 业务类型 | 策略类 | 算法类 | 客户数表 | 比例参数 | 特殊处理 |
|---------|-------|--------|---------|---------|---------|
| 按档位统一投放 | CityDistributionStrategy | CityCigaretteDistributionAlgorithm | region_clientNum_0_1 | ❌ | 目标固定为"全市" |
| 档位+区县 | CountyDistributionStrategy | countyCigaretteDistributionAlgorithm | region_clientNum_1_1 | ❌ | - |
| 档位+市场类型 | MarketDistributionStrategy | MarketProportionalCigaretteDistributionAlgorithm | region_clientNum_2_1 | ✅ | 支持城网/农网比例 |
| 档位+城乡分类代码 | UrbanRuralDistributionStrategy | UrbanRuralClassificationCodeDistributionAlgorithm | region_clientNum_3_1 | ❌ | - |
| 档位+业态 | BusinessFormatDistributionStrategy | BussinessFormatDistributionAlgorithm | region_clientNum_4_1 | ❌ | - |

---

## 🔧 关键技术组件

### 1. 策略管理器（DistributionStrategyManager）

```java
// 职责：根据投放方法和类型选择对应策略
public DistributionStrategy getStrategy(String deliveryMethod, String deliveryEtype) {
    // 策略映射
    if ("按档位统一投放".equals(deliveryMethod)) {
        return cityDistributionStrategy;
    } else if ("按档位扩展投放".equals(deliveryMethod)) {
        switch (deliveryEtype) {
            case "档位+区县": return countyDistributionStrategy;
            case "档位+市场类型": return marketDistributionStrategy;
            case "档位+城乡分类代码": return urbanRuralDistributionStrategy;
            case "档位+业态": return businessFormatDistributionStrategy;
        }
    }
    throw new IllegalArgumentException("不支持的投放类型");
}
```

### 2. 通用数据服务（CommonService）

```java
// 职责：为所有策略类提供统一的数据访问接口

// 方法1：获取区域列表
List<String> getAllRegionList(String deliveryMethod, String deliveryEtype) {
    // 1. 根据投放类型确定表名
    String tableName = TableNameGeneratorUtil.generateRegionClientTableName(...);
    
    // 2. 查询该表的所有区域
    SELECT DISTINCT region FROM {tableName} WHERE region IS NOT NULL
    
    // 3. 返回区域列表
}

// 方法2：构建客户数矩阵
RegionCustomerMatrix buildRegionCustomerMatrix(String deliveryMethod, String deliveryEtype) {
    // 1. 确定表名
    // 2. 查询所有区域的D30-D1数据
    // 3. 构建矩阵对象返回
}
```

### 3. SQL构建工具（CigaretteDistributionSqlBuilder）

```java
// 职责：统一管理所有SQL语句构建

buildAdvDataQuerySql(tableName, year, month, weekSeq)
    → "SELECT CIG_CODE, CIG_NAME, ADV, ... FROM {tableName}"

buildBatchInsertSql(tableName)
    → "INSERT INTO {tableName} (...) VALUES (...) ON DUPLICATE KEY UPDATE ..."

buildCheckTableExistsSql()
    → "SELECT COUNT(*) FROM information_schema.tables WHERE ..."
```

---

## 📊 性能优化机制

### 1. 缓存机制（@Cacheable）

```java
// 各策略类中的缓存
@Cacheable("regionCustomerMatrix")
private BigDecimal[][] getRegionCustomerMatrix() {
    // 首次查询数据库，后续从缓存读取
}

// 缓存key
- regionCustomerMatrix (城乡分类代码)
- businessFormatCustomerMatrix (业态)
- countyCustomerMatrix (区县)
- marketCustomerMatrix (市场类型)
- cityCustomerMatrix (统一投放)
```

### 2. 批量操作

```java
// 使用JdbcTemplate批量写回
jdbcTemplate.batchUpdate(sql, batchArgs);

// 相比逐条插入，性能提升10-100倍
```

---

## ⏱️ 完整请求时间线

```
T0: 前端发起请求

T0+10ms: Controller接收并验证参数

T0+20ms: 检查是否存在旧数据
    ├─ 如果存在
    │   └─ T0+50ms: 删除旧数据（DELETE操作）
    └─ 如果不存在
        └─ T0+30ms: 继续

T0+100ms: 查询卷烟基础数据
    └─ SELECT * FROM cigarette_distribution_info_2025_10_1
    └─ 假设查询到50个卷烟

T0+150ms: 开始遍历处理（50个卷烟）
    ├─ 卷烟1（档位+城乡分类代码）
    │   ├─ 获取区域列表（缓存命中: 0ms）
    │   ├─ 获取客户数矩阵（缓存命中: 0ms）
    │   ├─ 执行算法计算: 5ms
    │   └─ 计算实际投放量: 1ms
    │
    ├─ 卷烟2（档位+业态）
    │   ├─ 缓存命中: 0ms
    │   ├─ 算法计算: 5ms
    │   └─ 实际投放: 1ms
    │
    └─ ... (其他48个卷烟)
    
    总耗时: 约300ms

T0+450ms: 批量写回数据库
    └─ INSERT INTO cigarette_distribution_prediction_...
    └─ 批量写回50个卷烟×平均4个区域=200条记录
    └─ 耗时: 约100ms

T0+550ms: 构建响应并返回
    └─ 总耗时: 约550ms

前端收到响应: T0+600ms
```

**总结**: 处理50个卷烟，约600ms完成（含网络延迟）

---

## 🎯 数据流关键要点

### 1. 统一架构

所有5个业务类型使用统一的架构：
```
Controller → Service → Strategy → Algorithm → Database
```

### 2. 策略模式

- 根据投放类型自动选择策略
- 各策略类接口统一
- 易于扩展新类型

### 3. 缓存优化

- 客户数矩阵缓存
- 避免重复查询
- 显著提升性能

### 4. 批量操作

- JdbcTemplate批量写回
- 减少数据库交互
- 提升写入性能

---

## 📚 相关文档

- **算法详解**: `docs/档位+城乡分类代码算法说明.md`
- **策略类架构**: 代码中的Strategy包
- **测试报告**: `test docs/档位+城乡分类代码/测试报告_202用例_详细分析.md`
- **测试报告**: `test docs/档位+业态/测试报告_202用例_详细分析.md`

---

## 🎉 总结

本文档详细说明了一键生成分配方案API的完整数据流，包括：

✅ 从前端请求到数据库写回的完整链路  
✅ 5种业务类型的具体数据流  
✅ 档位+市场类型比例参数的特殊处理  
✅ 各层次的职责和交互关系  
✅ 关键技术组件和优化机制  
✅ 完整的时间线分析  

---

**文档版本**: v1.0  
**编写日期**: 2025-10-19

