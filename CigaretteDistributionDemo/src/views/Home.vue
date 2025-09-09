<template>
  <div class="layout-container">
    <!-- 左侧菜单栏 -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <h2>卷烟投放管理系统</h2>
      </div>
      <el-menu
        default-active="1"
        class="sidebar-menu"
        background-color="#2c3e50"
        text-color="#ffffff"
        active-text-color="#409EFF"
      >
        <el-menu-item index="1">
          <el-icon><Grid /></el-icon>
          <span>模型投放</span>
        </el-menu-item>
      </el-menu>
    </aside>

     <!-- 主内容区域 -->
     <main class="main-content">
       <!-- 数据导入功能区域 -->
       <section class="import-section">
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
       </section>
       
       <!-- 上方数据表格区域 -->
       <section class="data-table-section">
        <DataTable 
          ref="dataTable"
          :search-params="searchParams"
          @row-selected="handleRowSelected"
          @data-loaded="handleDataLoaded"
        />
      </section>

      <!-- 中间表单区域 -->
      <section class="form-section">
        <SearchForm 
          :selected-record="selectedRecord"
          :table-data="tableData"
          @search="handleSearch"
          @search-next="handleSearchNext"
          @reset="handleReset"
          @export="handleExport"
          @cigarette-name-matched="handleCigaretteNameMatched"
          @position-updated="handlePositionUpdated"
          @area-added="handleAreaAdded"
          @areas-deleted="handleAreasDeleted"
        />
      </section>

    </main>
    
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
            :on-remove="handleBasicInfoRemove"
          >
            <el-button type="primary">
              <el-icon><Plus /></el-icon>
              选择Excel文件
            </el-button>
          </el-upload>
          <div class="upload-tip">支持Excel格式(.xlsx, .xls)，文件大小不超过10MB</div>
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
        <el-form-item label="投放类型" required>
          <el-select
            v-model="customerImportForm.distributionType"
            placeholder="请选择投放类型"
            style="width: 100%"
            @change="handleCustomerImportTypeChange"
          >
            <el-option label="按档位统一投放" value="按档位统一投放" />
            <el-option label="按档位扩展投放" value="按档位扩展投放" />
          </el-select>
        </el-form-item>
        
        <el-form-item 
          v-if="customerImportForm.distributionType === '按档位扩展投放'" 
          label="扩展投放类型" 
          required
        >
          <el-select
            v-model="customerImportForm.extendedType"
            placeholder="请选择扩展投放类型"
            style="width: 100%"
          >
            <el-option label="档位+区县" value="档位+区县" />
            <el-option label="档位+城乡分类代码" value="档位+城乡分类代码" />
            <el-option label="档位+市场类型" value="档位+市场类型" />
            <el-option label="档位+业态" value="档位+业态" />
          </el-select>
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
            :on-remove="handleCustomerDataRemove"
          >
            <el-button type="primary">
              <el-icon><Plus /></el-icon>
              选择Excel文件
            </el-button>
          </el-upload>
          <div class="upload-tip">支持Excel格式(.xlsx, .xls)，文件大小不超过10MB</div>
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
import { Grid, DocumentAdd, DataAnalysis, Plus, Cpu } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import DataTable from '../components/DataTable.vue'
import SearchForm from '../components/SearchForm.vue'
import { cigaretteDistributionAPI } from '../services/api'

export default {
  name: 'Home',
  components: {
    Grid,
    DataTable,
    SearchForm,
    DocumentAdd,
    DataAnalysis,
    Plus,
    Cpu
  },
  data() {
    return {
      searchParams: {},
      selectedCigaretteName: '',
      currentPositionData: {},
      selectedRecord: null,
      tableData: [],
      
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
        distributionType: '',
        extendedType: ''
      },
      
      // 生成分配方案
      generatePlanDialogVisible: false,
      generatePlanForm: {
        year: null,
        month: null,
        weekSeq: null
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
      const hasDistributionType = this.customerImportForm.distributionType
      const hasExtendedType = this.customerImportForm.distributionType !== '按档位扩展投放' || 
                              this.customerImportForm.extendedType
      
      return hasFile && hasDistributionType && hasExtendedType && !this.customerDataImporting
    },
    
    // 生成分配方案时间表单是否完整
    isGeneratePlanTimeComplete() {
      return this.generatePlanForm.year && 
             this.generatePlanForm.month && 
             this.generatePlanForm.weekSeq
    },
    
    // 是否可以生成分配方案
    canGeneratePlan() {
      return this.isGeneratePlanTimeComplete && !this.generatingPlan
    }
  },
  methods: {
    handleSearch(searchForm) {
      console.log('搜索参数:', searchForm)
      
      // 更新搜索参数，触发表格和档位设置的更新
      this.searchParams = { 
        year: searchForm.year,
        month: searchForm.month,
        weekSeq: searchForm.week
      }
      
      ElMessage.success(`已查询：${searchForm.year}年${searchForm.month}月第${searchForm.week}周`)
    },
    
    handleDataLoaded(data) {
      // 当表格数据加载完成时
      this.tableData = data
      console.log('表格数据已加载:', data)
      
      // 注意：不在这里清除选中状态，让DataTable组件处理自动选中逻辑
    },
    
    handleRowSelected(row) {
      // 行被选中时，查找该卷烟在当前日期的所有投放记录
      const relatedRecords = this.tableData.filter(record => 
        record.cigName === row.cigName &&
        record.year === row.year &&
        record.month === row.month &&
        record.weekSeq === row.weekSeq
      )
      
      // 收集所有投放区域
      const allAreas = relatedRecords.map(record => record.deliveryArea).filter(area => area)
      
      // 更新选中的卷烟名称和记录
      this.selectedCigaretteName = row.cigName
      
      // 创建包含所有区域信息的选中记录
      this.selectedRecord = {
        ...row,
        allAreas: allAreas,
        totalRecords: relatedRecords.length
      }
      
      
      console.log('选中行及相关记录:', {
        selectedRow: row,
        relatedRecords: relatedRecords,
        allAreas: allAreas
      })
      
      ElMessage.info(`已选中：${row.cigName}，该日期共有 ${relatedRecords.length} 个投放区域`)
    },
    
    
    handleReset() {
      // 重置所有搜索条件和状态
      this.searchParams = {}
      this.selectedCigaretteName = ''
      this.currentPositionData = {}
      this.selectedRecord = null
      this.tableData = []
      
      // 通知DataTable组件清除选中状态
      if (this.$refs.dataTable) {
        this.$refs.dataTable.selectedRow = null
      }
      
      ElMessage.info('已重置查询条件')
    },
    
    handleExport(searchForm) {
      console.log('导出参数:', searchForm)
      
      // 检查是否有数据可导出
      if (!this.tableData || this.tableData.length === 0) {
        ElMessage.warning('暂无数据可导出，请先查询数据')
        return
      }
      
      // 调用DataTable组件的导出功能
      if (this.$refs.dataTable) {
        this.$refs.dataTable.handleExport()
      } else {
        ElMessage.error('导出组件未找到')
      }
    },
    
    handleSearchNext() {
      // 调用DataTable组件的searchNext方法
      if (this.$refs.dataTable) {
        this.$refs.dataTable.searchNext()
      }
    },
    
    handleCigaretteNameMatched(matchedRecords) {
      // 当卷烟名称搜索匹配到记录时，处理多个记录
      console.log('卷烟名称匹配到记录:', matchedRecords)
      
      if (Array.isArray(matchedRecords) && matchedRecords.length > 0) {
        // 取第一个记录作为主要记录
        const primaryRecord = matchedRecords[0]
        
        // 收集所有投放区域
        const allAreas = matchedRecords.map(record => record.deliveryArea).filter(area => area)
        
        // 更新选中的卷烟名称
        this.selectedCigaretteName = primaryRecord.cigName
        
        // 创建包含所有区域信息的选中记录
        this.selectedRecord = {
          ...primaryRecord,
          allAreas: allAreas,
          totalRecords: matchedRecords.length
        }
        
        
        // 同步更新DataTable的选中状态（选中第一个记录）
        if (this.$refs.dataTable) {
          this.$refs.dataTable.scrollToSelectedRecord(primaryRecord)
        }
        
        console.log('搜索匹配选中记录:', {
          primaryRecord: primaryRecord,
          allMatchedRecords: matchedRecords,
          allAreas: allAreas
        })
      }
    },
    
    // 处理档位设置更新
    async handlePositionUpdated(updateInfo) {
      console.log('档位设置已更新:', updateInfo)
      
      try {
        // 刷新表格数据以显示更新后的结果
        if (this.$refs.dataTable) {
          await this.$refs.dataTable.handleRefresh()
          
          // 保持当前选中状态
          setTimeout(() => {
            if (this.selectedRecord && this.selectedRecord.cigCode === updateInfo.cigCode) {
              // 找到更新后的记录并重新选中
              const updatedRecord = this.tableData.find(record => 
                record.cigCode === updateInfo.cigCode &&
                record.year === updateInfo.updateData.year &&
                record.month === updateInfo.updateData.month &&
                record.weekSeq === updateInfo.updateData.weekSeq
              )
              
              if (updatedRecord) {
                this.handleRowSelected(updatedRecord)
              }
            }
          }, 500)
        }
        
        ElMessage.success('表格数据已刷新，显示最新的档位设置')
      } catch (error) {
        console.error('刷新表格数据失败:', error)
        ElMessage.warning('档位设置已保存，但表格刷新失败，请手动刷新')
      }
    },
    
    // 处理新增投放区域
    async handleAreaAdded(addInfo) {
      console.log('新增投放区域:', addInfo)
      
      try {
        // 刷新表格数据以显示新增的记录
        if (this.$refs.dataTable) {
          await this.$refs.dataTable.handleRefresh()
          
          // 等待表格数据更新后，重新选中该卷烟的所有记录
          setTimeout(() => {
            if (this.selectedRecord && this.selectedRecord.cigCode === addInfo.cigCode) {
              // 找到该卷烟在当前日期的所有记录（包括新增的）
              const allRecordsForCigarette = this.tableData.filter(record => 
                record.cigCode === addInfo.cigCode &&
                record.year === this.selectedRecord.year &&
                record.month === this.selectedRecord.month &&
                record.weekSeq === this.selectedRecord.weekSeq
              )
              
              if (allRecordsForCigarette.length > 0) {
                // 选中第一个记录，这会触发所有相关记录的高亮显示
                this.handleRowSelected(allRecordsForCigarette[0])
                
                // 滚动到选中的记录组
                if (this.$refs.dataTable) {
                  this.$refs.dataTable.scrollToSelectedRecord(allRecordsForCigarette[0])
                }
              }
            }
          }, 500)
        }
        
        ElMessage.success(`表格已刷新，新增的投放区域记录已显示并集中相邻排列`)
      } catch (error) {
        console.error('刷新表格数据失败:', error)
        ElMessage.warning('投放区域已新增，但表格刷新失败，请手动刷新')
      }
    },
    
    // 处理删除投放区域
    async handleAreasDeleted(deleteInfo) {
      console.log('删除投放区域:', deleteInfo)
      
      try {
        // 刷新表格数据以移除删除的记录
        if (this.$refs.dataTable) {
          await this.$refs.dataTable.handleRefresh()
          
          // 等待表格数据更新后，重新选中该卷烟的剩余记录
          setTimeout(() => {
            if (this.selectedRecord && this.selectedRecord.cigCode === deleteInfo.cigCode) {
              // 找到该卷烟在当前日期的剩余记录
              const remainingRecordsForCigarette = this.tableData.filter(record => 
                record.cigCode === deleteInfo.cigCode &&
                record.year === this.selectedRecord.year &&
                record.month === this.selectedRecord.month &&
                record.weekSeq === this.selectedRecord.weekSeq
              )
              
              if (remainingRecordsForCigarette.length > 0) {
                // 选中第一个剩余记录，这会触发所有相关记录的高亮显示
                this.handleRowSelected(remainingRecordsForCigarette[0])
                
                // 滚动到选中的记录组
                if (this.$refs.dataTable) {
                  this.$refs.dataTable.scrollToSelectedRecord(remainingRecordsForCigarette[0])
                }
              } else {
                // 如果没有剩余记录，清除选中状态
                this.selectedRecord = null
                this.selectedCigaretteName = ''
              }
            }
          }, 500)
        }
        
        ElMessage.success(`表格已刷新，已删除的投放区域记录已移除`)
      } catch (error) {
        console.error('刷新表格数据失败:', error)
        ElMessage.warning('投放区域已删除，但表格刷新失败，请手动刷新')
      }
    },
    
    // =================== 导入功能方法 ===================
    
    // 显示基本信息导入对话框
    showBasicInfoImportDialog() {
      this.basicInfoImportDialogVisible = true
    },
    
    // 显示客户数据导入对话框
    showCustomerDataImportDialog() {
      this.customerDataImportDialogVisible = true
    },
    
    // 基本信息文件上传前检查
    handleBasicInfoBeforeUpload(file) {
      const isExcel = file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' || 
                      file.type === 'application/vnd.ms-excel'
      const isLt10M = file.size / 1024 / 1024 < 10
      
      if (!isExcel) {
        ElMessage.error('只能上传Excel文件!')
        return false
      }
      if (!isLt10M) {
        ElMessage.error('文件大小不能超过10MB!')
        return false
      }
      
      this.basicInfoFileList = [file]
      return false // 阻止自动上传
    },
    
    // 移除基本信息文件
    handleBasicInfoRemove() {
      this.basicInfoFileList = []
    },
    
    // 导入基本信息
    async handleBasicInfoImport() {
      if (!this.canImportBasicInfo) {
        ElMessage.warning('请检查文件和时间选择')
        return
      }
      
      this.basicInfoImporting = true
      
      try {
        const formData = new FormData()
        formData.append('file', this.basicInfoFileList[0])
        formData.append('year', this.basicInfoTimeForm.year)
        formData.append('month', this.basicInfoTimeForm.month)
        formData.append('weekSeq', this.basicInfoTimeForm.weekSeq)
        
        // 调用后端导入接口
        const response = await cigaretteDistributionAPI.importBasicInfo(formData)
        
        if (response.data.success) {
          ElMessage.success(`基本信息导入成功！共导入 ${response.data.importCount} 条记录`)
          
          // 关闭对话框并清理文件
          this.basicInfoImportDialogVisible = false
          this.basicInfoFileList = []
          this.basicInfoTimeForm = { year: null, month: null, weekSeq: null }
          
          // 刷新表格数据
          if (this.$refs.dataTable) {
            this.$refs.dataTable.handleRefresh()
          }
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
    
    // 客户数据投放类型变化
    handleCustomerImportTypeChange(value) {
      this.customerImportForm.extendedType = ''
    },
    
    // 客户数据文件上传前检查
    handleCustomerDataBeforeUpload(file) {
      const isExcel = file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' || 
                      file.type === 'application/vnd.ms-excel'
      const isLt10M = file.size / 1024 / 1024 < 10
      
      if (!isExcel) {
        ElMessage.error('只能上传Excel文件!')
        return false
      }
      if (!isLt10M) {
        ElMessage.error('文件大小不能超过10MB!')
        return false
      }
      
      this.customerDataFileList = [file]
      return false // 阻止自动上传
    },
    
    // 移除客户数据文件
    handleCustomerDataRemove() {
      this.customerDataFileList = []
    },
    
    // 导入客户数据
    async handleCustomerDataImport() {
      if (!this.canImportCustomerData) {
        ElMessage.warning('请检查文件和投放类型选择')
        return
      }
      
      this.customerDataImporting = true
      
      try {
        const formData = new FormData()
        formData.append('file', this.customerDataFileList[0])
        formData.append('distributionType', this.customerImportForm.distributionType)
        if (this.customerImportForm.extendedType) {
          formData.append('extendedType', this.customerImportForm.extendedType)
        }
        
        // 调用后端导入接口
        const response = await cigaretteDistributionAPI.importCustomerData(formData)
        
        if (response.data.success) {
          ElMessage.success(`客户数据导入成功！共导入 ${response.data.importCount} 条记录`)
          
          // 关闭对话框并清理文件
          this.customerDataImportDialogVisible = false
          this.customerDataFileList = []
          this.customerImportForm.distributionType = ''
          this.customerImportForm.extendedType = ''
          
          // 刷新表格数据
          if (this.$refs.dataTable) {
            this.$refs.dataTable.handleRefresh()
          }
        } else {
          throw new Error(response.data.message || '导入失败')
        }
      } catch (error) {
        console.error('导入客户数据失败:', error)
        ElMessage.error(`导入失败: ${error.message}`)
      } finally {
        this.customerDataImporting = false
      }
    },
    
    // =================== 生成分配方案功能方法 ===================
    
    // 显示生成分配方案对话框
    showGeneratePlanDialog() {
      this.generatePlanDialogVisible = true
    },
    
    // 生成分配方案
    async handleGeneratePlan() {
      if (!this.canGeneratePlan) {
        ElMessage.warning('请选择年份、月份和周序号')
        return
      }
      
      this.generatingPlan = true
      
      try {
        const requestData = {
          year: this.generatePlanForm.year,
          month: this.generatePlanForm.month,
          weekSeq: this.generatePlanForm.weekSeq
        }
        
        console.log('生成分配方案请求数据:', requestData)
        
        // 调用后端生成分配方案接口
        const response = await cigaretteDistributionAPI.generateDistributionPlan(requestData)
        
        if (response.data.success) {
          ElMessage.success({
            message: `分配方案生成成功！共处理 ${response.data.processedCount || 0} 条卷烟记录`,
            duration: 3000
          })
          
          // 关闭对话框并清理表单
          this.generatePlanDialogVisible = false
          this.generatePlanForm = { year: null, month: null, weekSeq: null }
          
          // 刷新表格数据
          if (this.$refs.dataTable) {
            this.$refs.dataTable.handleRefresh()
          }
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
  },
  
  beforeUnmount() {
    // 清理定时器
    if (this.updateTimer) {
      clearTimeout(this.updateTimer)
    }
  }
}
</script>

<style scoped>
.layout-container {
  display: flex;
  height: 100vh;
}

.sidebar {
  width: 240px;
  background: linear-gradient(180deg, #34495e 0%, #2c3e50 100%);
  color: white;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 20px;
  text-align: center;
  border-bottom: 1px solid #34495e;
}

.sidebar-header h2 {
  font-size: 18px;
  font-weight: 500;
  margin: 0;
}

.sidebar-menu {
  flex: 1;
  border: none;
}

.sidebar-menu .el-menu-item {
  height: 50px;
  line-height: 50px;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  overflow: auto;
  padding: 15px;
  gap: 15px;
  height: calc(100vh - 0px);
}

/* 导入功能区域样式 */
.import-section {
  flex: 0 0 auto;
}

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

.data-table-section {
  flex: 1.2;
  min-height: 280px;
  max-height: 32vh;
  padding: 12px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.form-section {
  flex: 0 0 auto;
  padding: 12px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}
</style>