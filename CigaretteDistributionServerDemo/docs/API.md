## 后端 API 说明文档

本文档说明卷烟分配后端的 HTTP 接口。所有接口默认返回 JSON，跨域已开启（`@CrossOrigin(origins = "*")`）。

### 通用
- **Base URL**: `/api`
- **鉴权**: 无（Demo 环境）
- **时间字段**: `year`、`month`、`weekSeq` 用于隔离不同期次数据

---

## 一、健康检查 CommonController

### GET `/api/common/health`
- **说明**: 服务健康检查
- **请求参数**: 无
- **响应示例**
```json
{
  "status": "UP",
  "message": "卷烟分配服务运行正常",
  "timestamp": 1710000000000
}
```

---

## 二、计算流程 DistributionCalculateController

### POST `/api/calculate/write-back`
- **说明**: 读取 info 表、按投放策略计算并写入 prediction 表
- **请求参数（query/form）**
  - `year` (int, required)
  - `month` (int, required)
  - `weekSeq` (int, required)
- **成功响应字段（节选）**
  - `success` (bool)
  - `successCount` (int) 成功分配的卷烟种数
  - `totalCount` (int) 总卷烟种数
  - `results` (array) 每种卷烟的计算明细
- **失败响应**: `500`，`{ success: false, message }`

### POST `/api/calculate/generate-distribution-plan`
- **说明**: 一键生成分配方案（如存在则先删除后重算并写回）
- **请求参数（query/form）**
  - `year` (int, required)
  - `month` (int, required)
  - `weekSeq` (int, required)
- **成功响应字段（节选）**
  - `success` (bool)
  - `operation` (string) 固定为“ 一键生成分配方案 ”
  - `deletedExistingData` (bool)
  - `deletedRecords` (int)
  - `processedCount` (int) 生成的分配记录数
  - `allocationResult` (object) 同 write-back 返回结构
- **失败响应**: `500`，包含 `error`、`message`、`processedCount`

### POST `/api/calculate/total-actual-delivery`
- **说明**: 统计某期所有卷烟的总实际投放量（按卷烟聚合）
- **请求参数（query/form）**
  - `year` (int, required)
  - `month` (int, required)
  - `weekSeq` (int, required)
- **成功响应字段（节选）**
  - `success` (bool)
  - `data` (object) `{"{cigCode}_{cigName}": BigDecimal}`
  - `totalRecords` (int) 原始记录数
  - `cigaretteCount` (int) 卷烟种数

---

## 三、数据管理 DataManageController

### POST `/api/data/query`
- **说明**: 查询 prediction 表原始数据，并附加每种卷烟的 `advAmount`、`actualDelivery`、编码/解码表达
- **请求体（JSON, QueryRequestDto）**
```json
{
  "year": 2025,
  "month": 9,
  "weekSeq": 3
}
```
- **成功响应（结构节选）**
```json
{
  "success": true,
  "total": 2,
  "data": [
    {
      "id": 1,
      "cigCode": "123",
      "cigName": "样例烟",
      "deliveryArea": "丹江",
      "year": 2025,
      "month": 9,
      "weekSeq": 3,
      "bz": "双周上浮",
      "advAmount": 1000,
      "deliveryMethod": "按档位扩展投放",
      "deliveryEtype": "档位+区县",
      "actualDelivery": 980,
      "encodedExpression": "...",
      "decodedExpression": "...",
      "d30": 0, "d29": 0, "d28": 0, "d1": 0
    }
  ]
}
```

### POST `/api/data/update-cigarette`
- **说明**: 更新某卷烟在某期次某区域的一条记录（含 D30~D1、投放方式、区域、备注）
- **请求体（JSON, UpdateCigaretteRequestDto）字段**
  - `cigCode` (string, required)
  - `cigName` (string, required)
  - `year` (int, required)
  - `month` (int, required)
  - `weekSeq` (int, required)
  - `deliveryMethod` (string, required)
  - `deliveryEtype` (string, required)
  - `deliveryArea` (string, required)
  - `distribution` (array[BigDecimal], required, 长度30，对应 D30..D1)
  - `bz` (string, optional)
- **响应**: `{ success: boolean, message, ... }`

### POST `/api/data/delete-delivery-areas`
- **说明**: 删除某卷烟在某期的若干投放区域（至少保留一个）
- **请求体（JSON, DeleteAreasRequestDto）字段**
  - `cigCode` (string, required)
  - `cigName` (string, required)
  - `year` (int, required)
  - `month` (int, required)
  - `weekSeq` (int, required)
  - `areasToDelete` (array[string], required)
- **响应**: 成功包含 `deletedCount`、`remainingAreas` 等；失败含 `error`、`message`

### POST `/api/data/batch-update-from-expressions`
- **说明**: 基于编码表达式批量更新某卷烟的多条记录（支持投放类型变更或增量更新）
- **请求体（JSON, BatchUpdateFromExpressionsRequestDto）字段**
  - `cigCode` (string, required)
  - `cigName` (string, required)
  - `year` (int, required)
  - `month` (int, required)
  - `weekSeq` (int, required)
  - `encodedExpressions` (array[string], required)
  - `bz` (string, optional)
- **响应（根据 operation 不同返回统计字段）**
  - `operation` ∈ {`投放类型变更`, `增量更新`}
  - 当为“投放类型变更”时返回: `deletedRecords`, `createdRecords`
  - 当为“增量更新”时返回: `newAreas`, `updatedAreas`, `deletedAreas`

---

## 四、Excel 导入 ExcelImportController

### POST `/api/import/cigarette-info`
- **说明**: 导入卷烟投放基础信息 Excel 到动态 info 表
- **请求**: `multipart/form-data`
  - `file` (file, required, 最大 10MB)
  - `year` (int, 2020-2099, required)
  - `month` (int, 1-12, required)
  - `weekSeq` (int, 1-5, required)
- **响应**: `{ success, tableName, insertedCount, message }`

### POST `/api/import/region-clientnum`
- **说明**: 导入区域客户数 Excel 到对应 `region_clientNum_{主序号}_{子序号}`
- **请求**: `multipart/form-data`
  - `file` (file, required, 最大 10MB)
  - `year` (int, 2020-2099, required)
  - `month` (int, 1-12, required)
  - `deliveryMethod` (string, required) 示例: `按档位统一投放` 或 `按档位扩展投放`
  - `deliveryEtype` (string, required) 当为扩展投放时示例: `档位+区县`/`档位+市场类型`/`档位+城乡分类代码`/`档位+业态`
  - `isBiWeeklyFloat` (bool, optional, 默认 false) 影响子序号（1 非上浮 / 2 上浮）
- **响应**: `{ success, tableName, insertedCount, mainSequenceNumber, subSequenceNumber, message }`

---

## 五、错误码与约定
- `FILE_EMPTY`/`FILE_TOO_LARGE`/`INVALID_DELIVERY_TYPE`/`IMPORT_FAILED`
- `DELETE_FAILED`/`RECORD_NOT_FOUND`/`CANNOT_DELETE_ALL_AREAS`/`INTERNAL_ERROR`
- 计算相关：`ALLOCATION_FAILED`/`GENERATION_FAILED`

## 六、示例 curl

```bash
# 写回分配矩阵
curl -X POST "http://localhost:8080/api/calculate/write-back?year=2025&month=9&weekSeq=3"

# 查询数据
curl -X POST "http://localhost:8080/api/data/query" \
  -H 'Content-Type: application/json' \
  -d '{"year":2025,"month":9,"weekSeq":3}'

# 导入 info 表
curl -X POST "http://localhost:8080/api/import/cigarette-info" \
  -F file=@/path/to/info.xlsx \
  -F year=2025 -F month=9 -F weekSeq=3

# 导入 region_clientNum 表（档位+区县，非双周上浮）
curl -X POST "http://localhost:8080/api/import/region-clientnum" \
  -F file=@/path/to/clientnum.xlsx \
  -F year=2025 -F month=9 \
  -F deliveryMethod='按档位扩展投放' \
  -F deliveryEtype='档位+区县' \
  -F isBiWeeklyFloat=false
```


