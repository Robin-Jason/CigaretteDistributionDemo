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
          <el-option label="2024" :value="2024" />
          <el-option label="2023" :value="2023" />
          <el-option label="2022" :value="2022" />
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
          placeholder="请输入卷烟名称"
          style="width: 200px"
          clearable
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
          <el-option label="按档位统一投放" value="unified" />
          <el-option label="按档位扩展投放" value="extended" />
        </el-select>
      </el-form-item>
      
      <el-form-item label="扩展投放类型" v-if="searchForm.distributionType === 'extended'">
        <el-select
          v-model="searchForm.extendedType"
          placeholder="请选择扩展类型"
          style="width: 160px"
          clearable
          @change="handleExtendedTypeChange"
        >
          <el-option label="档位+区县" value="position-county" />
          <el-option label="档位+市场类型" value="position-market" />
          <el-option label="档位+城乡分类代码" value="position-urban-rural" />
          <el-option label="档位+业态" value="position-business" />
        </el-select>
      </el-form-item>
      
      <el-form-item label="投放区域">
        <el-select
          v-model="searchForm.distributionArea"
          :placeholder="areaPlaceholder"
          style="width: 200px"
          multiple
          collapse-tags
          collapse-tags-tooltip
          :disabled="isAreaDisabled"
        >
          <el-option
            v-for="area in availableAreas"
            :key="area.value"
            :label="area.label"
            :value="area.value"
          />
        </el-select>
      </el-form-item>
      
      <!-- 新增：卷烟投放量信息显示 -->
      <div v-if="selectedRecord && selectedRecord.cigCode" class="cigarette-info-section">
        <el-divider content-position="left">卷烟投放量信息</el-divider>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">卷烟代码:</span>
            <span class="info-value">{{ selectedRecord.cigCode || '未设置' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">卷烟名称:</span>
            <span class="info-value">{{ selectedRecord.cigName || '未设置' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">预投放量:</span>
            <span class="info-value">{{ formatQuantity(selectedRecord.advAmount) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">实际投放量:</span>
            <span class="info-value">{{ formatQuantity(selectedRecord.actualDelivery) }}</span>
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
import { Search, RefreshLeft, Download, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'SearchForm',
  props: {
    selectedRecord: {
      type: Object,
      default: null
    }
  },
  components: {
    Search,
    RefreshLeft,
    Download,
    ArrowDown
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
        'unified': [
          { label: '全市', value: 'all' }
        ],
        'position-county': [
          { label: '丹江', value: 'danjiang' },
          { label: '房县', value: 'fangxian' },
          { label: '郧西', value: 'yunxi' },
          { label: '郧阳', value: 'yunyang' },
          { label: '竹山', value: 'zhushan' },
          { label: '竹溪', value: 'zhuxi' },
          { label: '城区', value: 'chengqu' }
        ],
        'position-market': [
          { label: '城网', value: 'urban-network' },
          { label: '农网', value: 'rural-network' }
        ],
        'position-urban-rural': [
          { label: '主城区', value: 'main-urban' },
          { label: '城乡结合区', value: 'urban-rural-junction' },
          { label: '镇中心区', value: 'town-center' },
          { label: '镇乡接合区', value: 'town-rural-junction' },
          { label: '特殊区域', value: 'special-area' },
          { label: '乡中心区', value: 'rural-center' },
          { label: '村庄', value: 'village' }
        ],
        'position-business': [
          { label: '便利店', value: 'convenience-store' },
          { label: '超市', value: 'supermarket' },
          { label: '商场', value: 'shopping-mall' },
          { label: '烟草专卖店', value: 'tobacco-store' },
          { label: '娱乐服务类', value: 'entertainment-service' },
          { label: '其他', value: 'other' }
        ]
      }
    }
  },
  computed: {
    availableAreas() {
      if (this.searchForm.distributionType === 'unified') {
        return this.areaOptions['unified']
      } else if (this.searchForm.distributionType === 'extended' && this.searchForm.extendedType) {
        return this.areaOptions[this.searchForm.extendedType] || []
      }
      return []
    },
    isAreaDisabled() {
      return !this.searchForm.distributionType || 
             (this.searchForm.distributionType === 'extended' && !this.searchForm.extendedType)
    },
    areaPlaceholder() {
      if (this.searchForm.distributionType === 'unified') {
        return '全市（统一投放）'
      } else if (this.searchForm.distributionType === 'extended') {
        if (!this.searchForm.extendedType) {
          return '请先选择扩展投放类型'
        }
        return '请选择投放区域'
      }
      return '请先选择投放类型'
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
          
          console.log('选中记录更新:', newRecord)
        }
      },
      immediate: true
    }
  },
  methods: {
    handleDistributionTypeChange(value) {
      // 投放类型变化时重置相关字段
      this.searchForm.extendedType = ''
      this.searchForm.distributionArea = []
      
      // 如果选择统一投放，自动设置为全市
      if (value === 'unified') {
        this.searchForm.distributionArea = ['all']
      }
    },
    handleExtendedTypeChange() {
      // 扩展投放类型变化时重置区域选择
      this.searchForm.distributionArea = []
    },
    handleSearch() {
      // 至少需要选择一个时间条件或卷烟名称
      if (!this.searchForm.year || !this.searchForm.month || !this.searchForm.week) {
        ElMessage.warning('请至少选择一个时间条件')
        return
      }
      
      // 如果选择了投放类型，需要完整填写相关信息
      if (this.searchForm.distributionType) {
        if (this.searchForm.distributionType === 'extended' && !this.searchForm.extendedType) {
          ElMessage.warning('请选择扩展投放类型')
          return
        }
        
        if (!this.searchForm.distributionArea.length) {
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
        distributionArea: []
      }
      ElMessage.info('已重置搜索条件')
      this.$emit('reset')
    },
    handleExport() {
      ElMessage.success('正在导出数据...')
      this.$emit('export', this.searchForm)
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

/* 卷烟投放量信息显示样式 */
.cigarette-info-section {
  margin: 15px 0;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
  margin-top: 10px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-label {
  font-weight: 500;
  color: #606266;
  min-width: 80px;
  font-size: 13px;
}

.info-value {
  color: #303133;
  font-weight: 600;
  font-size: 13px;
  padding: 4px 8px;
  background: #ffffff;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
  min-width: 120px;
  text-align: center;
}
</style>