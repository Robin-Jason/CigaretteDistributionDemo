package org.example.dto;

import lombok.Data;
import org.example.util.TableNameGeneratorUtil;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;

@Data
public class RegionClientNumImportRequestDto {
    
    @NotNull(message = "Excel文件不能为空")
    private MultipartFile file;
    
    @NotNull(message = "年份不能为空")
    @Min(value = 2020, message = "年份不能小于2020")
    @Max(value = 2099, message = "年份不能大于2099")
    private Integer year;
    
    @NotNull(message = "月份不能为空")
    @Min(value = 1, message = "月份不能小于1")
    @Max(value = 12, message = "月份不能大于12")
    private Integer month;
    
    @NotBlank(message = "投放类型不能为空")
    private String deliveryMethod;
    
    @NotBlank(message = "扩展投放类型不能为空")
    private String deliveryEtype;
    
    /**
     * 双周上浮标识
     * false: 非双周上浮 (子序号为1)
     * true: 双周上浮 (子序号为2)
     */
    private Boolean isBiWeeklyFloat = false;
    
    /**
     * 获取主序号映射（仅用于向后兼容，推荐使用getTableName()方法）
     * 0表示投放类型为按档位统一投放
     * 1-4均为按档位扩展投放：
     * 1表示扩展类型为档位+区县
     * 2表示扩展类型为档位+市场类型
     * 3表示扩展类型为档位+城乡分类代码
     * 4表示档位+业态
     */
    public Integer getSequenceNumber() {
        if ("按档位统一投放".equals(deliveryMethod) || "按档位投放".equals(deliveryMethod)) {
            return 0;
        } else if ("按档位扩展投放".equals(deliveryMethod)) {
            if ("档位+区县".equals(deliveryEtype)) {
                return 1;
            } else if ("档位+市场类型".equals(deliveryEtype)) {
                return 2;
            } else if ("档位+城乡分类代码".equals(deliveryEtype)) {
                return 3;
            } else if ("档位+业态".equals(deliveryEtype)) {
                return 4;
            }
        }
        // 默认返回0
        return 0;
    }
    
    /**
     * 获取子序号（双周上浮区分）
     * 1: 非双周上浮
     * 2: 双周上浮
     * 注意：所有投放类型均支持双周上浮区分
     */
    public Integer getSubSequenceNumber() {
        // 所有投放类型均支持双周上浮区分
        return (isBiWeeklyFloat != null && isBiWeeklyFloat) ? 2 : 1;
    }
    
    /**
     * 获取完整的表名
     * 格式：region_clientNum_{主序号}_{子序号}
     * 例如：region_clientNum_1_1 (档位+区县，非双周上浮)
     *      region_clientNum_1_2 (档位+区县，双周上浮)
     */
    public String getTableName() {
        return TableNameGeneratorUtil.generateRegionClientTableName(
                deliveryMethod, deliveryEtype, isBiWeeklyFloat);
    }
}
