-- ================================================================
-- 档位+城乡分类代码算法测试表创建脚本
-- ================================================================
-- 功能：创建测试用的info表和prediction表
-- 数据库：marketing
-- 测试时间：2099年1月第1周
-- ================================================================

USE marketing;

-- ================================================================
-- 1. 创建测试用例表（Info表）
-- ================================================================
DROP TABLE IF EXISTS cigarette_distribution_info_2099_1_1;

CREATE TABLE cigarette_distribution_info_2099_1_1 (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    CIG_CODE VARCHAR(20) NOT NULL COMMENT '卷烟代码（8位数字）',
    CIG_NAME VARCHAR(100) NOT NULL COMMENT '卷烟名称',
    ADV DECIMAL(15, 2) NOT NULL COMMENT '预投放量',
    DELIVERY_METHOD VARCHAR(50) DEFAULT '按档位扩展投放' COMMENT '投放方法',
    DELIVERY_ETYPE VARCHAR(50) DEFAULT '档位+城乡分类代码' COMMENT '扩展投放类型',
    DELIVERY_AREA VARCHAR(500) COMMENT '投放区域（从7个区域中组合）',
    bz VARCHAR(200) COMMENT '备注',
    UNIQUE KEY uk_cig_code_name (CIG_CODE, CIG_NAME)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试用例表-2099年1月第1周';

-- ================================================================
-- 2. 创建预测结果表（Prediction表）
-- ================================================================
DROP TABLE IF EXISTS cigarette_distribution_prediction_2099_1_1;

CREATE TABLE cigarette_distribution_prediction_2099_1_1 (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    CIG_CODE VARCHAR(20) NOT NULL COMMENT '卷烟代码',
    CIG_NAME VARCHAR(100) NOT NULL COMMENT '卷烟名称',
    YEAR INT NOT NULL DEFAULT 2099 COMMENT '年份',
    MONTH INT NOT NULL DEFAULT 1 COMMENT '月份',
    WEEK_SEQ INT NOT NULL DEFAULT 1 COMMENT '周序号',
    DELIVERY_AREA VARCHAR(200) COMMENT '投放区域',
    DELIVERY_METHOD VARCHAR(50) COMMENT '投放方法',
    DELIVERY_ETYPE VARCHAR(50) COMMENT '扩展投放类型',
    
    -- 30个档位字段
    D30 DECIMAL(15, 2) DEFAULT 0 COMMENT 'D30档位分配',
    D29 DECIMAL(15, 2) DEFAULT 0 COMMENT 'D29档位分配',
    D28 DECIMAL(15, 2) DEFAULT 0 COMMENT 'D28档位分配',
    D27 DECIMAL(15, 2) DEFAULT 0 COMMENT 'D27档位分配',
    D26 DECIMAL(15, 2) DEFAULT 0 COMMENT 'D26档位分配',
    D25 DECIMAL(15, 2) DEFAULT 0 COMMENT 'D25档位分配',
    D24 DECIMAL(15, 2) DEFAULT 0 COMMENT 'D24档位分配',
    D23 DECIMAL(15, 2) DEFAULT 0 COMMENT 'D23档位分配',
    D22 DECIMAL(15, 2) DEFAULT 0 COMMENT 'D22档位分配',
    D21 DECIMAL(15, 2) DEFAULT 0 COMMENT 'D21档位分配',
    D20 DECIMAL(15, 2) DEFAULT 0 COMMENT 'D20档位分配',
    D19 DECIMAL(15, 2) DEFAULT 0 COMMENT 'D19档位分配',
    D18 DECIMAL(15, 2) DEFAULT 0 COMMENT 'D18档位分配',
    D17 DECIMAL(15, 2) DEFAULT 0 COMMENT 'D17档位分配',
    D16 DECIMAL(15, 2) DEFAULT 0 COMMENT 'D16档位分配',
    D15 DECIMAL(15, 2) DEFAULT 0 COMMENT 'D15档位分配',
    D14 DECIMAL(15, 2) DEFAULT 0 COMMENT 'D14档位分配',
    D13 DECIMAL(15, 2) DEFAULT 0 COMMENT 'D13档位分配',
    D12 DECIMAL(15, 2) DEFAULT 0 COMMENT 'D12档位分配',
    D11 DECIMAL(15, 2) DEFAULT 0 COMMENT 'D11档位分配',
    D10 DECIMAL(15, 2) DEFAULT 0 COMMENT 'D10档位分配',
    D9 DECIMAL(15, 2) DEFAULT 0 COMMENT 'D9档位分配',
    D8 DECIMAL(15, 2) DEFAULT 0 COMMENT 'D8档位分配',
    D7 DECIMAL(15, 2) DEFAULT 0 COMMENT 'D7档位分配',
    D6 DECIMAL(15, 2) DEFAULT 0 COMMENT 'D6档位分配',
    D5 DECIMAL(15, 2) DEFAULT 0 COMMENT 'D5档位分配',
    D4 DECIMAL(15, 2) DEFAULT 0 COMMENT 'D4档位分配',
    D3 DECIMAL(15, 2) DEFAULT 0 COMMENT 'D3档位分配',
    D2 DECIMAL(15, 2) DEFAULT 0 COMMENT 'D2档位分配',
    D1 DECIMAL(15, 2) DEFAULT 0 COMMENT 'D1档位分配',
    
    bz VARCHAR(200) COMMENT '备注',
    ACTUAL_DELIVERY DECIMAL(15, 2) COMMENT '实际投放量',
    DEPLOYINFO_CODE VARCHAR(500) COMMENT '投放信息编码',
    
    KEY idx_cig_code_name (CIG_CODE, CIG_NAME),
    KEY idx_time (YEAR, MONTH, WEEK_SEQ),
    KEY idx_delivery_area (DELIVERY_AREA(50))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='算法测试结果表-2099年1月第1周';

-- ================================================================
-- 验证表创建
-- ================================================================
SELECT '✅ 测试表创建完成！' as '状态';
SELECT 
    TABLE_NAME as '表名', 
    TABLE_COMMENT as '说明',
    CREATE_TIME as '创建时间'
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'marketing' 
  AND TABLE_NAME IN ('cigarette_distribution_info_2099_1_1', 'cigarette_distribution_prediction_2099_1_1')
ORDER BY TABLE_NAME;

