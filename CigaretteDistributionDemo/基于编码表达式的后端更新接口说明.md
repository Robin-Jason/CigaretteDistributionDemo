# 编码表达式批量更新接口文档

## 📋 接口概述

**接口名称**: 根据编码表达式批量更新卷烟投放信息  
**接口路径**: `POST /api/data/batch-update-from-expressions`  
**功能描述**: 前端传入某卷烟的多条编码表达式，后端自动解码并根据投放类型变更情况采用不同的更新策略

---

## 🎯 业务场景

该接口主要用于处理以下业务场景：
1. **批量更新卷烟投放区域和档位分配**
2. **智能处理投放类型变更**
3. **自动同步区域投放记录**

---

## 🚀 核心优势

### 智能更新策略
- **投放类型组合改变**: 删除旧记录 → 重建新记录
- **投放类型组合不变**: 增量更新（新增 + 更新 + 删除）

### 数据一致性保障
- 事务处理，确保操作原子性
- 验证编码表达式投放类型一致性
- 自动处理区域记录的增删改

---

## 📝 请求参数

### HTTP 请求
```http
POST /api/data/batch-update-from-expressions
Content-Type: application/json
```

### 请求体参数

| 参数名 | 类型 | 必填 | 描述 | 示例 |
|--------|------|------|------|------|
| `cigCode` | String | ✅ | 卷烟代码 | "001" |
| `cigName` | String | ✅ | 卷烟名称 | "中华(软)" |
| `year` | Integer | ✅ | 年份 | 2024 |
| `month` | Integer | ✅ | 月份 | 12 |
| `weekSeq` | Integer | ✅ | 周序号 | 1 |
| `encodedExpressions` | Array\<String\> | ✅ | 编码表达式列表 | 见下方示例 |
| `remark` | String | ❌ | 备注信息 | "批量更新投放信息" |

### 请求示例

```json
{
    "cigCode": "001",
    "cigName": "中华(软)",
    "year": 2024,
    "month": 12,
    "weekSeq": 1,
    "encodedExpressions": [
        "B1（1+2）（2×2+14×1+14×0）",
        "B1（3+4）（2×1+14×2+14×0）",
        "B1（5+6）（2×3+14×1+14×0）"
    ],
    "remark": "年底投放调整"
}
```

---

## 📤 响应参数

### 成功响应 (200 OK)

#### 投放类型变更场景
```json
{
    "success": true,
    "message": "投放类型变更完成，已重建投放记录",
    "operation": "投放类型变更",
    "totalExpressions": 3,
    "deletedRecords": 8,
    "createdRecords": 6
}
```

#### 增量更新场景
```json
{
    "success": true,
    "message": "增量更新完成，新增 2 个区域，更新 3 个区域，删除 1 个区域",
    "operation": "增量更新", 
    "totalExpressions": 3,
    "newAreas": 2,
    "updatedAreas": 3,
    "deletedAreas": 1
}
```

### 响应字段说明

| 字段名 | 类型 | 描述 |
|--------|------|------|
| `success` | Boolean | 操作是否成功 |
| `message` | String | 操作结果描述 |
| `operation` | String | 操作类型："投放类型变更" 或 "增量更新" |
| `totalExpressions` | Integer | 处理的编码表达式总数 |

#### 投放类型变更时的额外字段
| 字段名 | 类型 | 描述 |
|--------|------|------|
| `deletedRecords` | Integer | 删除的旧投放记录数 |
| `createdRecords` | Integer | 创建的新投放记录数 |

#### 增量更新时的额外字段
| 字段名 | 类型 | 描述 |
|--------|------|------|
| `newAreas` | Integer | 新增的区域数量 |
| `updatedAreas` | Integer | 更新的区域数量 |
| `deletedAreas` | Integer | 删除的区域数量 |

---

## ❌ 错误响应

### 业务错误 (400 Bad Request)

#### 参数验证错误
```json
{
    "success": false,
    "message": "编码表达式列表不能为空",
    "error": "INVALID_PARAMETERS"
}
```

#### 投放类型不一致
```json
{
    "success": false,
    "message": "编码表达式中的投放类型不一致，无法批量更新"
}
```

#### 卷烟不存在
```json
{
    "success": false,
    "message": "卷烟在预投放量表中不存在"
}
```

### 系统错误 (500 Internal Server Error)
```json
{
    "success": false,
    "message": "系统内部错误: 数据库连接异常",
    "error": "SQLException"
}
```

---

## 🔍 业务逻辑详解

### 1. 编码表达式解析
- 每个编码表达式会被解析为：投放类型、扩展投放类型、投放区域列表、档位分配
- 系统会验证所有表达式的投放类型组合是否一致

### 2. 投放类型变更检查
系统会比较：
- **当前数据库中的投放类型组合**
- **编码表达式解析出的投放类型组合**

### 3. 更新策略

#### 策略A：投放类型组合改变
```
删除该卷烟指定日期的所有投放记录
↓
根据编码表达式重建所有投放记录
```

#### 策略B：投放类型组合不变
```
获取当前投放区域列表
↓
比较编码表达式中的区域
↓
新增：表达式有 & 数据库无
更新：表达式有 & 数据库有  
删除：表达式无 & 数据库有
```

---

## 💡 前端开发注意事项

### 1. 编码表达式格式
- 必须是有效的编码表达式
- 同一批次中所有表达式的投放类型必须一致
- 示例格式：`"B1（1+2+3）（2×2+14×1+14×0）"`

### 2. 错误处理
```javascript
// 推荐的错误处理方式
try {
    const response = await fetch('/api/data/batch-update-from-expressions', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestData)
    });
    
    const result = await response.json();
    
    if (!result.success) {
        // 业务错误处理
        console.error('更新失败:', result.message);
        showErrorMessage(result.message);
        return;
    }
    
    // 成功处理
    console.log('更新成功:', result.message);
    if (result.operation === '投放类型变更') {
        showMessage(`删除了${result.deletedRecords}条记录，创建了${result.createdRecords}条记录`);
    } else {
        showMessage(`新增${result.newAreas}个区域，更新${result.updatedAreas}个区域，删除${result.deletedAreas}个区域`);
    }
    
} catch (error) {
    // 网络错误处理
    console.error('网络错误:', error);
    showErrorMessage('网络连接异常，请稍后重试');
}
```

### 3. 加载状态管理
```javascript
// 建议添加加载状态
const [isUpdating, setIsUpdating] = useState(false);

const handleBatchUpdate = async () => {
    setIsUpdating(true);
    try {
        // 调用接口
        await batchUpdateFromExpressions(data);
        // 刷新数据
        await refreshData();
    } finally {
        setIsUpdating(false);
    }
};
```

### 4. 数据验证
```javascript
// 前端验证示例
const validateRequest = (data) => {
    if (!data.cigCode || !data.cigName) {
        throw new Error('卷烟代码和名称不能为空');
    }
    
    if (!data.encodedExpressions || data.encodedExpressions.length === 0) {
        throw new Error('编码表达式列表不能为空');
    }
    
    if (!data.year || !data.month || !data.weekSeq) {
        throw new Error('时间信息不完整');
    }
    
    return true;
};
```

---

## 🔄 与其他接口的关系

### 相关接口
1. **查询接口**: `POST /api/data/query` - 用于获取当前投放数据
2. **单条更新接口**: `POST /api/data/update-cigarette` - 用于单条记录更新
3. **删除接口**: `POST /api/data/delete-delivery-areas` - 用于删除指定区域

### 使用场景
- **批量导入**: 从Excel或其他系统导入编码表达式
- **投放策略调整**: 修改多个区域的投放策略
- **数据同步**: 与外部系统同步投放数据

---

## 📊 性能考虑

### 建议的最佳实践
1. **批次大小**: 建议每次不超过50个编码表达式
2. **超时处理**: 设置适当的请求超时时间（建议30秒）
3. **并发控制**: 避免同时对同一卷烟发起多个更新请求
4. **数据刷新**: 更新成功后及时刷新相关数据展示

### 性能优化
- 使用事务处理，减少数据库操作次数
- 智能判断更新策略，避免不必要的全量重建
- 批量操作，提高处理效率

---

## 📞 技术支持

如有接口相关问题，请联系后端开发团队：
- **文档版本**: v1.0
- **最后更新**: 2024-12-29
- **维护人员**: Backend Team

---

## 📝 更新日志

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| v1.0 | 2024-12-29 | 初始版本，新增编码表达式批量更新功能 |
