<template>
  <div class="data-table-container">
    <div class="table-header">
      <h3>卷烟投放数据统计表</h3>
      <div class="header-actions">
        <el-button type="primary" size="small" @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button 
          type="success" 
          size="small" 
          @click="handleExport"
          :loading="exportLoading"
          :disabled="tableData.length === 0"
        >
          <el-icon><Download /></el-icon>
          {{ exportLoading ? '导出中...' : '导出Excel' }}
        </el-button>
      </div>
    </div>
    
    <div class="table-content" v-if="tableData.length > 0">
      <el-table
        :data="tableData"
        style="width: 100%"
        height="100%"
        border
        stripe
        size="default"
        :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
        :row-class-name="getRowClassName"
        @row-click="handleRowClick"
        highlight-current-row
        v-loading="loading"
        element-loading-text="正在加载数据..."
      >
        <el-table-column prop="cigCode" label="卷烟代码" width="100" align="center" />
        <el-table-column prop="cigName" label="卷烟名称" width="180" />
        <el-table-column prop="dateDisplay" label="日期" width="150" align="center" />
        <el-table-column prop="deliveryAreas" label="投放区域" width="200" />
        
        <!-- 30个档位列，从30档开始到1档 -->
        <el-table-column 
          v-for="position in reversedPositions" 
          :key="`d${position}`" 
          :prop="`d${position}`" 
          :label="`${position}档`" 
          width="50" 
          align="center"
        >
          <template #default="scope">
            <span :class="getCellClass(scope.row[`d${position}`])">
              {{ scope.row[`d${position}`] || 0 }}
            </span>
          </template>
        </el-table-column>
        
        <el-table-column prop="remark" label="备注" min-width="120" />
      </el-table>
    </div>
    <div v-else class="no-data-tip">
      <el-empty description="暂无数据" :image-size="100">
        <template #description>
          <span>请先输入查询条件进行查询</span>
        </template>
      </el-empty>
    </div>
  </div>
</template>

<script>
import { Download, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { cigaretteDistributionAPI } from '../services/api'
import { ExcelExporter } from '../utils/excelExport'

export default {
  name: 'DataTable',
  components: {
    Download,
    Refresh
  },
  props: {
    searchParams: {
      type: Object,
      default: () => ({})
    }
  },
  emits: ['row-selected', 'data-loaded'],
  data() {
    return {
      selectedRow: null,
      tableData: [],
      loading: false,
      total: 0,
      originalTotal: 0,
      exportLoading: false
    }
  },
  computed: {
    reversedPositions() {
      // 从30档到1档的顺序
      return Array.from({length: 30}, (_, i) => 30 - i)
    }
  },
  watch: {
    searchParams: {
      handler(newParams) {
        if (newParams && Object.keys(newParams).length > 0) {
          this.loadData(newParams)
        }
      },
      deep: true,
      immediate: true
    }
  },
  methods: {
    async loadData(params) {
      // 检查必要的查询参数
      if (!params.year || !params.month || !params.weekSeq) {
        return
      }
      
      this.loading = true
      try {
        const response = await cigaretteDistributionAPI.queryDistribution({
          year: params.year,
          month: params.month,
          weekSeq: params.weekSeq
        })
        
        if (response.data.success) {
          this.tableData = response.data.data || []
          this.total = response.data.total || 0
          this.originalTotal = response.data.originalTotal || 0
          
          // 为每条数据添加日期显示字段
          this.tableData = this.tableData.map(item => ({
            ...item,
            dateDisplay: this.formatDate(item.year, item.month, item.weekSeq)
          }))
          
          ElMessage.success(`查询成功，共找到 ${this.total} 条记录（原始记录：${this.originalTotal} 条）`)
          this.$emit('data-loaded', this.tableData)
          
          // 如果有数据且只有一条记录，自动选中该记录并显示投放量信息
          if (this.tableData.length === 1) {
            this.handleRowClick(this.tableData[0])
            ElMessage.info('已自动选中唯一记录')
          }
        } else {
          ElMessage.error(response.data.message || '查询失败')
        }
      } catch (error) {
        console.error('查询数据失败:', error)
        ElMessage.error('查询数据失败，请检查网络连接')
      } finally {
        this.loading = false
      }
    },
    
    getCellClass(value) {
      if (!value || value === 0) return ''
      if (value > 30) return 'high-value'
      if (value > 10) return 'medium-value'
      return 'low-value'
    },
    
    getRowClassName({ row, rowIndex }) {
      let className = ''
      
      // 如果是选中的行，添加选中样式
      if (this.selectedRow && this.selectedRow.cigCode === row.cigCode && 
          this.selectedRow.year === row.year && this.selectedRow.month === row.month && 
          this.selectedRow.weekSeq === row.weekSeq) {
        className += 'selected-row '
      }
      
      return className.trim()
    },
    
    handleRowClick(row) {
      this.selectedRow = row
      
      // 确保传递完整的记录信息，包括预投放量和实际投放量
      const selectedRecord = {
        ...row,
        // 保持原有的字段映射
        cigCode: row.cigCode,
        cigName: row.cigName,
        year: row.year,
        month: row.month,
        weekSeq: row.weekSeq,
        // 添加投放量信息
        advAmount: row.advAmount,
        actualDelivery: row.actualDelivery,
        deliveryAreas: row.deliveryAreas,
        remark: row.remark
      }
      
      this.$emit('row-selected', selectedRecord)
    },
    
    handleRefresh() {
      if (this.searchParams && Object.keys(this.searchParams).length > 0) {
        this.loadData(this.searchParams)
      } else {
        ElMessage.warning('请先设置查询条件')
      }
    },
    
    handleExport() {
      if (this.tableData.length === 0) {
        ElMessage.warning('暂无数据可导出')
        return
      }
      
      this.exportLoading = true
      
      try {
        // 调用Excel导出工具类
        const result = ExcelExporter.exportCigaretteData(
          this.tableData, 
          this.searchParams
        )
        
        if (result.success) {
          ElMessage.success(`Excel文件导出成功：${result.filename}`)
        } else {
          ElMessage.error(result.message)
        }
      } catch (error) {
        console.error('导出失败:', error)
        ElMessage.error('导出失败，请稍后重试')
      } finally {
        this.exportLoading = false
      }
    },
    
    formatDate(year, month, weekSeq) {
      // 格式化日期显示：××年××月第×周
      if (year && month && weekSeq) {
        return `${year}年${month}月第${weekSeq}周`
      }
      return ''
    },
    
    // 更新档位数据的方法
    updatePositionData(cigCode, year, month, weekSeq, positionData) {
      const itemIndex = this.tableData.findIndex(row => 
        row.cigCode === cigCode && 
        row.year === year && 
        row.month === month && 
        row.weekSeq === weekSeq
      )
      
      if (itemIndex !== -1) {
        // 创建新的数据对象，确保响应式更新
        const updatedItem = { ...this.tableData[itemIndex] }
        
        // 更新档位数据，将position格式转换为d格式
        Object.keys(positionData).forEach(key => {
          if (key.startsWith('position')) {
            const positionNum = key.replace('position', '')
            const dKey = `d${positionNum}`
            updatedItem[dKey] = positionData[key]
          }
        })
        
        // 使用Vue 3的响应式方式更新数组元素
        this.tableData.splice(itemIndex, 1, updatedItem)
        
        console.log(`表格数据已更新: ${cigCode}`, updatedItem)
        ElMessage.success('档位数据更新成功')
      }
    }
  }
}
</script>

<style scoped>
.data-table-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e4e7ed;
}

.table-header h3 {
  margin: 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.table-content {
  flex: 1;
  overflow: hidden;
  max-height: calc(35vh - 100px);
}

.no-data-tip {
  text-align: center;
  color: #909399;
  padding: 40px;
  font-size: 14px;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.el-table) {
  font-size: 11px;
}

:deep(.el-table th) {
  padding: 8px 0;
}

:deep(.el-table td) {
  padding: 4px 0;
}

.high-value {
  color: #e6a23c;
  font-weight: bold;
}

.medium-value {
  color: #409eff;
}

.low-value {
  color: #909399;
}

/* 高亮行样式 */
:deep(.highlight-row) {
  background-color: #e6f3ff !important;
}

:deep(.highlight-row:hover) {
  background-color: #cce7ff !important;
}

/* 选中行样式 */
:deep(.selected-row) {
  background-color: #f0f9ff !important;
  border-left: 3px solid #409eff !important;
}

:deep(.selected-row:hover) {
  background-color: #e6f3ff !important;
}
</style>