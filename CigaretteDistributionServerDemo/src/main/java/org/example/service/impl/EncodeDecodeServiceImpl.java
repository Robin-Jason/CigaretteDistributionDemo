package org.example.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.entity.CigaretteDistributionPredictionData;
import org.example.service.EncodeDecodeService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 语义编码解码服务实现
 * 根据编码规则表实现卷烟投放情况的编码和解码功能
 */
@Slf4j
@Service
public class EncodeDecodeServiceImpl implements EncodeDecodeService {

    // 投放类型编码映射
    private static final Map<String, String> DELIVERY_METHOD_CODES = new HashMap<String, String>() {{
        put("按档位统一投放", "A");
        put("按档位投放", "A");  // 等价于"按档位统一投放"
        put("按档位扩展投放", "B");
        put("按需投放", "C");
    }};

    // 扩展投放类型编码映射
    private static final Map<String, String> DELIVERY_ETYPE_CODES = new HashMap<String, String>() {{
        put("档位+区县", "1");
        put("档位+市场类型", "2");
        put("档位+区县+市场类型", "3");
        put("档位+城乡分类代码", "4");
        put("档位+业态", "5");
    }};

    // 区域编码映射 - 档位+区县
    private static final Map<String, String> COUNTY_CODES = new HashMap<String, String>() {{
        put("城区", "1");
        put("丹江", "2");
        put("房县", "3");
        put("郧西", "4");
        put("郧阳", "5");
        put("竹山", "6");
        put("竹溪", "7");
    }};

    // 区域编码映射 - 档位+市场类型
    private static final Map<String, String> MARKET_CODES = new HashMap<String, String>() {{
        put("城网", "C");
        put("农网", "N");
    }};

    // 区域编码映射 - 档位+城乡分类代码
    private static final Map<String, String> URBAN_RURAL_CODES = new HashMap<String, String>() {{
        put("主城区", "①");
        put("城乡结合区", "②");
        put("镇中心区", "③");
        put("镇乡接合区", "④");
        put("特殊区域", "⑤");
        put("乡中心区", "⑥");
        put("村庄", "⑦");
    }};

    // 区域编码映射 - 档位+业态
    private static final Map<String, String> BUSINESS_FORMAT_CODES = new HashMap<String, String>() {{
        put("便利店", "a");
        put("超市", "b");
        put("商场", "c");
        put("烟草专业店", "d");
        put("娱乐服务类", "e");
        put("其他", "f");
    }};

    // 反向映射 - 用于解码
    private static final Map<String, String> REVERSE_DELIVERY_METHOD_CODES = reverseMap(DELIVERY_METHOD_CODES);
    private static final Map<String, String> REVERSE_DELIVERY_ETYPE_CODES = reverseMap(DELIVERY_ETYPE_CODES);
    private static final Map<String, String> REVERSE_COUNTY_CODES = reverseMap(COUNTY_CODES);
    private static final Map<String, String> REVERSE_MARKET_CODES = reverseMap(MARKET_CODES);
    private static final Map<String, String> REVERSE_URBAN_RURAL_CODES = reverseMap(URBAN_RURAL_CODES);
    private static final Map<String, String> REVERSE_BUSINESS_FORMAT_CODES = reverseMap(BUSINESS_FORMAT_CODES);

    /**
     * 为指定卷烟的所有投放记录生成编码化表达
     * 
     * @param cigCode 卷烟代码
     * @param cigName 卷烟名称
     * @param deliveryMethod 投放方法
     * @param deliveryEtype 扩展投放类型
     * @param cigaretteRecords 该卷烟的所有投放记录
     * @return 编码化表达字符串列表，每个字符串对应一组相同档位设置的区域
     */
    @Override
    public List<String> encode(String cigCode, String cigName, String deliveryMethod, String deliveryEtype, 
                        List<CigaretteDistributionPredictionData> cigaretteRecords) {
        
        log.debug("开始编码卷烟: {} - {}, 投放方法: {}, 扩展投放类型: {}, 记录数: {}", 
                 cigCode, cigName, deliveryMethod, deliveryEtype, cigaretteRecords.size());

        if (cigaretteRecords == null || cigaretteRecords.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // 第一步：投放类型编码
            String deliveryMethodCode = DELIVERY_METHOD_CODES.get(deliveryMethod);
            if (deliveryMethodCode == null) {
                log.warn("未知的投放方法: {}", deliveryMethod);
                return new ArrayList<>();
            }

            // 第二步：扩展投放类型编码（只有B才需要）
            String etypeCode = "";
            if ("B".equals(deliveryMethodCode)) {
                etypeCode = DELIVERY_ETYPE_CODES.get(deliveryEtype);
                if (etypeCode == null) {
                    log.warn("未知的扩展投放类型: {}", deliveryEtype);
                    return new ArrayList<>();
                }
            }

            // 第三步：按档位设置分组生成多个编码表达式
            List<String> encodedExpressions = generateMultipleEncodedExpressions(
                deliveryMethodCode, etypeCode, deliveryEtype, cigaretteRecords);

            log.debug("卷烟 {} - {} 编码完成，生成 {} 个编码表达式", cigCode, cigName, encodedExpressions.size());
            return encodedExpressions;

        } catch (Exception e) {
            log.error("编码卷烟 {} - {} 时发生错误", cigCode, cigName, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 编码方法（兼容性方法，返回合并的编码表达式字符串）
     * 为了保持向后兼容，将多个编码表达式用分号连接
     */
    @Override
    public String encodeToString(String cigCode, String cigName, String deliveryMethod, String deliveryEtype, 
                                List<CigaretteDistributionPredictionData> cigaretteRecords) {
        List<String> expressions = encode(cigCode, cigName, deliveryMethod, deliveryEtype, cigaretteRecords);
        
        if (expressions.isEmpty()) {
            return "";
        }
        
        // 如果只有一个编码表达式，直接返回
        if (expressions.size() == 1) {
            return expressions.get(0);
        }
        
        // 多个编码表达式用分号连接
        return String.join("; ", expressions);
    }
    
    /**
     * 解码编码化表达为自然语言描述
     * 
     * @param encodedExpression 编码化表达字符串
     * @return 解码化表达字符串
     */
    @Override
    public String decode(String encodedExpression) {
        log.debug("开始解码: {}", encodedExpression);

        if (encodedExpression == null || encodedExpression.trim().isEmpty()) {
            return "";
        }

        try {
            StringBuilder decodedResult = new StringBuilder();
            
            // 解析编码字符串
            String expression = encodedExpression.trim();
            int currentPos = 0;

            // 第一步：解码投放类型
            if (currentPos < expression.length()) {
                String deliveryMethodCode = expression.substring(currentPos, currentPos + 1);
                String deliveryMethod = REVERSE_DELIVERY_METHOD_CODES.get(deliveryMethodCode);
                if (deliveryMethod != null) {
                    decodedResult.append(deliveryMethod);
                    currentPos++;
                } else {
                    log.warn("无法解码投放方法代码: {}", deliveryMethodCode);
                    return "解码失败：未知的投放方法代码";
                }
            }

            // 第二步：解码扩展投放类型（如果是B类型）
            String etypeCode = null;
            if (currentPos < expression.length() && "B".equals(expression.substring(0, 1))) {
                etypeCode = expression.substring(currentPos, currentPos + 1);
                String deliveryEtype = REVERSE_DELIVERY_ETYPE_CODES.get(etypeCode);
                if (deliveryEtype != null) {
                    decodedResult.append("、").append(deliveryEtype);
                    currentPos++;
                } else {
                    log.warn("无法解码扩展投放类型代码: {}", etypeCode);
                    return "解码失败：未知的扩展投放类型代码";
                }
            }

            // 第三步和第四步：解码区域和档位投放量（在括号中）
            String remainingExpression = expression.substring(currentPos);
            
            // 确定扩展投放类型用于区域解码
            String deliveryEtypeForDecode = null;
            if (etypeCode != null) {
                deliveryEtypeForDecode = REVERSE_DELIVERY_ETYPE_CODES.get(etypeCode);
            }
            
            String regionDecoded = decodeRegionsFromExpressionWithEtype(remainingExpression, deliveryEtypeForDecode);
            String allocationDecoded = decodeAllocationsFromExpression(remainingExpression);

            if (!regionDecoded.isEmpty()) {
                decodedResult.append("、").append(regionDecoded);
            }
            
            if (!allocationDecoded.isEmpty()) {
                decodedResult.append("、").append(allocationDecoded);
            }

            String result = decodedResult.toString();
            log.debug("解码完成: {} -> {}", encodedExpression, result);
            return result;

        } catch (Exception e) {
            log.error("解码表达式 {} 时发生错误", encodedExpression, e);
            return "解码失败：" + e.getMessage();
        }
    }

    /**
     * 为特定区域的记录生成编码表达式
     * 根据该区域所属的档位设置组，返回该组的聚合编码表达式
     */
    @Override
    public String encodeForSpecificArea(String cigCode, String cigName, String deliveryMethod, String deliveryEtype, 
                                       String targetArea, List<CigaretteDistributionPredictionData> allCigaretteRecords) {
        if (targetArea == null || allCigaretteRecords == null || allCigaretteRecords.isEmpty()) {
            return "";
        }
        
        // 第一步：编码投放类型
        String deliveryMethodCode = DELIVERY_METHOD_CODES.get(deliveryMethod);
        if (deliveryMethodCode == null) {
            log.warn("无法编码投放类型: {}", deliveryMethod);
            return "";
        }
        
        // 第二步：编码扩展投放类型
        String etypeCode = "";
        if ("B".equals(deliveryMethodCode)) {
            etypeCode = DELIVERY_ETYPE_CODES.get(deliveryEtype);
            if (etypeCode == null) {
                log.warn("无法编码扩展投放类型: {}", deliveryEtype);
                return "";
            }
        }
        
        // 第三步：按档位设置分组，找到目标区域所属的组
        Map<String, List<CigaretteDistributionPredictionData>> gradeGroups = groupRecordsByGradeSettings(allCigaretteRecords);
        
        for (Map.Entry<String, List<CigaretteDistributionPredictionData>> entry : gradeGroups.entrySet()) {
            List<CigaretteDistributionPredictionData> groupRecords = entry.getValue();
            
            // 检查目标区域是否在这个组中
            boolean targetAreaInThisGroup = groupRecords.stream()
                .anyMatch(record -> targetArea.equals(record.getDeliveryArea()));
                
            if (targetAreaInThisGroup) {
                // 找到了目标区域所属的组，生成该组的编码表达式
                StringBuilder encodedResult = new StringBuilder();
                encodedResult.append(deliveryMethodCode).append(etypeCode);
                
                // 编码该组的区域信息
                String regionCodes = encodeRegionsForGroup(deliveryEtype, groupRecords);
                if (!regionCodes.isEmpty()) {
                    encodedResult.append("（").append(regionCodes).append("）");
                }
                
                // 编码该组的档位投放量信息
                String gradeAllocationCodes = encodeGradeSequencesForGroup(groupRecords);
                if (!gradeAllocationCodes.isEmpty()) {
                    encodedResult.append("（").append(gradeAllocationCodes).append("）");
                }
                
                return encodedResult.toString();
            }
        }
        
        // 如果没有找到目标区域，返回空字符串
        log.warn("未找到区域 {} 在卷烟 {} - {} 的档位设置组中", targetArea, cigCode, cigName);
        return "";
    }

    /**
     * 解析编码表达式为投放信息
     * 将编码表达式解析为具体的投放类型、区域列表和档位分配
     */
    @Override
    public ParsedExpressionData parseEncodedExpression(String encodedExpression) {
        log.debug("开始解析编码表达式: {}", encodedExpression);
        
        try {
            if (encodedExpression == null || encodedExpression.trim().isEmpty()) {
                throw new IllegalArgumentException("编码表达式不能为空");
            }
            
            String expression = encodedExpression.trim();
            ParsedExpressionData result = new ParsedExpressionData();
            int currentPos = 0;

            // 第一步：解析投放类型
            if (currentPos < expression.length()) {
                String deliveryMethodCode = expression.substring(currentPos, currentPos + 1);
                String deliveryMethod = REVERSE_DELIVERY_METHOD_CODES.get(deliveryMethodCode);
                if (deliveryMethod != null) {
                    result.setDeliveryMethod(deliveryMethod);
                    currentPos++;
                } else {
                    throw new IllegalArgumentException("无法解析投放方法代码: " + deliveryMethodCode);
                }
            }

            // 第二步：解析扩展投放类型（如果是B类型）
            String etypeCode = null;
            if (currentPos < expression.length() && "B".equals(expression.substring(0, 1))) {
                etypeCode = expression.substring(currentPos, currentPos + 1);
                String deliveryEtype = REVERSE_DELIVERY_ETYPE_CODES.get(etypeCode);
                if (deliveryEtype != null) {
                    result.setDeliveryEtype(deliveryEtype);
                    currentPos++;
                } else {
                    throw new IllegalArgumentException("无法解析扩展投放类型代码: " + etypeCode);
                }
            } else {
                result.setDeliveryEtype("NULL");
            }

            // 第三步：解析区域和档位信息
            String remainingExpression = expression.substring(currentPos);
            
            // 确定扩展投放类型用于区域解码
            String deliveryEtypeForDecode = null;
            if (etypeCode != null) {
                deliveryEtypeForDecode = REVERSE_DELIVERY_ETYPE_CODES.get(etypeCode);
            }
            
            // 解析区域列表
            List<String> regions = parseRegionsFromExpression(remainingExpression, deliveryEtypeForDecode);
            result.setDeliveryAreas(regions);
            
            // 解析档位分配（传递完整表达式，而不是剩余部分）
            BigDecimal[] gradeAllocations = parseGradeAllocationsFromExpression(expression);
            result.setGradeAllocations(gradeAllocations);
            
            log.debug("解析完成: 投放类型={}, 扩展类型={}, 区域数量={}, 档位分配={}条", 
                     result.getDeliveryMethod(), result.getDeliveryEtype(), 
                     result.getDeliveryAreas().size(), gradeAllocations.length);
            
            return result;
            
        } catch (Exception e) {
            log.error("解析编码表达式失败: {}", encodedExpression, e);
            throw new IllegalArgumentException("编码表达式解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从表达式中解析区域列表
     */
    private List<String> parseRegionsFromExpression(String expression, String deliveryEtype) {
        List<String> regions = new ArrayList<>();
        
        // 查找第一个括号中的内容（区域编码）
        int firstBracketStart = expression.indexOf("（");
        if (firstBracketStart == -1) {
            // 如果是A类型（全市统一投放），没有区域编码
            regions.add("全市");
            return regions;
        }
        
        int firstBracketEnd = expression.indexOf("）", firstBracketStart);
        if (firstBracketEnd == -1) {
            throw new IllegalArgumentException("编码表达式格式错误：缺少区域编码结束括号");
        }
        
        String regionCodes = expression.substring(firstBracketStart + 1, firstBracketEnd);
        Map<String, String> reverseCodeMap = getReverseRegionCodeMap(deliveryEtype);
        
        if (reverseCodeMap == null) {
            // 对于没有扩展投放类型的情况（如A类型），返回"全市"
            log.debug("无法确定区域编码映射类型: {}, 判断为全市统一投放", deliveryEtype);
            regions.add("全市");
            return regions;
        }
        
        // 解码区域编码为区域名称
        String[] codes = regionCodes.split("\\+");
        for (String code : codes) {
            String regionName = reverseCodeMap.get(code.trim());
            if (regionName != null) {
                regions.add(regionName);
            } else {
                throw new IllegalArgumentException("无法解码区域编码: " + code.trim());
            }
        }
        
        return regions;
    }

    /**
     * 从表达式中解析档位分配
     */
    private BigDecimal[] parseGradeAllocationsFromExpression(String expression) {
        BigDecimal[] grades = new BigDecimal[30]; // 30个档位
        Arrays.fill(grades, BigDecimal.ZERO); // 初始化为0
        
        // 查找档位分配括号（可能是第二个括号，或者对于A类型是第一个括号）
        int allocationBracketStart = -1;
        int bracketCount = 0;
        
        for (int i = 0; i < expression.length(); i++) {
            if (expression.charAt(i) == '（') {
                bracketCount++;
                // 对于A类型（全市统一投放），档位分配在第一个括号
                // 对于B类型（扩展投放），档位分配在第二个括号
                if ((bracketCount == 1 && expression.startsWith("A")) || 
                    (bracketCount == 2 && expression.startsWith("B"))) {
                    allocationBracketStart = i;
                    break;
                }
            }
        }
        
        if (allocationBracketStart == -1) {
            throw new IllegalArgumentException("编码表达式格式错误：找不到档位分配信息");
        }
        
        int allocationBracketEnd = expression.indexOf("）", allocationBracketStart);
        if (allocationBracketEnd == -1) {
            throw new IllegalArgumentException("编码表达式格式错误：缺少档位分配结束括号");
        }
        
        String allocationCode = expression.substring(allocationBracketStart + 1, allocationBracketEnd);
        
        // 解析档位分配编码，例如：2×2+14×1+14×0
        String[] segments = allocationCode.split("\\+");
        int currentGradeIndex = 0;
        
        for (String segment : segments) {
            String[] parts = segment.split("×");
            if (parts.length != 2) {
                throw new IllegalArgumentException("档位分配编码格式错误: " + segment);
            }
            
            try {
                int count = Integer.parseInt(parts[0].trim());
                BigDecimal value = new BigDecimal(parts[1].trim());
                
                // 填充对应数量的档位
                for (int i = 0; i < count && currentGradeIndex < 30; i++) {
                    grades[currentGradeIndex++] = value;
                }
                
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("档位分配编码数值格式错误: " + segment, e);
            }
        }
        
        return grades;
    }


    // ==================== 私有辅助方法 ====================


    /**
     * 将连续档位进行压缩编码（包含所有30个档位，包括0值）
     */
    private String encodeGradeSequences(BigDecimal[] grades) {
        List<String> sequences = new ArrayList<>();
        
        int start = 0;
        while (start < grades.length) {
            BigDecimal currentValue = grades[start];
            // 将null值统一处理为0
            if (currentValue == null) {
                currentValue = BigDecimal.ZERO;
            }

            int end = start;
            // 找到连续且值相同的档位（包括0值）
            while (end < grades.length) {
                BigDecimal endValue = grades[end];
                if (endValue == null) {
                    endValue = BigDecimal.ZERO;
                }
                
                if (endValue.compareTo(currentValue) == 0) {
                    end++;
                } else {
                    break;
                }
            }

            int count = end - start;
            if (count > 1) {
                sequences.add(count + "×" + formatAsInteger(currentValue));
            } else {
                sequences.add("1×" + formatAsInteger(currentValue));
            }
            
            start = end;
        }

        return String.join("+", sequences);
    }

    /**
     * 提取档位数据数组
     */
    private BigDecimal[] extractGrades(CigaretteDistributionPredictionData record) {
        return new BigDecimal[] {
            record.getD30(), record.getD29(), record.getD28(), record.getD27(), record.getD26(),
            record.getD25(), record.getD24(), record.getD23(), record.getD22(), record.getD21(),
            record.getD20(), record.getD19(), record.getD18(), record.getD17(), record.getD16(),
            record.getD15(), record.getD14(), record.getD13(), record.getD12(), record.getD11(),
            record.getD10(), record.getD9(), record.getD8(), record.getD7(), record.getD6(),
            record.getD5(), record.getD4(), record.getD3(), record.getD2(), record.getD1()
        };
    }

    /**
     * 根据扩展投放类型获取区域编码映射
     */
    private Map<String, String> getRegionCodeMap(String deliveryEtype) {
        if (deliveryEtype == null) {
            return null;
        }

        switch (deliveryEtype) {
            case "档位+区县":
                return COUNTY_CODES;
            case "档位+市场类型":
                return MARKET_CODES;
            case "档位+城乡分类代码":
                return URBAN_RURAL_CODES;
            case "档位+业态":
                return BUSINESS_FORMAT_CODES;
            default:
                return null;
        }
    }

    /**
     * 从表达式中解码区域信息（带指定的扩展投放类型）
     */
    private String decodeRegionsFromExpressionWithEtype(String expression, String deliveryEtype) {
        // 查找第一个括号中的内容（区域编码）
        int firstBracketStart = expression.indexOf("（");
        if (firstBracketStart == -1) return "";
        
        int firstBracketEnd = expression.indexOf("）", firstBracketStart);
        if (firstBracketEnd == -1) return "";
        
        String regionCodes = expression.substring(firstBracketStart + 1, firstBracketEnd);
        
        Map<String, String> reverseCodeMap = getReverseRegionCodeMap(deliveryEtype);
        
        if (reverseCodeMap == null) {
            return regionCodes; // 如果无法确定类型，返回原编码
        }
        
        // 解码区域编码为区域名称
        StringBuilder decodedRegions = new StringBuilder();
        String[] codes = regionCodes.split("\\+");
        
        for (String code : codes) {
            String regionName = reverseCodeMap.get(code.trim());
            if (regionName != null) {
                decodedRegions.append(regionName);
            } else {
                // 如果无法解码，保持原编码
                decodedRegions.append(code.trim());
            }
        }
        
        String result = decodedRegions.toString();
        return result;
    }


    /**
     * 从表达式中解码档位投放量信息
     */
    private String decodeAllocationsFromExpression(String expression) {
        // 查找第二个括号中的内容（档位投放量编码）
        int firstBracketEnd = expression.indexOf("）");
        if (firstBracketEnd == -1) return "";
        
        int secondBracketStart = expression.indexOf("（", firstBracketEnd);
        if (secondBracketStart == -1) return "";
        
        int secondBracketEnd = expression.indexOf("）", secondBracketStart);
        if (secondBracketEnd == -1) return "";
        
        String allocationCodes = expression.substring(secondBracketStart + 1, secondBracketEnd);
        // 投放量编码无需解码，直接返回
        return "（" + allocationCodes + "）";
    }
    
    /**
     * 从编码表达式中确定扩展投放类型
     */
    private String determineDeliveryEtypeFromExpression(String expression) {
        if (expression == null || expression.length() < 2) {
            return null;
        }
        
        // 如果不是B开头，则无扩展投放类型
        if (!expression.startsWith("B")) {
            return null;
        }
        
        // 获取第二个字符作为扩展投放类型编码
        String etypeCode = expression.substring(1, 2);
        return REVERSE_DELIVERY_ETYPE_CODES.get(etypeCode);
    }
    
    /**
     * 根据扩展投放类型获取反向区域编码映射
     */
    private Map<String, String> getReverseRegionCodeMap(String deliveryEtype) {
        if (deliveryEtype == null) {
            return null;
        }

        switch (deliveryEtype) {
            case "档位+区县":
                return REVERSE_COUNTY_CODES;
            case "档位+市场类型":
                return REVERSE_MARKET_CODES;
            case "档位+城乡分类代码":
                return REVERSE_URBAN_RURAL_CODES;
            case "档位+业态":
                return REVERSE_BUSINESS_FORMAT_CODES;
            default:
                return null;
        }
    }

    /**
     * 创建反向映射
     */
    private static Map<String, String> reverseMap(Map<String, String> originalMap) {
        Map<String, String> reversedMap = new HashMap<>();
        for (Map.Entry<String, String> entry : originalMap.entrySet()) {
            reversedMap.put(entry.getValue(), entry.getKey());
        }
        return reversedMap;
    }
    
    /**
     * 将BigDecimal格式化为整数字符串
     * 如果是小数，则四舍五入到最近的整数
     */
    private String formatAsInteger(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        // 使用 intValue() 直接取整数部分，或者使用 setScale(0, RoundingMode.HALF_UP) 四舍五入
        return String.valueOf(value.setScale(0, java.math.RoundingMode.HALF_UP).intValue());
    }
    
    /**
     * 按档位设置分组生成多个编码表达式
     * 相同档位设置的区域聚合成一个编码表达式，不同档位设置的区域生成不同的编码表达式
     */
    private List<String> generateMultipleEncodedExpressions(String deliveryMethodCode, String etypeCode, 
                                                           String deliveryEtype, List<CigaretteDistributionPredictionData> records) {
        List<String> expressions = new ArrayList<>();
        
        // 按档位设置对区域进行分组
        Map<String, List<CigaretteDistributionPredictionData>> gradeGroups = groupRecordsByGradeSettings(records);
        
        for (Map.Entry<String, List<CigaretteDistributionPredictionData>> entry : gradeGroups.entrySet()) {
            List<CigaretteDistributionPredictionData> groupRecords = entry.getValue();
            
            StringBuilder encodedResult = new StringBuilder();
            
            // 添加投放类型编码
            encodedResult.append(deliveryMethodCode).append(etypeCode);
            
            // 添加区域编码（聚合相同档位设置的区域）
            String regionCodes = encodeRegionsForGroup(deliveryEtype, groupRecords);
            if (!regionCodes.isEmpty()) {
                encodedResult.append("（").append(regionCodes).append("）");
            }
            
            // 添加档位投放量编码
            String gradeAllocationCodes = encodeGradeSequencesForGroup(groupRecords);
            if (!gradeAllocationCodes.isEmpty()) {
                encodedResult.append("（").append(gradeAllocationCodes).append("）");
            }
            
            expressions.add(encodedResult.toString());
        }
        
        return expressions;
    }
    
    /**
     * 按档位设置对区域进行分组
     * 档位设置完全相同的区域归为一组
     */
    private Map<String, List<CigaretteDistributionPredictionData>> groupRecordsByGradeSettings(List<CigaretteDistributionPredictionData> records) {
        Map<String, List<CigaretteDistributionPredictionData>> groups = new HashMap<>();
        
        for (CigaretteDistributionPredictionData record : records) {
            String gradePattern = generateGradePattern(record);
            groups.computeIfAbsent(gradePattern, k -> new ArrayList<>()).add(record);
        }
        
        return groups;
    }
    
    /**
     * 生成区域的档位设置模式字符串，用于分组
     */
    private String generateGradePattern(CigaretteDistributionPredictionData record) {
        BigDecimal[] grades = extractGrades(record);
        StringBuilder pattern = new StringBuilder();
        
        for (int i = 0; i < grades.length; i++) {
            if (i > 0) pattern.append(",");
            pattern.append(grades[i] != null ? formatAsInteger(grades[i]) : "0");
        }
        
        return pattern.toString();
    }
    
    /**
     * 为分组编码区域信息
     */
    private String encodeRegionsForGroup(String deliveryEtype, List<CigaretteDistributionPredictionData> groupRecords) {
        if (groupRecords == null || groupRecords.isEmpty()) {
            return "";
        }

        // 获取分组中所有区域并去重
        Set<String> regions = groupRecords.stream()
                .map(CigaretteDistributionPredictionData::getDeliveryArea)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, String> codeMap = getRegionCodeMap(deliveryEtype);
        if (codeMap == null) {
            return "";
        }

        List<String> regionCodes = new ArrayList<>();
        for (String region : regions) {
            String code = codeMap.get(region);
            if (code != null) {
                regionCodes.add(code);
            }
        }

        // 排序并用+连接
        Collections.sort(regionCodes);
        return String.join("+", regionCodes);
    }
    
    /**
     * 为分组编码档位投放量信息
     */
    private String encodeGradeSequencesForGroup(List<CigaretteDistributionPredictionData> groupRecords) {
        if (groupRecords == null || groupRecords.isEmpty()) {
            return "";
        }
        
        // 使用第一个记录的档位设置（因为同组内档位设置相同）
        CigaretteDistributionPredictionData representative = groupRecords.get(0);
        BigDecimal[] grades = extractGrades(representative);
        return encodeGradeSequences(grades);
    }
}
