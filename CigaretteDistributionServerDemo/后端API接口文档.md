# 卷烟分配系统 - 后端API接口文档

## 📋 文档信息

**系统名称**: 卷烟分配算法系统  
**版本**: v1.0  
**基础URL**: `http://localhost:28080`  
**更新日期**: 2025年9月27日  

## 🎯 系统概述

卷烟分配系统是一个基于算法的智能分配平台，支持多种投放类型的卷烟分配策略计算，提供完整的数据管理和Excel导入导出功能。

### 核心功能
- **多种投放类型**: 支持5种不同的投放策略
- **智能算法**: 基于客户数矩阵的最优分配算法
- **数据管理**: 完整的CRUD操作和查询功能
- **Excel导入**: 支持预投放量和客户数表的Excel导入
- **语义编码**: 创新的编码解码功能，实现投放策略的语义化表达

## 🔧 通用说明

### 请求头
```http
Content-Type: application/json
Accept: application/json
```

### 通用响应格式
```json
{
  "success": true|false,
  "message": "操作结果信息",
  "data": {}, // 具体数据
  "total": 0, // 总记录数（查询接口）
  "error": "ERROR_CODE" // 错误码（失败时）
}
```

### HTTP状态码
- `200`: 请求成功
- `400`: 请求参数错误
- `500`: 服务器内部错误

---

## 🏥 1. 系统健康检查

### 1.1 健康检查
**接口**: `GET /api/common/health`  
**描述**: 检查系统运行状态

#### 请求示例
```http
GET /api/common/health
```

#### 响应示例
```json
{
  "status": "UP",
  "message": "卷烟分配服务运行正常",
  "timestamp": 1727427600000
}
```

---

## 📊 2. 数据管理接口

### 2.1 查询卷烟分配数据 ⭐ **新增编码功能**
**接口**: `POST /api/data/query`  
**描述**: 根据日期查询卷烟分配数据，包含编码解码表达

#### 请求参数
```json
{
  "year": 2025,
  "month": 9,
  "weekSeq": 1
}
```

#### 响应示例
```json
{
  "success": true,
  "message": "查询成功",
  "data": [
    {
      "id": 1,
      "cigCode": "42010114",
      "cigName": "红金龙（硬爱你爆珠）",
      "deliveryArea": "丹江",
      "year": 2025,
      "month": 9,
      "weekSeq": 1,
      "deliveryMethod": "按档位扩展投放", // 🔄 现从demo_test_data表获取
      "deliveryEtype": "档位+区县", // 🔄 现从demo_test_data表获取
      "advAmount": 1000.00,
      "actualDelivery": 980.50,
      "encodedExpression": "B1（2+3+4+5）（5×9+5×8+10×7+10×6）", // 🆕 编码表达
      "decodedExpression": "按档位扩展投放、档位+区县、丹江房县郧西郧阳、（5×9+5×8+10×7+10×6）", // 🆕 解码表达
      "d30": 9,
      "d29": 9,
      "d28": 9,
      "d27": 9,
      "d26": 9,
      // ... 其他档位数据 d25-d1
      "remark": "算法自动生成"
    }
  ],
  "total": 150
}
```

#### 编码解码字段说明
- **encodedExpression**: 投放策略的编码化表达
  - 格式: `投放类型+扩展类型（区域编码）（档位投放量编码）`
  - 示例: `B1（2+3+4）（5×9+5×8）`
- **decodedExpression**: 编码的自然语言解码
  - 格式: `投放类型、扩展类型、区域名称、投放量编码`
  - 示例: `按档位扩展投放、档位+区县、丹江房县郧西、（5×9+5×8）`

### 2.2 更新卷烟信息
**接口**: `POST /api/data/update-cigarette`  
**描述**: 更新指定卷烟的投放信息

#### 请求参数
```json
{
  "cigCode": "42010114",
  "cigName": "红金龙（硬爱你爆珠）",
  "year": 2025,
  "month": 9,
  "weekSeq": 1,
  "deliveryMethod": "按档位扩展投放", // 🔄 投放方法
  "deliveryEtype": "档位+区县", // 🔄 扩展投放类型
  "deliveryArea": "丹江",
  "distribution": [10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 0, ...], // 30个档位的分配值
  "remark": "手动调整"
}
```

#### 响应示例
```json
{
  "success": true,
  "message": "卷烟信息更新成功",
  "updatedCount": 2
}
```

### 2.3 删除投放区域
**接口**: `POST /api/data/delete-delivery-areas`  
**描述**: 删除指定卷烟的部分投放区域记录

#### 请求参数
```json
{
  "cigCode": "001",
  "cigName": "测试卷烟A",
  "year": 2025,
  "month": 9,
  "weekSeq": 1,
  "areasToDelete": ["竹山", "竹溪"]
}
```

#### 响应示例
```json
{
  "success": true,
  "message": "删除成功",
  "deletedCount": 2,
  "deletedAreas": ["竹山", "竹溪"],
  "remainingAreas": ["丹江", "房县", "郧西"],
  "remainingCount": 3
}
```

---

## ⚡ 3. 分配计算接口

### 3.1 一键生成分配方案 ⭐ **核心功能**
**接口**: `POST /api/calculate/generate-distribution-plan`  
**描述**: 删除指定日期的历史数据，重新执行算法生成完整分配方案

#### 请求参数
```json
{
  "year": 2025,
  "month": 9,
  "weekSeq": 1
}
```

#### 响应示例
```json
{
  "success": true,
  "message": "一键分配方案生成成功",
  "operation": "一键生成分配方案",
  "year": 2025,
  "month": 9,
  "weekSeq": 1,
  "startTime": 1727427600000,
  "endTime": 1727427650000,
  "processingTime": "50000ms",
  "deletedExistingData": true,
  "deletedRecords": 120,
  "totalCigarettes": 25,
  "successfulAllocations": 24,
  "processedCount": 480,
  "allocationResult": {
    "success": true,
    "totalCount": 25,
    "successCount": 24,
    "results": [
      {
        "cigCode": "001",
        "cigName": "测试卷烟A",
        "targetType": "区县分配",
        "algorithm": "countyCigaretteDistributionAlgorithm",
        "writeBackStatus": "成功"
      }
      // ... 其他卷烟结果
    ]
  }
}
```

### 3.2 算法分配矩阵写回
**接口**: `POST /api/calculate/write-back`  
**描述**: 执行各投放类型算法并写回分配结果

#### 请求参数
```http
POST /api/calculate/write-back?year=2025&month=9&weekSeq=1
```

#### 响应示例
```json
{
  "success": true,
  "message": "分配矩阵写回完成，成功: 24/25",
  "totalCount": 25,
  "successCount": 24,
  "results": [
    {
      "cigCode": "001",
      "cigName": "测试卷烟A",
      "deliveryEtype": "档位+区县",
      "targetType": "区县分配",
      "algorithm": "countyCigaretteDistributionAlgorithm",
      "writeBackStatus": "成功",
      "writeBackMessage": "分配矩阵已成功写回数据库"
    }
  ]
}
```

### 3.3 计算总实际投放量
**接口**: `POST /api/calculate/total-actual-delivery`  
**描述**: 计算指定时间范围内所有卷烟的总实际投放量

#### 请求参数
```http
POST /api/calculate/total-actual-delivery?year=2025&month=9&weekSeq=1
```

#### 响应示例
```json
{
  "success": true,
  "message": "总实际投放量计算成功",
  "data": {
    "001_测试卷烟A": 980.50,
    "002_测试卷烟B": 1205.75,
    "003_测试卷烟C": 875.25
  },
  "year": 2025,
  "month": 9,
  "weekSeq": 1,
  "totalRecords": 150,
  "cigaretteCount": 3
}
```

---

## 🔄 3.5 投放类型字段重构说明 ⭐ **重要变更**

### 背景
为了提高数据一致性和简化前端操作，投放类型信息 (`deliveryMethod` 和 `deliveryEtype`) 的存储和管理策略已更新。

### 主要变更
#### 1. **数据存储变更**
- **之前**: 投放类型仅存储在 `demo_test_ADVdata` 表中
- **现在**: 投放类型同时存储在 `demo_test_data` 表中，实现数据冗余保证一致性

#### 2. **算法写回增强**
- `DistributionCalculateService` 写回分配矩阵时，自动将 `demo_test_ADVdata` 中的投放类型信息同步到 `demo_test_data` 表
- 确保每条分配记录都包含完整的投放类型信息

#### 3. **前端更新操作简化**
- **之前**: 更新投放类型需要修改 `demo_test_ADVdata` 表
- **现在**: 直接修改 `demo_test_data` 表中的投放类型字段

### 数据字段说明
```json
{
  // demo_test_data 表新增字段
  "deliveryMethod": "按档位扩展投放", // 投放方法
  "deliveryEtype": "档位+区县"        // 扩展投放类型
}
```

### API行为变化
#### 查询操作 (`/api/data/query`)
- ✅ `deliveryMethod` 和 `deliveryEtype` 现在从 `demo_test_data` 表读取
- ✅ 保证数据的一致性和完整性

#### 更新操作 (`/api/data/update-cigarette`)
- ✅ 支持直接更新投放类型字段
- ✅ 自动处理数据同步和一致性维护

### 兼容性
- ✅ 完全向后兼容，现有API接口无需修改
- ✅ 前端代码无需调整，响应格式保持一致
- ✅ 自动数据迁移和同步机制

---

## 📥 4. Excel导入接口

### 4.1 导入卷烟投放基础信息
**接口**: `POST /api/import/cigarette-info`  
**描述**: 导入Excel格式的卷烟投放基础信息数据

#### 请求参数
- **Content-Type**: `multipart/form-data`
- **文件字段**: `file`
- **其他参数**: `year`, `month`, `weekSeq`

#### 请求示例
```http
POST /api/import/cigarette-info
Content-Type: multipart/form-data

file: [Excel文件]
year: 2025
month: 9
weekSeq: 1
```

#### 响应示例
```json
{
  "success": true,
  "message": "导入成功",
  "tableName": "cigarette_distribution_info_2025_9_1",
  "insertedCount": 25,
  "operation": "卷烟投放基础信息导入",
  "fileSize": "2.5MB",
  "processingTime": "3.2秒"
}
```

### 4.2 导入区域客户数表
**接口**: `POST /api/import/region-clientnum`  
**描述**: 导入Excel格式的区域客户数矩阵数据

#### 请求参数
```http
POST /api/import/region-clientnum
Content-Type: multipart/form-data

file: [Excel文件]
year: 2025
month: 9
deliveryMethod: "按档位扩展投放"
deliveryEtype: "档位+区县"
isBiWeeklyFloat: false
```

#### 响应示例
```json
{
  "success": true,
  "message": "导入成功",
  "tableName": "region_clientNum_1_1",
  "mainSequenceNumber": 1,
  "subSequenceNumber": 1,
  "insertedCount": 7,
  "operation": "区域客户数表导入",
  "supportsBiWeeklyFloat": true,
  "fileSize": "1.8MB"
}
```

#### 表命名规则说明
- **卷烟投放信息表**: `cigarette_distribution_info_{year}_{month}_{weekSeq}`
- **区域客户数表**: `region_clientNum_{主序号}_{子序号}`
  - 主序号: 0(统一投放), 1(区县), 2(市场), 3(城乡), 4(业态)
  - 子序号: 1(非双周上浮), 2(双周上浮)

---

## 🔍 5. 投放类型说明

### 支持的投放类型
| 投放方式 | 扩展投放类型 | 编码 | 典型应用场景 |
|---------|-------------|------|-------------|
| 按档位统一投放 | - | A | 全市统一策略 |
| 按档位扩展投放 | 档位+区县 | B1 | 按区县差异化投放 |
| 按档位扩展投放 | 档位+市场类型 | B2 | 城网/农网区分 |
| 按档位扩展投放 | 档位+城乡分类代码 | B4 | 城乡差异化投放 |
| 按档位扩展投放 | 档位+业态 | B5 | 按经营业态投放 |

### 档位说明
- **档位范围**: D30(最高档) 到 D1(最低档)，共30个档位
- **约束条件**: 每个区域的档位分配值必须非递增 (D30 ≥ D29 ≥ ... ≥ D1)
- **计算公式**: 实际投放量 = Σ(档位分配值 × 该档位客户数)

---

## 🎨 6. 编码解码功能详解 ⭐ **新功能**

### 6.1 编码规则
#### 四步编码结构
1. **投放类型编码**: A(统一) / B(扩展) / C(按需)
2. **扩展类型编码**: 1(区县) / 2(市场) / 3(区县+市场) / 4(城乡) / 5(业态)
3. **区域编码**: 根据投放类型映射具体区域
4. **档位投放量编码**: 连续档位压缩 (如: 5×9 表示5个连续档位各投放9)

#### 区域编码映射表
**档位+区县**:
- 城区(1), 丹江(2), 房县(3), 郧西(4), 郧阳(5), 竹山(6), 竹溪(7)

**档位+市场类型**:
- 城网(C), 农网(N)

**档位+城乡分类代码**:
- 主城区(①), 城乡结合区(②), 镇中心区(③), 镇乡接合区(④), 特殊区域(⑤), 乡中心区(⑥), 村庄(⑦)

**档位+业态**:
- 便利店(a), 超市(b), 商场(c), 烟草专卖店(d), 娱乐服务类(e), 其他(f)

### 6.2 编码示例
#### 示例1: 区县投放
- **原始数据**: 档位+区县投放，涉及丹江、房县、郧西、郧阳，D30-D26各投放9，D25-D21各投放8，D20-D11各投放7，D10-D1各投放6
- **编码结果**: `B1（2+3+4+5）（5×9+5×8+10×7+10×6）`
- **解码结果**: `按档位扩展投放、档位+区县、丹江房县郧西郧阳、（5×9+5×8+10×7+10×6）`

#### 示例2: 市场类型投放
- **编码结果**: `B2（C+N）（3×10+3×9+3×8）`
- **解码结果**: `按档位扩展投放、档位+市场类型、城网农网、（3×10+3×9+3×8）`

#### 示例3: 全市统一投放
- **编码结果**: `A（1×10+2×9+3×8）`
- **解码结果**: `按档位统一投放、（1×10+2×9+3×8）`

---

## ⚠️ 7. 错误处理

### 常见错误码
| 错误码 | 说明 | 解决方案 |
|-------|------|---------|
| `FILE_EMPTY` | 上传文件为空 | 请选择有效的Excel文件 |
| `FILE_TOO_LARGE` | 文件过大(>10MB) | 压缩文件或分批上传 |
| `INVALID_DELIVERY_TYPE` | 投放类型无效 | 检查投放方法和扩展投放类型组合 |
| `RECORD_NOT_FOUND` | 未找到指定记录 | 检查查询条件和日期参数 |
| `CANNOT_DELETE_ALL_AREAS` | 不能删除所有投放区域 | 至少保留一个投放区域 |
| `ALLOCATION_FAILED` | 算法分配失败 | 检查预投放量数据和客户数矩阵 |
| `IMPORT_FAILED` | Excel导入失败 | 检查文件格式和数据完整性 |

### 错误响应格式
```json
{
  "success": false,
  "message": "具体错误信息",
  "error": "ERROR_CODE",
  "details": "详细错误描述"
}
```

---

## 🚀 8. 使用流程建议

### 8.1 标准使用流程
1. **数据准备阶段**
   - 导入卷烟投放基础信息 (`/api/import/cigarette-info`)
   - 导入区域客户数矩阵 (`/api/import/region-clientnum`)

2. **算法计算阶段**
   - 执行一键生成分配方案 (`/api/calculate/generate-distribution-plan`)
   - 或者手动执行算法写回 (`/api/calculate/write-back`)

3. **数据查询阶段**
   - 查询分配结果 (`/api/data/query`)
   - 查看编码解码表达，理解投放策略

4. **数据调整阶段**（可选）
   - 更新特定卷烟信息 (`/api/data/update-cigarette`)
   - 删除不需要的投放区域 (`/api/data/delete-delivery-areas`)

### 8.2 前端开发建议

#### 数据展示
- 利用 `encodedExpression` 字段快速识别投放模式
- 使用 `decodedExpression` 字段为用户提供易懂的策略描述
- 档位数据建议用表格或图表形式展示

#### 用户体验优化
- 一键生成功能添加进度提示（根据 `processingTime` 估算）
- Excel导入提供文件格式验证和错误提示
- 大量数据查询建议分页处理

#### 错误处理
- 统一错误提示组件
- 根据 `error` 字段显示具体的错误帮助信息
- 网络超时和重试机制

---

## 📞 9. 技术支持

### 开发环境
- **Java版本**: JDK 8+
- **Spring Boot**: 2.7.18
- **数据库**: MySQL 8.0
- **服务端口**: 28080

### 联系方式
- **API问题**: 请提供完整的请求参数和响应信息
- **算法问题**: 请说明具体的投放类型和数据特征
- **Excel导入问题**: 请提供Excel文件样本和错误日志

---

**文档版本**: v1.0  
**最后更新**: 2025年9月27日  
**维护状态**: ✅ 持续维护
