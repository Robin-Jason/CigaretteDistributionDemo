package org.example.service.strategy;

import java.math.BigDecimal;
import java.util.List;

/**
 * 分配策略接口
 * 
 * 【核心功能】
 * 定义卷烟分配算法的统一接口，使用策略模式替代冗长的switch-case逻辑
 * 
 * 【支持的投放类型】
 * - 全市统一投放：城市级别的统一分配
 * - 档位+区县：区县级精准投放
 * - 档位+市场类型：城网/农网分类投放  
 * - 档位+城乡分类代码：城乡属性分类投放
 * - 档位+业态：业态类型分类投放
 * 
 * 【设计模式】
 * - 策略模式：封装不同的分配算法
 * - 模板方法：统一的处理流程
 * - 依赖注入：通过Spring管理策略实例
 * 
 * @author Robin
 * @version 1.0
 * @since 2025-10-11
 */
public interface DistributionStrategy {
    
    /**
     * 获取策略支持的投放类型标识
     * 
     * @return 投放类型标识（如："档位+区县"、"档位+市场类型"等）
     */
    String getDeliveryType();
    
    /**
     * 获取目标投放区域列表
     * 根据投放区域字段值解析出具体的投放目标列表
     * 
     * @param deliveryArea 投放区域字段值（可能包含通配符、分隔符等）
     * @return 具体的投放目标列表（区域名称、业态类型等）
     * @throws IllegalArgumentException 当投放区域无效时抛出异常
     * 
     * @example
     * getTargetList("丹江,房县,郧西") -> ["丹江", "房县", "郧西"]
     * getTargetList("城网") -> ["城网"]
     */
    List<String> getTargetList(String deliveryArea);
    
    /**
     * 计算分配矩阵
     * 根据目标列表和预投放量计算具体的分配矩阵
     * 
     * @param targetList 目标投放列表
     * @param targetAmount 预投放量
     * @return 分配矩阵 [目标数量][30个档位]
     * @throws IllegalArgumentException 当参数无效时抛出异常
     * @throws RuntimeException 当算法计算失败时抛出异常
     * 
     * @example
     * calculateMatrix(["丹江", "房县"], BigDecimal.valueOf(1000)) 
     * -> 2x30的分配矩阵，每行代表一个区县的30个档位分配值
     */
    BigDecimal[][] calculateMatrix(List<String> targetList, BigDecimal targetAmount);
    
    /**
     * 获取算法名称
     * 用于日志记录和调试
     * 
     * @return 算法类名或描述
     * 
     * @example
     * "CountyCigaretteDistributionAlgorithm"
     * "MarketProportionalCigaretteDistributionAlgorithm"
     */
    String getAlgorithmName();
    
    /**
     * 获取分配目标类型描述
     * 用于结果展示和日志记录
     * 
     * @return 目标类型描述
     * 
     * @example
     * "区县分配"、"市场类型分配"、"城乡分类代码分配"
     */
    String getTargetTypeDescription();
    
    /**
     * 验证投放区域的有效性
     * 子类可以重写此方法实现特定的验证逻辑
     * 
     * @param deliveryArea 投放区域字段值
     * @return true表示有效，false表示无效
     * 
     * @default 默认实现：非空且非空白字符串即为有效
     */
    default boolean isValidDeliveryArea(String deliveryArea) {
        return deliveryArea != null && !deliveryArea.trim().isEmpty();
    }
    
    /**
     * 验证目标列表的有效性
     * 子类可以重写此方法实现特定的验证逻辑
     * 
     * @param targetList 目标列表
     * @return true表示有效，false表示无效
     * 
     * @default 默认实现：非空且包含至少一个元素即为有效
     */
    default boolean isValidTargetList(List<String> targetList) {
        return targetList != null && !targetList.isEmpty();
    }
    
    /**
     * 验证预投放量的有效性
     * 子类可以重写此方法实现特定的验证逻辑
     * 
     * @param targetAmount 预投放量
     * @return true表示有效，false表示无效
     * 
     * @default 默认实现：非null且大于0即为有效
     */
    default boolean isValidTargetAmount(BigDecimal targetAmount) {
        return targetAmount != null && targetAmount.compareTo(BigDecimal.ZERO) > 0;
    }
}
