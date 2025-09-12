package org.example.algorithm;

import org.example.entity.CityClientNumData;
import org.example.entity.DemoTestAdvData;
import org.example.entity.DemoTestData;
import org.example.repository.CityClientNumDataRepository;
import org.example.repository.DemoTestAdvDataRepository;
import org.example.repository.DemoTestDataRepository;
import org.example.service.CityPredictionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class CityPredictionServiceTest {

    @Autowired
    private CityPredictionService predictionService;

    @Autowired
    private DemoTestAdvDataRepository advDataRepository;

    @Autowired
    private CityClientNumDataRepository cityClientNumDataRepository;

    @Autowired
    private DemoTestDataRepository testDataRepository;

    @BeforeEach
    public void setUp() {
        // 清理旧的预测数据，确保测试环境纯净
        testDataRepository.deleteAll();
    }

    @Test
    @Transactional
    public void testPredictAndSaveFunction() {
        System.out.println("=== 全市投放量预测服务测试 ===");

        // 1. 获取预投放量数据
        List<DemoTestAdvData> advDataList = advDataRepository.findAll();
        assertFalse(advDataList.isEmpty(), "预投放量数据(demo_test_ADVdata)不应为空");
        System.out.println("\n1. 预投放量数据:");
        for (DemoTestAdvData advData : advDataList) {
            System.out.printf("卷烟代码: %s, 卷烟名称: %s, 预投放量: %s%n",
                    advData.getCigCode(), advData.getCigName(), advData.getAdv());
        }

        // 2. 获取“全市”的客户数数据
        Optional<CityClientNumData> cityDataOptional = cityClientNumDataRepository.findByUrbanRuralCode("全市");
        assertTrue(cityDataOptional.isPresent(), "客户数数据表(city_clientnum_data)中必须包含“全市”的记录");
        CityClientNumData cityData = cityDataOptional.get();
        System.out.println("\n2. 全市客户数数据:");
        System.out.printf("投放区域: %s, 总客户数: %s%n",
                cityData.getUrbanRuralCode(), cityData.getTotal());

        // 3. 定义测试参数并执行预测
        Integer year = 2025;
        Integer month = 1;
        Integer weekSeq = 1;
        System.out.printf("\n3. 开始执行预测服务，年份: %d, 月份: %d, 周序号: %d%n", year, month, weekSeq);
        predictionService.predictAndSave(year, month, weekSeq);
        System.out.println("4. 预测服务执行完毕");

        // 5. 验证结果
        System.out.println("\n5. 验证数据库中的预测结果:");
        List<DemoTestData> predictionResults = testDataRepository.findByYearAndMonthAndWeekSeq(year, month, weekSeq);
        assertEquals(advDataList.size(), predictionResults.size(), "生成的预测数据条数应与预投放量数据条数一致");

        // 准备全市客户数矩阵用于计算实际投放量
        BigDecimal[] cityCustomerMatrixRow = {
                cityData.getD30(), cityData.getD29(), cityData.getD28(), cityData.getD27(), cityData.getD26(),
                cityData.getD25(), cityData.getD24(), cityData.getD23(), cityData.getD22(), cityData.getD21(),
                cityData.getD20(), cityData.getD19(), cityData.getD18(), cityData.getD17(), cityData.getD16(),
                cityData.getD15(), cityData.getD14(), cityData.getD13(), cityData.getD12(), cityData.getD11(),
                cityData.getD10(), cityData.getD9(), cityData.getD8(), cityData.getD7(), cityData.getD6(),
                cityData.getD5(), cityData.getD4(), cityData.getD3(), cityData.getD2(), cityData.getD1()
        };

        for (DemoTestData result : predictionResults) {
            System.out.printf("\n--- 验证卷烟: %s (%s) ---%n", result.getCigName(), result.getCigCode());
            System.out.printf("投放区域: %s%n", result.getDeliveryArea());

            // 找到对应的预投放量
            BigDecimal targetAmount = advDataList.stream()
                    .filter(adv -> adv.getCigCode().equals(result.getCigCode()))
                    .findFirst()
                    .map(DemoTestAdvData::getAdv)
                    .orElse(BigDecimal.ZERO);

            printAllocationResult(result, cityCustomerMatrixRow, targetAmount);
        }
        System.out.println("\n=== 测试成功结束 ===");
    }

    /**
     * 输出分配结果
     */
    private void printAllocationResult(DemoTestData result,
                                       BigDecimal[] cityCustomerMatrixRow,
                                       BigDecimal targetAmount) {
        System.out.println("分配矩阵结果:");
        System.out.println("档位:    " + String.format("%-8s", "D30") + String.format("%-8s", "D29") +
                String.format("%-8s", "D28") + String.format("%-8s", "D27") +
                String.format("%-8s", "D26") + String.format("%-8s", "D25") + "...");

        BigDecimal[] allocation = {
                result.getD30(), result.getD29(), result.getD28(), result.getD27(), result.getD26(), result.getD25()
        };

        System.out.printf("区域%-4s: ", "全市");
        for (int j = 0; j < 6; j++) {
            System.out.printf("%-8s", allocation[j]);
        }
        System.out.println("...");

        // 计算实际投放量
        BigDecimal actualAmount = calculateActualAmount(result, cityCustomerMatrixRow);

        System.out.printf("目标投放量: %s%n", targetAmount);
        System.out.printf("实际投放量: %s%n", actualAmount);
        System.out.printf("误差: %s%n", targetAmount.subtract(actualAmount).abs());

        // 验证非递增约束
        boolean isValid = validateNonIncreasingConstraint(result);
        System.out.printf("非递增约束验证: %s%n", isValid ? "通过" : "失败");
    }

    /**
     * 计算单条记录的实际投放量
     */
    private BigDecimal calculateActualAmount(DemoTestData data, BigDecimal[] customerMatrix) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal[] distribution = {
                data.getD30(), data.getD29(), data.getD28(), data.getD27(), data.getD26(), data.getD25(),
                data.getD24(), data.getD23(), data.getD22(), data.getD21(), data.getD20(), data.getD19(),
                data.getD18(), data.getD17(), data.getD16(), data.getD15(), data.getD14(), data.getD13(),
                data.getD12(), data.getD11(), data.getD10(), data.getD9(), data.getD8(), data.getD7(),
                data.getD6(), data.getD5(), data.getD4(), data.getD3(), data.getD2(), data.getD1()
        };

        for (int i = 0; i < 30; i++) {
            if (distribution[i] != null && customerMatrix[i] != null) {
                total = total.add(distribution[i].multiply(customerMatrix[i]));
            }
        }
        return total;
    }

    /**
     * 验证单条记录的档位分配是否满足非递增约束
     */
    private boolean validateNonIncreasingConstraint(DemoTestData data) {
        BigDecimal[] distribution = {
                data.getD30(), data.getD29(), data.getD28(), data.getD27(), data.getD26(), data.getD25(),
                data.getD24(), data.getD23(), data.getD22(), data.getD21(), data.getD20(), data.getD19(),
                data.getD18(), data.getD17(), data.getD16(), data.getD15(), data.getD14(), data.getD13(),
                data.getD12(), data.getD11(), data.getD10(), data.getD9(), data.getD8(), data.getD7(),
                data.getD6(), data.getD5(), data.getD4(), data.getD3(), data.getD2(), data.getD1()
        };

        for (int i = 0; i < distribution.length - 1; i++) {
            BigDecimal current = distribution[i] == null ? BigDecimal.ZERO : distribution[i];
            BigDecimal next = distribution[i+1] == null ? BigDecimal.ZERO : distribution[i+1];
            if (current.compareTo(next) < 0) {
                return false;
            }
        }
        return true;
    }
}