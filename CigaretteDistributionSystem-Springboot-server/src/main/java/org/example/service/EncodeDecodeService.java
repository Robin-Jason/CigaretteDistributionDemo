package org.example.service;

import org.example.entity.CigaretteDistributionPredictionData;

import java.math.BigDecimal;
import java.util.List;

/**
 * 语义编码解码服务接口
 * 
 * 【核心功能】
 * 基于标准编码规则实现卷烟投放情况的语义化编码和解码功能，支持智能分组和高效数据传输
 * 
 * 【主要职责】
 * - 智能分组编码：按档位设置自动分组，相同设置的区域合并为单个表达式
 * - 语义解码：将紧凑的编码表达式还原为自然语言描述
 * - 结构化解析：支持编码表达式的完整解析和数据提取
 * - 特定区域查询：快速定位指定区域所属的档位设置组
 * 
 * 【编码规则体系】
 * - **投放方法编码**：A=按档位统一投放，B=按档位扩展投放，C=按需投放
 * - **扩展类型编码**：1=档位+区县，2=档位+市场类型，3=档位+区县+市场类型，4=档位+城乡分类代码，5=档位+业态
 * - **区域编码映射**：
 *   * 档位+区县：城区(1)，丹江(2)，房县(3)，郧西(4)，郧阳(5)，竹山(6)，竹溪(7)
 *   * 档位+市场类型：城网(C)，农网(N)
 *   * 档位+城乡分类代码：主城区(①)，城乡结合区(②)，镇中心区(③)，镇乡接合区(④)，特殊区域(⑤)，乡中心区(⑥)，村庄(⑦)
 *   * 档位+业态：便利店(a)，超市(b)，商场(c)，烟草专业店(d)，娱乐服务类(e)，其他(f)
 * - **档位编码**：采用压缩格式，如"2×5+14×3+14×0"表示前2个档位分配5条，中间14个档位分配3条，后14个档位分配0条
 * 
 * 【编码格式规范】
 * - **A类型格式**：A（档位分配）- 如：A（2×5+28×0）
 * - **B类型格式**：B[扩展类型]（区域编码）（档位分配）- 如：B1（3+4）（2×5+14×3+14×0）
 * - **多表达式**：不同档位设置生成不同表达式，用分号分隔
 * 
 * 【智能分组机制】
 * - 自动识别相同档位设置的区域并合并编码
 * - 不同档位设置的区域生成独立的编码表达式
 * - 支持复杂投放方案的高效表示
 * 
 * 
 * @author Robin
 * @version 4.0 - 智能分组编码与完整编码规则体系版本
 * @since 2025-10-10
 */
public interface EncodeDecodeService {

    /**
     * 智能分组编码生成
     * 
     * 根据档位设置自动分组投放记录，相同档位设置的区域合并为单个编码表达式。
     * 采用完整的编码规则体系，支持A类型（全市统一）和B类型（扩展投放）的紧凑编码。
     * 
     * @param cigCode 卷烟代码（必填）
     * @param cigName 卷烟名称（必填）
     * @param deliveryMethod 投放方法（必填：按档位统一投放/按档位扩展投放/按需投放）
     * @param deliveryEtype 扩展投放类型（B类型必填：档位+区县/档位+市场类型/档位+城乡分类代码/档位+业态）
     * @param cigaretteRecords 该卷烟的所有投放记录（必填，包含各区域的档位分配数据）
     * @return 编码表达式列表，每个表达式对应一组相同档位设置的区域
     * 
     * @example
     * 输入：按档位扩展投放-档位+区县，房县D30:5,D29:3；郧西D30:5,D29:3；竹山D30:2,D29:1
     * 输出：["B1（3+4）（2×5+1×3+27×0）", "B1（6）（1×2+1×1+28×0）"]
     * 解释：房县(3)和郧西(4)有相同档位设置被合并，竹山(6)单独一组
     */
    List<String> encode(String cigCode, String cigName, String deliveryMethod, String deliveryEtype, 
                       List<CigaretteDistributionPredictionData> cigaretteRecords);
    
    /**
     * 兼容性编码方法
     * 
     * 为保持向后兼容性，将智能分组生成的多个编码表达式用分号连接成单个字符串返回。
     * 内部调用encode()方法生成表达式列表，然后使用"; "分隔符连接。
     * 
     * @param cigCode 卷烟代码（必填）
     * @param cigName 卷烟名称（必填）
     * @param deliveryMethod 投放方法（必填）
     * @param deliveryEtype 扩展投放类型（B类型必填）
     * @param cigaretteRecords 该卷烟的所有投放记录（必填）
     * @return 合并的编码表达式字符串，多个表达式用"; "分隔，单个表达式直接返回
     * 
     * @example
     * 返回："B1（3+4）（2×5+1×3+27×0）; B1（6）（1×2+1×1+28×0）"
     */
    String encodeToString(String cigCode, String cigName, String deliveryMethod, String deliveryEtype, 
                         List<CigaretteDistributionPredictionData> cigaretteRecords);
    
    /**
     * 语义解码转换
     * 
     * 将紧凑的编码表达式转换为易读的自然语言描述，便于用户理解投放方案。
     * 支持完整的解码规则，自动识别投放方法、扩展类型、区域编码和档位分配的语义转换。
     * 
     * @param encodedExpression 编码表达式字符串（必填，标准编码格式）
     * @return 自然语言描述字符串，包含投放方案的详细说明
     * 
     * @example
     * 输入："B1（3+4）（2×5+1×3+27×0）"
     * 输出："按档位扩展投放、档位+区县、房县郧西、（2×5+1×3+27×0）"
     * 解释：B1解码为按档位扩展投放+档位+区县，3+4解码为房县+郧西，档位分配保持原格式
     */
    String decode(String encodedExpression);

    /**
     * 特定区域编码查询
     * 
     * 为指定区域快速定位其所属的档位设置组，并生成该组的聚合编码表达式。
     * 自动识别该区域与其他区域的档位设置相似性，返回完整的分组编码表达式。
     * 
     * @param cigCode 卷烟代码（必填）
     * @param cigName 卷烟名称（必填）
     * @param deliveryMethod 投放方法（必填）
     * @param deliveryEtype 扩展投放类型（必填）
     * @param targetArea 目标区域名称（必填）
     * @param allCigaretteRecords 该卷烟的所有投放记录（必填，用于分组分析）
     * @return 该区域所属分组的完整编码表达式，未找到时返回空字符串
     * 
     * @example
     * targetArea="房县"，经分析与郧西有相同档位设置（D30:5,D29:3）
     * -> 返回："B1（3+4）（2×5+1×3+27×0）"
     * 解释：房县(3)和郧西(4)被分在同一组，生成合并的编码表达式
     */
    String encodeForSpecificArea(String cigCode, String cigName, String deliveryMethod, String deliveryEtype, 
                                String targetArea, List<CigaretteDistributionPredictionData> allCigaretteRecords);

    /**
     * 编码表达式结构化解析
     * 
     * 将紧凑的编码表达式完整解析为结构化的投放信息对象，支持程序化处理。
     * 严格按照编码规则体系解析投放方法、扩展类型、区域列表和档位分配等完整信息。
     * 
     * @param encodedExpression 编码表达式字符串（必填，标准编码格式）
     * @return ParsedExpressionData对象，包含解析后的结构化数据
     * @throws IllegalArgumentException 当编码表达式格式错误或无法解析时抛出
     * 
     * @example
     * 输入："B1（3+4）（2×5+1×3+27×0）"
     * 输出：ParsedExpressionData {
     *   deliveryMethod: "按档位扩展投放",
     *   deliveryEtype: "档位+区县",
     *   deliveryAreas: ["房县", "郧西"],
     *   gradeAllocations: [5,5,3,0,0,...] (30个档位数组，前2个为5，第3个为3，其余为0)
     * }
     */
    ParsedExpressionData parseEncodedExpression(String encodedExpression);

    /**
     * 解析后的表达式数据类
     * 
     * 用于封装编码表达式解析后的结构化数据，支持程序化访问和处理。
     * 包含投放方案的所有关键信息：投放方法、扩展类型、区域列表和完整的档位分配数组。
     * 
     * 【数据结构】
     * - deliveryMethod: 投放方法（按档位统一投放/按档位扩展投放）
     * - deliveryEtype: 扩展投放类型（档位+区县/档位+市场类型等）
     * - deliveryAreas: 投放区域列表（该表达式涉及的所有区域）
     * - gradeAllocations: 档位分配数组（30个档位的分配值，从D30到D1）
     * 
     * 【使用场景】
     * - 编码表达式解析：将字符串转换为结构化对象便于处理
     * - 数据验证：验证编码表达式的完整性和正确性
     * - 业务逻辑：为投放计算和数据更新提供标准化数据格式
     */
    class ParsedExpressionData {
        private String deliveryMethod;
        private String deliveryEtype;
        private List<String> deliveryAreas;
        private BigDecimal[] gradeAllocations;
        
        public String getDeliveryMethod() { return deliveryMethod; }
        public void setDeliveryMethod(String deliveryMethod) { this.deliveryMethod = deliveryMethod; }
        
        public String getDeliveryEtype() { return deliveryEtype; }
        public void setDeliveryEtype(String deliveryEtype) { this.deliveryEtype = deliveryEtype; }
        
        public List<String> getDeliveryAreas() { return deliveryAreas; }
        public void setDeliveryAreas(List<String> deliveryAreas) { this.deliveryAreas = deliveryAreas; }
        
        public BigDecimal[] getGradeAllocations() { return gradeAllocations; }
        public void setGradeAllocations(BigDecimal[] gradeAllocations) { this.gradeAllocations = gradeAllocations; }
    }
}