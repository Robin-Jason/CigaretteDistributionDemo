# CommonService 开发重构指导文档

## 📋 文档概述

**文档目标**: 为各个业务类型的服务开发人员提供CommonService的使用指导，支持系统重构  
**适用对象**: 投放算法开发人员、业务服务开发人员、系统架构师  
**版本**: v1.0  
**更新日期**: 2025年9月18日  

## 🎯 CommonService 简介

CommonService是卷烟分配系统的**核心通用服务类**，为五种投放类型提供统一的数据访问和操作方法。该服务封装了数据库操作、投放类型适配、区域客户数矩阵处理等通用功能，避免各业务服务重复实现相同逻辑。

### 设计理念
- **🔧 统一接口**: 为不同投放类型提供一致的API接口
- **📊 数据抽象**: 屏蔽底层数据表差异，提供统一的数据访问层
- **⚡ 性能优化**: 优化的SQL查询和数据处理逻辑
- **🔒 事务保障**: 完善的事务管理和异常处理

## 🗂️ 支持的投放类型映射

### 投放类型与数据表对应关系

| 投放类型组合 | deliveryMethod | deliveryEtype | 数据表 | 区域字段 |
|-------------|---------------|---------------|--------|----------|
| 按档位统一投放 | 按档位统一投放 | NULL | city_clientnum_data | URBAN_RURAL_CODE |
| 档位+区县 | 按档位扩展投放 | 档位+区县 | demo_test_county_client_numdata | COUNTY |
| 档位+市场类型 | 按档位扩展投放 | 档位+市场类型 | demo_market_test_clientnumdata | URBAN_RURAL_CODE |
| 档位+城乡分类代码 | 按档位扩展投放 | 档位+城乡分类代码 | demo_test_clientNumdata | URBAN_RURAL_CODE |
| 档位+业态 | 按档位扩展投放 | 档位+业态 | demo_test_businessFormat_clientNumData | BusinessFormat |

### 档位字段统一标准
所有客户数表都使用标准的30个档位字段：`D30, D29, D28, ..., D2, D1`

## 🔧 公共方法详细说明

### 方法1: getAllRegionList() - 获取投放区域列表

#### 方法签名
```java
public List<String> getAllRegionList(String deliveryMethod, String deliveryEtype)
```

#### 功能描述
根据投放类型组合从对应的区域客户数表获取所有可用的投放区域列表。

#### 参数说明
- `deliveryMethod` (String): 投放方法
  - 按档位统一投放
  - 按档位扩展投放
- `deliveryEtype` (String): 扩展投放类型
  - NULL (用于按档位统一投放)
  - 档位+区县
  - 档位+市场类型
  - 档位+城乡分类代码
  - 档位+业态

#### 返回值
- `List<String>`: 投放区域名称列表，按字母顺序排序

#### 使用示例
```java
@Autowired
private CommonService commonService;

// 获取城乡分类代码投放类型的区域列表
List<String> regions = commonService.getAllRegionList("按档位扩展投放", "档位+城乡分类代码");
// 返回示例: ["城镇结合区", "村庄"]

// 获取全市统一投放的区域列表
List<String> cityRegions = commonService.getAllRegionList("按档位统一投放", null);
// 返回示例: ["全市"]

// 获取区县投放类型的区域列表
List<String> countyRegions = commonService.getAllRegionList("按档位扩展投放", "档位+区县");
// 返回示例: ["丹江", "郧西", "郧阳", ...]
```

#### 重构建议
- ✅ 在算法服务中使用此方法获取可用区域，避免硬编码区域名称
- ✅ 在验证投放区域有效性时调用此方法进行校验
- ✅ 在前端区域选择器中使用此方法填充选项

---

### 方法2: buildRegionCustomerMatrix() - 构建区域客户数矩阵

#### 方法签名
```java
public RegionCustomerMatrix buildRegionCustomerMatrix(String deliveryMethod, String deliveryEtype)
```

#### 功能描述
从对应的区域客户数表获取完整的区域客户数矩阵，输出格式为n×30矩阵（n个区域×30个档位）。

#### 参数说明
- 同 `getAllRegionList()` 方法

#### 返回值
- `RegionCustomerMatrix`: 区域客户数矩阵对象
  - `regionNames`: 区域名称列表
  - `customerMatrix`: 客户数矩阵 (List<BigDecimal[]>)

#### RegionCustomerMatrix 对象方法
```java
// 获取区域数量
int getRegionCount()

// 获取档位数量 (固定30)
int getGradeCount()

// 根据区域名称获取客户数数组
BigDecimal[] getCustomerCountsByRegion(String regionName)

// 检查矩阵是否为空
boolean isEmpty()
```

#### 使用示例
```java
// 构建城乡分类代码的客户数矩阵
RegionCustomerMatrix matrix = commonService.buildRegionCustomerMatrix("按档位扩展投放", "档位+城乡分类代码");

if (!matrix.isEmpty()) {
    List<String> regions = matrix.getRegionNames();
    System.out.println("区域数量: " + matrix.getRegionCount()); // 2
    System.out.println("档位数量: " + matrix.getGradeCount()); // 30
    
    // 获取城镇的客户数分布
    BigDecimal[] urbanCustomers = matrix.getCustomerCountsByRegion("城镇");
    if (urbanCustomers != null) {
        System.out.println("城镇D30档位客户数: " + urbanCustomers[0]);
        System.out.println("城镇D1档位客户数: " + urbanCustomers[29]);
    }
    
    // 遍历所有区域和档位
    List<BigDecimal[]> customerMatrix = matrix.getCustomerMatrix();
    for (int i = 0; i < regions.size(); i++) {
        String regionName = regions.get(i);
        BigDecimal[] customerCounts = customerMatrix.get(i);
        System.out.println("区域: " + regionName);
        for (int j = 0; j < 30; j++) {
            int grade = 30 - j; // D30对应索引0，D1对应索引29
            System.out.println("  D" + grade + ": " + customerCounts[j]);
        }
    }
}
```

#### 算法开发中的使用
```java
public class CityCigaretteDistributionAlgorithm extends BaseCigaretteDistributionAlgorithm {
    
    @Autowired
    private CommonService commonService;
    
    @Override
    public BigDecimal[][] execute(String deliveryEtype, Integer year, Integer month, Integer weekSeq) {
        // 1. 获取客户数矩阵
        RegionCustomerMatrix customerMatrix = commonService.buildRegionCustomerMatrix("按档位统一投放", null);
        
        // 2. 获取预投放量数据
        List<DemoTestAdvData> advDataList = commonService.getAdvDataByDeliveryType(deliveryEtype, year, month, weekSeq);
        
        // 3. 执行算法计算...
        // 4. 返回分配矩阵
    }
}
```

#### 重构建议
- ✅ 在所有算法服务中使用此方法获取客户数矩阵，统一数据源
- ✅ 利用RegionCustomerMatrix对象的便捷方法简化数据处理
- ✅ 在计算实际投放量时使用此矩阵数据确保一致性

---

### 方法3: getAdvDataByDeliveryType() - 获取预投放量数据

#### 方法签名
```java
public List<DemoTestAdvData> getAdvDataByDeliveryType(String deliveryEtype, Integer year, Integer month, Integer weekSeq)
```

#### 功能描述
从demo_test_ADVdata表中获取指定投放类型和时间的预投放量数据。

#### 参数说明
- `deliveryEtype` (String): 投放类型
  - "NULL" 或 null (按档位统一投放)
  - "档位+区县"
  - "档位+市场类型"
  - "档位+城乡分类代码"
  - "档位+业态"
- `year` (Integer): 年份
- `month` (Integer): 月份
- `weekSeq` (Integer): 周序号

#### 返回值
- `List<DemoTestAdvData>`: 预投放量数据列表

#### DemoTestAdvData 主要字段
```java
String cigCode;        // 卷烟代码
String cigName;        // 卷烟名称
String deliveryEtype;  // 投放类型
BigDecimal adv;        // 预投放量
String deliveryArea;   // 投放区域
Integer year;          // 年份
Integer month;         // 月份
Integer weekSeq;       // 周序号
```

#### 使用示例
```java
// 获取2025年9月第1周的城乡分类代码投放数据
List<DemoTestAdvData> advData = commonService.getAdvDataByDeliveryType("档位+城乡分类代码", 2025, 9, 1);

for (DemoTestAdvData data : advData) {
    System.out.println("卷烟: " + data.getCigCode() + " - " + data.getCigName());
    System.out.println("投放区域: " + data.getDeliveryArea());
    System.out.println("预投放量: " + data.getAdv());
    System.out.println("---");
}

// 获取按档位统一投放的数据
List<DemoTestAdvData> cityAdvData = commonService.getAdvDataByDeliveryType(null, 2025, 9, 1);
```

#### 重构建议
- ✅ 在算法计算前调用此方法获取预投放量基础数据
- ✅ 根据返回的卷烟列表进行循环计算
- ✅ 使用预投放量数据驱动分配算法的执行

---

### 方法4: batchInsertTestData() - 批量写入测试数据

#### 方法签名
```java
@Transactional
public Map<String, Object> batchInsertTestData(List<DemoTestData> testDataList)
```

#### 功能描述
批量将DemoTestData对象列表写入demo_test_data表，支持事务回滚。
各个成员服务层将算法输出的档位和相关的信息整理成可写入数据库表的数据列表

#### 参数说明
- `testDataList` (List<DemoTestData>): 待写入的测试数据列表

#### 返回值
- `Map<String, Object>`: 操作结果信息
  - `success` (Boolean): 操作是否成功
  - `totalCount` (Integer): 总记录数
  - `successCount` (Integer): 成功写入记录数
  - `failCount` (Integer): 失败记录数
  - `message` (String): 操作结果消息
  - `errorDetails` (String): 错误详情 (失败时)

#### 使用示例
```java
// 准备测试数据
List<DemoTestData> testDataList = new ArrayList<>();

DemoTestData data1 = new DemoTestData();
data1.setCigCode("C001");
data1.setCigName("测试卷烟A");
data1.setYear(2025);
data1.setMonth(9);
data1.setWeekSeq(1);
data1.setDeliveryArea("城镇");
data1.setD30(new BigDecimal("100"));
data1.setD29(new BigDecimal("95"));
// ... 设置其他档位
data1.setD1(new BigDecimal("10"));
data1.setActualDelivery(new BigDecimal("1000"));
testDataList.add(data1);

// 批量写入
Map<String, Object> result = commonService.batchInsertTestData(testDataList);

// 检查结果
if ((Boolean) result.get("success")) {
    System.out.println("写入成功: " + result.get("successCount") + " 条记录");
} else {
    System.out.println("写入失败: " + result.get("message"));
    System.out.println("错误详情: " + result.get("errorDetails"));
}
```

#### 算法服务中的使用
```java
public class CountyCigaretteDistributionAlgorithm extends BaseCigaretteDistributionAlgorithm {
    
    @Autowired
    private CommonService commonService;
    
    public void writeBackResults(BigDecimal[][] allocationMatrix, List<String> regions, 
                                List<DemoTestAdvData> advDataList, Integer year, Integer month, Integer weekSeq) {
        List<DemoTestData> resultData = new ArrayList<>();
        
        // 根据分配矩阵构建结果数据
        for (int i = 0; i < regions.size(); i++) {
            for (DemoTestAdvData advData : advDataList) {
                DemoTestData testData = new DemoTestData();
                testData.setCigCode(advData.getCigCode());
                testData.setCigName(advData.getCigName());
                testData.setYear(year);
                testData.setMonth(month);
                testData.setWeekSeq(weekSeq);
                testData.setDeliveryArea(regions.get(i));
                
                // 设置档位分配值
                BigDecimal[] allocation = allocationMatrix[i];
                testData.setD30(allocation[0]);
                testData.setD29(allocation[1]);
                // ... 设置其他档位
                testData.setD1(allocation[29]);
                
                // 计算实际投放量
                BigDecimal actualDelivery = calculateActualDelivery(allocation, regions.get(i));
                testData.setActualDelivery(actualDelivery);
                
                resultData.add(testData);
            }
        }
        
        // 批量写入结果
        Map<String, Object> writeResult = commonService.batchInsertTestData(resultData);
        log.info("算法结果写入: {}", writeResult.get("message"));
    }
}
```

#### 重构建议
- ✅ 在算法计算完成后使用此方法写入结果数据
- ✅ 利用事务特性确保数据一致性
- ✅ 检查返回结果进行错误处理和日志记录

---

### 方法5: deleteSpecificTestData() - 删除指定条件数据

#### 方法签名
```java
@Transactional
public Map<String, Object> deleteSpecificTestData(String cigCode, String cigName, 
                                                 Integer year, Integer month, Integer weekSeq, 
                                                 String deliveryArea)
```

#### 功能描述
删除demo_test_data表中符合指定条件的历史数据，支持精确匹配删除。
各个成员可以通过这个方法删除表里面你的投放类型的旧卷烟预测记录

#### 参数说明
- `cigCode` (String): 卷烟代码
- `cigName` (String): 卷烟名称
- `year` (Integer): 年份
- `month` (Integer): 月份
- `weekSeq` (Integer): 周序号
- `deliveryArea` (String): 投放区域

#### 返回值
- `Map<String, Object>`: 删除结果信息
  - `success` (Boolean): 操作是否成功
  - `deletedCount` (Integer): 删除的记录数
  - `message` (String): 操作结果消息
  - `errorDetails` (String): 错误详情 (失败时)

#### 使用示例
```java
// 删除指定条件的历史数据
Map<String, Object> deleteResult = commonService.deleteSpecificTestData(
    "C001",           // 卷烟代码
    "测试卷烟A",       // 卷烟名称
    2025,             // 年份
    9,                // 月份
    1,                // 周序号
    "城镇"            // 投放区域
);

// 检查删除结果
if ((Boolean) deleteResult.get("success")) {
    System.out.println("删除成功: " + deleteResult.get("deletedCount") + " 条记录");
} else {
    System.out.println("删除失败: " + deleteResult.get("message"));
}
```


## 🏗️ 重构最佳实践

### 1. 依赖注入
```java
@Service
public class YourBusinessService {
    
    @Autowired
    private CommonService commonService;
    
    // 使用CommonService的方法...
}
```

### 2. 投放类型参数标准化
```java
// 推荐：使用常量定义投放类型
public class DeliveryTypeConstants {
    public static final String UNIFIED_DELIVERY_METHOD = "按档位统一投放";
    public static final String EXTENDED_DELIVERY_METHOD = "按档位扩展投放";
    
    public static final String COUNTY_DELIVERY_ETYPE = "档位+区县";
    public static final String MARKET_DELIVERY_ETYPE = "档位+市场类型";
    public static final String URBAN_RURAL_DELIVERY_ETYPE = "档位+城乡分类代码";
    public static final String BUSINESS_FORMAT_DELIVERY_ETYPE = "档位+业态";
}
```

### 3. 异常处理
```java
try {
    List<String> regions = commonService.getAllRegionList(deliveryMethod, deliveryEtype);
    if (regions.isEmpty()) {
        log.warn("未找到投放区域: deliveryMethod={}, deliveryEtype={}", deliveryMethod, deliveryEtype);
        return Collections.emptyList();
    }
    // 继续处理...
} catch (Exception e) {
    log.error("获取投放区域失败", e);
    throw new BusinessException("获取投放区域失败: " + e.getMessage());
}
```

### 4. 性能优化
```java
// 推荐：一次性获取矩阵数据，避免重复查询
RegionCustomerMatrix matrix = commonService.buildRegionCustomerMatrix(deliveryMethod, deliveryEtype);
List<String> regions = matrix.getRegionNames();

for (String region : regions) {
    BigDecimal[] customerCounts = matrix.getCustomerCountsByRegion(region);
    // 处理该区域的客户数...
}

// 不推荐：在循环中重复调用数据库
for (String region : regions) {
    // 这样会导致重复查询数据库
    RegionCustomerMatrix singleMatrix = commonService.buildRegionCustomerMatrix(deliveryMethod, deliveryEtype);
    BigDecimal[] customerCounts = singleMatrix.getCustomerCountsByRegion(region);
}
```

### 5. 数据验证
```java
public boolean validateDeliveryTypeSupported(String deliveryMethod, String deliveryEtype) {
    List<String> regions = commonService.getAllRegionList(deliveryMethod, deliveryEtype);
    return !regions.isEmpty();
}

public boolean validateRegionExists(String deliveryMethod, String deliveryEtype, String targetRegion) {
    List<String> regions = commonService.getAllRegionList(deliveryMethod, deliveryEtype);
    return regions.contains(targetRegion);
}
```

## 🔄 服务重构指导

### 算法服务重构
```java
// 重构前：直接操作数据库
@Service
public class OldAlgorithmService {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    public void execute() {
        // 硬编码SQL查询
        String sql = "SELECT * FROM demo_test_clientNumdata";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
        // 手动处理数据...
    }
}

// 重构后：使用CommonService
@Service
public class NewAlgorithmService {
    @Autowired
    private CommonService commonService;
    
    public void execute(String deliveryMethod, String deliveryEtype, Integer year, Integer month, Integer weekSeq) {
        // 1. 获取区域列表
        List<String> regions = commonService.getAllRegionList(deliveryMethod, deliveryEtype);
        
        // 2. 获取客户数矩阵
        RegionCustomerMatrix matrix = commonService.buildRegionCustomerMatrix(deliveryMethod, deliveryEtype);
        
        // 3. 获取预投放量数据
        List<DemoTestAdvData> advData = commonService.getAdvDataByDeliveryType(deliveryEtype, year, month, weekSeq);
        
        // 4. 执行算法计算...
        BigDecimal[][] allocationMatrix = calculateAllocation(matrix, advData);
        
        // 5. 写入结果数据
        List<DemoTestData> resultData = buildResultData(allocationMatrix, regions, advData, year, month, weekSeq);
        Map<String, Object> writeResult = commonService.batchInsertTestData(resultData);
        
        log.info("算法执行完成: {}", writeResult.get("message"));
    }
}
```

### 数据管理服务重构
```java
// 重构前：分散的数据库操作
@Service
public class OldDataService {
    @Autowired
    private DemoTestDataRepository testDataRepository;
    
    public void deleteOldData(String cigCode, String cigName) {
        // 需要手动构建查询条件
        List<DemoTestData> oldData = testDataRepository.findByCigCodeAndCigName(cigCode, cigName);
        testDataRepository.deleteAll(oldData);
    }
}

// 重构后：使用CommonService精确删除
@Service
public class NewDataService {
    @Autowired
    private CommonService commonService;
    
    public Map<String, Object> cleanupSpecificData(String cigCode, String cigName, 
                                                  Integer year, Integer month, Integer weekSeq,
                                                  List<String> targetAreas) {
        int totalDeleted = 0;
        for (String area : targetAreas) {
            Map<String, Object> deleteResult = commonService.deleteSpecificTestData(
                cigCode, cigName, year, month, weekSeq, area);
            if ((Boolean) deleteResult.get("success")) {
                totalDeleted += (Integer) deleteResult.get("deletedCount");
            }
        }
        return Map.of("success", true, "totalDeleted", totalDeleted);
    }
}
```
#### 数据管理中的使用
```java
public class DataManagementService {
    
    @Autowired
    private CommonService commonService;
    
    public Map<String, Object> updateCigaretteDeliveryAreas(String cigCode, String cigName,
                                                           Integer year, Integer month, Integer weekSeq,
                                                           List<String> newDeliveryAreas) {
        // 1. 获取当前投放区域
        List<String> currentAreas = getCurrentDeliveryAreas(cigCode, cigName, year, month, weekSeq);
        
        // 2. 删除不再需要的区域数据
        for (String area : currentAreas) {
            if (!newDeliveryAreas.contains(area)) {
                Map<String, Object> deleteResult = commonService.deleteSpecificTestData(
                    cigCode, cigName, year, month, weekSeq, area);
                log.info("删除区域 {} 的数据: {}", area, deleteResult.get("message"));
            }
        }
        
        // 3. 添加新的区域数据...
        return Map.of("success", true, "message", "更新完成");
    }
}
```


## 📊 迁移检查清单

### ✅ 重构前检查
- [ ] 确认当前服务使用的投放类型参数格式
- [ ] 识别直接的数据库查询操作
- [ ] 检查硬编码的区域名称和表名
- [ ] 确认数据处理的事务需求

### ✅ 重构中实施
- [ ] 添加CommonService依赖注入
- [ ] 替换直接数据库查询为CommonService方法调用
- [ ] 更新投放类型参数为标准格式
- [ ] 添加适当的异常处理和日志记录

### ✅ 重构后验证
- [ ] 运行单元测试确保功能正常
- [ ] 验证数据查询结果的一致性
- [ ] 检查性能是否有改善
- [ ] 确认事务行为符合预期

## 🆘 常见问题解答

### Q1: 如何处理不支持的投放类型？
```java
List<String> regions = commonService.getAllRegionList(deliveryMethod, deliveryEtype);
if (regions.isEmpty()) {
    throw new UnsupportedDeliveryTypeException(
        String.format("不支持的投放类型组合: %s - %s", deliveryMethod, deliveryEtype));
}
```

### Q2: 如何优化大数据量的处理？
```java
// 分批处理大量数据
List<DemoTestData> allData = ...; // 大量数据
int batchSize = 1000;

for (int i = 0; i < allData.size(); i += batchSize) {
    int endIndex = Math.min(i + batchSize, allData.size());
    List<DemoTestData> batch = allData.subList(i, endIndex);
    Map<String, Object> result = commonService.batchInsertTestData(batch);
    log.info("批次 {}-{} 写入结果: {}", i, endIndex-1, result.get("message"));
}
```

### Q3: 如何处理并发访问？
CommonService的方法都是线程安全的，但在高并发场景下建议：
```java
@Service
@Transactional
public class ConcurrentSafeService {
    @Autowired
    private CommonService commonService;
    
    public synchronized Map<String, Object> safeDataOperation() {
        // 在需要时使用synchronized或锁机制
        return commonService.batchInsertTestData(...);
    }
}
```

