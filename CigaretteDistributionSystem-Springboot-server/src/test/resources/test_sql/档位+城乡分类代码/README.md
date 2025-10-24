# 档位+城乡分类代码算法测试SQL脚本

## 📋 概述

本目录包含用于测试"档位+城乡分类代码"分配算法的SQL脚本，用于创建测试表和生成大规模测试数据。

**测试时间维度**: 2099年1月第1周  
**测试用例数量**: 202个  
**投放量范围**: 100条 ~ 200,000条  
**区域组合**: 从7个城乡分类区域中随机组合

---

## 📁 文件说明

### 01-create-test-tables.sql
**功能**: 创建测试所需的数据库表

**创建的表**:
1. `cigarette_distribution_info_2099_1_1` - 测试用例表（输入）
   - 存储卷烟投放基本信息
   - 包含预投放量、投放区域等信息
   
2. `cigarette_distribution_prediction_2099_1_1` - 算法结果表（输出）
   - 存储算法计算的分配结果
   - 包含30个档位字段（D30-D1）
   - 包含实际投放量和编码信息

**执行方式**:
```bash
mysql -uroot -p'YOUR_PASSWORD' < 01-create-test-tables.sql
```

---

### 02-generate-test-data.sql
**功能**: 生成测试用例数据

**测试数据特点**:

#### 1. 卷烟代码规则
- ✅ 格式：8位数字
- ✅ 范围：10000001 ~ 10000202
- ✅ 连续递增

#### 2. 预投放量规则
按以下规则生成，覆盖从小到大的投放量：

| 编号 | 投放量范围 | 生成规则 | 说明 |
|-----|-----------|---------|------|
| 1 | 100条 | 固定值 | 极小量级测试 |
| 2 | 500条 | 固定值 | 小量级测试 |
| 3 | 1,000条 | 固定值 | 基准量级测试 |
| 4+ | 1K-200K | 区间随机 | 每1000为一个区间，在区间内随机生成一个数 |

**区间随机规则示例**:
- 1000-2000区间: 随机生成如 1063
- 2000-3000区间: 随机生成如 2339
- 3000-4000区间: 随机生成如 3424
- ...
- 199000-200000区间: 随机生成如 199243

#### 3. 投放区域规则

**可用区域** (7个城乡分类代码):
1. 主城区
2. 乡中心区
3. 城乡结合区
4. 村庄
5. 特殊区域
6. 镇中心区
7. 镇乡结合区

**区域组合策略**: 根据投放量自动调整区域数

| 投放量范围 | 区域数 | 业务逻辑 |
|-----------|-------|---------|
| < 5,000 | 2-3个 | 小量级投放，集中在少数区域 |
| 5K-20K | 3-4个 | 中小量级投放，适度扩散 |
| 20K-50K | 4-5个 | 中量级投放，多区域覆盖 |
| 50K-100K | 5-6个 | 大量级投放，广泛覆盖 |
| ≥ 100K | 6-7个 | 超大量级投放，全区域覆盖 |

**执行方式**:
```bash
mysql -uroot -p'YOUR_PASSWORD' < 02-generate-test-data.sql
```

---

## 🚀 快速开始

### 步骤1: 创建测试表
```bash
cd "src/test/resources/test sql/档位+城乡分类代码"
mysql -uroot -p'LuvuubyRK*Jason1258' < 01-create-test-tables.sql
```

**预期输出**:
```
✅ 测试表创建完成！
表名: cigarette_distribution_info_2099_1_1
表名: cigarette_distribution_prediction_2099_1_1
```

### 步骤2: 生成测试数据
```bash
mysql -uroot -p'LuvuubyRK*Jason1258' < 02-generate-test-data.sql
```

**预期输出**:
```
✅ 测试数据生成完成！
测试用例总数: 202
投放量范围统计、区域数统计、卷烟代码格式验证等
```

### 步骤3: 执行算法测试

使用后端API进行测试：

```bash
# 执行分配算法并写回结果
curl -X POST "http://localhost:8080/api/calculate/write-back?year=2099&month=1&weekSeq=1"
```

---

## 📊 测试数据统计

### 按投放量范围分布

| 投放量范围 | 用例数 | 最小值 | 最大值 |
|-----------|-------|--------|--------|
| ≤100 | 1 | 100 | 100 |
| 100-1,000 | 2 | 500 | 1,000 |
| 1,000-10,000 | 9 | 1,063 | 9,710 |
| 10,000-50,000 | 40 | 10,555 | 49,399 |
| 50,000-100,000 | 50 | 50,583 | 99,164 |
| 100,000-150,000 | 50 | 100,784 | 149,966 |
| 150,000-200,000 | 50 | 150,009 | 199,243 |
| **总计** | **202** | **100** | **199,243** |

### 按区域数分布

| 区域数 | 用例数 | 最小投放量 | 最大投放量 |
|-------|-------|-----------|-----------|
| 2个区域 | 5 | 100 | 4,744 |
| 3个区域 | 10 | 500 | 19,786 |
| 4个区域 | 24 | 1,000 | 49,399 |
| 5个区域 | 39 | 20,419 | 99,164 |
| 6个区域 | 77 | 50,583 | 198,008 |
| 7个区域 | 47 | 102,087 | 199,243 |
| **总计** | **202** | **100** | **199,243** |

---

## 🔍 数据质量验证

### 卷烟代码格式检查
- ✅ 总数: 202个
- ✅ 符合8位数字格式: 202个
- ✅ 不符合格式: 0个
- ✅ 格式正确率: 100%

### 字段完整性检查
```sql
-- 检查必填字段
SELECT 
    SUM(CASE WHEN CIG_CODE IS NULL THEN 1 ELSE 0 END) as '代码空值',
    SUM(CASE WHEN CIG_NAME IS NULL THEN 1 ELSE 0 END) as '名称空值',
    SUM(CASE WHEN ADV IS NULL OR ADV <= 0 THEN 1 ELSE 0 END) as '投放量异常',
    SUM(CASE WHEN DELIVERY_AREA IS NULL OR DELIVERY_AREA = '' THEN 1 ELSE 0 END) as '区域空值'
FROM cigarette_distribution_info_2099_1_1;

-- 预期结果：所有列都应该为 0
```

---

## 🧪 测试场景覆盖

### 1. 量级测试
- ✅ 极小量级: 100条
- ✅ 小量级: 500-10,000条
- ✅ 中量级: 10,000-50,000条
- ✅ 大量级: 50,000-100,000条
- ✅ 超大量级: 100,000-200,000条

### 2. 区域组合测试
- ✅ 2区域组合: 21种可能（随机选择）
- ✅ 3区域组合: 35种可能（随机选择）
- ✅ 4区域组合: 35种可能（随机选择）
- ✅ 5区域组合: 21种可能（随机选择）
- ✅ 6区域组合: 7种可能（随机选择）
- ✅ 7区域组合: 1种（全部区域）

### 3. 边界测试
- ✅ 最小投放量: 100条
- ✅ 最大投放量: ~200,000条
- ✅ 最少区域: 2个
- ✅ 最多区域: 7个（除全市外的全部区域）

---

## 📈 查询示例

### 查看所有测试用例
```sql
SELECT 
    CIG_CODE as '卷烟代码',
    CIG_NAME as '卷烟名称',
    ADV as '预投放量',
    DELIVERY_AREA as '投放区域',
    LENGTH(DELIVERY_AREA) - LENGTH(REPLACE(DELIVERY_AREA, ',', '')) + 1 as '区域数',
    bz as '备注'
FROM cigarette_distribution_info_2099_1_1
ORDER BY ADV;
```

### 查看算法执行结果
```sql
SELECT 
    p.CIG_CODE as '卷烟代码',
    p.CIG_NAME as '卷烟名称',
    i.ADV as '预投放量',
    p.ACTUAL_DELIVERY as '实际投放量',
    (p.ACTUAL_DELIVERY - i.ADV) as '误差',
    ROUND((ABS(p.ACTUAL_DELIVERY - i.ADV) / i.ADV) * 100, 4) as '误差率%',
    p.DELIVERY_AREA as '区域'
FROM cigarette_distribution_prediction_2099_1_1 p
JOIN cigarette_distribution_info_2099_1_1 i 
    ON p.CIG_CODE = i.CIG_CODE AND p.CIG_NAME = i.CIG_NAME
ORDER BY i.ADV, p.DELIVERY_AREA;
```

### 统计算法性能
```sql
SELECT 
    i.CIG_CODE,
    i.CIG_NAME,
    i.ADV as '预投放量',
    SUM(p.ACTUAL_DELIVERY) as '总实际投放量',
    (SUM(p.ACTUAL_DELIVERY) - i.ADV) as '总误差',
    ROUND((ABS(SUM(p.ACTUAL_DELIVERY) - i.ADV) / i.ADV) * 100, 4) as '误差率%',
    COUNT(p.id) as '区域数'
FROM cigarette_distribution_info_2099_1_1 i
LEFT JOIN cigarette_distribution_prediction_2099_1_1 p 
    ON i.CIG_CODE = p.CIG_CODE AND i.CIG_NAME = p.CIG_NAME
GROUP BY i.CIG_CODE, i.CIG_NAME, i.ADV
ORDER BY i.ADV;
```

---

## 🛠️ 维护说明

### 清空测试数据
```sql
TRUNCATE TABLE cigarette_distribution_info_2099_1_1;
TRUNCATE TABLE cigarette_distribution_prediction_2099_1_1;
```

### 删除测试表
```sql
DROP TABLE IF EXISTS cigarette_distribution_info_2099_1_1;
DROP TABLE IF EXISTS cigarette_distribution_prediction_2099_1_1;
```

### 重新生成测试数据
```bash
# 先清空，再重新生成
mysql -uroot -p'YOUR_PASSWORD' -e "TRUNCATE TABLE marketing.cigarette_distribution_info_2099_1_1;"
mysql -uroot -p'YOUR_PASSWORD' < 02-generate-test-data.sql
```

---

## ⚠️ 注意事项

1. **数据库密码**: 示例中的密码需要替换为实际密码
2. **执行顺序**: 必须先执行 01 脚本创建表，再执行 02 脚本生成数据
3. **随机性**: 每次执行 02 脚本会生成不同的随机数据（投放量在区间内随机）
4. **时间维度**: 使用 2099 年是为了避免与真实业务数据冲突
5. **性能**: 202个测试用例可能需要几秒钟完成全部算法计算

---

## 📚 相关文档

- [算法说明文档](../../../../../algorithm docs/档位+城乡分类代码算法描述.md)
- [API文档](../../../../../docs/API.md)
- [策略模式指南](../../../../../docs/STRATEGY_PATTERN_GUIDE.md)

---

## 📧 问题反馈

如果在使用过程中遇到问题，请检查：
1. 数据库连接是否正常
2. 表是否已成功创建
3. 数据是否已成功插入
4. region_clientNum表是否存在对应的区域数据

---

**创建时间**: 2025年10月19日  
**最后更新**: 2025年10月19日  
**版本**: v1.0  
**作者**: AI Assistant

