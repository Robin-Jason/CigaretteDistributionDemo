<template>
  <div class="search-form-container">
    <el-form :model="searchForm" :inline="true" class="search-form">
      <el-form-item label="年份">
        <el-select
          v-model="searchForm.year"
          placeholder="请选择年份"
          style="width: 120px"
          clearable
        >
          <el-option 
            v-for="year in yearOptions" 
            :key="year" 
            :label="year" 
            :value="year"
          />
        </el-select>
      </el-form-item>
      
      <el-form-item label="月份">
        <el-select
          v-model="searchForm.month"
          placeholder="请选择月份"
          style="width: 120px"
          clearable
        >
          <el-option v-for="month in 12" :key="month" :label="`${month}月`" :value="month" />
        </el-select>
      </el-form-item>
      
      <el-form-item label="周序号">
        <el-input-number
          v-model="searchForm.week"
          placeholder="请输入周序号"
          style="width: 120px"
          :min="1"
          :max="5"
          clearable
        />
      </el-form-item>
      
      <el-form-item label="卷烟名称">
        <el-input
          v-model="searchForm.cigaretteName"
          :placeholder="cigaretteNamePlaceholder"
          style="width: 200px"
          clearable
          :disabled="!isDateComplete"
          @input="handleCigaretteNameInput"
          @change="handleCigaretteNameChange"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      
      <el-form-item label="投放类型">
        <el-select
          v-model="searchForm.distributionType"
          placeholder="请选择投放类型"
          style="width: 180px"
          clearable
          @change="handleDistributionTypeChange"
        >
          <el-option label="按档位统一投放" value="按档位统一投放" />
          <el-option label="按档位扩展投放" value="按档位扩展投放" />
        </el-select>
      </el-form-item>
      
      <el-form-item label="扩展投放类型" v-if="searchForm.distributionType === '按档位扩展投放'">
        <el-select
          v-model="searchForm.extendedType"
          placeholder="请选择扩展类型"
          style="width: 160px"
          clearable
          @change="handleExtendedTypeChange"
        >
          <el-option label="档位+区县" value="档位+区县" />
          <el-option label="档位+市场类型" value="档位+市场类型" />
          <el-option label="档位+城乡分类代码" value="档位+城乡分类代码" />
          <el-option label="档位+业态" value="档位+业态" />
        </el-select>
      </el-form-item>
      
      <el-form-item label="投放区域">
        <el-select
          v-model="searchForm.distributionArea"
          :placeholder="areaPlaceholder"
          style="width: 300px"
          multiple
          collapse-tags
          collapse-tags-tooltip
          clearable
        >
          <el-option
            v-for="option in deliveryAreaOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      
      <!-- 新增：预投放量和实际投放量显示框（仅在选中卷烟后显示） -->
      <el-form-item label="预投放量" v-if="selectedRecord && selectedRecord.cigCode">
        <el-input
          :value="formatQuantity(selectedRecord.advAmount)"
          style="width: 150px"
          readonly
          suffix-icon="el-icon-info"
        />
      </el-form-item>
      
      <el-form-item label="实际投放量" v-if="selectedRecord && selectedRecord.cigCode">
        <el-input
          :value="formatQuantity(selectedRecord.actualDelivery)"
          style="width: 150px"
          readonly
          suffix-icon="el-icon-info"
        />
      </el-form-item>
      
      <!-- 档位设置区域（仅在选中卷烟后显示） -->
      <div v-if="selectedRecord && selectedRecord.cigCode" class="position-settings-section">
        <el-divider content-position="left">
          <el-icon><Setting /></el-icon>
          档位设置
        </el-divider>
        
        <div class="position-grid">
          <div v-for="(position, index) in positionData" :key="`d${30 - index}`" class="position-item">
            <span class="position-label">D{{ 30 - index }}:</span>
            <el-input-number
              v-model="positionData[index]"
              :min="0"
              :precision="0"
              :step="1"
              size="small"
              style="width: 90px"
            />
          </div>
        </div>
        
        <div class="position-actions">
          <el-button 
            type="primary" 
            size="small"
            @click="handleSavePositions"
            :loading="savingPositions"
            :disabled="!isPositionDataValid"
          >
            <el-icon><Check /></el-icon>
            保存档位设置
          </el-button>
          <el-button 
            type="info" 
            size="small"
            @click="handleResetPositions"
          >
            <el-icon><RefreshLeft /></el-icon>
            重置档位
          </el-button>
          <el-button 
            type="success" 
            size="small"
            @click="handleAddNewArea"
            :disabled="!canAddNewArea"
          >
            <el-icon><Plus /></el-icon>
            新增投放区域
          </el-button>
          <el-button 
            type="danger" 
            size="small"
            @click="handleDeleteAreas"
            :disabled="!canDeleteAreas"
          >
            <el-icon><Delete /></el-icon>
            删除投放区域
          </el-button>
        </div>
        
        <!-- 删除投放区域选择框（仅在有现有区域时显示） -->
        <div v-if="selectedRecord && selectedRecord.allAreas && selectedRecord.allAreas.length > 0" class="delete-area-section">
          <el-divider content-position="left">
            <el-icon><Delete /></el-icon>
            选择要删除的投放区域
          </el-divider>
          <el-checkbox-group v-model="areasToDelete" class="delete-area-checkboxes">
            <el-checkbox 
              v-for="area in selectedRecord.allAreas" 
              :key="area" 
              :value="area"
              :disabled="selectedRecord.allAreas.length <= 1"
            >
              {{ area }}
            </el-checkbox>
          </el-checkbox-group>
          <div class="delete-area-tips">
            <el-alert
              v-if="selectedRecord.allAreas.length <= 1"
              title="无法删除：该卷烟至少需要保留一个投放区域"
              type="warning"
              :closable="false"
              show-icon
            />
            <el-alert
              v-else-if="areasToDelete.length === 0"
              title="请选择要删除的投放区域"
              type="info"
              :closable="false"
              show-icon
            />
            <el-alert
              v-else
              :title="`已选择 ${areasToDelete.length} 个区域进行删除`"
              type="success"
              :closable="false"
              show-icon
            />
          </div>
        </div>
      </div>
      
      <el-form-item>
        <el-button 
          type="primary" 
          @click="handleSearch"
          :disabled="!searchForm.year || !searchForm.month || !searchForm.week"
        >
          <el-icon><Search /></el-icon>
          查询
        </el-button>
        <el-button 
          type="info" 
          @click="handleSearchNext"
          :disabled="!searchForm.cigaretteName"
        >
          <el-icon><ArrowDown /></el-icon>
          下一个
        </el-button>
        <el-button @click="handleReset">
          <el-icon><RefreshLeft /></el-icon>
          重置
        </el-button>
        <el-button type="success" @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { Search, RefreshLeft, Download, ArrowDown, LocationInformation, Setting, Check, Plus, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cigaretteDistributionAPI } from '@/services/api'

export default {
  name: 'SearchForm',
  props: {
    selectedRecord: {
      type: Object,
      default: null
    },
    tableData: {
      type: Array,
      default: () => []
    }
  },
  components: {
    Search,
    RefreshLeft,
    Download,
    ArrowDown,
    LocationInformation,
    Setting,
    Check,
    Plus,
    Delete
  },
  data() {
    return {
      searchForm: {
        year: null,
        month: null,
        week: null,
        cigaretteName: '',
        distributionType: '',
        extendedType: '',
        distributionArea: []
      },
      // 不同扩展类型对应的区域选项
      areaOptions: {
        '按档位统一投放': [
          { label: '全市', value: '全市' }
        ],
        '档位+区县': [
          { label: '丹江', value: '丹江' },
          { label: '房县', value: '房县' },
          { label: '郧西', value: '郧西' },
          { label: '郧阳', value: '郧阳' },
          { label: '竹山', value: '竹山' },
          { label: '竹溪', value: '竹溪' },
          { label: '城区', value: '城区' }
        ],
        '档位+城乡分类代码': [
          { label: '主城区', value: '主城区' },
          { label: '城乡结合区', value: '城乡结合区' },
          { label: '镇中心区', value: '镇中心区' },
          { label: '镇乡结合区', value: '镇乡结合区' },
          { label: '特殊区域', value: '特殊区域' },
          { label: '乡中心区', value: '乡中心区' },
          { label: '村庄', value: '村庄' }
        ],
        '档位+市场类型': [
          { label: '城网', value: '城网' },
          { label: '农网', value: '农网' }
        ],
        '档位+业态': [
          { label: '便利店', value: '便利店' },
          { label: '超市', value: '超市' },
          { label: '商场', value: '商场' },
          { label: '烟草专卖店', value: '烟草专卖店' },
          { label: '娱乐服务类', value: '娱乐服务类' },
          { label: '其他', value: '其他' }
        ]
      },
      // 档位数据（D30到D1，30个档位）
      positionData: new Array(30).fill(0),
      // 保存状态
      savingPositions: false,
      // 要删除的投放区域
      areasToDelete: []
    }
  },
  computed: {
    // 年份选项（当前年份往前2年，往后10年）
    yearOptions() {
      const currentYear = new Date().getFullYear()
      const years = []
      for (let year = currentYear - 2; year <= currentYear + 10; year++) {
        years.push(year)
      }
      return years
    },
    isDateComplete() {
      return this.searchForm.year && this.searchForm.month && this.searchForm.week
    },
    cigaretteNamePlaceholder() {
      if (!this.isDateComplete) {
        return '请先填充年份、月份和周序号'
      }
      return '请输入卷烟名称'
    },
    deliveryAreaOptions() {
      // 根据投放类型和扩展投放类型确定可选的投放区域
      if (this.searchForm.distributionType === '按档位统一投放') {
        return this.areaOptions['按档位统一投放'] || []
      } else if (this.searchForm.distributionType === '按档位扩展投放') {
        if (this.searchForm.extendedType) {
          return this.areaOptions[this.searchForm.extendedType] || []
        } else {
          return []
        }
      }
      return []
    },
    areaPlaceholder() {
      if (this.searchForm.distributionType === '按档位统一投放') {
        return '请选择投放区域（统一投放）'
      } else if (this.searchForm.distributionType === '按档位扩展投放' && !this.searchForm.extendedType) {
        return '请先选择扩展投放类型'
      } else if (this.deliveryAreaOptions.length > 0) {
        return '请选择投放区域'
      }
      return '请先选择投放类型'
    },
    // 验证档位数据是否有效
    isPositionDataValid() {
      if (!this.positionData || this.positionData.length !== 30) {
        return false
      }
      
      // 只检查是否有数值，不再检查约束条件
      return this.positionData.some(val => val > 0)
    },
    // 检查是否可以新增投放区域
    canAddNewArea() {
      return this.selectedRecord && 
             this.selectedRecord.cigCode && 
             this.searchForm.distributionArea && 
             this.searchForm.distributionArea.length > 0 &&
             this.isPositionDataValid
    },
    // 检查是否可以删除投放区域
    canDeleteAreas() {
      return this.selectedRecord && 
             this.selectedRecord.cigCode && 
             this.selectedRecord.allAreas && 
             this.selectedRecord.allAreas.length > 1 && // 至少要保留一个区域
             this.areasToDelete && 
             this.areasToDelete.length > 0
    }
  },
  watch: {
    selectedRecord: {
      handler(newRecord) {
        if (newRecord) {
          // 当选中记录变化时，自动填充表单
          this.searchForm.year = newRecord.year
          this.searchForm.month = newRecord.month
          this.searchForm.week = newRecord.weekSeq || newRecord.week
          this.searchForm.cigaretteName = newRecord.cigName || newRecord.cigaretteName
          
          // 自动填充投放类型信息
          if (newRecord.deliveryMethod) {
            this.searchForm.distributionType = newRecord.deliveryMethod
          }
          
          // 自动填充扩展投放类型
          if (newRecord.deliveryEtype) {
            this.searchForm.extendedType = newRecord.deliveryEtype
          }
          
          // 自动填充投放区域
          if (newRecord.allAreas && Array.isArray(newRecord.allAreas)) {
            // 如果传入的是多个记录（通过搜索选中），则自动勾选所有投放区域
            this.searchForm.distributionArea = [...newRecord.allAreas]
          } else if (newRecord.deliveryArea) {
            // 如果是单个记录，则勾选该记录的投放区域
            this.searchForm.distributionArea = [newRecord.deliveryArea]
          }
          
          // 加载档位数据
          this.loadPositionData(newRecord)
          
          console.log('选中记录更新并自动填充:', {
            record: newRecord,
            form: this.searchForm
          })
        } else {
          // 清空档位数据和删除选择
          this.positionData = new Array(30).fill(0)
          this.areasToDelete = []
        }
      },
      immediate: true
    }
  },
  methods: {
    handleDistributionTypeChange(value) {
      // 投放类型变化时重置相关字段（仅当非自动填充时）
      if (!this.selectedRecord) {
        this.searchForm.extendedType = ''
        this.searchForm.distributionArea = []
      }
    },
    handleExtendedTypeChange() {
      // 扩展投放类型变化时重置区域选择（仅当非自动填充时）
      if (!this.selectedRecord) {
        this.searchForm.distributionArea = []
      }
    },
    handleSearch() {
      // 至少需要选择一个时间条件或卷烟名称
      if (!this.searchForm.year || !this.searchForm.month || !this.searchForm.week) {
        ElMessage.warning('请至少选择一个时间条件')
        return
      }
      
      // 如果选择了投放类型，需要完整填写相关信息
      if (this.searchForm.distributionType) {
        if (this.searchForm.distributionType === '按档位扩展投放' && !this.searchForm.extendedType) {
          ElMessage.warning('请选择扩展投放类型')
          return
        }
        
        if (!this.searchForm.distributionArea) {
          ElMessage.warning('请选择投放区域')
          return
        }
      }
      
      console.log('搜索条件:', this.searchForm)
      
      let message = '查询条件：'
      if (this.searchForm.year) message += `${this.searchForm.year}年 `
      if (this.searchForm.month) message += `${this.searchForm.month}月 `
      if (this.searchForm.week) message += `第${this.searchForm.week}周 `
      if (this.searchForm.cigaretteName) message += `卷烟：${this.searchForm.cigaretteName}`
      
      ElMessage.success(message)
      this.$emit('search', this.searchForm)
    },
    handleSearchNext() {
      if (!this.searchForm.cigaretteName.trim()) {
        ElMessage.warning('请先输入卷烟名称')
        return
      }
      
      this.$emit('search-next')
    },
    handleReset() {
      this.searchForm = {
        year: null,
        month: null,
        week: null,
        cigaretteName: '',
        distributionType: '',
        extendedType: '',
        distributionArea: ''
      }
      ElMessage.info('已重置搜索条件')
      this.$emit('reset')
    },
    handleExport() {
      ElMessage.success('正在导出数据...')
      this.$emit('export', this.searchForm)
    },
    handleCigaretteNameInput(value) {
      // 实时搜索时去除前后空格
      this.searchForm.cigaretteName = value.trim()
    },
    handleCigaretteNameChange() {
      // 检查是否已填充日期表单
      if (!this.searchForm.year || !this.searchForm.month || !this.searchForm.week) {
        ElMessage.warning('请先填充年份、月份和周序号，然后再搜索卷烟名称')
        return
      }
      
      // 当卷烟名称输入完成时，尝试匹配表格中的记录
      if (this.searchForm.cigaretteName && this.tableData.length > 0) {
        // 找到该卷烟在当前日期的所有投放记录
        const matchedRecords = this.tableData.filter(record => 
          record.cigName && 
          record.cigName.includes(this.searchForm.cigaretteName) &&
          record.year === this.searchForm.year &&
          record.month === this.searchForm.month &&
          record.weekSeq === this.searchForm.week
        )
        
        if (matchedRecords.length > 0) {
          // 找到匹配记录，触发选中事件（传递所有匹配的记录）
          this.$emit('cigarette-name-matched', matchedRecords)
          ElMessage.success(`已自动选中匹配的卷烟：${matchedRecords[0].cigName}，共找到 ${matchedRecords.length} 个投放区域`)
        } else {
          ElMessage.info('未找到匹配的卷烟记录')
        }
      }
    },
    formatQuantity(value) {
      if (value === null || value === undefined || value === '') {
        return '未设置'
      }
      
      // 如果是数字，格式化为带小数点的形式
      const numValue = parseFloat(value)
      if (!isNaN(numValue)) {
        return numValue.toFixed(2)
      }
      
      return value
    },
    
    // 加载档位数据
    loadPositionData(record) {
      if (!record) {
        this.positionData = new Array(30).fill(0)
        return
      }
      
      // 从记录中提取D30到D1的数据
      const positions = []
      for (let i = 30; i >= 1; i--) {
        const key = `d${i}`
        const value = record[key] || 0
        positions.push(Number(value))
      }
      
      this.positionData = positions.length === 30 ? positions : new Array(30).fill(0)
      
      // 详细的调试信息
      console.log('=== 档位数据加载调试 ===')
      console.log('从后端接收的原始数据字段:', Object.keys(record).filter(key => key.startsWith('d')).sort())
      console.log('转换后的positionData数组:', this.positionData)
      console.log('界面将显示的顺序:', this.positionData.map((val, idx) => `D${30-idx}=${val}`).join(', '))
      console.log('数组第一个值 (对应D30):', this.positionData[0])
      console.log('数组最后一个值 (对应D1):', this.positionData[29])
    },
    
    // 档位数据验证（已移除约束检查）
    validatePositionConstraints() {
      // 不再进行约束条件检查，允许任意档位值
      console.log('档位数据已更新，不再检查约束条件')
    },
    
    // 保存档位设置
    async handleSavePositions() {
      if (!this.selectedRecord || !this.selectedRecord.cigCode) {
        ElMessage.error('请先选中一个卷烟记录')
        return
      }
      
      if (!this.isPositionDataValid) {
        ElMessage.error('请检查档位数据，至少设置一个档位值')
        return
      }
      
      try {
        this.savingPositions = true
        
        // 构建更新请求数据
        const updateData = {
          cigCode: this.selectedRecord.cigCode,
          cigName: this.selectedRecord.cigName,
          year: this.selectedRecord.year,
          month: this.selectedRecord.month,
          weekSeq: this.selectedRecord.weekSeq,
          deliveryMethod: this.searchForm.distributionType,
          deliveryEtype: this.searchForm.extendedType,
          deliveryArea: this.searchForm.distributionArea.join(','),
          distribution: [...this.positionData], // D30到D1的分配值数组
          remark: this.selectedRecord.remark || '档位设置更新'
        }
        
        // 详细的调试信息
        console.log('=== 档位设置数据传输调试 ===')
        console.log('界面显示顺序:', this.positionData.map((val, idx) => `D${30-idx}=${val}`).join(', '))
        console.log('发送给后端的distribution数组:', updateData.distribution)
        console.log('数组长度:', updateData.distribution.length)
        console.log('数组第一个值 (应该是D30):', updateData.distribution[0])
        console.log('数组最后一个值 (应该是D1):', updateData.distribution[29])
        console.log('完整请求数据:', updateData)
        
        const response = await cigaretteDistributionAPI.updateCigaretteInfo(updateData)
        
        if (response.data.success) {
          ElMessage.success({
            message: `档位设置保存成功！更新了${response.data.updatedRecords || 1}条记录`,
            duration: 2000
          })
          
          // 触发数据刷新
          this.$emit('position-updated', {
            cigCode: updateData.cigCode,
            updateData: updateData
          })
        } else {
          throw new Error(response.data.message || '保存失败')
        }
      } catch (error) {
        console.error('保存档位设置失败:', error)
        ElMessage.error(`保存失败: ${error.message}`)
      } finally {
        this.savingPositions = false
      }
    },
    
    // 重置档位数据
    handleResetPositions() {
      if (this.selectedRecord) {
        this.loadPositionData(this.selectedRecord)
        ElMessage.info('已重置档位数据')
      } else {
        this.positionData = new Array(30).fill(0)
        ElMessage.info('已清空档位数据')
      }
    },
    
    // 新增投放区域
    async handleAddNewArea() {
      if (!this.canAddNewArea) {
        ElMessage.warning('请确保已选中卷烟并设置了有效的档位数据')
        return
      }
      
      try {
        // 获取当前选中的投放区域中，不在原记录中的新区域
        const originalAreas = this.selectedRecord.allAreas || [this.selectedRecord.deliveryArea]
        const selectedAreas = this.searchForm.distributionArea
        const newAreas = selectedAreas.filter(area => !originalAreas.includes(area))
        
        if (newAreas.length === 0) {
          ElMessage.warning('没有选择新的投放区域')
          return
        }
        
        const result = await ElMessageBox.confirm(
          `确定要为卷烟 "${this.selectedRecord.cigName}" 新增投放区域：${newAreas.join(', ')} 吗？`,
          '确认新增投放区域',
          {
            confirmButtonText: '确定新增',
            cancelButtonText: '取消',
            type: 'info'
          }
        )
        
        if (result === 'confirm') {
          // 为每个新区域创建记录
          const addPromises = newAreas.map(area => {
            const addData = {
              cigCode: this.selectedRecord.cigCode,
              cigName: this.selectedRecord.cigName,
              year: this.selectedRecord.year,
              month: this.selectedRecord.month,
              weekSeq: this.selectedRecord.weekSeq,
              deliveryMethod: this.searchForm.distributionType,
              deliveryEtype: this.searchForm.extendedType,
              deliveryArea: area,
              distribution: [...this.positionData], // 使用当前的档位设置
              remark: `新增投放区域: ${area}`
            }
            
            // 新增区域的调试信息
            console.log(`=== 新增区域 ${area} 数据传输调试 ===`)
            console.log('界面显示顺序:', this.positionData.map((val, idx) => `D${30-idx}=${val}`).join(', '))
            console.log('发送给后端的distribution数组:', addData.distribution)
            console.log('数组第一个值 (应该是D30):', addData.distribution[0])
            console.log('数组最后一个值 (应该是D1):', addData.distribution[29])
            
            return cigaretteDistributionAPI.updateCigaretteInfo(addData)
          })
          
          const responses = await Promise.all(addPromises)
          const successCount = responses.filter(res => res.data.success).length
          
          if (successCount === newAreas.length) {
            ElMessage.success({
              message: `成功新增 ${successCount} 个投放区域记录`,
              duration: 2000
            })
            
            // 触发数据刷新
            this.$emit('area-added', {
              cigCode: this.selectedRecord.cigCode,
              newAreas: newAreas,
              positionData: this.positionData
            })
          } else {
            ElMessage.warning(`部分新增成功：${successCount}/${newAreas.length}`)
          }
        }
      } catch (error) {
        if (error === 'cancel') {
          return // 用户取消操作
        }
        console.error('新增投放区域失败:', error)
        ElMessage.error(`新增失败: ${error.message}`)
      }
    },
    
    // 删除投放区域
    async handleDeleteAreas() {
      if (!this.canDeleteAreas) {
        ElMessage.warning('请选择要删除的投放区域')
        return
      }
      
      try {
        const areasToDeleteList = [...this.areasToDelete]
        const remainingAreas = this.selectedRecord.allAreas.filter(area => !areasToDeleteList.includes(area))
        
        if (remainingAreas.length === 0) {
          ElMessage.error('不能删除所有投放区域，至少需要保留一个')
          return
        }
        
        const result = await ElMessageBox.confirm(
          `确定要删除卷烟 "${this.selectedRecord.cigName}" 的以下投放区域吗？\n\n${areasToDeleteList.join(', ')}\n\n删除后剩余区域：${remainingAreas.join(', ')}`,
          '确认删除投放区域',
          {
            confirmButtonText: '确定删除',
            cancelButtonText: '取消',
            type: 'warning',
            dangerouslyUseHTMLString: false
          }
        )
        
        if (result === 'confirm') {
          // 构建删除请求数据
          const deleteData = {
            cigCode: this.selectedRecord.cigCode,
            cigName: this.selectedRecord.cigName,
            year: this.selectedRecord.year,
            month: this.selectedRecord.month,
            weekSeq: this.selectedRecord.weekSeq,
            areasToDelete: areasToDeleteList
          }
          
          console.log('删除投放区域请求数据:', deleteData)
          
          // 调用后端删除接口
          const response = await cigaretteDistributionAPI.deleteDeliveryAreas(deleteData)
          
          if (response.data.success) {
            ElMessage.success({
              message: `成功删除 ${areasToDeleteList.length} 个投放区域记录`,
              duration: 2000
            })
            
            // 清空删除选择
            this.areasToDelete = []
            
            // 触发数据刷新
            this.$emit('areas-deleted', {
              cigCode: this.selectedRecord.cigCode,
              deletedAreas: areasToDeleteList,
              remainingAreas: remainingAreas
            })
          } else {
            throw new Error(response.data.message || '删除失败')
          }
        }
      } catch (error) {
        if (error === 'cancel') {
          return // 用户取消操作
        }
        console.error('删除投放区域失败:', error)
        ElMessage.error(`删除失败: ${error.message}`)
      }
    }
  }
}
</script>

<style scoped>
.search-form-container {
  background: #fafbfc;
  padding: 15px;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
}

.search-form {
  margin: 0;
}

:deep(.el-form-item) {
  margin-bottom: 15px;
  margin-right: 15px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: #606266;
  font-size: 13px;
}

:deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #c0c4cc inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #409eff inset;
}

:deep(.el-select .el-input__wrapper) {
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

:deep(.el-select .el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #c0c4cc inset;
}

:deep(.el-select .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #409eff inset;
}

:deep(.el-select .el-input__wrapper.is-disabled) {
  background: #f5f7fa;
  color: #c0c4cc;
}

/* 档位设置区域样式 */
.position-settings-section {
  margin: 20px 0;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.position-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
  margin: 15px 0;
}

.position-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  background: #ffffff;
  border-radius: 6px;
  border: 1px solid #dcdfe6;
  transition: all 0.2s ease;
}

.position-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 4px rgba(64, 158, 255, 0.1);
}

.position-label {
  font-weight: 600;
  color: #409eff;
  min-width: 32px;
  font-size: 12px;
  text-align: center;
}

.position-actions {
  display: flex;
  gap: 10px;
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #e4e7ed;
}

/* 档位输入框样式优化 */
:deep(.position-item .el-input-number) {
  width: 90px !important;
}

:deep(.position-item .el-input-number .el-input__wrapper) {
  border-radius: 4px;
  font-size: 12px;
}

:deep(.position-item .el-input-number--small .el-input__wrapper) {
  padding: 4px 8px;
}

/* 档位约束错误提示 */
.position-item.error {
  border-color: #f56c6c;
  background: #fef0f0;
}

.position-item.error .position-label {
  color: #f56c6c;
}

/* 删除投放区域样式 */
.delete-area-section {
  margin: 20px 0;
  padding: 15px;
  background: #fef9f9;
  border-radius: 6px;
  border: 1px solid #f5c6cb;
}

.delete-area-checkboxes {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  margin: 15px 0;
}

.delete-area-checkboxes .el-checkbox {
  margin: 0;
  padding: 8px 12px;
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.delete-area-checkboxes .el-checkbox:hover {
  border-color: #f56c6c;
  box-shadow: 0 2px 4px rgba(245, 108, 108, 0.1);
}

.delete-area-checkboxes .el-checkbox.is-checked {
  background: #fef0f0;
  border-color: #f56c6c;
}

.delete-area-checkboxes .el-checkbox.is-disabled {
  background: #f5f7fa;
  color: #c0c4cc;
  cursor: not-allowed;
}

.delete-area-tips {
  margin-top: 10px;
}

.delete-area-tips .el-alert {
  margin: 8px 0;
}

</style>