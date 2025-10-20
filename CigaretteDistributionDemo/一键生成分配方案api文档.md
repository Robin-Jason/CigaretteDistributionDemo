# 一键生成分配方案 API 文档

## 📋 文档概要

**版本**: v1.0  
**更新日期**: 2025-10-19  
**适用对象**: 前端开发人员  
**服务地址**: `http://localhost:28080`

---

## 🎯 API概述

本文档介绍卷烟分配系统的核心API接口，包括：
1. **执行分配计算并写回** - `/api/calculate/write-back`
2. **一键生成分配方案** - `/api/calculate/generate-distribution-plan`

两个接口都支持为**档位+市场类型**传入**城网/农网比例参数**。

---

## 📡 接口1: 执行分配计算并写回

### 基本信息

**接口地址**: `/api/calculate/write-back`  
**请求方式**: `POST`  
**Content-Type**: `application/x-www-form-urlencoded` 或 `application/json`

### 功能说明

读取指定时间周期的卷烟投放基础信息，执行分配算法计算，并将结果写回到预测数据表。

**适用场景**:
- 已存在卷烟投放基础数据
- 需要执行算法计算并保存结果
- 可能覆盖已有的分配结果

---

### 请求参数

#### 必填参数

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|-------|------|------|------|------|
| `year` | Integer | ✅ | 年份 (2020-2099) | `2025` |
| `month` | Integer | ✅ | 月份 (1-12) | `10` |
| `weekSeq` | Integer | ✅ | 周序号 (1-5) | `1` |

#### 可选参数（仅用于档位+市场类型）

| 参数名 | 类型 | 必填 | 说明 | 示例 | 默认值 |
|-------|------|------|------|------|--------|
| `urbanRatio` | BigDecimal | ❌ | 城网分配比例 | `0.45` | `0.4` (40%) |
| `ruralRatio` | BigDecimal | ❌ | 农网分配比例 | `0.55` | `0.6` (60%) |

---

### 比例参数说明 ⭐

#### 📊 使用规则

1. **仅用于档位+市场类型**
   - 其他投放类型（档位+区县、档位+城乡分类代码、档位+业态等）会忽略这些参数

2. **同时传入或都不传**
   - ✅ 正确: 同时传入 `urbanRatio=0.45` 和 `ruralRatio=0.55`
   - ✅ 正确: 都不传（使用默认值 40%/60%）
   - ❌ 错误: 只传一个参数（会被忽略，使用默认值）

3. **比例值范围**
   - 建议范围: 0.0 - 1.0
   - 两个比例之和建议为 1.0（如 0.45 + 0.55 = 1.0）
   - 系统不强制检查总和，但建议前端验证

4. **默认值**
   - 城网: **40%** (0.4)
   - 农网: **60%** (0.6)

---

### 请求示例

#### 示例1: 不传比例参数（使用默认值）

**cURL**:
```bash
curl -X POST "http://localhost:28080/api/calculate/write-back?year=2025&month=10&weekSeq=1"
```

**JavaScript (Fetch API)**:
```javascript
fetch('http://localhost:28080/api/calculate/write-back', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded',
  },
  body: new URLSearchParams({
    year: '2025',
    month: '10',
    weekSeq: '1'
  })
})
.then(response => response.json())
.then(data => console.log(data));
```

**结果**: 档位+市场类型使用默认比例 - 城网40%，农网60%

---

#### 示例2: 传入自定义比例参数（城网45%，农网55%）

**cURL**:
```bash
curl -X POST "http://localhost:28080/api/calculate/write-back?year=2025&month=10&weekSeq=1&urbanRatio=0.45&ruralRatio=0.55"
```

**JavaScript (Fetch API)**:
```javascript
fetch('http://localhost:28080/api/calculate/write-back', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded',
  },
  body: new URLSearchParams({
    year: '2025',
    month: '10',
    weekSeq: '1',
    urbanRatio: '0.45',
    ruralRatio: '0.55'
  })
})
.then(response => response.json())
.then(data => console.log(data));
```

**JavaScript (Axios)**:
```javascript
axios.post('http://localhost:28080/api/calculate/write-back', null, {
  params: {
    year: 2025,
    month: 10,
    weekSeq: 1,
    urbanRatio: 0.45,
    ruralRatio: 0.55
  }
})
.then(response => {
  console.log('分配计算成功:', response.data);
})
.catch(error => {
  console.error('分配计算失败:', error);
});
```

**Vue.js 示例**:
```vue
<template>
  <div>
    <h2>卷烟分配计算</h2>
    
    <!-- 基本参数 -->
    <div>
      <label>年份: <input v-model.number="year" type="number" /></label>
      <label>月份: <input v-model.number="month" type="number" /></label>
      <label>周序号: <input v-model.number="weekSeq" type="number" /></label>
    </div>
    
    <!-- 市场类型比例参数 -->
    <div>
      <h3>市场类型比例设置（可选）</h3>
      <label>
        <input v-model="useCustomRatio" type="checkbox" />
        自定义城网/农网比例
      </label>
      
      <div v-if="useCustomRatio">
        <label>城网比例: <input v-model.number="urbanRatio" type="number" step="0.01" /></label>
        <label>农网比例: <input v-model.number="ruralRatio" type="number" step="0.01" /></label>
        <span>总和: {{ (urbanRatio + ruralRatio).toFixed(2) }}</span>
      </div>
      <div v-else>
        <p>使用默认比例 - 城网: 40%, 农网: 60%</p>
      </div>
    </div>
    
    <button @click="executeCalculation">执行分配计算</button>
  </div>
</template>

<script>
export default {
  data() {
    return {
      year: 2025,
      month: 10,
      weekSeq: 1,
      useCustomRatio: false,
      urbanRatio: 0.45,
      ruralRatio: 0.55
    };
  },
  methods: {
    async executeCalculation() {
      try {
        const params = {
          year: this.year,
          month: this.month,
          weekSeq: this.weekSeq
        };
        
        // 如果勾选自定义比例，添加比例参数
        if (this.useCustomRatio) {
          params.urbanRatio = this.urbanRatio;
          params.ruralRatio = this.ruralRatio;
        }
        
        const response = await axios.post('/api/calculate/write-back', null, { params });
        
        if (response.data.success) {
          alert(`分配计算成功！成功: ${response.data.successCount}/${response.data.totalCount}`);
          console.log('详细结果:', response.data);
        } else {
          alert('分配计算失败: ' + response.data.message);
        }
      } catch (error) {
        console.error('请求失败:', error);
        alert('请求失败: ' + error.message);
      }
    }
  }
};
</script>
```

**结果**: 档位+市场类型使用自定义比例 - 城网45%，农网55%

---

### 响应格式

#### 成功响应

```json
{
  "success": true,
  "totalCount": 50,
  "successCount": 48,
  "failureCount": 2,
  "message": "分配矩阵写回完成，成功48个，失败2个",
  "writeBackResults": [
    {
      "cigCode": "12345678",
      "cigName": "黄鹤楼(1916中支)",
      "adv": 10000,
      "deliveryArea": "城网,农网",
      "deliveryEtype": "档位+市场类型",
      "targetType": "市场类型分配",
      "algorithm": "MarketProportionalCigaretteDistributionAlgorithm",
      "writeBackStatus": "成功",
      "recordCount": 2
    },
    // ... 更多卷烟记录
  ]
}
```

#### 失败响应

```json
{
  "success": false,
  "message": "分配矩阵写回失败: 表不存在",
  "totalCount": 0,
  "successCount": 0
}
```

---

## 📡 接口2: 一键生成分配方案

### 基本信息

**接口地址**: `/api/calculate/generate-distribution-plan`  
**请求方式**: `POST`  
**Content-Type**: `application/x-www-form-urlencoded` 或 `application/json`

### 功能说明

一键完成以下操作：
1. 检查是否存在旧的分配数据
2. 如果存在，先删除旧数据
3. 执行分配算法计算
4. 写回新的分配结果
5. 返回完整的执行报告

**适用场景**:
- 需要重新生成分配方案
- 确保数据是最新的算法结果
- 替代原有分配数据

---

### 请求参数

#### 必填参数

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|-------|------|------|------|------|
| `year` | Integer | ✅ | 年份 (2020-2099) | `2025` |
| `month` | Integer | ✅ | 月份 (1-12) | `10` |
| `weekSeq` | Integer | ✅ | 周序号 (1-5) | `1` |

#### 可选参数（仅用于档位+市场类型）

| 参数名 | 类型 | 必填 | 说明 | 示例 | 默认值 |
|-------|------|------|------|------|--------|
| `urbanRatio` | BigDecimal | ❌ | 城网分配比例 | `0.45` | `0.4` (40%) |
| `ruralRatio` | BigDecimal | ❌ | 农网分配比例 | `0.55` | `0.6` (60%) |

**比例参数规则**: 与接口1相同

---

### 请求示例

#### 示例1: 不传比例参数（使用默认值）

**cURL**:
```bash
curl -X POST "http://localhost:28080/api/calculate/generate-distribution-plan?year=2025&month=10&weekSeq=1"
```

**JavaScript (Fetch API)**:
```javascript
fetch('http://localhost:28080/api/calculate/generate-distribution-plan', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded',
  },
  body: new URLSearchParams({
    year: '2025',
    month: '10',
    weekSeq: '1'
  })
})
.then(response => response.json())
.then(data => {
  if (data.success) {
    console.log('生成成功!');
    console.log('删除记录:', data.deletedRecords);
    console.log('新增记录:', data.generatedRecords);
  }
});
```

---

#### 示例2: 传入自定义比例（城网45%，农网55%）

**cURL**:
```bash
curl -X POST "http://localhost:28080/api/calculate/generate-distribution-plan?year=2025&month=10&weekSeq=1&urbanRatio=0.45&ruralRatio=0.55"
```

**JavaScript (Axios)**:
```javascript
const generatePlan = async (year, month, weekSeq, urbanRatio = null, ruralRatio = null) => {
  try {
    const params = {
      year,
      month,
      weekSeq
    };
    
    // 如果传入了比例参数，添加到请求中
    if (urbanRatio !== null && ruralRatio !== null) {
      params.urbanRatio = urbanRatio;
      params.ruralRatio = ruralRatio;
    }
    
    const response = await axios.post(
      'http://localhost:28080/api/calculate/generate-distribution-plan',
      null,
      { params }
    );
    
    return response.data;
  } catch (error) {
    console.error('生成分配方案失败:', error);
    throw error;
  }
};

// 使用示例
// 使用默认比例
await generatePlan(2025, 10, 1);

// 使用自定义比例
await generatePlan(2025, 10, 1, 0.45, 0.55);
```

**React 组件示例**:
```jsx
import React, { useState } from 'react';
import axios from 'axios';

function DistributionPlanGenerator() {
  const [year, setYear] = useState(2025);
  const [month, setMonth] = useState(10);
  const [weekSeq, setWeekSeq] = useState(1);
  const [useCustomRatio, setUseCustomRatio] = useState(false);
  const [urbanRatio, setUrbanRatio] = useState(0.45);
  const [ruralRatio, setRuralRatio] = useState(0.55);
  const [loading, setLoading] = useState(false);

  const handleGenerate = async () => {
    setLoading(true);
    
    try {
      const params = {
        year,
        month,
        weekSeq
      };
      
      // 如果使用自定义比例，添加参数
      if (useCustomRatio) {
        params.urbanRatio = urbanRatio;
        params.ruralRatio = ruralRatio;
      }
      
      const response = await axios.post(
        'http://localhost:28080/api/calculate/generate-distribution-plan',
        null,
        { params }
      );
      
      if (response.data.success) {
        alert(`生成成功！\n删除: ${response.data.deletedRecords}条\n新增: ${response.data.generatedRecords}条`);
      } else {
        alert('生成失败: ' + response.data.message);
      }
    } catch (error) {
      alert('请求失败: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h2>一键生成分配方案</h2>
      
      <div>
        <label>
          年份: <input type="number" value={year} onChange={e => setYear(e.target.value)} />
        </label>
        <label>
          月份: <input type="number" value={month} onChange={e => setMonth(e.target.value)} />
        </label>
        <label>
          周序号: <input type="number" value={weekSeq} onChange={e => setWeekSeq(e.target.value)} />
        </label>
      </div>
      
      <div>
        <label>
          <input 
            type="checkbox" 
            checked={useCustomRatio} 
            onChange={e => setUseCustomRatio(e.target.checked)} 
          />
          自定义市场类型比例
        </label>
        
        {useCustomRatio && (
          <div>
            <label>
              城网比例: 
              <input 
                type="number" 
                step="0.01" 
                value={urbanRatio} 
                onChange={e => setUrbanRatio(parseFloat(e.target.value))} 
              />
            </label>
            <label>
              农网比例: 
              <input 
                type="number" 
                step="0.01" 
                value={ruralRatio} 
                onChange={e => setRuralRatio(parseFloat(e.target.value))} 
              />
            </label>
            <span>总和: {(urbanRatio + ruralRatio).toFixed(2)}</span>
          </div>
        )}
        
        {!useCustomRatio && (
          <p>使用默认比例 - 城网: 40%, 农网: 60%</p>
        )}
      </div>
      
      <button onClick={handleGenerate} disabled={loading}>
        {loading ? '生成中...' : '生成分配方案'}
      </button>
    </div>
  );
}

export default DistributionPlanGenerator;
```

---

### 响应格式

#### 成功响应

```json
{
  "success": true,
  "year": 2025,
  "month": 10,
  "weekSeq": 1,
  "deletedExistingData": true,
  "deletedRecords": 45,
  "generatedRecords": 50,
  "totalCigarettes": 50,
  "successCount": 48,
  "failureCount": 2,
  "message": "分配方案生成成功，删除45条旧数据，生成50条新数据",
  "detailedResults": [
    {
      "cigCode": "12345678",
      "cigName": "黄鹤楼(1916中支)",
      "targetType": "市场类型分配",
      "algorithm": "MarketProportionalCigaretteDistributionAlgorithm",
      "writeBackStatus": "成功",
      "recordCount": 2
    }
    // ... 更多卷烟记录
  ]
}
```

#### 失败响应

```json
{
  "success": false,
  "year": 2025,
  "month": 10,
  "weekSeq": 1,
  "error": "TABLE_NOT_FOUND",
  "message": "指定时间的卷烟投放基本信息表不存在"
}
```

---

## 📊 两个接口的区别

| 特性 | /write-back | /generate-distribution-plan |
|-----|-------------|---------------------------|
| **功能** | 直接执行计算并写回 | 先删除旧数据，再计算并写回 |
| **是否删除旧数据** | ❌ 否（覆盖） | ✅ 是（完全重建） |
| **适用场景** | 首次生成或增量更新 | 重新生成或数据重置 |
| **事务保护** | 单个写回事务 | 删除+写回完整事务 |
| **返回信息** | 写回统计 | 删除+生成统计 |

**建议**:
- **首次生成**: 使用任意一个接口
- **更新数据**: 使用 `/write-back`
- **重新生成**: 使用 `/generate-distribution-plan`

---

## 🎨 前端实现建议

### 1. 比例参数表单验证

```javascript
// 比例验证函数
function validateMarketRatios(urbanRatio, ruralRatio) {
  // 检查是否为数字
  if (isNaN(urbanRatio) || isNaN(ruralRatio)) {
    return {
      valid: false,
      message: '比例必须是数字'
    };
  }
  
  // 检查范围
  if (urbanRatio < 0 || urbanRatio > 1 || ruralRatio < 0 || ruralRatio > 1) {
    return {
      valid: false,
      message: '比例必须在0-1之间'
    };
  }
  
  // 检查总和
  const sum = urbanRatio + ruralRatio;
  if (Math.abs(sum - 1.0) > 0.01) {
    return {
      valid: false,
      message: `比例总和应为1.0，当前为${sum.toFixed(2)}`
    };
  }
  
  return {
    valid: true,
    message: '验证通过'
  };
}

// 使用示例
const validation = validateMarketRatios(0.45, 0.55);
if (!validation.valid) {
  alert(validation.message);
  return;
}
```

---

### 2. 默认比例提示

```javascript
// 比例参数组件
const MarketRatioInput = ({ onChange }) => {
  const [useCustom, setUseCustom] = useState(false);
  const [urbanRatio, setUrbanRatio] = useState(0.45);
  const [ruralRatio, setRuralRatio] = useState(0.55);
  
  useEffect(() => {
    // 通知父组件比例变化
    onChange(useCustom ? { urbanRatio, ruralRatio } : null);
  }, [useCustom, urbanRatio, ruralRatio]);
  
  return (
    <div className="market-ratio-input">
      <label>
        <input 
          type="checkbox" 
          checked={useCustom} 
          onChange={e => setUseCustom(e.target.checked)} 
        />
        自定义市场类型比例
      </label>
      
      {useCustom ? (
        <div className="custom-ratio">
          <div>
            <label>城网比例:</label>
            <input 
              type="number" 
              step="0.01" 
              min="0" 
              max="1"
              value={urbanRatio}
              onChange={e => setUrbanRatio(parseFloat(e.target.value))}
            />
            <span>{(urbanRatio * 100).toFixed(1)}%</span>
          </div>
          
          <div>
            <label>农网比例:</label>
            <input 
              type="number" 
              step="0.01" 
              min="0" 
              max="1"
              value={ruralRatio}
              onChange={e => setRuralRatio(parseFloat(e.target.value))}
            />
            <span>{(ruralRatio * 100).toFixed(1)}%</span>
          </div>
          
          <div className="ratio-sum">
            总和: {((urbanRatio + ruralRatio) * 100).toFixed(1)}%
            {Math.abs(urbanRatio + ruralRatio - 1.0) > 0.01 && (
              <span className="warning">⚠️ 总和应为100%</span>
            )}
          </div>
        </div>
      ) : (
        <div className="default-ratio">
          <p>✅ 使用默认比例</p>
          <p>城网: 40% | 农网: 60%</p>
        </div>
      )}
    </div>
  );
};
```

---

### 3. 完整的API调用封装

```javascript
// api/distribution.js
import axios from 'axios';

const BASE_URL = 'http://localhost:28080/api/calculate';

/**
 * 分配计算API服务
 */
export const distributionApi = {
  /**
   * 执行分配计算并写回
   * @param {number} year - 年份
   * @param {number} month - 月份
   * @param {number} weekSeq - 周序号
   * @param {object} marketRatios - 市场类型比例（可选）
   * @param {number} marketRatios.urbanRatio - 城网比例
   * @param {number} marketRatios.ruralRatio - 农网比例
   * @returns {Promise} 响应数据
   */
  async writeBack(year, month, weekSeq, marketRatios = null) {
    const params = { year, month, weekSeq };
    
    if (marketRatios) {
      params.urbanRatio = marketRatios.urbanRatio;
      params.ruralRatio = marketRatios.ruralRatio;
    }
    
    try {
      const response = await axios.post(`${BASE_URL}/write-back`, null, { params });
      return response.data;
    } catch (error) {
      console.error('分配计算失败:', error);
      throw error;
    }
  },
  
  /**
   * 一键生成分配方案
   * @param {number} year - 年份
   * @param {number} month - 月份
   * @param {number} weekSeq - 周序号
   * @param {object} marketRatios - 市场类型比例（可选）
   * @returns {Promise} 响应数据
   */
  async generatePlan(year, month, weekSeq, marketRatios = null) {
    const params = { year, month, weekSeq };
    
    if (marketRatios) {
      params.urbanRatio = marketRatios.urbanRatio;
      params.ruralRatio = marketRatios.ruralRatio;
    }
    
    try {
      const response = await axios.post(`${BASE_URL}/generate-distribution-plan`, null, { params });
      return response.data;
    } catch (error) {
      console.error('生成分配方案失败:', error);
      throw error;
    }
  }
};

// 使用示例
import { distributionApi } from '@/api/distribution';

// 示例1: 使用默认比例
const result1 = await distributionApi.generatePlan(2025, 10, 1);

// 示例2: 使用自定义比例
const result2 = await distributionApi.generatePlan(2025, 10, 1, {
  urbanRatio: 0.45,
  ruralRatio: 0.55
});
```

---

## 📋 响应字段说明

### 通用字段

| 字段名 | 类型 | 说明 |
|-------|------|------|
| `success` | Boolean | 操作是否成功 |
| `message` | String | 操作结果描述信息 |
| `totalCount` | Integer | 处理的卷烟总数 |
| `successCount` | Integer | 成功计算的卷烟数 |
| `failureCount` | Integer | 失败的卷烟数 |

### write-back 特有字段

| 字段名 | 类型 | 说明 |
|-------|------|------|
| `writeBackResults` | Array | 每个卷烟的详细写回结果 |
| `writeBackResults[].cigCode` | String | 卷烟代码 |
| `writeBackResults[].cigName` | String | 卷烟名称 |
| `writeBackResults[].targetType` | String | 目标类型（如"市场类型分配"） |
| `writeBackResults[].algorithm` | String | 使用的算法名称 |
| `writeBackResults[].writeBackStatus` | String | 写回状态（"成功"/"失败"） |
| `writeBackResults[].recordCount` | Integer | 写回的记录数 |

### generate-distribution-plan 特有字段

| 字段名 | 类型 | 说明 |
|-------|------|------|
| `year` | Integer | 年份 |
| `month` | Integer | 月份 |
| `weekSeq` | Integer | 周序号 |
| `deletedExistingData` | Boolean | 是否删除了旧数据 |
| `deletedRecords` | Integer | 删除的记录数 |
| `generatedRecords` | Integer | 生成的记录数 |
| `startTime` | Long | 开始时间戳 |
| `endTime` | Long | 结束时间戳 |
| `duration` | Long | 执行耗时（毫秒） |

---

## ⚠️ 注意事项

### 1. 比例参数的作用范围

**仅影响**: 投放类型为"档位+市场类型"的卷烟

**不影响**: 
- 档位+区县
- 档位+城乡分类代码
- 档位+业态
- 按档位统一投放

**示例数据**:
```
2025年10月第1周的卷烟列表:
1. 黄鹤楼(1916) - 档位+市场类型 → 使用传入的比例参数 ✅
2. 中华(硬) - 档位+区县 → 不使用比例参数 ❌
3. 芙蓉王 - 档位+城乡分类代码 → 不使用比例参数 ❌
```

---

### 2. 比例参数的默认值

**默认规则**:
- 如果`urbanRatio`和`ruralRatio`都不传: 使用默认值 **40%/60%**
- 如果只传一个: 忽略该参数，使用默认值 **40%/60%**
- 如果都传: 使用传入的值

**后端日志**:
```
// 使用默认值时
[INFO] 使用默认市场类型比例 - 城网: 40%, 农网: 60%

// 使用自定义值时  
[INFO] 使用前端传入的市场类型比例 - 城网: 45%, 农网: 55%
```

---

### 3. 数据表要求

**必须存在的表**:

1. **卷烟投放基础信息表**:
   - 表名: `cigarette_distribution_info_{year}_{month}_{weekSeq}`
   - 示例: `cigarette_distribution_info_2025_10_1`

2. **区域客户数表** (各投放类型):
   - `region_clientNum_0_1` (按档位统一投放)
   - `region_clientNum_1_1` (档位+区县)
   - `region_clientNum_2_1` (档位+市场类型)
   - `region_clientNum_3_1` (档位+城乡分类代码)
   - `region_clientNum_4_1` (档位+业态)

**如果表不存在**: 返回错误响应

---

### 4. 常见错误处理

#### 错误1: 表不存在
```json
{
  "success": false,
  "message": "指定时间的卷烟投放基本信息表不存在: cigarette_distribution_info_2025_10_1"
}
```

**前端处理建议**:
```javascript
if (!response.data.success && response.data.message.includes('表不存在')) {
  alert('指定时间周期没有卷烟数据，请先导入基础数据');
}
```

---

#### 错误2: 参数验证失败
```json
{
  "success": false,
  "message": "年份参数无效"
}
```

**前端处理建议**:
```javascript
// 请求前验证
if (year < 2020 || year > 2099) {
  alert('年份必须在2020-2099之间');
  return;
}

if (month < 1 || month > 12) {
  alert('月份必须在1-12之间');
  return;
}

if (weekSeq < 1 || weekSeq > 5) {
  alert('周序号必须在1-5之间');
  return;
}
```

---

## 🎯 推荐的前端实现流程

### 完整流程

```javascript
// 1. 用户选择时间周期
const year = 2025;
const month = 10;
const weekSeq = 1;

// 2. 用户选择是否自定义比例
const useCustomRatio = true;  // 复选框
const urbanRatio = 0.45;      // 输入框
const ruralRatio = 0.55;      // 输入框

// 3. 前端验证
// 验证年月周参数
if (year < 2020 || month < 1 || weekSeq < 1) {
  alert('参数错误');
  return;
}

// 验证比例参数（如果勾选了自定义）
if (useCustomRatio) {
  const validation = validateMarketRatios(urbanRatio, ruralRatio);
  if (!validation.valid) {
    alert(validation.message);
    return;
  }
}

// 4. 构建请求参数
const params = { year, month, weekSeq };
if (useCustomRatio) {
  params.urbanRatio = urbanRatio;
  params.ruralRatio = ruralRatio;
}

// 5. 调用API
try {
  const response = await axios.post(
    'http://localhost:28080/api/calculate/generate-distribution-plan',
    null,
    { params }
  );
  
  // 6. 处理响应
  if (response.data.success) {
    // 成功处理
    showSuccessMessage(response.data);
    refreshDataTable();  // 刷新数据表格
  } else {
    // 失败处理
    showErrorMessage(response.data.message);
  }
} catch (error) {
  // 异常处理
  console.error('请求失败:', error);
  showErrorMessage('网络请求失败，请检查服务器连接');
}

// 7. 成功提示函数
function showSuccessMessage(data) {
  const message = `
    ✅ 分配方案生成成功！
    
    删除旧数据: ${data.deletedRecords}条
    生成新数据: ${data.generatedRecords}条
    成功卷烟: ${data.successCount}/${data.totalCigarettes}
    耗时: ${data.duration}ms
  `;
  alert(message);
}
```

---

## 📚 支持的投放类型

### 五种投放类型及其参数

| 投放类型 | 投放方法 | 扩展类型 | 是否支持比例参数 |
|---------|---------|---------|---------------|
| 按档位统一投放 | 按档位统一投放 | null | ❌ |
| 档位+区县 | 按档位扩展投放 | 档位+区县 | ❌ |
| 档位+市场类型 | 按档位扩展投放 | 档位+市场类型 | ✅ **支持** |
| 档位+城乡分类代码 | 按档位扩展投放 | 档位+城乡分类代码 | ❌ |
| 档位+业态 | 按档位扩展投放 | 档位+业态 | ❌ |

---

## 🔧 调试建议

### 1. 查看后端日志

**传入比例参数时的日志**:
```
[INFO] 接收写回请求，年份: 2025, 月份: 10, 周序号: 1
[INFO] 接收市场类型比例参数 - 城网: 0.45, 农网: 0.55
[INFO] 使用前端传入的市场类型比例 - 城网: 0.45, 农网: 0.55
```

**使用默认值时的日志**:
```
[INFO] 接收写回请求，年份: 2025, 月份: 10, 周序号: 1
[INFO] 使用默认市场类型比例 - 城网: 40%, 农网: 60%
```

---

### 2. 网络请求调试

**Chrome DevTools**:
1. 打开开发者工具 (F12)
2. 切换到 Network 标签
3. 执行请求
4. 查看请求详情:
   - Request URL: `http://localhost:28080/api/calculate/write-back?year=2025&month=10&weekSeq=1&urbanRatio=0.45&ruralRatio=0.55`
   - Request Method: `POST`
   - Status Code: `200 OK`
   - Response: JSON数据

---

## 🎉 快速开始

### 最简单的调用方式

```javascript
// 1. 使用默认比例（最简单）
fetch('http://localhost:28080/api/calculate/generate-distribution-plan?year=2025&month=10&weekSeq=1', {
  method: 'POST'
})
.then(res => res.json())
.then(data => console.log(data));

// 2. 使用自定义比例
fetch('http://localhost:28080/api/calculate/generate-distribution-plan?year=2025&month=10&weekSeq=1&urbanRatio=0.45&ruralRatio=0.55', {
  method: 'POST'
})
.then(res => res.json())
.then(data => console.log(data));
```

---

## 📞 技术支持

### 常见问题

**Q1: 城网和农网比例必须传吗？**  
A: 不是必须的。如果不传，系统使用默认值 - 城网40%，农网60%。

**Q2: 比例总和必须是100%吗？**  
A: 建议是100%，但系统不强制检查。前端建议添加验证提示。

**Q3: 只传一个比例参数可以吗？**  
A: 不可以。必须同时传`urbanRatio`和`ruralRatio`，或者都不传。只传一个会被忽略。

**Q4: 比例参数对所有卷烟都生效吗？**  
A: 仅对投放类型为"档位+市场类型"的卷烟生效，其他投放类型不受影响。

**Q5: 如何知道使用了哪个比例？**  
A: 查看后端日志，会明确记录使用的比例（前端传入 or 默认值）。

---

## 📝 版本历史

### v1.0 (2025-10-19)
- ✅ 新增城网/农网比例参数支持
- ✅ 两个接口同时支持比例参数
- ✅ 默认值: 城网40%，农网60%
- ✅ 向后兼容: 不传参数时使用默认值

---

**文档编写日期**: 2025-10-19  
**后端版本**: v1.0  
**文档维护人**: AI Assistant

