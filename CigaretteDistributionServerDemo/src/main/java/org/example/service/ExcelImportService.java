package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.example.dto.CigaretteImportRequestDto;
import org.example.dto.RegionClientNumImportRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
public class ExcelImportService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 导入卷烟投放基础信息Excel
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importCigaretteDistributionInfo(CigaretteImportRequestDto request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("开始导入卷烟投放基础信息，年份: {}, 月份: {}, 周序号: {}", 
                    request.getYear(), request.getMonth(), request.getWeekSeq());
            
            // 1. 验证文件
            if (!validateExcelFile(request.getFile())) {
                result.put("success", false);
                result.put("message", "文件格式不正确，请上传Excel文件");
                return result;
            }
            
            // 2. 生成表名
            String tableName = String.format("cigarette_distribution_info_%d_%d_%d", 
                    request.getYear(), request.getMonth(), request.getWeekSeq());
            
            // 3. 读取Excel数据
            List<Map<String, Object>> excelData = readCigaretteInfoFromExcel(request.getFile());
            if (excelData.isEmpty()) {
                result.put("success", false);
                result.put("message", "Excel文件为空或格式不正确");
                return result;
            }
            
            // 4. 验证数据结构
            if (!validateCigaretteInfoStructure(excelData.get(0))) {
                result.put("success", false);
                result.put("message", "Excel文件结构不符合要求，请检查列名是否与demo_test_ADVdata表一致");
                return result;
            }
            
            // 5. 创建表
            createCigaretteInfoTable(tableName);
            
            // 6. 插入数据
            int insertedCount = insertCigaretteInfoData(tableName, excelData);
            
            result.put("success", true);
            result.put("message", "导入成功");
            result.put("tableName", tableName);
            result.put("insertedCount", insertedCount);
            result.put("totalRows", excelData.size());
            
            log.info("卷烟投放基础信息导入完成，表名: {}, 插入记录数: {}", tableName, insertedCount);
            
        } catch (Exception e) {
            log.error("导入卷烟投放基础信息失败", e);
            result.put("success", false);
            result.put("message", "导入失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 导入区域客户数表Excel
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importRegionClientNumData(RegionClientNumImportRequestDto request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("开始导入区域客户数表，年份: {}, 月份: {}, 投放类型: {}, 扩展投放类型: {}, 双周上浮: {}", 
                    request.getYear(), request.getMonth(), request.getDeliveryMethod(), request.getDeliveryEtype(), request.getIsBiWeeklyFloat());
            
            // 1. 验证文件
            if (!validateExcelFile(request.getFile())) {
                result.put("success", false);
                result.put("message", "文件格式不正确，请上传Excel文件");
                return result;
            }
            
            // 2. 生成表名（使用新的命名规则）
            String tableName = request.getTableName();
            
            // 3. 读取Excel数据
            List<Map<String, Object>> excelData = readRegionClientNumFromExcel(request.getFile());
            if (excelData.isEmpty()) {
                result.put("success", false);
                result.put("message", "Excel文件为空或格式不正确");
                return result;
            }
            
            // 4. 验证数据结构
            if (!validateRegionClientNumStructure(excelData.get(0))) {
                result.put("success", false);
                result.put("message", "Excel文件结构不符合要求，请检查列名是否与demo_test_clientNumdata表一致");
                return result;
            }
            
            // 5. 检查表是否存在，不存在则创建
            ensureRegionClientNumTableExists(tableName);
            
            // 6. 清空表数据并插入新数据
            int insertedCount = replaceRegionClientNumData(tableName, excelData);
            
            result.put("success", true);
            result.put("message", "导入成功");
            result.put("tableName", tableName);
            result.put("insertedCount", insertedCount);
            result.put("totalRows", excelData.size());
            result.put("mainSequenceNumber", request.getSequenceNumber());
            result.put("subSequenceNumber", request.getSubSequenceNumber());
            result.put("deliveryMethod", request.getDeliveryMethod());
            result.put("deliveryEtype", request.getDeliveryEtype());
            result.put("isBiWeeklyFloat", request.getIsBiWeeklyFloat());
            
            log.info("区域客户数表导入完成，表名: {}, 插入记录数: {}", tableName, insertedCount);
            
        } catch (Exception e) {
            log.error("导入区域客户数表失败", e);
            result.put("success", false);
            result.put("message", "导入失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 验证Excel文件格式
     */
    private boolean validateExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            return false;
        }
        
        return fileName.toLowerCase().endsWith(".xlsx") || fileName.toLowerCase().endsWith(".xls");
    }

    /**
     * 从Excel读取卷烟投放基础信息
     */
    private List<Map<String, Object>> readCigaretteInfoFromExcel(MultipartFile file) throws IOException {
        List<Map<String, Object>> data = new ArrayList<>();
        
        try (Workbook workbook = createWorkbook(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // 读取表头
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return data;
            }
            
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(getCellValueAsString(cell));
            }
            
            // 读取数据行
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                Map<String, Object> rowData = new HashMap<>();
                for (int j = 0; j < headers.size() && j < row.getLastCellNum(); j++) {
                    Cell cell = row.getCell(j);
                    String columnName = headers.get(j);
                    Object cellValue = getCellValue(cell);
                    rowData.put(columnName, cellValue);
                }
                
                data.add(rowData);
            }
        }
        
        return data;
    }

    /**
     * 从Excel读取区域客户数信息
     */
    private List<Map<String, Object>> readRegionClientNumFromExcel(MultipartFile file) throws IOException {
        List<Map<String, Object>> data = new ArrayList<>();
        
        try (Workbook workbook = createWorkbook(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // 读取表头
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return data;
            }
            
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(getCellValueAsString(cell));
            }
            
            // 读取数据行
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                Map<String, Object> rowData = new HashMap<>();
                for (int j = 0; j < headers.size() && j < row.getLastCellNum(); j++) {
                    Cell cell = row.getCell(j);
                    String columnName = headers.get(j);
                    Object cellValue = getCellValue(cell);
                    rowData.put(columnName, cellValue);
                }
                
                data.add(rowData);
            }
        }
        
        return data;
    }

    /**
     * 创建Workbook对象
     */
    private Workbook createWorkbook(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName != null && fileName.toLowerCase().endsWith(".xlsx")) {
            return new XSSFWorkbook(file.getInputStream());
        } else {
            return new HSSFWorkbook(file.getInputStream());
        }
    }

    /**
     * 获取单元格值
     */
    private Object getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                } else {
                    return BigDecimal.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }

    /**
     * 获取单元格值作为字符串
     */
    private String getCellValueAsString(Cell cell) {
        Object value = getCellValue(cell);
        return value != null ? value.toString() : "";
    }

    /**
     * 验证卷烟投放基础信息数据结构
     */
    private boolean validateCigaretteInfoStructure(Map<String, Object> sampleRow) {
        // 必须包含的列名（与demo_test_ADVdata表一致）
        List<String> requiredColumns = Arrays.asList(
            "CIG_CODE", "CIG_NAME", "DELIVERY_AREA", "DELIVERY_METHOD", 
            "DELIVERY_ETYPE", "URS", "ADV"
        );
        
        Set<String> actualColumns = sampleRow.keySet();
        for (String column : requiredColumns) {
            if (!actualColumns.contains(column)) {
                log.warn("缺少必需列: {}", column);
                return false;
            }
        }
        
        return true;
    }

    /**
     * 验证区域客户数数据结构
     */
    private boolean validateRegionClientNumStructure(Map<String, Object> sampleRow) {
        // 必须包含的列名（与demo_test_clientNumdata表一致）
        List<String> requiredColumns = Arrays.asList(
            "URBAN_RURAL_CODE", "D30", "D29", "D28", "D27", "D26", "D25", "D24", 
            "D23", "D22", "D21", "D20", "D19", "D18", "D17", "D16", "D15", "D14", 
            "D13", "D12", "D11", "D10", "D9", "D8", "D7", "D6", "D5", "D4", "D3", "D2", "D1", "TOTAL"
        );
        
        Set<String> actualColumns = sampleRow.keySet();
        for (String column : requiredColumns) {
            if (!actualColumns.contains(column)) {
                log.warn("缺少必需列: {}", column);
                return false;
            }
        }
        
        return true;
    }

    /**
     * 创建卷烟投放基础信息表
     */
    private void createCigaretteInfoTable(String tableName) {
        // 先删除已存在的表
        String dropSql = "DROP TABLE IF EXISTS " + tableName;
        jdbcTemplate.execute(dropSql);
        
        // 创建新表（结构与demo_test_ADVdata一致）
        String createSql = String.format(
            "CREATE TABLE %s (" +
            "id INT AUTO_INCREMENT PRIMARY KEY, " +
            "CIG_CODE VARCHAR(255), " +
            "CIG_NAME VARCHAR(255), " +
            "DELIVERY_AREA TEXT, " +
            "DELIVERY_METHOD VARCHAR(255), " +
            "DELIVERY_ETYPE VARCHAR(255), " +
            "URS DECIMAL(10,2), " +
            "ADV DECIMAL(10,2), " +
            "created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4", tableName);
        
        jdbcTemplate.execute(createSql);
        log.info("创建卷烟投放基础信息表: {}", tableName);
    }

    /**
     * 确保区域客户数表存在，如果不存在则创建
     */
    private void ensureRegionClientNumTableExists(String tableName) {
        // 检查表是否存在
        String checkTableSql = "SELECT COUNT(*) FROM information_schema.tables " +
                              "WHERE table_schema = DATABASE() AND table_name = ?";
        
        Integer tableCount = jdbcTemplate.queryForObject(checkTableSql, Integer.class, tableName);
        
        if (tableCount == null || tableCount == 0) {
            // 表不存在，创建新表（结构与demo_test_clientNumdata一致）
            StringBuilder createSql = new StringBuilder();
            createSql.append(String.format("CREATE TABLE %s (", tableName));
            createSql.append("id INT AUTO_INCREMENT PRIMARY KEY, ");
            createSql.append("URBAN_RURAL_CODE VARCHAR(255), ");
            
            // 添加D30到D1列
            for (int i = 30; i >= 1; i--) {
                createSql.append(String.format("D%d DECIMAL(10,2), ", i));
            }
            
            createSql.append("TOTAL DECIMAL(10,2), ");
            createSql.append("created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, ");
            createSql.append("updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
            createSql.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            
            jdbcTemplate.execute(createSql.toString());
            log.info("创建区域客户数表: {}", tableName);
        } else {
            log.info("区域客户数表已存在，将覆盖数据: {}", tableName);
        }
    }

    /**
     * 插入卷烟投放基础信息数据
     */
    private int insertCigaretteInfoData(String tableName, List<Map<String, Object>> data) {
        String sql = String.format(
            "INSERT INTO %s (CIG_CODE, CIG_NAME, DELIVERY_AREA, DELIVERY_METHOD, DELIVERY_ETYPE, URS, ADV) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)", tableName);
        
        int insertedCount = 0;
        
        for (Map<String, Object> row : data) {
            try {
                jdbcTemplate.update(sql,
                    row.get("CIG_CODE"),
                    row.get("CIG_NAME"),
                    row.get("DELIVERY_AREA"),
                    row.get("DELIVERY_METHOD"),
                    row.get("DELIVERY_ETYPE"),
                    row.get("URS"),
                    row.get("ADV"));
                insertedCount++;
            } catch (Exception e) {
                log.error("插入数据失败: {}", row, e);
            }
        }
        
        return insertedCount;
    }

    /**
     * 替换区域客户数数据（清空旧数据并插入新数据）
     */
    private int replaceRegionClientNumData(String tableName, List<Map<String, Object>> data) {
        try {
            // 1. 清空表中的所有数据
            String deleteSql = String.format("DELETE FROM %s", tableName);
            int deletedCount = jdbcTemplate.update(deleteSql);
            log.info("清空表 {} 中的数据，删除了 {} 条记录", tableName, deletedCount);
            
            // 2. 重置自增ID
            String resetAutoIncrementSql = String.format("ALTER TABLE %s AUTO_INCREMENT = 1", tableName);
            jdbcTemplate.execute(resetAutoIncrementSql);
            
            // 3. 构建插入SQL
            StringBuilder sql = new StringBuilder();
            sql.append(String.format("INSERT INTO %s (URBAN_RURAL_CODE, ", tableName));
            
            // 添加D30到D1列
            for (int i = 30; i >= 1; i--) {
                sql.append(String.format("D%d, ", i));
            }
            sql.append("TOTAL) VALUES (?, ");
            
            // 添加占位符
            for (int i = 30; i >= 1; i--) {
                sql.append("?, ");
            }
            sql.append("?)");
            
            // 4. 批量插入新数据
            int insertedCount = 0;
            
            for (Map<String, Object> row : data) {
                try {
                    List<Object> params = new ArrayList<>();
                    params.add(row.get("URBAN_RURAL_CODE"));
                    
                    // 添加D30到D1的值
                    for (int i = 30; i >= 1; i--) {
                        params.add(row.get("D" + i));
                    }
                    params.add(row.get("TOTAL"));
                    
                    jdbcTemplate.update(sql.toString(), params.toArray());
                    insertedCount++;
                } catch (Exception e) {
                    log.error("插入数据失败: {}", row, e);
                }
            }
            
            log.info("成功向表 {} 插入 {} 条新记录", tableName, insertedCount);
            return insertedCount;
            
        } catch (Exception e) {
            log.error("替换区域客户数数据失败", e);
            throw new RuntimeException("替换数据失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 插入区域客户数数据（保留原方法，但已被replaceRegionClientNumData替代）
     */
    @Deprecated
    private int insertRegionClientNumData(String tableName, List<Map<String, Object>> data) {
        // 构建插入SQL
        StringBuilder sql = new StringBuilder();
        sql.append(String.format("INSERT INTO %s (URBAN_RURAL_CODE, ", tableName));
        
        // 添加D30到D1列
        for (int i = 30; i >= 1; i--) {
            sql.append(String.format("D%d, ", i));
        }
        sql.append("TOTAL) VALUES (?, ");
        
        // 添加占位符
        for (int i = 30; i >= 1; i--) {
            sql.append("?, ");
        }
        sql.append("?)");
        
        int insertedCount = 0;
        
        for (Map<String, Object> row : data) {
            try {
                List<Object> params = new ArrayList<>();
                params.add(row.get("URBAN_RURAL_CODE"));
                
                // 添加D30到D1的值
                for (int i = 30; i >= 1; i--) {
                    params.add(row.get("D" + i));
                }
                params.add(row.get("TOTAL"));
                
                jdbcTemplate.update(sql.toString(), params.toArray());
                insertedCount++;
            } catch (Exception e) {
                log.error("插入数据失败: {}", row, e);
            }
        }
        
        return insertedCount;
    }
}
