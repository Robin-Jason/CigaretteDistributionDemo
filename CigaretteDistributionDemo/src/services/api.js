import axios from 'axios'

// 创建axios实例
const api = axios.create({
  baseURL: process.env.VUE_APP_API_BASE_URL || '/api/cigarette',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  config => {
    console.log('发送请求:', config.method && config.method.toUpperCase(), config.url, config.data || config.params)
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  response => {
    console.log('响应数据:', response.data)
    return response
  },
  error => {
    console.error('响应错误:', error)
    return Promise.reject(error)
  }
)

// 卷烟分配服务相关API
export const cigaretteDistributionAPI = {
  // 健康检查
  healthCheck() {
    return api.get('/health')
  },
  
  // 查询卷烟分配
  queryDistribution(params) {
    return api.post('/query', params)
  },
  
  // 查询原始数据
  queryRawData(params) {
    return api.post('/query-raw', params)
  },
  
  // 更新卷烟分配
  updateDistribution(data) {
    return api.post('/update', data)
  },
  
  // 卷烟信息更新（推荐使用）
  updateCigaretteInfo(data) {
    return api.post('/update-cigarette', data)
  },
  
  // 写回分配矩阵
  writeBackDistribution(data) {
    return api.post('/write-back', data)
  },
  
  // 测试分配算法
  testAlgorithm() {
    return api.get('/test-algorithm')
  },
  
  // 删除投放区域
  deleteDeliveryAreas(data) {
    return api.post('/delete-delivery-areas', data)
  },
  
  // 导入卷烟投放基本信息
  importBasicInfo(formData) {
    return api.post('/import-basic-info', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },
  
  // 导入区域客户数
  importCustomerData(formData) {
    return api.post('/import-customer-data', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },
  
  // 生成分配方案
  generateDistributionPlan(data) {
    return api.post('/generate-distribution-plan', data)
  }
}

// 卷烟投放方案相关API（保留原有接口）
export const distributionPlanAPI = {
  // 查询卷烟投放方案
  queryPlan(params) {
    return api.get('/distribution-plan/query', { params })
  },
  
  // 保存档位设置
  savePositions(data) {
    return api.post('/distribution-plan/save-positions', data)
  },
  
  // 获取档位数据
  getPositionData(cigaretteName, year, month, week) {
    return api.get('/distribution-plan/positions', {
      params: { cigaretteName, year, month, week }
    })
  }
}

// 卷烟数据相关API
export const cigaretteAPI = {
  // 获取卷烟列表
  getCigaretteList() {
    return api.get('/cigarettes')
  },
  
  // 搜索卷烟
  searchCigarettes(keyword) {
    return api.get('/cigarettes/search', { params: { keyword } })
  }
}

export default api
