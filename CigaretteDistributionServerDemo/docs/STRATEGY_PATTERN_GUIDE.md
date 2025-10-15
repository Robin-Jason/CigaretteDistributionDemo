# 策略模式架构开发指南

## 1. 引言

### 1.1. 文档目的
本文档旨在为项目所有开发人员提供一个清晰、统一的开发指南，详细阐述本项目核心的 **策略模式 (Strategy Pattern) 架构**。通过遵循本指南，开发人员可以快速、规范地扩展新的业务功能（如新增一种卷烟投放方式），同时确保项目代码的长期可维护性和健壮性。

### 1.2. 为何采用此架构？
我们选择策略模式作为核心架构，因为它能带来诸多好处：
- **高扩展性**：新增业务算法时，无需修改现有代码，只需添加新策略即可，完美符合“开闭原则”。
- **职责清晰**：将复杂的业务**算法**与**业务流程的编排**彻底分离，使代码更易于理解和维护。
- **高复用性**：每个策略都是一个独立的、可复用的组件。
- **易于测试**：可以独立地对每个策略算法进行单元测试，确保其正确性。

---

## 2. 核心组件解析

我们的策略模式架构由以下四个核心角色构成：

1.  **`DistributionStrategy` (策略接口)**
    -   **职责**: 定义所有分配策略都必须遵守的“契约”或“蓝图”。它规定了所有策略类都必须有一个标准的 `execute` 方法，用于执行算法。
    -   **位置**: `org.example.service.strategy.DistributionStrategy`

2.  **具体策略类 (Concrete Strategy)**
    -   **职责**: 封装一个具体的业务算法。例如，`CityDistributionStrategy` 就封装了“全市统一投放”的所有复杂计算逻辑。每个具体策略都是 `DistributionStrategy` 接口的一个实现。
    -   **位置**: `org.example.service.strategy.impl` 包下 (如: `CityDistributionStrategy.java`)

3.  **`DistributionStrategyManager` (策略管理器)**
    -   **职责**: 策略的“调度中心”或“工厂”。它负责管理系统中所有的策略实例，并根据业务方（Client）的请求，动态地提供一个正确的策略实例。这是业务代码与具体算法解耦的关键。
    -   **位置**: `org.example.service.strategy.DistributionStrategyManager`

4.  **业务服务类 (Business Service / Client)**
    -   **职责**: 策略的“调用方”。它代表了一个具体的业务场景，负责**编排**整个业务流程：准备数据、向 `Strategy Manager` 请求所需策略、调用策略执行计算、处理返回结果。它不关心算法的具体实现细节。
    -   **位置**: 各业务包下 (如: `org.example.service.CityUnifiedDistribution.CityPredictionService`)

---

## 3. 工作流程

当一个业务请求（例如“一键生成全市分配方案”）到达时，系统内部的协作流程如下：

```
+--------------------------+     +-------------------------------+     +----------------------------------+
| 业务服务                 |     | DistributionStrategyManager   |     | 具体策略                         |
| (e.g. CityPredictionSvc) |     | (策略管理器)                  |     | (e.g. CityDistributionStrategy)  |
+--------------------------+     +-------------------------------+     +----------------------------------+
           |                                 |                                        |
           | 1. 接收请求, 准备计算所需数据     |                                        |
           |-------------------------------->|                                        |
           |                                 |                                        |
           | 2. 根据业务类型("全市统一投放")   |                                        |
           |    请求对应的策略                 |                                        |
           |-------------------------------->| 3. 从内部Map中查找并返回对应策略实例 |
           |                                 |--------------------------------------->|
           |                                 |                                        |
           | 4. 接收到具体的策略实例           |                                        |
           |<------------------------------- |                                        |
           |                                 |                                        |
           | 5. 调用策略的execute()方法,     |                                        |
           |    并传入准备好的数据             |                                        |
           |------------------------------------------------------------------------>| 6. 执行核心分配算法
           |                                 |                                        | 7. 返回计算结果
           |<------------------------------------------------------------------------|
           |                                 |                                        |
           | 8. 接收计算结果, 进行后续处理     |                                        |
           |    (如数据持久化)                 |                                        |
           |                                 |                                        |
```

---

## 4. 如何添加一种新的投放方式？(实战指南)

这是本指南最核心的部分。假设我们需要新增一种 **“按客户经理等级投放”** 的分配方式。请严格遵循以下步骤：

### 第一步：创建新的策略实现类

在 `org.example.service.strategy.impl` 包下，创建一个新的Java类 `ManagerLevelDistributionStrategy.java`。

这个类必须：
1.  实现 `DistributionStrategy` 接口。
2.  使用Spring的 `@Service` 注解将其声明为一个Bean。**注解的值必须是将来用于查找该策略的唯一Key**。

```java
package org.example.service.strategy.impl;

import org.example.service.strategy.DistributionStrategy;
import org.springframework.stereotype.Service;

// 使用 @Service 注解，并为其指定一个唯一的Bean名称
@Service("按客户经理等级投放")
public class ManagerLevelDistributionStrategy implements DistributionStrategy {

    @Override
    public BigDecimal[][] execute(AllocationParameter params) {
        // 在下一步中实现这里的具体算法
        System.out.println("正在执行按客户经理等级投放算法...");
        // ... 具体的计算逻辑 ...
        return new BigDecimal[0][]; // 返回计算结果
    }
}
```

### 第二步：在 `DistributionStrategyManager` 中注册新策略

**这一步在我们的项目中是自动完成的**

因为 `DistributionStrategyManager` 利用了Spring的依赖注入特性，它会自动收集所有实现了 `DistributionStrategy` 接口的Bean。你**唯一需要做的**就是确保在上一步中正确地使用了 `@Service("策略名称")` 注解。管理器会自动将这个新策略加载到它的策略池中。

### 第三步：在业务代码中调用新策略

现在，你可以在任何业务服务类中，通过 `DistributionStrategyManager` 来获取并使用你的新策略。

```java
@Service
public class NewBusinessService {

    @Autowired
    private DistributionStrategyManager strategyManager;

    public void generatePlan() {
        // 1. 定义你要使用的策略的Key (必须与@Service注解中的名称完全一致)
        String strategyKey = "按客户经理等级投放";

        // 2. 从管理器获取策略实例
        DistributionStrategy strategy = strategyManager.getStrategy(strategyKey);

        // 3. 准备该策略所需的参数
        AllocationParameter params = prepareDataForManagerLevel();

        // 4. 执行策略
        BigDecimal[][] allocationMatrix = strategy.execute(params);

        // 5. 处理结果
        processResults(allocationMatrix);
    }
    
    // ... 其他辅助方法 ...
}
```

注意事项：
-   **保持策略纯粹**：策略类应只负责核心算法。数据查询、事务管理、结果持久化等操作应由**业务服务类**负责。
-   **无状态策略**：策略类应该是无状态的（Stateless），不应包含任何成员变量来存储请求相关的数据。这能保证它们是线程安全的。
-   **统一参数对象**：所有策略的 `execute` 方法都应接收一个统一的参数对象（如 `AllocationParameter`），封装该次计算所需的所有输入数据。
-   **常量定义Key**：建议将策略的Key（如 `"按客户经理等级投放"`）定义在统一的常量类中，避免在代码中硬编码字符串，防止拼写错误。
