# 合并后的一些更新说明

1. 有些地方合并之后测试的时候有些bug我修复了并写在后端的对应说明文件下
2. 移除了本来就该移除的一些配置和依赖文件
   1. 前端项目 (CigaretteDistributionDemo):
      + node_modules/ - NPM依赖
      + package-lock.json - 版本锁定文
      + dist/ 和 build/ - 构建输出
      + .env* - 环境变量文件
   2. 后端项目 (CIgaretteDistributionServerDemo):
      + target/ - Maven构建目录
      + .idea/ - IntelliJ IDEA配置
      + *.class - Java编译文件*
      + .log - 日志文件
   3. 通用文件:
      + .DS_Store - macOS系统文件
      + 编辑器配置文件
      + 临时文件



**之后clone到本地运行前端npm install重新安装一下依赖再npm run serve，后端同步一下maven依赖**
