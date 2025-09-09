# 卷烟分配系统后端接口文档

## 服务器配置
- **服务器地址**: http://localhost:28080
- **数据库**: MySQL (marketing)

## 接口概览

### 1. 卷烟分配控制器 (CigaretteDistributionController)
**基础路径**: `/api/cigarette`

### 2. 优化算法测试控制器 (OptimizedAlgorithmTestController)
**基础路径**: `/api/optimized-algorithm`

---

## 详细接口说明

### 1. 查询卷烟分配数据
**接口地址**: `POST /api/cigarette/query`  
**功能描述**: 查询指定年月周的卷烟分配数据，返回原始数据并添加预投放量和实际投放量

#### 请求参数 (JSON)
```json
{
    "year": 2024,
    "month": 1,
    "weekSeq": 1
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| year | Integer | 是 | 年份 |
| month | Integer | 是 | 月份 |
| weekSeq | Integer | 是 | 周序号 |

#### 响应数据示例
```json
{
    "success": true,
    "message": "查询成功",
    "total": 10,
    "data": [
        {
            "id": 1,
            "cigCode": "42010114",
            "cigName": "红金龙(硬爱你爆珠)",
            "deliveryArea": "410101,410102",
            "year": 2024,
            "month": 1,
            "weekSeq": 1,
            "remark": "备注信息",
            "advAmount": 1000.00,
            "actualDelivery": 980.50,
            "deliveryMethod": "投放方式",
            "deliveryEtype": "扩展投放方式",
            "d30": 50.00,
            "d29": 45.00,
            "d28": 40.00,
            "d27": 35.00,
            "d26": 30.00,
            "d25": 25.00,
            "d24": 20.00,
            "d23": 18.00,
            "d22": 16.00,
            "d21": 14.00,
            "d20": 12.00,
            "d19": 10.00,
            "d18": 9.00,
            "d17": 8.00,
            "d16": 7.00,
            "d15": 6.00,
            "d14": 5.50,
            "d13": 5.00,
            "d12": 4.50,
            "d11": 4.00,
            "d10": 3.50,
            "d9": 3.00,
            "d8": 2.50,
            "d7": 2.00,
            "d6": 1.80,
            "d5": 1.60,
            "d4": 1.40,
            "d3": 1.20,
            "d2": 1.00,
            "d1": 0.80
        }
    ]
}
```

---

### 2. 查询原始数据
**接口地址**: `POST /api/cigarette/query-raw`  
**功能描述**: 查询原始数据，不进行合并处理

#### 请求参数
同查询卷烟分配数据接口

#### 响应数据
返回原始数据记录，不包含预投放量和实际投放量字段

---

### 3. 更新卷烟分配数据
**接口地址**: `POST /api/cigarette/update`  
**功能描述**: 更新卷烟分配数据，将复合区域记录拆分后写回数据库

#### 请求参数 (JSON)
```json
{
    "cigCode": "42010114",
    "cigName": "红金龙(硬爱你爆珠)",
    "distribution": [50.00, 45.00, 40.00, 35.00, 30.00, 25.00, 20.00, 18.00, 16.00, 14.00, 12.00, 10.00, 9.00, 8.00, 7.00, 6.00, 5.50, 5.00, 4.50, 4.00, 3.50, 3.00, 2.50, 2.00, 1.80, 1.60, 1.40, 1.20, 1.00, 0.80],
    "deliveryAreas": "410101,410102,410103",
    "year": 2024,
    "month": 1,
    "weekSeq": 1
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| cigCode | String | 是 | 卷烟代码 |
| cigName | String | 是 | 卷烟名称 |
| distribution | List&lt;BigDecimal&gt; | 是 | D30到D1的分配值数组 |
| deliveryAreas | String | 是 | 投放区域，多个区域用逗号分隔 |
| year | Integer | 是 | 年份 |
| month | Integer | 是 | 月份 |
| weekSeq | Integer | 是 | 周序号 |

#### 响应数据示例
```json
{
    "success": true,
    "message": "更新成功"
}
```

---

### 4. 更新卷烟信息
**接口地址**: `POST /api/cigarette/update-cigarette`  
**功能描述**: 更新卷烟的投放方式、扩展投放方式、投放区域、档位分配和备注

#### 请求参数 (JSON)
```json
{
    "cigCode": "42010114",
    "cigName": "红金龙(硬爱你爆珠)",
    "year": 2024,
    "month": 1,
    "weekSeq": 1,
    "deliveryMethod": "投放方式",
    "deliveryEtype": "扩展投放方式",
    "deliveryArea": "410101,410102",
    "distribution": [50.00, 45.00, 40.00, 35.00, 30.00, 25.00, 20.00, 18.00, 16.00, 14.00, 12.00, 10.00, 9.00, 8.00, 7.00, 6.00, 5.50, 5.00, 4.50, 4.00, 3.50, 3.00, 2.50, 2.00, 1.80, 1.60, 1.40, 1.20, 1.00, 0.80],
    "remark": "备注信息"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| cigCode | String | 是 | 卷烟代码 |
| cigName | String | 是 | 卷烟名称 |
| year | Integer | 是 | 年份 |
| month | Integer | 是 | 月份 |
| weekSeq | Integer | 是 | 周序号 |
| deliveryMethod | String | 是 | 投放方式 |
| deliveryEtype | String | 是 | 扩展投放方式 |
| deliveryArea | String | 是 | 投放区域 |
| distribution | List&lt;BigDecimal&gt; | 是 | D30到D1的分配值数组 |
| remark | String | 否 | 备注信息 |

#### 响应数据示例
```json
{
    "success": true,
    "message": "更新成功",
    "updatedRecords": 3
}
```

---

### 5. 写回分配矩阵
**接口地址**: `POST /api/cigarette/write-back`  
**功能描述**: 将算法输出的分配矩阵写回数据库

#### 请求参数 (Query Parameters)
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| year | Integer | 是 | 年份 |
| month | Integer | 是 | 月份 |
| weekSeq | Integer | 是 | 周序号 |

#### 响应数据示例
```json
{
    "success": true,
    "message": "分配矩阵写回成功",
    "totalCount": 50,
    "successCount": 48,
    "failCount": 2
}
```

---

### 6. 删除投放区域
**接口地址**: `POST /api/cigarette/delete-delivery-areas`  
**功能描述**: 删除指定卷烟在特定时间的特定投放区域记录

#### 请求参数 (JSON)
```json
{
    "cigCode": "42010114",
    "cigName": "红金龙(硬爱你爆珠)",
    "year": 2024,
    "month": 1,
    "weekSeq": 1,
    "areasToDelete": ["乡中心区", "村庄"]
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| cigCode | String | 是 | 卷烟代码，用于精确识别卷烟品种 |
| cigName | String | 是 | 卷烟名称，用于日志记录和验证 |
| year | Integer | 是 | 年份，用于定位时间范围 |
| month | Integer | 是 | 月份，用于定位时间范围 |
| weekSeq | Integer | 是 | 周序号，用于定位具体时间 |
| areasToDelete | List&lt;String&gt; | 是 | 要删除的投放区域列表 |

#### 响应数据示例

##### 成功响应
```json
{
    "success": true,
    "message": "删除成功",
    "deletedCount": 2,
    "deletedAreas": ["乡中心区", "村庄"],
    "remainingAreas": ["镇中心区", "镇乡结合区"],
    "remainingCount": 2
}
```

##### 失败响应示例
```json
{
    "success": false,
    "message": "删除失败：不能删除所有投放区域，至少需要保留一个",
    "error": "CANNOT_DELETE_ALL_AREAS",
    "currentAreas": ["乡中心区"],
    "areasToDelete": ["乡中心区"]
}
```

#### 响应字段说明
| 字段名 | 类型 | 说明 |
|--------|------|------|
| success | Boolean | 操作是否成功 |
| message | String | 操作结果消息 |
| deletedCount | Integer | 成功删除的记录数量 |
| deletedAreas | List&lt;String&gt; | 已删除的投放区域列表 |
| remainingAreas | List&lt;String&gt; | 剩余的投放区域列表 |
| remainingCount | Integer | 剩余记录数量 |
| error | String | 错误代码（失败时） |
| currentAreas | List&lt;String&gt; | 当前存在的投放区域（失败时） |

#### 错误码说明
- `INVALID_PARAMETERS` - 请求参数无效
- `CANNOT_DELETE_ALL_AREAS` - 不能删除所有投放区域
- `RECORD_NOT_FOUND` - 找不到要删除的记录
- `DELETE_FAILED` - 删除操作失败
- `INTERNAL_ERROR` - 服务器内部错误

---

### 7. 导入卷烟投放基础信息Excel
**接口地址**: `POST /api/cigarette/import-cigarette-info`  
**功能描述**: 导入卷烟投放基础信息Excel文件，自动创建对应的数据表

#### 请求参数 (Form Data)
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | MultipartFile | 是 | Excel文件（.xlsx或.xls格式） |
| year | Integer | 是 | 年份（2020-2099） |
| month | Integer | 是 | 月份（1-12） |
| weekSeq | Integer | 是 | 周序号（1-5） |

#### Excel文件要求
- **文件格式**: .xlsx或.xls
- **文件大小**: 最大10MB
- **数据结构**: 必须与demo_test_ADVdata表结构一致
- **必需列名**: CIG_CODE, CIG_NAME, DELIVERY_AREA, DELIVERY_METHOD, DELIVERY_ETYPE, URS, ADV

#### 生成表名规则
`cigarette_distribution_info_年_月_周序号`

例如：`cigarette_distribution_info_2024_1_1`

#### 响应数据示例

##### 成功响应
```json
{
    "success": true,
    "message": "导入成功",
    "tableName": "cigarette_distribution_info_2024_1_1",
    "insertedCount": 150,
    "totalRows": 150
}
```

##### 失败响应示例
```json
{
    "success": false,
    "message": "Excel文件结构不符合要求，请检查列名是否与demo_test_ADVdata表一致",
    "error": "STRUCTURE_MISMATCH"
}
```

---

### 8. 导入区域客户数表Excel
**接口地址**: `POST /api/cigarette/import-region-clientnum`  
**功能描述**: 导入区域客户数表Excel文件，根据投放类型映射到对应的数据表并覆盖原有数据

#### 请求参数 (Form Data)
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | MultipartFile | 是 | Excel文件（.xlsx或.xls格式） |
| year | Integer | 是 | 年份（2020-2099） |
| month | Integer | 是 | 月份（1-12） |
| deliveryMethod | String | 是 | 投放类型 |
| deliveryEtype | String | 是 | 扩展投放类型 |

#### 投放类型映射规则
| 投放类型 | 扩展投放类型 | 序号 | 说明 |
|----------|--------------|------|------|
| 按档位统一投放 | - | 0 | 统一投放模式 |
| 按档位扩展投放 | 档位+区县 | 1 | 按区县扩展 |
| 按档位扩展投放 | 档位+市场类型 | 2 | 按市场类型扩展 |
| 按档位扩展投放 | 档位+城乡分类代码 | 3 | 按城乡分类扩展 |
| 按档位扩展投放 | 档位+业态 | 4 | 按业态扩展 |

#### Excel文件要求
- **文件格式**: .xlsx或.xls
- **文件大小**: 最大10MB
- **数据结构**: 必须与demo_test_clientNumdata表结构一致
- **必需列名**: URBAN_RURAL_CODE, D30-D1（30个档位列）, TOTAL

#### 表名映射规则
`region_clientNum_序号`

例如：`region_clientNum_0`（按档位统一投放）

**注意**: 
- 如果对应的表不存在，系统会自动创建
- 如果表已存在，将清空原有数据后插入新数据（覆盖模式）
- 每次导入都会完全替换表中的所有数据

#### 响应数据示例

##### 成功响应
```json
{
    "success": true,
    "message": "导入成功",
    "tableName": "region_clientNum_3",
    "insertedCount": 50,
    "totalRows": 50,
    "sequenceNumber": 3,
    "deliveryMethod": "按档位扩展投放",
    "deliveryEtype": "档位+城乡分类代码"
}
```

##### 失败响应示例
```json
{
    "success": false,
    "message": "投放类型和扩展投放类型组合无效",
    "error": "INVALID_DELIVERY_TYPE",
    "deliveryMethod": "无效投放类型",
    "deliveryEtype": "无效扩展类型"
}
```

#### 错误码说明
- `FILE_EMPTY` - 文件为空
- `FILE_TOO_LARGE` - 文件超过大小限制
- `STRUCTURE_MISMATCH` - Excel结构不匹配
- `INVALID_DELIVERY_TYPE` - 投放类型组合无效
- `IMPORT_FAILED` - 导入失败

---

### 9. 健康检查
**接口地址**: `GET /api/cigarette/health`  
**功能描述**: 检查服务运行状态

#### 响应数据示例
```json
{
    "status": "UP",
    "message": "卷烟分配服务运行正常",
    "timestamp": 1704067200000
}
```

---

### 7. 测试分配算法
**接口地址**: `GET /api/cigarette/test-algorithm`  
**功能描述**: 测试卷烟分配算法的运行情况

#### 响应数据示例
```json
{
    "success": true,
    "message": "算法测试完成",
    "results": [
        {
            "cigCode": "42010114",
            "cigName": "红金龙(硬爱你爆珠)",
            "adv": 1000.00,
            "deliveryArea": "410101,410102",
            "targetRegions": ["410101", "410102"],
            "actualAmount": 980.50,
            "error": 19.50,
            "constraintValid": true
        }
    ]
}
```

---

### 8. 测试优化算法
**接口地址**: `GET /api/optimized-algorithm/test-optimized-algorithm`  
**功能描述**: 测试优化后的卷烟分配算法

#### 响应数据示例
```json
{
    "success": true,
    "message": "优化算法测试完成",
    "advDataCount": 10,
    "testDataCount": 50,
    "clientDataCount": 20,
    "regions": ["410101", "410102", "410103"],
    "algorithmResults": [
        {
            "cigCode": "42010114",
            "cigName": "红金龙(硬爱你爆珠)",
            "adv": 1000.00,
            "deliveryMethod": "投放方式",
            "deliveryEtype": "扩展投放方式",
            "deliveryArea": "410101,410102",
            "matchedRegions": ["410101", "410102"],
            "actualAmount": 980.50,
            "error": 19.50,
            "errorPercentage": 1.95,
            "constraintValid": true,
            "allocationMatrix": {
                "region_0": {
                    "region": "410101",
                    "d30": 25.00,
                    "d29": 22.50,
                    "d28": 20.00,
                    "d27": 17.50,
                    "d26": 15.00,
                    "d25": 12.50,
                    "d24": 10.00,
                    "d23": 9.00,
                    "d22": 8.00,
                    "d21": 7.00,
                    "d20": 6.00,
                    "d19": 5.00,
                    "d18": 4.50,
                    "d17": 4.00,
                    "d16": 3.50,
                    "d15": 3.00,
                    "d14": 2.75,
                    "d13": 2.50,
                    "d12": 2.25,
                    "d11": 2.00,
                    "d10": 1.75,
                    "d9": 1.50,
                    "d8": 1.25,
                    "d7": 1.00,
                    "d6": 0.90,
                    "d5": 0.80,
                    "d4": 0.70,
                    "d3": 0.60,
                    "d2": 0.50,
                    "d1": 0.40
                }
            },
            "constraintDetails": {
                "violationCount": 0,
                "violations": [],
                "isValid": true
            }
        }
    ]
}
```

---

### 9. 调试接口：测试实际投放量计算
**接口地址**: `GET /api/cigarette/debug-actual-amount`  
**功能描述**: 调试实际投放量计算功能

#### 响应数据示例
```json
{
    "cigCode": "42010114",
    "cigName": "红金龙(硬爱你爆珠)",
    "actualAmount": 980.50,
    "success": true
}
```

---

### 10. 测试ADV数据表结构
**接口地址**: `GET /api/cigarette/test-advdata-structure`  
**功能描述**: 检查demo_test_advdata表结构

#### 响应数据示例
```json
{
    "success": true,
    "message": "查询成功",
    "data": [
        {
            "id": 1,
            "cig_code": "42010114",
            "cig_name": "红金龙(硬爱你爆珠)",
            "delivery_area": "410101,410102",
            "delivery_method": "投放方式",
            "delivery_etype": "扩展投放方式",
            "urs": 100.00,
            "adv": 1000.00
        }
    ],
    "columns": ["id", "cig_code", "cig_name", "delivery_area", "delivery_method", "delivery_etype", "urs", "adv"]
}
```

---

### 11. 测试客户数量数据
**接口地址**: `GET /api/cigarette/test-client-numdata-direct`  
**功能描述**: 直接查询demo_test_clientNumdata表数据

#### 响应数据示例
```json
{
    "success": true,
    "message": "查询成功",
    "rowCount": 5,
    "data": [
        {
            "id": 1,
            "urban_rural_code": "410101",
            "d30": 100,
            "d29": 95,
            "d28": 90,
            "d27": 85,
            "d26": 80,
            "d25": 75,
            "d24": 70,
            "d23": 65,
            "d22": 60,
            "d21": 55,
            "d20": 50,
            "d19": 45,
            "d18": 40,
            "d17": 35,
            "d16": 30,
            "d15": 25,
            "d14": 22,
            "d13": 20,
            "d12": 18,
            "d11": 16,
            "d10": 14,
            "d9": 12,
            "d8": 10,
            "d7": 8,
            "d6": 7,
            "d5": 6,
            "d4": 5,
            "d3": 4,
            "d2": 3,
            "d1": 2,
            "total": 1000
        }
    ],
    "columns": ["id", "urban_rural_code", "d30", "d29", "d28", "d27", "d26", "d25", "d24", "d23", "d22", "d21", "d20", "d19", "d18", "d17", "d16", "d15", "d14", "d13", "d12", "d11", "d10", "d9", "d8", "d7", "d6", "d5", "d4", "d3", "d2", "d1", "total"]
}
```

---

### 12. 测试区域客户数矩阵
**接口地址**: `GET /api/cigarette/test-region-customer-matrix`  
**功能描述**: 查看区域客户数矩阵的输出

#### 响应数据示例
```json
{
    "success": true,
    "message": "查询成功",
    "matrixSize": "20x30",
    "regionCount": 20,
    "regions": ["410101", "410102", "410103"],
    "matrixData": [
        {
            "regionIndex": 0,
            "regionName": "410101",
            "gradeData": {
                "D30": 100,
                "D29": 95,
                "D28": 90,
                "D27": 85,
                "D26": 80,
                "D25": 75,
                "D24": 70,
                "D23": 65,
                "D22": 60,
                "D21": 55,
                "D20": 50,
                "D19": 45,
                "D18": 40,
                "D17": 35,
                "D16": 30,
                "D15": 25,
                "D14": 22,
                "D13": 20,
                "D12": 18,
                "D11": 16,
                "D10": 14,
                "D9": 12,
                "D8": 10,
                "D7": 8,
                "D6": 7,
                "D5": 6,
                "D4": 5,
                "D3": 4,
                "D2": 3,
                "D1": 2
            }
        }
    ]
}
```

---

### 13. 测试KMP匹配功能
**接口地址**: `GET /api/cigarette/test-kmp-matching`  
**功能描述**: 测试KMP字符串匹配功能，用于投放区域匹配

#### 响应数据示例
```json
{
    "success": true,
    "message": "KMP匹配测试完成",
    "allRegions": ["410101", "410102", "410103"],
    "testResults": [
        {
            "cigCode": "42010114",
            "cigName": "红金龙(硬爱你爆珠)",
            "deliveryArea": "410101,410102",
            "matchedRegions": ["410101", "410102"],
            "matchCount": 2
        }
    ]
}
```

---

## 错误响应格式

当接口调用失败时，返回统一的错误格式：

```json
{
    "success": false,
    "message": "错误描述信息",
    "error": "详细错误信息（可选）"
}
```

## 数据字典

### 档位说明
- **D30-D1**: 表示30个不同的客户档位，D30为最高档位，D1为最低档位
- **分配值**: 每个档位对应的分配数量，必须满足非递增约束（D30 >= D29 >= ... >= D1）

### 投放区域编码
- **格式**: 使用城乡代码，多个区域用逗号分隔
- **示例**: "410101,410102,410103"

### 时间参数
- **年份**: 4位数字，如2024
- **月份**: 1-12
- **周序号**: 1-5（一个月最多5周）

## 使用示例

### 查询数据示例
```bash
curl -X POST http://localhost:28080/api/cigarette/query \
  -H "Content-Type: application/json" \
  -d '{
    "year": 2024,
    "month": 1,
    "weekSeq": 1
  }'
```

### 更新数据示例
```bash
curl -X POST http://localhost:28080/api/cigarette/update-cigarette \
  -H "Content-Type: application/json" \
  -d '{
    "cigCode": "42010114",
    "cigName": "红金龙(硬爱你爆珠)",
    "year": 2024,
    "month": 1,
    "weekSeq": 1,
    "deliveryMethod": "投放方式",
    "deliveryEtype": "扩展投放方式",
    "deliveryArea": "410101,410102",
    "distribution": [50.00, 45.00, 40.00, 35.00, 30.00, 25.00, 20.00, 18.00, 16.00, 14.00, 12.00, 10.00, 9.00, 8.00, 7.00, 6.00, 5.50, 5.00, 4.50, 4.00, 3.50, 3.00, 2.50, 2.00, 1.80, 1.60, 1.40, 1.20, 1.00, 0.80],
    "remark": "备注信息"
  }'
```

### Excel导入示例
```bash
# 导入卷烟投放基础信息
curl -X POST http://localhost:28080/api/cigarette/import-cigarette-info \
  -F "file=@cigarette_info.xlsx" \
  -F "year=2024" \
  -F "month=1" \
  -F "weekSeq=1"

# 导入区域客户数表
curl -X POST http://localhost:28080/api/cigarette/import-region-clientnum \
  -F "file=@region_clientnum.xlsx" \
  -F "year=2024" \
  -F "month=1" \
  -F "deliveryMethod=按档位扩展投放" \
  -F "deliveryEtype=档位+城乡分类代码"
```
