import * as XLSX from 'xlsx'
import { saveAs } from 'file-saver'

/**
 * Excel导出工具类
 * 支持卷烟投放数据和档位设置的Excel导出
 */
export class ExcelExporter {
  /**
   * 导出卷烟投放数据统计表
   * @param {Array} data - 卷烟投放数据数组
   * @param {Object} searchParams - 查询参数
   * @param {string} filename - 文件名（可选）
   */
  static exportCigaretteData(data, searchParams = {}, filename = '') {
    try {
      // 验证数据
      if (!data || !Array.isArray(data) || data.length === 0) {
        throw new Error('没有可导出的数据')
      }

      // 生成文件名
      const timestamp = new Date().toISOString().slice(0, 19).replace(/:/g, '-')
      const defaultFilename = `卷烟投放数据统计表_${timestamp}.xlsx`
      const finalFilename = filename || defaultFilename

      // 准备表头数据
      const headers = this.generateHeaders()
      
      // 准备表格数据
      const tableData = this.prepareTableData(data, headers)
      
      // 创建工作簿
      const workbook = XLSX.utils.book_new()
      
      // 创建工作表
      const worksheet = XLSX.utils.aoa_to_sheet(tableData)
      
      // 设置列宽
      this.setColumnWidths(worksheet, headers)
      
      // 设置样式
      this.setWorksheetStyles(worksheet, tableData.length)
      
      // 添加工作表到工作簿
      XLSX.utils.book_append_sheet(workbook, worksheet, '卷烟投放数据')
      
      // 添加查询条件信息
      if (Object.keys(searchParams).length > 0) {
        this.addSearchInfoSheet(workbook, searchParams)
      }
      
      // 导出文件
      const excelBuffer = XLSX.write(workbook, { 
        bookType: 'xlsx', 
        type: 'array' 
      })
      
      const blob = new Blob([excelBuffer], { 
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' 
      })
      
      saveAs(blob, finalFilename)
      
      return {
        success: true,
        message: 'Excel文件导出成功',
        filename: finalFilename
      }
      
    } catch (error) {
      console.error('Excel导出失败:', error)
      return {
        success: false,
        message: `导出失败: ${error.message}`,
        error: error
      }
    }
  }

  /**
   * 生成表头
   * @returns {Array} 表头数组
   */
  static generateHeaders() {
    const baseHeaders = [
      '卷烟代码',
      '卷烟名称', 
      '年份',
      '月份',
      '周序号',
      '投放区域',
      '预投放量',
      '实际投放量'
    ]
    
    // 添加30个档位列（从30档到1档）
    const positionHeaders = []
    for (let i = 30; i >= 1; i--) {
      positionHeaders.push(`${i}档`)
    }
    
    // 备注列放在最后
    const remarkHeader = ['备注']
    
    return [...baseHeaders, ...positionHeaders, ...remarkHeader]
  }

  /**
   * 准备表格数据
   * @param {Array} data - 原始数据
   * @param {Array} headers - 表头
   * @returns {Array} 表格数据数组
   */
  static prepareTableData(data, headers) {
    const tableData = []
    
    // 添加表头
    tableData.push(headers)
    
    // 添加数据行
    data.forEach(item => {
      const row = [
        item.cigCode || '',
        item.cigName || '',
        item.year || '',
        item.month || '',
        item.weekSeq || '',
        item.deliveryArea || '',
        item.advAmount || 0,
        item.actualDelivery || 0
      ]
      
      // 添加30个档位数据（从30档到1档）
      for (let i = 30; i >= 1; i--) {
        const dKey = `d${i}`
        row.push(item[dKey] || 0)
      }
      
      // 备注列放在最后
      row.push(item.remark || '')
      
      tableData.push(row)
    })
    
    return tableData
  }

  /**
   * 设置列宽
   * @param {Object} worksheet - 工作表对象
   * @param {Array} headers - 表头数组
   */
  static setColumnWidths(worksheet, headers) {
    const columnWidths = {
      '卷烟代码': 12,
      '卷烟名称': 25,
      '年份': 8,
      '月份': 8,
      '周序号': 10,
      '投放区域': 30,
      '预投放量': 12,
      '实际投放量': 12,
      '备注': 20
    }
    
    // 设置基础列宽
    headers.forEach((header, index) => {
      const colKey = XLSX.utils.encode_col(index)
      if (columnWidths[header]) {
        worksheet[`${colKey}1`] = { 
          ...worksheet[`${colKey}1`],
          w: columnWidths[header]
        }
      } else if (header.includes('档')) {
        // 档位列设置固定宽度
        worksheet[`${colKey}1`] = { 
          ...worksheet[`${colKey}1`],
          w: 8
        }
      }
    })
  }

  /**
   * 设置工作表样式
   * @param {Object} worksheet - 工作表对象
   * @param {number} dataRowCount - 数据行数
   */
  static setWorksheetStyles(worksheet, dataRowCount) {
    const range = XLSX.utils.decode_range(worksheet['!ref'])
    
    // 设置表头样式
    for (let col = range.s.c; col <= range.e.c; col++) {
      const colKey = XLSX.utils.encode_col(col)
      const headerCell = worksheet[`${colKey}1`]
      
      if (headerCell) {
        headerCell.s = {
          font: { bold: true, color: { rgb: 'FFFFFF' } },
          fill: { fgColor: { rgb: '4472C4' } },
          alignment: { horizontal: 'center', vertical: 'center' },
          border: {
            top: { style: 'thin' },
            bottom: { style: 'thin' },
            left: { style: 'thin' },
            right: { style: 'thin' }
          }
        }
      }
    }
    
    // 设置数据行样式
    for (let row = 2; row <= dataRowCount + 1; row++) {
      for (let col = range.s.c; col <= range.e.c; col++) {
        const colKey = XLSX.utils.encode_col(col)
        const cell = worksheet[`${colKey}${row}`]
        
        if (cell) {
          // 数值列居中对齐
          if (col >= 8 && col <= 37) { // 档位列（从第9列到第38列，0-based索引）
            cell.s = {
              alignment: { horizontal: 'center', vertical: 'center' },
              border: {
                top: { style: 'thin' },
                bottom: { style: 'thin' },
                left: { style: 'thin' },
                right: { style: 'thin' }
              }
            }
          } else {
            cell.s = {
              border: {
                top: { style: 'thin' },
                bottom: { style: 'thin' },
                left: { style: 'thin' },
                right: { style: 'thin' }
              }
            }
          }
        }
      }
    }
  }

  /**
   * 添加查询条件信息工作表
   * @param {Object} workbook - 工作簿对象
   * @param {Object} searchParams - 查询参数
   */
  static addSearchInfoSheet(workbook, searchParams) {
    const infoData = [
      ['查询条件信息'],
      [''],
      ['查询时间', new Date().toLocaleString('zh-CN')],
      ['年份', searchParams.year || '未指定'],
      ['月份', searchParams.month || '未指定'],
      ['周序号', searchParams.weekSeq || '未指定'],
      [''],
      ['导出说明'],
      ['本文件包含卷烟投放数据统计表，包含30个档位的分配值'],
      ['数据来源：系统查询结果'],
      ['导出时间：' + new Date().toLocaleString('zh-CN')]
    ]
    
    const infoWorksheet = XLSX.utils.aoa_to_sheet(infoData)
    
    // 设置列宽
    infoWorksheet['A1'] = { ...infoWorksheet['A1'], w: 20 }
    infoWorksheet['B1'] = { ...infoWorksheet['B1'], w: 30 }
    
    // 设置样式
    infoWorksheet['A1'].s = {
      font: { bold: true, size: 14, color: { rgb: '4472C4' } }
    }
    
    XLSX.utils.book_append_sheet(workbook, infoWorksheet, '查询信息')
  }

  /**
   * 导出档位设置数据
   * @param {Object} positionData - 档位数据
   * @param {Object} cigaretteInfo - 卷烟信息
   * @param {string} filename - 文件名（可选）
   */
  static exportPositionData(positionData, cigaretteInfo = {}, filename = '') {
    try {
      if (!positionData || Object.keys(positionData).length === 0) {
        throw new Error('没有可导出的档位数据')
      }

      const timestamp = new Date().toISOString().slice(0, 19).replace(/:/g, '-')
      const defaultFilename = `档位设置_${cigaretteInfo.cigName || '卷烟'}_${timestamp}.xlsx`
      const finalFilename = filename || defaultFilename

      // 创建工作簿
      const workbook = XLSX.utils.book_new()
      
      // 准备档位数据
      const positionTableData = this.preparePositionTableData(positionData, cigaretteInfo)
      
      // 创建工作表
      const worksheet = XLSX.utils.aoa_to_sheet(positionTableData)
      
      // 设置列宽
      worksheet['A1'] = { ...worksheet['A1'], w: 15 }
      worksheet['B1'] = { ...worksheet['B1'], w: 12 }
      
      // 设置样式
      worksheet['A1'].s = {
        font: { bold: true, color: { rgb: 'FFFFFF' } },
        fill: { fgColor: { rgb: '4472C4' } },
        alignment: { horizontal: 'center', vertical: 'center' }
      }
      
      worksheet['B1'].s = {
        font: { bold: true, color: { rgb: 'FFFFFF' } },
        fill: { fgColor: { rgb: '4472C4' } },
        alignment: { horizontal: 'center', vertical: 'center' }
      }
      
      // 添加工作表到工作簿
      XLSX.utils.book_append_sheet(workbook, worksheet, '档位设置')
      
      // 导出文件
      const excelBuffer = XLSX.write(workbook, { 
        bookType: 'xlsx', 
        type: 'array' 
      })
      
      const blob = new Blob([excelBuffer], { 
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' 
      })
      
      saveAs(blob, finalFilename)
      
      return {
        success: true,
        message: '档位设置Excel文件导出成功',
        filename: finalFilename
      }
      
    } catch (error) {
      console.error('档位设置Excel导出失败:', error)
      return {
        success: false,
        message: `导出失败: ${error.message}`,
        error: error
      }
    }
  }

  /**
   * 准备档位设置表格数据
   * @param {Object} positionData - 档位数据
   * @param {Object} cigaretteInfo - 卷烟信息
   * @returns {Array} 档位设置表格数据
   */
  static preparePositionTableData(positionData, cigaretteInfo) {
    const tableData = [
      ['档位', '分配值']
    ]
    
    // 添加档位数据（从30档到1档）
    for (let i = 30; i >= 1; i--) {
      const positionKey = `position${i}`
      const value = positionData[positionKey] || 0
      tableData.push([`${i}档`, value])
    }
    
    // 添加卷烟信息
    if (cigaretteInfo.cigName) {
      tableData.unshift(['卷烟名称', cigaretteInfo.cigName])
      tableData.unshift([''])
    }
    
    return tableData
  }
}

export default ExcelExporter
