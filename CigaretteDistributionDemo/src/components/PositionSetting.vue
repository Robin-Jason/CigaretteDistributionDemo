<template>
  <div class="position-setting-container">
    <div class="setting-header">
      <h3 class="section-title">投放档位设置</h3>
      <div class="header-actions">
        <el-button 
          type="primary" 
          size="small"
          @click="handleSaveAll"
          :disabled="!isDataLoaded"
        >
          <el-icon><Check /></el-icon>
          保存全部
        </el-button>
        <el-button 
          type="warning" 
          size="small"
          @click="handleResetAll"
          :disabled="!isDataLoaded"
        >
          <el-icon><RefreshLeft /></el-icon>
          重置全部
        </el-button>
        <el-button 
          type="info" 
          size="small"
          @click="handleBatchSet"
          :disabled="!isDataLoaded"
        >
          <el-icon><Setting /></el-icon>
          批量设置
        </el-button>
        <el-button 
          type="warning" 
          size="small"
          @click="handleExportExcel"
          :disabled="!isDataLoaded"
          :loading="exportLoading"
        >
          <el-icon><Download /></el-icon>
          {{ exportLoading ? '导出中...' : '导出Excel' }}
        </el-button>
      </div>
    </div>
    
    <div class="position-grid-container" v-if="isDataLoaded">
      <div class="position-grid">
        <div 
          v-for="position in positions" 
          :key="position.id"
          class="position-item"
          :class="{ 'active': position.value > 0 }"
        >
          <div class="position-label">{{ position.label }}</div>
          <el-input-number
            v-model="position.value"
            :min="0"
            :max="999"
            size="small"
            :controls="false"
            @change="handlePositionChange(position)"
            class="position-input-number"
          />
        </div>
      </div>
    </div>
    <div v-else class="no-data-tip">
      请先查询卷烟名称以显示档位设置
    </div>
    
    <!-- 批量设置对话框 -->
    <el-dialog
      v-model="batchDialogVisible"
      title="批量设置档位数值"
      width="400px"
      :before-close="handleCloseBatchDialog"
    >
      <el-form :model="batchForm" label-width="120px">
        <el-form-item label="设置范围">
          <el-select v-model="batchForm.range" style="width: 100%">
            <el-option label="全部档位 (1-30)" value="all" />
            <el-option label="前15档 (1-15)" value="first-half" />
            <el-option label="后15档 (16-30)" value="second-half" />
            <el-option label="自定义范围" value="custom" />
          </el-select>
        </el-form-item>
        
        <el-form-item v-if="batchForm.range === 'custom'" label="自定义范围">
          <div style="display: flex; align-items: center; gap: 10px;">
            <el-input-number v-model="batchForm.startPos" :min="1" :max="30" size="small" />
            <span>至</span>
            <el-input-number v-model="batchForm.endPos" :min="1" :max="30" size="small" />
          </div>
        </el-form-item>
        
        <el-form-item label="设置数值">
          <el-input-number
            v-model="batchForm.value"
            :min="0"
            :max="999"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="batchDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleBatchConfirm">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { Check, RefreshLeft, Setting, Download } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ExcelExporter } from '../utils/excelExport'

export default {
  name: 'PositionSetting',
  components: {
    Check,
    RefreshLeft,
    Setting,
    Download
  },
  props: {
    cigaretteName: {
      type: String,
      default: ''
    },
    positionData: {
      type: Object,
      default: () => ({})
    },
    distributionAreas: {
      type: Array,
      default: () => []
    },
    // 新增：当前选中的卷烟记录
    selectedRecord: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      positions: [],
      batchDialogVisible: false,
      batchForm: {
        range: 'all',
        startPos: 1,
        endPos: 30,
        value: 0
      },
      currentCigaretteName: '',
      isDataLoaded: false,
      exportLoading: false
    }
  },
  watch: {
    cigaretteName: {
      handler(newName) {
        if (newName) {
          this.currentCigaretteName = newName
          this.isDataLoaded = true
        } else {
          this.clearPositionData()
        }
      },
      immediate: true
    },
    positionData: {
      handler(newData) {
        if (newData && Object.keys(newData).length > 0) {
          this.initPositions(newData)
          this.isDataLoaded = true
        } else if (!this.cigaretteName) {
          this.clearPositionData()
        }
      },
      deep: true,
      immediate: true
    },
    selectedRecord: {
      handler(newRecord) {
        if (newRecord && newRecord.cigCode) {
          this.currentCigaretteName = newRecord.cigName
          // 将后端数据格式转换为组件需要的格式
          const convertedData = {}
          for (let i = 1; i <= 30; i++) {
            convertedData[`position${i}`] = newRecord[`d${i}`] || 0
          }
          this.initPositions(convertedData)
          this.isDataLoaded = true
        }
      },
      immediate: true
    }
  },
  mounted() {
    this.initPositions()
  },
  methods: {
    initPositions(data = {}) {
      // 初始化30个档位，按照设计图的布局排列
      const layout = [
        [30, 29, 28, 27, 26],
        [25, 24, 23, 22, 21],
        [20, 19, 18, 17, 16],
        [15, 14, 13, 12, 11],
        [10, 9, 8, 7, 6],
        [5, 4, 3, 2, 1]
      ]
      
      this.positions = []
      layout.forEach(row => {
        row.forEach(num => {
          this.positions.push({
            id: num,
            label: `${num}档`,
            value: data[`position${num}`] || 0
          })
        })
      })
    },

    clearPositionData() {
      this.initPositions()
      this.isDataLoaded = false
      this.currentCigaretteName = ''
    },
    handlePositionChange(position) {
      console.log(`档位 ${position.id} 数值变更为: ${position.value}`)
      
      // 只发射单个档位的变化，避免发送整个数据集
      const singlePositionData = {
        [`position${position.id}`]: position.value
      }
      
      // 使用nextTick确保数据更新后再发射事件
      this.$nextTick(() => {
        this.$emit('position-change', singlePositionData)
      })
    },
    handleSaveAll() {
      if (!this.selectedRecord) {
        ElMessage.warning('请先选择一条卷烟记录')
        return
      }
      
      const positionData = {}
      this.positions.forEach(pos => {
        positionData[`position${pos.id}`] = pos.value
      })
      
      // 构建更新请求数据
      const updateData = {
        cigCode: this.selectedRecord.cigCode,
        cigName: this.selectedRecord.cigName,
        year: this.selectedRecord.year,
        month: this.selectedRecord.month,
        weekSeq: this.selectedRecord.weekSeq,
        deliveryMethod: this.selectedRecord.deliveryMethod || '按档位扩展投放',
        deliveryEtype: this.selectedRecord.deliveryEtype || '档位+城乡分类代码',
        deliveryArea: this.selectedRecord.deliveryAreas || '',
        distribution: this.positions.map(pos => pos.value),
        remark: this.selectedRecord.remark || '手动更新'
      }
      
      console.log('准备更新卷烟信息:', updateData)
      
      // 发射保存事件，让父组件处理API调用
      this.$emit('save-cigarette-info', updateData)
    },
    handleResetAll() {
      ElMessageBox.confirm(
        '确定要重置所有档位数值吗？',
        '确认重置',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
        }
      ).then(() => {
        this.positions.forEach(p => {
          p.value = 0
        })
        
        // 发射重置事件
        const positionData = {}
        this.positions.forEach(pos => {
          positionData[`position${pos.id}`] = 0
        })
        
        this.$emit('position-change', positionData)
        ElMessage.success('已重置所有档位')
      }).catch(() => {
        ElMessage.info('已取消重置')
      })
    },
    handleBatchSet() {
      this.batchDialogVisible = true
    },
    handleCloseBatchDialog() {
      this.batchDialogVisible = false
      this.batchForm = {
        range: 'all',
        startPos: 1,
        endPos: 30,
        value: 0
      }
    },
    handleBatchConfirm() {
      let targetPositions = []
      
      switch (this.batchForm.range) {
        case 'all':
          targetPositions = this.positions
          break
        case 'first-half':
          targetPositions = this.positions.filter(p => p.id <= 15)
          break
        case 'second-half':
          targetPositions = this.positions.filter(p => p.id >= 16)
          break
        case 'custom':
          targetPositions = this.positions.filter(p => 
            p.id >= this.batchForm.startPos && p.id <= this.batchForm.endPos
          )
          break
      }
      
      targetPositions.forEach(p => {
        p.value = this.batchForm.value
      })
      
      // 发射批量设置事件
      const positionData = {}
      this.positions.forEach(pos => {
        positionData[`position${pos.id}`] = pos.value
      })
      
      this.$emit('position-change', positionData)
      
      ElMessage.success(`已批量设置 ${targetPositions.length} 个档位为 ${this.batchForm.value}`)
      this.batchDialogVisible = false
    },
    async handleExportExcel() {
      if (!this.isDataLoaded) {
        ElMessage.warning('请先加载数据')
        return
      }

      this.exportLoading = true
      try {
        // 将档位数据转换为position格式
        const positionData = {}
        this.positions.forEach(pos => {
          positionData[`position${pos.id}`] = pos.value
        })

        // 准备卷烟信息
        const cigaretteInfo = {
          cigName: this.currentCigaretteName
        }

        // 调用Excel导出工具类
        const result = ExcelExporter.exportPositionData(positionData, cigaretteInfo)
        
        if (result.success) {
          ElMessage.success(`档位设置Excel文件导出成功：${result.filename}`)
        } else {
          ElMessage.error(result.message)
        }
      } catch (error) {
        console.error('导出失败:', error)
        ElMessage.error('档位设置数据导出失败！')
      } finally {
        this.exportLoading = false
      }
    }
  }
}
</script>

<style scoped>
.position-setting-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.setting-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.section-title {
  margin: 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.position-grid-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 420px;
  padding: 5px;
}

.position-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  grid-template-rows: repeat(6, 1fr);
  gap: 8px;
  flex: 1;
  width: 100%;
  height: 100%;
  min-height: 420px;
}

.position-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 6px 4px;
  border: 2px solid #e4e7ed;
  border-radius: 6px;
  background: #ffffff;
  transition: all 0.3s ease;
  min-height: 65px;
  max-height: 75px;
}

.position-item:hover {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.25);
  transform: translateY(-1px);
}

.position-item.active {
  border-color: #67c23a;
  background: linear-gradient(135deg, #f0f9ff 0%, #e6f7ff 100%);
  box-shadow: 0 2px 8px rgba(103, 194, 58, 0.2);
}

.position-item.active .position-label {
  color: #67c23a;
}

.position-item.active :deep(.el-input__inner) {
  color: #67c23a;
  font-weight: 700;
}

.position-label {
  font-size: 16px;
  font-weight: 800;
  color: #303133;
  line-height: 1.2;
  text-shadow: 0 1px 3px rgba(0,0,0,0.15);
  letter-spacing: 0.5px;
}

.position-input-number {
  width: 85px;
}

/* 控制按钮已移除，不再需要相关样式 */

:deep(.position-input-number .el-input__wrapper) {
  padding: 6px 8px;
  border-radius: 6px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.12);
  border: 2px solid #e4e7ed;
  min-height: 36px;
}

:deep(.position-input-number .el-input__inner) {
  font-size: 16px;
  font-weight: 700;
  text-align: center;
  padding: 4px 8px;
  color: #409eff;
  letter-spacing: 0.8px;
  transition: all 0.2s ease;
  min-height: 24px;
  line-height: 1.2;
}

:deep(.position-input-number .el-input__inner:focus) {
  color: #303133;
  font-weight: 800;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.25);
  border-color: #409eff;
  transform: scale(1.02);
}

:deep(.position-input-number .el-input__wrapper:focus-within) {
  border-color: #409eff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.15), 0 2px 8px rgba(0,0,0,0.1);
}

/* 增加数值为0时的特殊样式 */
:deep(.position-input-number .el-input__inner[value="0"]) {
  color: #909399;
  font-style: italic;
  font-weight: 500;
}

/* 增强激活状态的输入框样式 */
.position-item.active :deep(.el-input__wrapper) {
  border-color: #67c23a;
  box-shadow: 0 2px 8px rgba(103, 194, 58, 0.2);
}

.position-item.active :deep(.el-input__inner) {
  color: #67c23a;
  font-weight: 800;
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

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 响应式优化 */
@media (max-height: 950px) {
  .position-grid {
    min-height: 520px;
    gap: 10px;
  }
  
  .position-item {
    min-height: 82px;
    max-height: 100px;
    padding: 8px 6px;
    gap: 6px;
  }
  
  .position-label {
    font-size: 15px;
  }
  
  .position-input-number {
    width: 80px;
  }
  
  :deep(.position-input-number .el-input__inner) {
    font-size: 15px;
  }
  
  :deep(.position-input-number .el-input__wrapper) {
    min-height: 34px;
  }
}

@media (max-height: 850px) {
  .position-grid {
    min-height: 480px;
    gap: 8px;
  }
  
  .position-item {
    min-height: 75px;
    max-height: 90px;
    padding: 6px 5px;
    gap: 5px;
  }
  
  .position-label {
    font-size: 14px;
  }
  
  .position-input-number {
    width: 75px;
  }
  
  :deep(.position-input-number .el-input__inner) {
    font-size: 14px;
  }
  
  :deep(.position-input-number .el-input__wrapper) {
    min-height: 32px;
  }
}

@media (max-height: 750px) {
  .position-grid {
    gap: 6px;
    min-height: 420px;
  }
  
  .position-item {
    min-height: 65px;
    max-height: 78px;
    padding: 5px 3px;
    gap: 3px;
  }
  
  .position-label {
    font-size: 13px;
  }
  
  .position-input-number {
    width: 65px;
  }
  
  :deep(.position-input-number .el-input__inner) {
    font-size: 13px;
  }
  
  :deep(.position-input-number .el-input__wrapper) {
    min-height: 28px;
  }
}
</style>