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
          @search="handleSearch"
          @search-next="handleSearchNext"
          @reset="handleReset"
          @export="handleExport"
        />
      </section>

      <!-- 下方档位设置区域 -->
      <section class="position-setting-section">
        <PositionSetting 
          :cigarette-name="selectedCigaretteName"
          :position-data="currentPositionData"
          :distribution-areas="searchParams.distributionArea || []"
          :selected-record="selectedRecord"
          @position-change="handlePositionChange"
          @save-cigarette-info="handleSaveCigaretteInfo"
        />
      </section>
    </main>
  </div>
</template>

<script>
import { Grid } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import DataTable from '../components/DataTable.vue'
import SearchForm from '../components/SearchForm.vue'
import PositionSetting from '../components/PositionSetting.vue'
import { cigaretteDistributionAPI } from '../services/api'

export default {
  name: 'Home',
  components: {
    Grid,
    DataTable,
    SearchForm,
    PositionSetting
  },
  data() {
    return {
      searchParams: {},
      selectedCigaretteName: '',
      currentPositionData: {},
      selectedRecord: null,
      tableData: []
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
      // 行被选中时，更新档位设置区域
      this.selectedCigaretteName = row.cigName
      this.selectedRecord = row
      
      // 将后端数据格式转换为档位设置组件需要的格式
      const positionData = {}
      for (let i = 1; i <= 30; i++) {
        positionData[`position${i}`] = row[`d${i}`] || 0
      }
      this.currentPositionData = positionData
      
      console.log('选中行:', row)
      ElMessage.info(`已选中：${row.cigName}`)
    },
    
    handlePositionChange(positionData) {
      // 档位数据变化时，实时更新表格显示
      console.log('档位数据变化:', positionData)
      
      // 更新当前档位数据
      this.currentPositionData = { ...this.currentPositionData, ...positionData }
      
      // 清除之前的防抖定时器
      if (this.updateTimer) {
        clearTimeout(this.updateTimer)
      }
      
      // 使用防抖机制，避免频繁更新
      this.updateTimer = setTimeout(() => {
        if (this.$refs.dataTable && this.selectedRecord) {
          this.$refs.dataTable.updatePositionData(
            this.selectedRecord.cigCode,
            this.selectedRecord.year,
            this.selectedRecord.month,
            this.selectedRecord.weekSeq,
            positionData
          )
          console.log('表格数据已更新')
        }
      }, 100) // 100ms防抖延迟
    },
    
    async handleSaveCigaretteInfo(updateData) {
      try {
        console.log('准备更新卷烟信息:', updateData)
        
        // 调用后端API更新卷烟信息
        const response = await cigaretteDistributionAPI.updateCigaretteInfo(updateData)
        
        if (response.data.success) {
          ElMessage.success('卷烟信息更新成功！')
          
          // 刷新表格数据
          if (this.$refs.dataTable) {
            this.$refs.dataTable.handleRefresh()
          }
        } else {
          ElMessage.error(response.data.message || '更新失败')
        }
      } catch (error) {
        console.error('更新卷烟信息失败:', error)
        ElMessage.error('更新失败，请稍后重试')
      }
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

.position-setting-section {
  flex: 1.8;
  min-height: 480px;
  max-height: 52vh;
  padding: 12px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  overflow: visible;
}
</style>