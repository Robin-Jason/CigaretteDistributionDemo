档位+市场类型算法测试SQL脚本📋 测试概述测试对象: 档位+市场类型分配算法 (MarketProportionalCigaretteDistributionAlgorithm)测试表: cigarette_distribution_info_2099_1_2 (输入) → cigarette_distribution_prediction_2099_1_2 (输出)测试用例数: 约202个市场类型范围: 2个市场类型（城网、农网）及其组合📂 文件说明文件名功能说明01-create-test-tables.sql创建测试表创建 _info_2099_1_2 和 _prediction_2099_1_2 表02-generate-test-data.sql生成测试数据生成202个测试用例 (仅城网/仅农网/城网+农网)03-query-test-results.sql查询测试结果10个预定义查询分析算法结果run-all-tests.sh一键运行测试自动化执行全部测试流程README.md使用说明本文档🚀 快速开始（一键执行）方法1：使用Shell脚本（推荐）Bash# 1. 进入脚本目录 (假设你已创建 market 目录)
cd "src/test/resources/test_sql/market" # 或者你的实际目录名

# 2. 添加执行权限
chmod +x run-all-tests.sh

# 3. 运行测试
./run-all-tests.sh
说明: 脚本会自动完成建表、生成数据、调用API、查询结果等所有步骤。方法2：手动执行（分步骤）步骤1：创建测试表Bash# 确保在 market 目录下
mysql -h localhost -u root -p marketing < 01-create-test-tables.sql
步骤2：生成测试数据Bash# 确保在 market 目录下
mysql -h localhost -u root -p marketing < 02-generate-test-data.sql
预期结果: 生成约202个测试用例步骤3：调用后端API执行算法Bash# 注意 year=2099, month=1, weekSeq=2
# 可以选择性地添加 urbanRatio 和 ruralRatio 参数进行测试
# 示例：使用默认比例
curl -X POST "http://localhost:28080/api/calculate/write-back?year=2099&month=1&weekSeq=2"

# 示例：指定比例 (仅在城网农网都存在时有效)
# curl -X POST "http://localhost:28080/api/calculate/write-back?year=2099&month=1&weekSeq=2&urbanRatio=0.45&ruralRatio=0.55"

预期响应: {"success": true, "successCount": 202, ...}步骤4：查询测试结果Bash# 确保在 market 目录下
mysql -h localhost -u root -p marketing < 03-query-test-results.sql > market_type_test_results.txt
步骤5：查看结果Bashcat market_type_test_results.txt
📊 测试用例设计预投放量分布范围数量说明1001固定用例 (仅城网)5001固定用例 (仅农网)10001固定用例 (城网+农网)1000-200000199每1000区间随机1个总计202-投放区域组合 (DELIVERY_AREA)市场类型:城网农网组合规则: 覆盖以下三种场景，并在 1000-200000 区间内大致均匀分布：| 场景     | DELIVERY_AREA | 说明                         | 占比   || :------- | :-------------- | :--------------------------- | :----- || 仅城网   | "城网"          | 测试仅有城网的情况           | ~33.7% || 仅农网   | "农网"          | 测试仅有农网的情况           | ~33.2% || 城网+农网| "城网,农网"     | 测试按比例分配（或默认比例） | ~33.2% |🎯 预期测试结果性能指标（参考档位+城乡分类代码及业态测试结果）指标预期值说明平均误差率< 0.2%算法精确度高最大绝对误差< 200硬性要求完美率（误差=0）> 20%大量完美匹配优秀率（误差≤5）> 50%多数为优秀最大相对误差率< 5%控制在合理范围（小量级除外）📝 测试结果查询说明查询1：基础统计信息测试用例总数算法输出记录数（城网+农网时会产生2条记录）预投放量总和实际投放量总和查询2：误差分析（按卷烟汇总）每个测试用例的预投放、实际投放、误差、误差率、投放市场查询3：误差统计汇总平均绝对误差、平均误差率、最大误差率查询4：按投放量范围统计误差不同投放量级下的平均和最大误差率查询5：按投放场景统计误差对比“仅城网”、“仅农网”、“城网+农网”三种场景的误差表现查询6 & 7：分配详情和档位统计展示具体案例的市场类型分配量和内部档位分配情况查询8：最佳和最差误差率案例找出误差率最小和最大的10个用例查询9：市场类型投放量排名统计城网和农网各自接收的总投放量查询10：算法性能总结根据平均误差率给出总体评级⚠️ 注意事项1. 前提条件必须存在的表:region_clientNum_2_1 - 档位+市场类型（非双周上浮）客户数基础数据表region_clientNum_2_2 - 档位+市场类型（双周上浮）客户数基础数据表 (如果测试数据中有双周上浮的备注)检查方法:SQLSELECT COUNT(*) FROM region_clientNum_2_1;
SELECT COUNT(*) FROM region_clientNum_2_2;
如果表不存在或无数据，请先通过Excel导入功能导入市场类型客户数基础数据。2. 后端服务状态确保Spring Boot服务运行在端口 28080：Bash# 检查服务状态
curl http://localhost:28080/api/common/health # 使用 /api/common/health

# 如果未运行，启动服务 (请在项目根目录执行)
cd /path/to/CigaretteDistributionServerDemo
mvn spring-boot:run
3. 数据库连接确保MySQL服务运行，并且可以连接到 marketing 数据库。🔧 故障排除问题1：存储过程执行失败现象: 生成数据时 (02-generate-test-data.sql) 报错解决: 检查 DELIMITER 设置和 SQL 语法，确保目标表已存在（如果脚本内不创建）。问题2：API调用失败 (run-all-tests.sh)现象: curl 返回 404, 500 或连接错误解决:确认后端服务在 28080 端口运行且健康检查通过。确认 API URL (/api/calculate/write-back) 正确。查看后端 Spring Boot 应用的控制台日志获取详细错误信息。问题3：查询结果为空 (03-query-test-results.sql)现象: cigarette_distribution_prediction_2099_1_2 表无数据解决:确认 API 调用步骤是否成功执行，并检查 API 的响应 (success:true?)。检查后端日志，确认 writeBackToDatabase 方法是否被调用以及是否有错误。验证 region_clientNum_2_1 (或 _2_2) 表是否有对应的客户数数据。问题4：误差过大现象: 03-query-test-results.sql 查询结果显示误差超过预期（例如 > 200）解决:检查 region_clientNum_2_1 (或 _2_2) 表中的客户数数据是否准确、合理。检查 MarketProportionalCigaretteDistributionAlgorithm.java 中的算法逻辑，特别是比例计算和核心算法调用部分。检查 DistributionCalculateServiceImpl 中调用市场类型策略和计算实际投放量的逻辑。📚 相关文档算法描述: CigaretteDistributionServerDemo/algorithm docs/卷烟按市场类型比例投放算法描述.pdfAPI文档: CigaretteDistributionServerDemo/docs/API.md一键生成数据流: CigaretteDistributionServerDemo/docs/一键生成分配方案API数据流说明.md🎉 完成测试后测试完成后，使用 market_type_test_results.txt 文件中的数据生成详细测试报告。报告应包含：总体统计（成功率、平均误差、误差率、最大绝对误差）按预投放量范围分析按投放场景（仅城网/仅农网/组合）分析典型案例分析（包括误差最大的案例）数据分布和梯度说明确认最大绝对误差是否满足 < 200 的要求。建议报告位置: CigaretteDistributionServerDemo/test docs/档位+市场类型/测试报告.md (请手动创建)脚本版本: v1.1 (适配市场类型)创建日期: 2025-10-22