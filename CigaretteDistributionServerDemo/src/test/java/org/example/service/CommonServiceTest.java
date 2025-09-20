package org.example.service;

import org.example.entity.DemoTestAdvData;
import org.example.entity.DemoTestData;
import org.example.repository.DemoTestAdvDataRepository;
import org.example.repository.DemoTestDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * CommonService测试类
 * 测试CommonService的各项功能
 */
@DisplayName("CommonService功能测试")
public class CommonServiceTest {

    @Mock
    private DemoTestAdvDataRepository advDataRepository;

    @Mock
    private DemoTestDataRepository testDataRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private CommonService commonService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("测试档位+城乡分类代码投放类型的预投放量数据获取")
    void testGetAdvDataForUrbanRuralClassification() {
        // 准备测试数据
        Integer year = 2024;
        Integer month = 1;
        Integer weekSeq = 1;
        String deliveryEtype = "档位+城乡分类代码";

        // 模拟数据库返回的预投放量数据
        List<DemoTestAdvData> mockAdvData = createMockAdvData(deliveryEtype);
        
        when(advDataRepository.findByYearAndMonthAndWeekSeq(year, month, weekSeq))
            .thenReturn(mockAdvData);

        // 执行测试
        List<DemoTestAdvData> result = commonService.getAdvDataByDeliveryType(
            deliveryEtype, year, month, weekSeq);

        // 验证结果
        assertNotNull(result, "返回结果不应为null");
        assertEquals(2, result.size(), "应该返回2条匹配的数据");
        
        // 验证数据内容
        DemoTestAdvData firstResult = result.get(0);
        assertEquals("档位+城乡分类代码", firstResult.getDeliveryEtype(), 
            "投放类型应为'档位+城乡分类代码'");
        assertEquals("C001", firstResult.getCigCode(), "卷烟代码应正确");
        assertEquals("测试卷烟A", firstResult.getCigName(), "卷烟名称应正确");
        
        // 验证方法调用
        verify(advDataRepository, times(1))
            .findByYearAndMonthAndWeekSeq(year, month, weekSeq);
        
        System.out.println("✅ 档位+城乡分类代码预投放量数据获取测试通过");
        System.out.println("📊 获取到 " + result.size() + " 条预投放量数据");
    }

    @Test
    @DisplayName("测试批量写入DemoTestData数据功能")
    void testBatchInsertTestData() {
        // 准备测试数据
        List<DemoTestData> testDataList = createMockTestDataList();
        
        // 模拟JPA Repository的saveAll方法
        when(testDataRepository.saveAll(testDataList))
            .thenReturn(testDataList);

        // 执行测试
        Map<String, Object> result = commonService.batchInsertTestData(testDataList);

        // 验证结果
        assertNotNull(result, "返回结果不应为null");
        assertTrue((Boolean) result.get("success"), "写入应该成功");
        assertEquals(3, result.get("totalCount"), "总记录数应为3");
        assertEquals(3, result.get("successCount"), "成功记录数应为3");
        assertEquals(0, result.get("failCount"), "失败记录数应为0");
        
        // 验证方法调用
        verify(testDataRepository, times(1)).saveAll(testDataList);
        
        System.out.println("✅ 批量写入DemoTestData数据功能测试通过");
        System.out.println("📝 成功写入 " + result.get("successCount") + " 条测试数据");
    }

    @Test
    @DisplayName("测试批量写入空数据列表")
    void testBatchInsertEmptyList() {
        // 执行测试 - 传入空列表
        Map<String, Object> result = commonService.batchInsertTestData(new ArrayList<>());

        // 验证结果
        assertNotNull(result, "返回结果不应为null");
        assertTrue((Boolean) result.get("success"), "空列表写入应该成功");
        assertEquals(0, result.get("totalCount"), "总记录数应为0");
        assertEquals(0, result.get("successCount"), "成功记录数应为0");
        assertEquals(0, result.get("failCount"), "失败记录数应为0");
        assertEquals("没有数据需要写入", result.get("message"), "消息应正确");
        
        // 验证方法没有被调用
        verify(testDataRepository, never()).saveAll(any());
        
        System.out.println("✅ 批量写入空数据列表测试通过");
    }

    @Test
    @DisplayName("测试删除指定条件的测试数据功能")
    void testDeleteSpecificTestData() {
        // 准备测试数据
        String cigCode = "C001";
        String cigName = "测试卷烟A";
        Integer year = 2024;
        Integer month = 1;
        Integer weekSeq = 1;
        String deliveryArea = "城镇";
        
        // 创建模拟的现有数据
        List<DemoTestData> existingData = createMockTestDataForDelete(cigCode, cigName, year, month, weekSeq, deliveryArea);
        
        // 模拟Repository查询方法
        when(testDataRepository.findByYearAndMonthAndWeekSeqAndCigCodeAndDeliveryAreaIn(
            year, month, weekSeq, cigCode, Arrays.asList(deliveryArea)))
            .thenReturn(existingData);
        
        // 执行测试
        Map<String, Object> result = commonService.deleteSpecificTestData(
            cigCode, cigName, year, month, weekSeq, deliveryArea);

        // 验证结果
        assertNotNull(result, "返回结果不应为null");
        assertTrue((Boolean) result.get("success"), "删除应该成功");
        assertEquals(1, result.get("deletedCount"), "删除记录数应为1");
        assertTrue(result.get("message").toString().contains("成功删除"), "消息应包含成功信息");
        
        // 验证方法调用
        verify(testDataRepository, times(1)).findByYearAndMonthAndWeekSeqAndCigCodeAndDeliveryAreaIn(
            year, month, weekSeq, cigCode, Arrays.asList(deliveryArea));
        verify(testDataRepository, times(1)).deleteAll(any());
        
        System.out.println("✅ 删除指定条件的测试数据功能测试通过");
        System.out.println("🗑️ 成功删除 " + result.get("deletedCount") + " 条记录");
    }

    @Test
    @DisplayName("测试删除功能的参数验证")
    void testDeleteSpecificTestDataValidation() {
        // 测试卷烟代码为空的情况
        Map<String, Object> result1 = commonService.deleteSpecificTestData(
            null, "测试卷烟A", 2024, 1, 1, "城镇");
        assertFalse((Boolean) result1.get("success"), "卷烟代码为空时应该失败");
        assertEquals("卷烟代码不能为空", result1.get("message"), "错误消息应正确");
        
        // 测试卷烟名称为空的情况
        Map<String, Object> result2 = commonService.deleteSpecificTestData(
            "C001", "", 2024, 1, 1, "城镇");
        assertFalse((Boolean) result2.get("success"), "卷烟名称为空时应该失败");
        assertEquals("卷烟名称不能为空", result2.get("message"), "错误消息应正确");
        
        // 测试时间参数为空的情况
        Map<String, Object> result3 = commonService.deleteSpecificTestData(
            "C001", "测试卷烟A", null, 1, 1, "城镇");
        assertFalse((Boolean) result3.get("success"), "时间参数为空时应该失败");
        assertEquals("时间参数不能为空", result3.get("message"), "错误消息应正确");
        
        // 测试投放区域为空的情况
        Map<String, Object> result4 = commonService.deleteSpecificTestData(
            "C001", "测试卷烟A", 2024, 1, 1, null);
        assertFalse((Boolean) result4.get("success"), "投放区域为空时应该失败");
        assertEquals("投放区域不能为空", result4.get("message"), "错误消息应正确");
        
        System.out.println("✅ 删除功能参数验证测试通过");
    }

    @Test
    @DisplayName("测试删除功能未找到匹配记录的情况")
    void testDeleteSpecificTestDataNoMatch() {
        // 准备测试数据
        String cigCode = "C001";
        String cigName = "测试卷烟A";
        Integer year = 2024;
        Integer month = 1;
        Integer weekSeq = 1;
        String deliveryArea = "城镇";
        
        // 模拟没有找到匹配记录
        when(testDataRepository.findByYearAndMonthAndWeekSeqAndCigCodeAndDeliveryAreaIn(
            year, month, weekSeq, cigCode, Arrays.asList(deliveryArea)))
            .thenReturn(new ArrayList<>());
        
        // 执行测试
        Map<String, Object> result = commonService.deleteSpecificTestData(
            cigCode, cigName, year, month, weekSeq, deliveryArea);

        // 验证结果
        assertNotNull(result, "返回结果不应为null");
        assertTrue((Boolean) result.get("success"), "无匹配记录时也应该返回成功");
        assertEquals(0, result.get("deletedCount"), "删除记录数应为0");
        assertEquals("未找到匹配的记录，无需删除", result.get("message"), "消息应正确");
        
        // 验证方法调用
        verify(testDataRepository, times(1)).findByYearAndMonthAndWeekSeqAndCigCodeAndDeliveryAreaIn(
            year, month, weekSeq, cigCode, Arrays.asList(deliveryArea));
        verify(testDataRepository, never()).deleteAll(any());
        
        System.out.println("✅ 删除功能未找到匹配记录测试通过");
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建模拟的预投放量数据
     */
    private List<DemoTestAdvData> createMockAdvData(String deliveryEtype) {
        List<DemoTestAdvData> mockData = new ArrayList<>();
        
        // 创建匹配的数据
        DemoTestAdvData data1 = new DemoTestAdvData();
        data1.setCigCode("C001");
        data1.setCigName("测试卷烟A");
        data1.setDeliveryEtype("档位+城乡分类代码");
        data1.setAdv(new BigDecimal("1000"));
        data1.setDeliveryArea("城镇");
        mockData.add(data1);
        
        DemoTestAdvData data2 = new DemoTestAdvData();
        data2.setCigCode("C002");
        data2.setCigName("测试卷烟B");
        data2.setDeliveryEtype("档位+城乡分类代码");
        data2.setAdv(new BigDecimal("800"));
        data2.setDeliveryArea("乡村");
        mockData.add(data2);
        
        // 创建不匹配的数据（不同投放类型）
        DemoTestAdvData data3 = new DemoTestAdvData();
        data3.setCigCode("C003");
        data3.setCigName("测试卷烟C");
        data3.setDeliveryEtype("档位+区县");
        data3.setAdv(new BigDecimal("600"));
        data3.setDeliveryArea("某区县");
        mockData.add(data3);
        
        return mockData;
    }

    /**
     * 创建模拟的测试数据列表
     */
    private List<DemoTestData> createMockTestDataList() {
        List<DemoTestData> testDataList = new ArrayList<>();
        
        // 创建测试数据1
        DemoTestData data1 = new DemoTestData();
        data1.setCigCode("C001");
        data1.setCigName("测试卷烟A");
        data1.setYear(2024);
        data1.setMonth(1);
        data1.setWeekSeq(1);
        data1.setDeliveryArea("城镇");
        data1.setD30(new BigDecimal("100"));
        data1.setD29(new BigDecimal("95"));
        data1.setD28(new BigDecimal("90"));
        data1.setD1(new BigDecimal("10"));
        data1.setBz("测试备注1");
        data1.setActualDelivery(new BigDecimal("1000"));
        testDataList.add(data1);
        
        // 创建测试数据2
        DemoTestData data2 = new DemoTestData();
        data2.setCigCode("C002");
        data2.setCigName("测试卷烟B");
        data2.setYear(2024);
        data2.setMonth(1);
        data2.setWeekSeq(1);
        data2.setDeliveryArea("乡村");
        data2.setD30(new BigDecimal("80"));
        data2.setD29(new BigDecimal("75"));
        data2.setD28(new BigDecimal("70"));
        data2.setD1(new BigDecimal("8"));
        data2.setBz("测试备注2");
        data2.setActualDelivery(new BigDecimal("800"));
        testDataList.add(data2);
        
        // 创建测试数据3
        DemoTestData data3 = new DemoTestData();
        data3.setCigCode("C003");
        data3.setCigName("测试卷烟C");
        data3.setYear(2024);
        data3.setMonth(1);
        data3.setWeekSeq(1);
        data3.setDeliveryArea("全市");
        data3.setD30(new BigDecimal("120"));
        data3.setD29(new BigDecimal("115"));
        data3.setD28(new BigDecimal("110"));
        data3.setD1(new BigDecimal("12"));
        data3.setBz("测试备注3");
        data3.setActualDelivery(new BigDecimal("1200"));
        testDataList.add(data3);
        
        return testDataList;
    }

    /**
     * 创建模拟的测试数据用于删除测试
     */
    private List<DemoTestData> createMockTestDataForDelete(String cigCode, String cigName, 
                                                          Integer year, Integer month, Integer weekSeq, 
                                                          String deliveryArea) {
        List<DemoTestData> testDataList = new ArrayList<>();
        
        // 创建匹配的测试数据
        DemoTestData data = new DemoTestData();
        data.setCigCode(cigCode);
        data.setCigName(cigName);
        data.setYear(year);
        data.setMonth(month);
        data.setWeekSeq(weekSeq);
        data.setDeliveryArea(deliveryArea);
        data.setD30(new BigDecimal("100"));
        data.setD29(new BigDecimal("95"));
        data.setD28(new BigDecimal("90"));
        data.setD1(new BigDecimal("10"));
        data.setBz("测试删除数据");
        data.setActualDelivery(new BigDecimal("1000"));
        testDataList.add(data);
        
        return testDataList;
    }

}