module.exports = {
  transpileDependencies: [],
  lintOnSave: false,
  
  // 开发服务器配置
  devServer: {
    port: 8080,
    proxy: {
      '/api/cigarette': {
        target: 'http://localhost:28080',
        changeOrigin: true,
        pathRewrite: {
          '^/api/cigarette': '/api/cigarette'
        }
      }
    }
  }
}
