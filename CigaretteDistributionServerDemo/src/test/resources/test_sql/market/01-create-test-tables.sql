-- ================================================================
-- 档位+市场类型算法测试表创建脚本
-- ================================================================
-- 功能：创建测试用的 info 表和 prediction 表
-- 数据库：marketing
-- 测试时间：2099年1月第2周 (与业态测试共用时间，区别于城乡分类代码)
-- 表名后缀：_2099_1_2
-- ** 注意：已将 collation 修改为 utf8mb4_general_ci 以兼容旧版 MySQL **
-- ================================================================

USE marketing;

-- ================================================================
-- 1. 创建测试用例表（Info表）
-- ================================================================
DROP TABLE IF EXISTS cigarette_distribution_info_2099_1_2;

CREATE TABLE `cigarette_distribution_info_2099_1_2` (
                                                        `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                                        `CIG_CODE` varchar(32) DEFAULT NULL COMMENT '卷烟代码（8位数字）',
                                                        `CIG_NAME` varchar(100) DEFAULT NULL COMMENT '卷烟名称',
                                                        `ADV` decimal(15,2) DEFAULT NULL COMMENT '预投放量',
                                                        `DELIVERY_METHOD` varchar(50) DEFAULT '按档位扩展投放' COMMENT '投放方法',
                                                        `DELIVERY_ETYPE` varchar(50) DEFAULT '档位+市场类型' COMMENT '投放类型',
                                                        `DELIVERY_AREA` varchar(500) DEFAULT NULL COMMENT '投放区域（城网/农网/组合）',
                                                        `bz` varchar(255) DEFAULT NULL COMMENT '备注',
                                                        `YEAR` int DEFAULT 2099 COMMENT '年份',
                                                        `MONTH` int DEFAULT 1 COMMENT '月份',
                                                        `WEEK_SEQ` int DEFAULT 2 COMMENT '周序号',
                                                        PRIMARY KEY (`id`),
                                                        UNIQUE KEY `uk_cig_code_name` (`CIG_CODE`,`CIG_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='档位+市场类型算法测试输入表_2099_1_2'; -- 修改 Collation

-- ================================================================
-- 2. 创建预测结果表（Prediction表）
-- ================================================================
DROP TABLE IF EXISTS cigarette_distribution_prediction_2099_1_2;

CREATE TABLE `cigarette_distribution_prediction_2099_1_2` (
                                                              `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                                              `CIG_CODE` varchar(32) DEFAULT NULL COMMENT '卷烟代码',
                                                              `CIG_NAME` varchar(100) DEFAULT NULL COMMENT '卷烟名称',
                                                              `YEAR` year DEFAULT 2099 COMMENT '年份',
                                                              `MONTH` tinyint DEFAULT 1 COMMENT '月份',
                                                              `WEEK_SEQ` tinyint DEFAULT 2 COMMENT '周序号',
                                                              `DELIVERY_AREA` varchar(100) DEFAULT NULL COMMENT '投放区域（城网/农网）',
                                                              `DELIVERY_METHOD` varchar(50) DEFAULT NULL COMMENT '投放方法',
                                                              `DELIVERY_ETYPE` varchar(50) DEFAULT NULL COMMENT '投放类型',
                                                              `D30` decimal(18,2) DEFAULT NULL COMMENT 'D30档位分配',
                                                              `D29` decimal(18,2) DEFAULT NULL COMMENT 'D29档位分配',
                                                              `D28` decimal(18,2) DEFAULT NULL COMMENT 'D28档位分配',
                                                              `D27` decimal(18,2) DEFAULT NULL COMMENT 'D27档位分配',
                                                              `D26` decimal(18,2) DEFAULT NULL COMMENT 'D26档位分配',
                                                              `D25` decimal(18,2) DEFAULT NULL COMMENT 'D25档位分配',
                                                              `D24` decimal(18,2) DEFAULT NULL COMMENT 'D24档位分配',
                                                              `D23` decimal(18,2) DEFAULT NULL COMMENT 'D23档位分配',
                                                              `D22` decimal(18,2) DEFAULT NULL COMMENT 'D22档位分配',
                                                              `D21` decimal(18,2) DEFAULT NULL COMMENT 'D21档位分配',
                                                              `D20` decimal(18,2) DEFAULT NULL COMMENT 'D20档位分配',
                                                              `D19` decimal(18,2) DEFAULT NULL COMMENT 'D19档位分配',
                                                              `D18` decimal(18,2) DEFAULT NULL COMMENT 'D18档位分配',
                                                              `D17` decimal(18,2) DEFAULT NULL COMMENT 'D17档位分配',
                                                              `D16` decimal(18,2) DEFAULT NULL COMMENT 'D16档位分配',
                                                              `D15` decimal(18,2) DEFAULT NULL COMMENT 'D15档位分配',
                                                              `D14` decimal(18,2) DEFAULT NULL COMMENT 'D14档位分配',
                                                              `D13` decimal(18,2) DEFAULT NULL COMMENT 'D13档位分配',
                                                              `D12` decimal(18,2) DEFAULT NULL COMMENT 'D12档位分配',
                                                              `D11` decimal(18,2) DEFAULT NULL COMMENT 'D11档位分配',
                                                              `D10` decimal(18,2) DEFAULT NULL COMMENT 'D10档位分配',
                                                              `D9` decimal(18,2) DEFAULT NULL COMMENT 'D9档位分配',
                                                              `D8` decimal(18,2) DEFAULT NULL COMMENT 'D8档位分配',
                                                              `D7` decimal(18,2) DEFAULT NULL COMMENT 'D7档位分配',
                                                              `D6` decimal(18,2) DEFAULT NULL COMMENT 'D6档位分配',
                                                              `D5` decimal(18,2) DEFAULT NULL COMMENT 'D5档位分配',
                                                              `D4` decimal(18,2) DEFAULT NULL COMMENT 'D4档位分配',
                                                              `D3` decimal(18,2) DEFAULT NULL COMMENT 'D3档位分配',
                                                              `D2` decimal(18,2) DEFAULT NULL COMMENT 'D2档位分配',
                                                              `D1` decimal(18,2) DEFAULT NULL COMMENT 'D1档位分配',
                                                              `BZ` varchar(255) DEFAULT NULL COMMENT '备注',
                                                              `ACTUAL_DELIVERY` decimal(18,2) DEFAULT NULL COMMENT '实际投放量',
                                                              `DEPLOYINFO_CODE` text COMMENT '编码表达式',
                                                              PRIMARY KEY (`id`),
                                                              UNIQUE KEY `unique_cigarette_area` (`CIG_CODE`,`CIG_NAME`,`DELIVERY_AREA`,`YEAR`,`MONTH`,`WEEK_SEQ`),
                                                              KEY `idx_time` (`YEAR`,`MONTH`,`WEEK_SEQ`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='档位+市场类型算法测试输出表_2099_1_2'; -- 修改 Collation

-- ================================================================
-- 验证表创建
-- ================================================================
SELECT '✅ 市场类型测试表创建完成！(使用兼容 Collation)' as '状态'; -- 修改提示
SELECT
    TABLE_NAME as '表名',
        TABLE_COMMENT as '说明',
        CREATE_TIME as '创建时间'
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'marketing'
  AND TABLE_NAME IN ('cigarette_distribution_info_2099_1_2', 'cigarette_distribution_prediction_2099_1_2')
ORDER BY TABLE_NAME;