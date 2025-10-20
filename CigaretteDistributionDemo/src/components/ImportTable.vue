<template>
  <div class="import-table-component">
    <!-- 导入功能按钮区域 -->
    <div class="import-buttons-row">
      <el-button 
        type="primary" 
        size="default"
        @click="showBasicInfoImportDialog"
      >
        <el-icon><DocumentAdd /></el-icon>
        导入卷烟投放基本信息
      </el-button>
      <el-button 
        type="success" 
        size="default"
        @click="showCustomerDataImportDialog"
      >
        <el-icon><DataAnalysis /></el-icon>
        导入区域客户数
      </el-button>
      <el-button 
        type="warning" 
        size="default"
        @click="showGeneratePlanDialog"
      >
        <el-icon><Cpu /></el-icon>
        生成分配方案
      </el-button>
    </div>

    <!-- 卷烟投放基本信息导入对话框 -->
    <el-dialog
      v-model="basicInfoImportDialogVisible"
      title="导入卷烟投放基本信息"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="basicInfoTimeForm" label-width="80px">
        <el-form-item label="年份" required>
          <el-select 
            v-model="basicInfoTimeForm.year" 
            placeholder="选择年份"
            style="width: 100%"
          >
            <el-option 
              v-for="year in yearOptions" 
              :key="year" 
              :label="year" 
              :value="year"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="月份" required>
          <el-select 
            v-model="basicInfoTimeForm.month" 
            placeholder="选择月份"
            style="width: 100%"
          >
            <el-option 
              v-for="month in monthOptions" 
              :key="month" 
              :label="`${month}月`" 
              :value="month"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="周序号" required>
          <el-select 
            v-model="basicInfoTimeForm.weekSeq" 
            placeholder="选择周序号"
            style="width: 100%"
          >
            <el-option 
              v-for="week in weekOptions" 
              :key="week" 
              :label="`第${week}周`" 
              :value="week"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="选择文件" required>
          <el-upload
            ref="basicInfoUpload"
            class="basic-info-upload"
            :auto-upload="false"
            :show-file-list="true"
            accept=".xlsx,.xls"
            :limit="1"
            :file-list="basicInfoFileList"
            :before-upload="handleBasicInfoBeforeUpload"
            :on-change="handleBasicInfoChange"
            :on-remove="handleBasicInfoRemove"
          >
            <el-button type="primary">
              <el-icon><Plus /></el-icon>
              选择Excel文件
            </el-button>
          </el-upload>
          <div class="upload-tip">支持Excel格式(.xlsx, .xls)，文件大小不超过10MB</div>
        </el-form-item>
        
        <!-- 表结构说明 -->
        <el-form-item label="表结构要求">
          <el-alert
            title="Excel文件必须包含以下列（大小写敏感）"
            type="info"
            :closable="false"
            show-icon
          >
            <template #default>
              <div class="structure-requirements">
                <p><strong>基本信息列：</strong></p>
                <ul>
                  <li><code>CIG_CODE</code> - 卷烟代码</li>
                  <li><code>CIG_NAME</code> - 卷烟名称</li>
                  <li><code>YEAR</code> - 年份</li>
                  <li><code>MONTH</code> - 月份</li>
                  <li><code>WEEK_SEQ</code> - 周序号</li>
                  <li><code>URS</code> - URS</li>
                  <li><code>ADV</code> - ADV</li>
                  <li><code>DELIVERY_METHOD</code> - 档位投放方式</li>
                  <li><code>DELIVERY_ETYPE</code> - 扩展投放方式</li>
                  <li><code>DELIVERY_AREA</code> - 投放区域</li>
                  <li><code>remark</code> - 备注</li>
                </ul>
                <p><strong>可选列：</strong></p>
                <ul>
                  <li><code>id</code> - 主键ID（可选，系统会自动生成）</li>
                </ul>
              </div>
            </template>
          </el-alert>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="basicInfoImportDialogVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="handleBasicInfoImport"
            :loading="basicInfoImporting"
            :disabled="!canImportBasicInfo"
          >
            确定导入
          </el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 区域客户数导入对话框 -->
    <el-dialog
      v-model="customerDataImportDialogVisible"
      title="导入区域客户数"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form :model="customerImportForm" label-width="120px">
        <el-form-item label="投放方法" required>
          <el-select
            v-model="customerImportForm.deliveryMethod"
            placeholder="请选择投放方法"
            style="width: 100%"
            @change="handleCustomerImportTypeChange"
          >
            <el-option label="按档位统一投放" value="按档位统一投放" />
            <el-option label="按档位扩展投放" value="按档位扩展投放" />
          </el-select>
        </el-form-item>
        
        <el-form-item 
          v-if="customerImportForm.deliveryMethod === '按档位扩展投放'" 
          label="扩展投放类型" 
          required
        >
          <el-select
            v-model="customerImportForm.deliveryEtype"
            placeholder="请选择扩展投放类型"
            style="width: 100%"
          >
            <el-option label="档位+区县" value="档位+区县" />
            <el-option label="档位+城乡分类代码" value="档位+城乡分类代码" />
            <el-option label="档位+市场类型" value="档位+市场类型" />
            <el-option label="档位+业态" value="档位+业态" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="年份" required>
          <el-select
            v-model="customerImportForm.year"
            placeholder="请选择年份"
            style="width: 100%"
            :disabled="customerDataImporting"
          >
            <el-option
              v-for="year in yearOptions"
              :key="year"
              :label="year"
              :value="year"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="月份" required>
          <el-select
            v-model="customerImportForm.month"
            placeholder="请选择月份"
            style="width: 100%"
            :disabled="customerDataImporting"
          >
            <el-option
              v-for="month in monthOptions"
              :key="month"
              :label="month"
              :value="month"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="双周上浮">
          <el-checkbox 
            v-model="customerImportForm.isBiWeeklyFloat"
            :disabled="customerDataImporting"
          >
          </el-checkbox>
        </el-form-item>
        
        
        <el-form-item label="选择文件" required>
          <el-upload
            ref="customerDataUpload"
            class="customer-data-upload"
            :auto-upload="false"
            :show-file-list="true"
            accept=".xlsx,.xls"
            :limit="1"
            :file-list="customerDataFileList"
            :before-upload="handleCustomerDataBeforeUpload"
            :on-change="handleCustomerDataChange"
            :on-remove="handleCustomerDataRemove"
          >
            <el-button type="primary">
              <el-icon><Plus /></el-icon>
              选择Excel文件
            </el-button>
          </el-upload>
          <div class="upload-tip">支持Excel格式(.xlsx, .xls)，文件大小不超过10MB</div>
        </el-form-item>
        
        <!-- 表结构说明 -->
        <el-form-item label="表结构要求">
          <el-alert
            title="Excel文件必须包含以下列（大小写敏感）"
            type="info"
            :closable="false"
            show-icon
          >
            <template #default>
              <div class="structure-requirements">
                <p><strong>客户数据列：</strong></p>
                <ul>
                  <li><code>region</code> - 区域标识</li>
                  <li><code>D30</code> 到 <code>D1</code> - 30个档位客户数列</li>
                  <li><code>TOTAL</code> - 总客户数</li>
                </ul>
                <p><strong>注意：</strong>必须包含D30到D1共30个档位列，不能缺少任何一个</p>
              </div>
            </template>
          </el-alert>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="customerDataImportDialogVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="handleCustomerDataImport"
            :loading="customerDataImporting"
            :disabled="!canImportCustomerData"
          >
            确定导入
          </el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 生成分配方案对话框 -->
    <el-dialog
      v-model="generatePlanDialogVisible"
      title="生成分配方案"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="generate-plan-content">
        <div class="plan-description">
          <el-alert
            title="分配方案生成说明"
            type="info"
            :closable="false"
            show-icon
          >
            <template #default>
              <p>系统将根据选定时间的卷烟投放基本信息和区域客户数据，自动计算生成各卷烟的档位分配方案。</p>
              <p>请选择需要生成分配方案的时间范围：</p>
            </template>
          </el-alert>
        </div>
        
        <el-divider />
        
        <el-form :model="generatePlanForm" label-width="80px">
          <el-form-item label="年份" required>
            <el-select 
              v-model="generatePlanForm.year" 
              placeholder="选择年份"
              style="width: 100%"
            >
              <el-option 
                v-for="year in yearOptions" 
                :key="year" 
                :label="year" 
                :value="year"
              />
            </el-select>
          </el-form-item>
          
          <el-form-item label="月份" required>
            <el-select 
              v-model="generatePlanForm.month" 
              placeholder="选择月份"
              style="width: 100%"
            >
              <el-option 
                v-for="month in monthOptions" 
                :key="month" 
                :label="`${month}月`" 
                :value="month"
              />
            </el-select>
          </el-form-item>
          
          <el-form-item label="周序号" required>
            <el-select 
              v-model="generatePlanForm.weekSeq" 
              placeholder="选择周序号"
              style="width: 100%"
            >
              <el-option 
                v-for="week in weekOptions" 
                :key="week" 
                :label="`第${week}周`" 
                :value="week"
              />
            </el-select>
          </el-form-item>
          
          <el-divider content-position="left">
            <span style="font-size: 13px; color: #606266;">
              档位+市场类型分配比例
              <el-tooltip content="仅用于投放方式为'档位+市场类型'的卷烟" placement="top">
                <el-icon style="margin-left: 5px; cursor: help;"><QuestionFilled /></el-icon>
              </el-tooltip>
            </span>
          </el-divider>
          
          <el-alert
            title="比例参数说明"
            type="info"
            :closable="false"
            show-icon
            style="margin-bottom: 15px;"
          >
            <template #default>
              <div style="font-size: 12px; line-height: 1.6;">
                <p style="margin: 4px 0;">• 仅用于投放方式为"档位+市场类型"的卷烟</p>
                <p style="margin: 4px 0;">• 其他投放方式（档位+区县、档位+城乡分类代码等）不受影响</p>
                <p style="margin: 4px 0;">• 比例总和必须为100%</p>
                <p style="margin: 4px 0;">• 不设置时使用默认值：城网40%，农网60%</p>
              </div>
            </template>
          </el-alert>
          
          <el-form-item label="城网比例" required>
            <el-input-number
              v-model="generatePlanForm.urbanRatio"
              :min="0"
              :max="100"
              :precision="0"
              :step="5"
              style="width: 100%"
              @change="handleUrbanRatioChange"
            >
              <template #suffix>%</template>
            </el-input-number>
          </el-form-item>
          
          <el-form-item label="农网比例" required>
            <el-input-number
              v-model="generatePlanForm.ruralRatio"
              :min="0"
              :max="100"
              :precision="0"
              :step="5"
              style="width: 100%"
              @change="handleRuralRatioChange"
            >
              <template #suffix>%</template>
            </el-input-number>
          </el-form-item>
          
          <el-form-item>
            <el-alert
              :title="ratioValidationMessage"
              :type="ratioValidationType"
              :closable="false"
              show-icon
            />
          </el-form-item>
        </el-form>
        
        <div class="generate-tips">
          <el-alert
            v-if="!isGeneratePlanTimeComplete"
            title="请选择完整的时间信息后再生成分配方案"
            type="warning"
            :closable="false"
            show-icon
          />
          <el-alert
            v-else
            :title="`将为 ${generatePlanForm.year}年${generatePlanForm.month}月第${generatePlanForm.weekSeq}周 生成分配方案`"
            type="success"
            :closable="false"
            show-icon
          />
        </div>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="generatePlanDialogVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="handleGeneratePlan"
            :loading="generatingPlan"
            :disabled="!canGeneratePlan"
          >
            {{ generatingPlan ? '生成中...' : '确定生成' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { DocumentAdd, DataAnalysis, Plus, Cpu, QuestionFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cigaretteDistributionAPI } from '../services/api'

export default {
  name: 'ImportTable',
  components: {
    DocumentAdd,
    DataAnalysis,
    Plus,
    Cpu,
    QuestionFilled
  },
  emits: ['import-success', 'data-refresh'],
  data() {
    return {
      // 卷烟投放基本信息导入
      basicInfoImportDialogVisible: false,
      basicInfoFileList: [],
      basicInfoImporting: false,
      basicInfoTimeForm: {
        year: null,
        month: null,
        weekSeq: null
      },
      
      // 区域客户数导入
      customerDataImportDialogVisible: false,
      customerDataFileList: [],
      customerDataImporting: false,
      customerImportForm: {
        deliveryMethod: '',
        deliveryEtype: '',
        year: null,
        month: null,
        isBiWeeklyFloat: false
      },
      
      // 生成分配方案
      generatePlanDialogVisible: false,
      generatePlanForm: {
        year: null,
        month: null,
        weekSeq: null,
        urbanRatio: 40,  // 城网比例，默认40%
        ruralRatio: 60   // 农网比例，默认60%
      },
      generatingPlan: false
    }
  },
  computed: {
    // 年份选项
    yearOptions() {
      const currentYear = new Date().getFullYear()
      const years = []
      for (let year = currentYear - 2; year <= currentYear + 2; year++) {
        years.push(year)
      }
      return years
    },
    
    // 月份选项
    monthOptions() {
      return Array.from({ length: 12 }, (_, i) => i + 1)
    },
    
    // 周序号选项
    weekOptions() {
      return [1, 2, 3, 4, 5]
    },
    
    // 基本信息时间表单是否完整
    isBasicInfoTimeComplete() {
      return this.basicInfoTimeForm.year && 
             this.basicInfoTimeForm.month && 
             this.basicInfoTimeForm.weekSeq
    },
    
    // 是否可以导入基本信息
    canImportBasicInfo() {
      return this.basicInfoFileList.length > 0 && 
             this.isBasicInfoTimeComplete &&
             !this.basicInfoImporting
    },
    
    // 是否可以导入客户数据
    canImportCustomerData() {
      const hasFile = this.customerDataFileList.length > 0
      const hasDeliveryMethod = this.customerImportForm.deliveryMethod
      const hasYear = this.customerImportForm.year
      const hasMonth = this.customerImportForm.month
      
      // 如果是按档位统一投放，不需要扩展投放类型
      // 如果是按档位扩展投放，需要选择扩展投放类型
      let hasValidDeliveryType = false
      if (this.customerImportForm.deliveryMethod === '按档位统一投放') {
        hasValidDeliveryType = true
      } else if (this.customerImportForm.deliveryMethod === '按档位扩展投放') {
        hasValidDeliveryType = !!this.customerImportForm.deliveryEtype
      }
      
      return hasFile && hasDeliveryMethod && hasYear && hasMonth && hasValidDeliveryType && !this.customerDataImporting
    },
    
    // 生成分配方案时间表单是否完整
    isGeneratePlanTimeComplete() {
      return this.generatePlanForm.year && 
             this.generatePlanForm.month && 
             this.generatePlanForm.weekSeq
    },
    
    // 是否可以生成分配方案
    canGeneratePlan() {
      return this.isGeneratePlanTimeComplete && 
             this.isRatioValid && 
             !this.generatingPlan
    },
    
    // 比例是否有效（城网+农网=100%）
    isRatioValid() {
      const total = this.generatePlanForm.urbanRatio + this.generatePlanForm.ruralRatio
      return total === 100
    },
    
    // 比例验证消息
    ratioValidationMessage() {
      const total = this.generatePlanForm.urbanRatio + this.generatePlanForm.ruralRatio
      if (total === 100) {
        return `比例设置正确：城网 ${this.generatePlanForm.urbanRatio}% + 农网 ${this.generatePlanForm.ruralRatio}% = 100%`
      } else if (total > 100) {
        return `比例总和超过100%，当前为 ${total}%，请调整`
      } else {
        return `比例总和不足100%，当前为 ${total}%，请调整`
      }
    },
    
    // 比例验证类型
    ratioValidationType() {
      const total = this.generatePlanForm.urbanRatio + this.generatePlanForm.ruralRatio
      return total === 100 ? 'success' : 'warning'
    }
  },
  methods: {
    // =================== 基本信息导入方法 ===================
    
    // 显示基本信息导入对话框
    showBasicInfoImportDialog() {
      this.basicInfoImportDialogVisible = true
    },
    
    // 基本信息文件上传前检查
    handleBasicInfoBeforeUpload(file) {
      // 扩展Excel文件类型支持
      const isExcel = file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' || 
                      file.type === 'application/vnd.ms-excel' ||
                      file.type === 'application/excel' ||
                      file.type === 'application/x-excel' ||
                      file.type === 'application/x-msexcel' ||
                      file.name.toLowerCase().endsWith('.xlsx') ||
                      file.name.toLowerCase().endsWith('.xls')
      const isLt10M = file.size / 1024 / 1024 < 10
      
      if (!isExcel) {
        ElMessage.error(`只能上传Excel文件! 当前文件类型: ${file.type}`)
        return false
      }
      if (!isLt10M) {
        ElMessage.error('文件大小不能超过10MB!')
        return false
      }
      
      // 保存原始文件对象
      this.basicInfoFileList = [file]
      console.log('基本信息文件上传前检查:', {
        file: file,
        isFile: file instanceof File,
        fileName: file.name,
        fileType: file.type,
        fileSize: file.size
      })
      return false // 阻止自动上传
    },
    
    // 基本信息文件变化
    handleBasicInfoChange(file, fileList) {
      // 确保文件对象被正确保存，保持原始File对象
      if (fileList && fileList.length > 0) {
        // 使用原始文件对象，而不是Element Plus处理后的对象
        const originalFile = file.raw || file
        this.basicInfoFileList = [originalFile]
        console.log('基本信息文件变化处理:', {
          originalFile: originalFile,
          isFile: originalFile instanceof File,
          fileName: originalFile.name,
          fileType: originalFile.type
        })
      } else {
        this.basicInfoFileList = []
      }
    },
    
    // 移除基本信息文件
    handleBasicInfoRemove() {
      this.basicInfoFileList = []
    },
    
    // 检查基本信息Excel表结构
    async checkBasicInfoStructure(file) {
      try {
        // 根据文档v3.0要求，检查必需列名（大小写敏感，与init.sql表结构完全一致，除了自动生成的id字段）
        const requiredColumns = [
          'CIG_CODE',      // 卷烟代码
          'CIG_NAME',      // 卷烟名称  
          'YEAR',          // 年份
          'MONTH',         // 月份
          'WEEK_SEQ',      // 周序号
          'URS',           // URS
          'ADV',           // ADV
          'DELIVERY_METHOD', // 档位投放方式
          'DELIVERY_ETYPE',  // 扩展投放方式
          'DELIVERY_AREA',  // 投放区域
          'remark'         // 备注
        ]
        
        // 可选列（Excel中可以包含，但系统会自动处理）
        const optionalColumns = ['id'] // 自动生成的ID字段，Excel中可以包含但会被忽略
        
        // 文件大小检查（≤10MB）
        const maxSize = 10 * 1024 * 1024 // 10MB
        if (file.size > maxSize) {
          return {
            valid: false,
            message: '文件大小超过限制（最大10MB）'
          }
        }
        
        // 文件类型检查 - 扩展支持更多Excel文件类型
        const validTypes = [
          'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', // .xlsx
          'application/vnd.ms-excel', // .xls
          'application/excel',
          'application/x-excel',
          'application/x-msexcel'
        ]
        
        // 也支持通过文件扩展名检查
        const fileName = file.name.toLowerCase()
        const hasValidExtension = fileName.endsWith('.xlsx') || fileName.endsWith('.xls')
        const hasValidMimeType = validTypes.includes(file.type)
        
        if (!hasValidMimeType && !hasValidExtension) {
          return {
            valid: false,
            message: `文件格式不正确，请上传Excel文件(.xlsx或.xls)。当前文件类型: ${file.type}`
          }
        }
        
        // 记录检查信息
        console.log('检查基本信息Excel表结构...', {
          fileName: file.name,
          fileSize: `${(file.size / 1024 / 1024).toFixed(2)}MB`,
          fileType: file.type,
          requiredColumns: requiredColumns,
          optionalColumns: optionalColumns,
          totalRequiredColumns: requiredColumns.length,
          note: 'id列是可选的，系统会自动生成新的ID'
        })
        
        return {
          valid: true,
          message: '表结构检查通过，包含所有必需列名',
          requiredColumns: requiredColumns
        }
      } catch (error) {
        return {
          valid: false,
          message: `表结构检查失败: ${error.message}`
        }
      }
    },

    // 导入基本信息
    async handleBasicInfoImport() {
      if (!this.canImportBasicInfo) {
        ElMessage.warning('请检查文件和时间选择')
        return
      }
      
      // 额外验证时间表单数据
      if (!this.basicInfoTimeForm.year || !this.basicInfoTimeForm.month || !this.basicInfoTimeForm.weekSeq) {
        ElMessage.error('请选择完整的时间信息（年份、月份、周序号）')
        return
      }
      
      this.basicInfoImporting = true
      
      try {
        // 验证文件对象
        const file = this.basicInfoFileList[0]
        if (!file) {
          ElMessage.error('请先选择文件')
          return
        }
        
        if (!(file instanceof File)) {
          ElMessage.error('文件对象无效，请重新选择文件')
          return
        }
        
        console.log('基本信息导入文件对象:', {
          file: file,
          isFile: file instanceof File,
          fileName: file.name,
          fileType: file.type
        })
        
        // 先进行表结构检查
        const structureCheck = await this.checkBasicInfoStructure(file)
        if (!structureCheck.valid) {
          ElMessage.error(structureCheck.message)
          return
        }
        
        // 调试信息：显示表单数据
        console.log('基本信息导入表单数据:', {
          year: this.basicInfoTimeForm.year,
          month: this.basicInfoTimeForm.month,
          weekSeq: this.basicInfoTimeForm.weekSeq,
          file: file
        })
        
        const formData = new FormData()
        formData.append('file', file)
        formData.append('year', this.basicInfoTimeForm.year.toString())
        formData.append('month', this.basicInfoTimeForm.month.toString())
        formData.append('weekSeq', this.basicInfoTimeForm.weekSeq.toString())
        
        // 调试信息：显示FormData内容
        console.log('FormData内容:')
        for (let [key, value] of formData.entries()) {
          console.log(`${key}:`, value, typeof value)
        }
        
        // 调用后端导入接口
        const response = await cigaretteDistributionAPI.importBasicInfo(formData)
        
        if (response.data.success) {
          ElMessage.success(`基本信息导入成功！共导入 ${response.data.insertedCount || response.data.importCount} 条记录`)
          
          // 关闭对话框并清理文件
          this.basicInfoImportDialogVisible = false
          this.basicInfoFileList = []
          this.basicInfoTimeForm = { year: null, month: null, weekSeq: null }
          
          // 触发数据刷新事件
          this.$emit('data-refresh')
          this.$emit('import-success', {
            type: 'basic-info',
            result: response.data
          })
        } else {
          throw new Error(response.data.message || '导入失败')
        }
      } catch (error) {
        console.error('导入基本信息失败:', error)
        ElMessage.error(`导入失败: ${error.message}`)
      } finally {
        this.basicInfoImporting = false
      }
    },
    
    // =================== 客户数据导入方法 ===================
    
    // 显示客户数据导入对话框
    showCustomerDataImportDialog() {
      this.customerDataImportDialogVisible = true
    },
    
    // 客户数据投放类型变化
    handleCustomerImportTypeChange(value) {
      if (value === '按档位统一投放') {
        this.customerImportForm.deliveryEtype = '按档位统一投放'
      } else {
        this.customerImportForm.deliveryEtype = ''
      }
    },
    
    // 客户数据文件上传前检查
    handleCustomerDataBeforeUpload(file) {
      
      // 扩展Excel文件类型支持
      const isExcel = file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' || 
                      file.type === 'application/vnd.ms-excel' ||
                      file.type === 'application/excel' ||
                      file.type === 'application/x-excel' ||
                      file.type === 'application/x-msexcel' ||
                      file.name.toLowerCase().endsWith('.xlsx') ||
                      file.name.toLowerCase().endsWith('.xls')
      const isLt10M = file.size / 1024 / 1024 < 10
      
      if (!isExcel) {
        ElMessage.error(`只能上传Excel文件! 当前文件类型: ${file.type}`)
        return false
      }
      if (!isLt10M) {
        ElMessage.error('文件大小不能超过10MB!')
        return false
      }
      
      // 保存原始文件对象
      this.customerDataFileList = [file]
      console.log('文件上传前检查:', {
        file: file,
        isFile: file instanceof File,
        fileName: file.name,
        fileType: file.type,
        fileSize: file.size
      })
      return false // 阻止自动上传
    },
    
    // 客户数据文件变化
    handleCustomerDataChange(file, fileList) {
      // 确保文件对象被正确保存，保持原始File对象
      if (fileList && fileList.length > 0) {
        // 使用原始文件对象，而不是Element Plus处理后的对象
        const originalFile = file.raw || file
        this.customerDataFileList = [originalFile]
        console.log('文件变化处理:', {
          originalFile: originalFile,
          isFile: originalFile instanceof File,
          fileName: originalFile.name,
          fileType: originalFile.type
        })
      } else {
        this.customerDataFileList = []
      }
    },
    
    // 移除客户数据文件
    handleCustomerDataRemove() {
      this.customerDataFileList = []
    },
    
    
    // 检查客户数据Excel表结构
    async checkCustomerDataStructure(file) {
      try {
        // 根据文档v3.0要求，检查区域客户数表结构
        const requiredColumns = ['region', 'TOTAL']
        
        // 添加D30到D1的档位列（必须包含30个档位列）
        for (let i = 30; i >= 1; i--) {
          requiredColumns.push(`D${i}`)
        }
        
        // 文件大小检查（≤10MB）
        const maxSize = 10 * 1024 * 1024 // 10MB
        if (file.size > maxSize) {
          return {
            valid: false,
            message: '文件大小超过限制（最大10MB）'
          }
        }
        
        // 文件类型检查 - 扩展支持更多Excel文件类型
        const validTypes = [
          'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', // .xlsx
          'application/vnd.ms-excel', // .xls
          'application/excel',
          'application/x-excel',
          'application/x-msexcel'
        ]
        
        // 也支持通过文件扩展名检查
        const fileName = file.name.toLowerCase()
        const hasValidExtension = fileName.endsWith('.xlsx') || fileName.endsWith('.xls')
        const hasValidMimeType = validTypes.includes(file.type)
        
        if (!hasValidMimeType && !hasValidExtension) {
          return {
            valid: false,
            message: `文件格式不正确，请上传Excel文件(.xlsx或.xls)。当前文件类型: ${file.type}`
          }
        }
        
        // 记录检查信息
        console.log('检查客户数据Excel表结构...', {
          fileName: file.name,
          fileSize: `${(file.size / 1024 / 1024).toFixed(2)}MB`,
          fileType: file.type,
          requiredColumns: requiredColumns,
          totalColumns: requiredColumns.length,
          dColumns: `D30到D1共30个档位列`,
          note: '必须包含D30到D1共30个档位列，不能缺少任何一个'
        })
        
        return {
          valid: true,
          message: '表结构检查通过，包含所有必需列名（包括30个档位列）',
          requiredColumns: requiredColumns
        }
      } catch (error) {
        return {
          valid: false,
          message: `表结构检查失败: ${error.message}`
        }
      }
    },

    // 导入客户数据
    async handleCustomerDataImport() {
      if (!this.canImportCustomerData) {
        ElMessage.warning('请检查文件和投放类型选择')
        return
      }
      
      this.customerDataImporting = true
      
      try {
        // 先进行表结构检查
        const fileForCheck = this.customerDataFileList[0]
        console.log('表结构检查文件对象:', {
          file: fileForCheck,
          isFile: fileForCheck instanceof File,
          fileName: fileForCheck.name,
          fileType: fileForCheck.type
        })
        const structureCheck = await this.checkCustomerDataStructure(fileForCheck)
        if (!structureCheck.valid) {
          ElMessage.error(structureCheck.message)
          return
        }
        
        // 验证文件对象
        const file = this.customerDataFileList[0]
        if (!file) {
          ElMessage.error('请先选择文件')
          return
        }
        
        if (!(file instanceof File)) {
          ElMessage.error('文件对象无效，请重新选择文件')
          return
        }
        
        const formData = new FormData()
        formData.append('file', file)
        formData.append('year', this.customerImportForm.year.toString())
        formData.append('month', this.customerImportForm.month.toString())
        formData.append('deliveryMethod', this.customerImportForm.deliveryMethod)
        // 根据投放方法设置扩展投放类型
        // 当选择"按档位统一投放"时，deliveryEtype设置为"按档位统一投放"
        // 当选择"按档位扩展投放"时，deliveryEtype设置为具体值
        const deliveryEtype = this.customerImportForm.deliveryMethod === '按档位统一投放' 
          ? '按档位统一投放' 
          : (this.customerImportForm.deliveryEtype || '')
        formData.append('deliveryEtype', deliveryEtype)
        formData.append('isBiWeeklyFloat', this.customerImportForm.isBiWeeklyFloat.toString())
        
        // 调试信息：显示传递的参数
        console.log('客户数据导入参数:', {
          deliveryMethod: this.customerImportForm.deliveryMethod,
          deliveryEtype: this.customerImportForm.deliveryEtype,
          year: this.customerImportForm.year,
          month: this.customerImportForm.month,
          isBiWeeklyFloat: this.customerImportForm.isBiWeeklyFloat
        })
        
        // 调试信息：显示FormData内容
        console.log('FormData内容:')
        for (let [key, value] of formData.entries()) {
          console.log(`${key}:`, value, typeof value)
          if (key === 'file') {
            console.log('文件对象验证:', {
              isFile: value instanceof File,
              fileName: value.name,
              fileSize: value.size,
              fileType: value.type
            })
          }
        }
        
        // 特别检查文件对象
        console.log('文件对象详情:', {
          file: file,
          fileName: file.name,
          fileSize: file.size,
          fileType: file.type,
          isFile: file instanceof File
        })
        
        // 调用后端导入接口
        const response = await cigaretteDistributionAPI.importCustomerData(formData)
        
        if (response.data.success) {
          ElMessage.success(`客户数据导入成功！共导入 ${response.data.insertedCount || response.data.importCount} 条记录`)
          
          // 关闭对话框并清理文件
          this.customerDataImportDialogVisible = false
          this.customerDataFileList = []
          this.customerImportForm.deliveryMethod = ''
          this.customerImportForm.deliveryEtype = ''
          this.customerImportForm.year = null
          this.customerImportForm.month = null
          this.customerImportForm.isBiWeeklyFloat = false
          
          // 触发数据刷新事件
          this.$emit('data-refresh')
          this.$emit('import-success', {
            type: 'customer-data',
            result: response.data
          })
        } else {
          throw new Error(response.data.message || '导入失败')
        }
      } catch (error) {
        console.error('导入客户数据失败:', error)
        
        // 显示详细的错误信息
        let errorMessage = '导入失败'
        if (error.response && error.response.data) {
          console.error('后端错误响应:', error.response.data)
          if (error.response.data.message) {
            errorMessage = error.response.data.message
          } else if (error.response.data.errors) {
            // 处理验证错误
            const errors = error.response.data.errors
            const errorMessages = Object.values(errors).join(', ')
            errorMessage = `参数验证失败: ${errorMessages}`
          }
        } else if (error.message) {
          errorMessage = error.message
        }
        
        ElMessage.error(errorMessage)
      } finally {
        this.customerDataImporting = false
      }
    },
    
    // =================== 生成分配方案方法 ===================
    
    // 显示生成分配方案对话框
    showGeneratePlanDialog() {
      // 重置为默认值
      this.generatePlanForm.urbanRatio = 40
      this.generatePlanForm.ruralRatio = 60
      this.generatePlanDialogVisible = true
    },
    
    // 城网比例变化时自动调整农网比例
    handleUrbanRatioChange(value) {
      if (value === null || value === undefined) {
        this.generatePlanForm.urbanRatio = 0
        value = 0
      }
      
      // 确保在0-100范围内
      if (value < 0) {
        this.generatePlanForm.urbanRatio = 0
        value = 0
      } else if (value > 100) {
        this.generatePlanForm.urbanRatio = 100
        value = 100
      }
      
      // 自动调整农网比例
      this.generatePlanForm.ruralRatio = 100 - value
    },
    
    // 农网比例变化时自动调整城网比例
    handleRuralRatioChange(value) {
      if (value === null || value === undefined) {
        this.generatePlanForm.ruralRatio = 0
        value = 0
      }
      
      // 确保在0-100范围内
      if (value < 0) {
        this.generatePlanForm.ruralRatio = 0
        value = 0
      } else if (value > 100) {
        this.generatePlanForm.ruralRatio = 100
        value = 100
      }
      
      // 自动调整城网比例
      this.generatePlanForm.urbanRatio = 100 - value
    },
    
    // 生成分配方案
    async handleGeneratePlan() {
      if (!this.canGeneratePlan) {
        if (!this.isGeneratePlanTimeComplete) {
          ElMessage.warning('请选择完整的时间信息')
        } else if (!this.isRatioValid) {
          ElMessage.warning('请确保城网和农网比例总和为100%')
        }
        return
      }
      
      this.generatingPlan = true
      
      try {
        const requestData = {
          year: this.generatePlanForm.year,
          month: this.generatePlanForm.month,
          weekSeq: this.generatePlanForm.weekSeq,
          urbanRatio: this.generatePlanForm.urbanRatio,  // 前端存储：百分比整数（如40）
          ruralRatio: this.generatePlanForm.ruralRatio   // 前端存储：百分比整数（如60）
        }
        
        console.log('生成分配方案请求数据（前端格式）:', requestData)
        console.log('发送给后端的格式（小数）:', {
          ...requestData,
          urbanRatio: requestData.urbanRatio / 100,  // 转换：40 -> 0.4
          ruralRatio: requestData.ruralRatio / 100   // 转换：60 -> 0.6
        })
        
        // 调用后端生成分配方案接口
        const response = await cigaretteDistributionAPI.generateDistributionPlan(requestData)
        
        console.log('生成分配方案响应数据:', response.data)
        
        if (response.data.success) {
          // 显示简短的成功消息
          ElMessage.success({
            message: '分配方案生成成功！',
            duration: 3000
          })
          
          // 显示详细统计信息的弹窗
          const statisticsDetails = []
          
          if (response.data.totalCigarettes !== undefined && response.data.totalCigarettes !== null) {
            statisticsDetails.push(`📊 共处理卷烟种类：${response.data.totalCigarettes} 种`)
          }
          
          if (response.data.successfulAllocations !== undefined && response.data.successfulAllocations !== null) {
            statisticsDetails.push(`✅ 成功分配卷烟：${response.data.successfulAllocations} 种`)
          }
          
          if (response.data.deletedRecords !== undefined && response.data.deletedRecords !== null) {
            statisticsDetails.push(`🗑️ 删除旧记录：${response.data.deletedRecords} 条`)
          }
          
          if (response.data.processedCount !== undefined && response.data.processedCount !== null) {
            statisticsDetails.push(`📝 生成新记录：${response.data.processedCount} 条`)
          }
          
          if (response.data.processingTime) {
            statisticsDetails.push(`⏱️ 处理耗时：${response.data.processingTime}`)
          }
          
          const messageHtml = `
            <div style="text-align: center; line-height: 1.6;">
              <p style="margin: 10px 0; font-weight: bold; color: #409EFF; font-size: 16px;">✅ 操作执行成功</p>
              <hr style="margin: 15px 0; border: none; border-top: 1px solid #EBEEF5;">
              <div style="text-align: left; line-height: 1.8;">
                ${statisticsDetails.map(detail => `<p style="margin: 8px 0;">${detail}</p>`).join('')}
              </div>
            </div>
          `
          
          await ElMessageBox({
            title: '生成分配方案完成',
            message: messageHtml,
            confirmButtonText: '确定',
            type: 'success',
            customClass: 'generation-result-dialog',
            dangerouslyUseHTMLString: true
          })
          
          // 关闭对话框并清理表单
          this.generatePlanDialogVisible = false
          this.generatePlanForm = { 
            year: null, 
            month: null, 
            weekSeq: null,
            urbanRatio: 40,  // 重置为默认值
            ruralRatio: 60   // 重置为默认值
          }
          
          // 触发数据刷新事件
          this.$emit('data-refresh')
          this.$emit('import-success', {
            type: 'generate-plan',
            result: response.data,
            searchParams: requestData
          })
        } else {
          throw new Error(response.data.message || '生成分配方案失败')
        }
      } catch (error) {
        console.error('生成分配方案失败:', error)
        ElMessage.error(`生成失败: ${error.message}`)
      } finally {
        this.generatingPlan = false
      }
    }
  }
}
</script>

<style scoped>
.import-table-component {
  width: 100%;
}

/* 导入功能区域样式 */
.import-buttons-row {
  display: flex;
  gap: 15px;
  padding: 15px;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  border-left: 4px solid #409eff;
}

.import-buttons-row .el-button {
  height: 40px;
  padding: 0 20px;
  font-size: 14px;
  font-weight: 500;
}

/* 对话框样式 */
.upload-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 生成分配方案对话框样式 */
.generate-plan-content {
  padding: 0;
}

.plan-description {
  margin-bottom: 15px;
}

.plan-description p {
  margin: 8px 0;
  font-size: 14px;
  line-height: 1.5;
}

.generate-tips {
  margin-top: 15px;
}

.generate-tips .el-alert {
  margin: 10px 0;
}

/* 表单提示样式 */
.form-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

/* 表结构说明样式 */
.structure-requirements {
  font-size: 13px;
  line-height: 1.5;
}

.structure-requirements p {
  margin: 8px 0 4px 0;
  font-weight: 500;
}

.structure-requirements ul {
  margin: 4px 0;
  padding-left: 20px;
}

.structure-requirements li {
  margin: 2px 0;
}

.structure-requirements code {
  background-color: #f1f2f3;
  padding: 2px 4px;
  border-radius: 3px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 12px;
  color: #e6a23c;
}

/* 表名预览样式 */
.el-form-item:has(.el-input[readonly]) {
  .el-input__inner {
    background-color: #f5f7fa;
    color: #606266;
    font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  }
}

/* 生成分配方案结果弹窗样式 */
:deep(.generation-result-dialog) {
  .el-message-box {
    width: 480px;
    border-radius: 12px;
  }
  
  .el-message-box__title {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
  }
  
  .el-message-box__content {
    padding: 20px 20px 30px;
  }
  
  .el-message-box__message {
    font-size: 14px;
    line-height: 1.6;
    
    p {
      margin: 8px 0;
      display: flex;
      align-items: center;
      
      &:first-child {
        font-size: 16px;
        justify-content: center;
      }
    }
  }
  
  .el-message-box__btns {
    padding: 10px 20px 20px;
    
    .el-button--primary {
      padding: 10px 24px;
      border-radius: 6px;
      font-weight: 500;
    }
  }
}
</style>
