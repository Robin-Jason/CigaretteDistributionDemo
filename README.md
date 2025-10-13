# 更新说明

**项目clone到本地运行前进行如下步骤：**

1. 前端npm install重新安装一下依赖再npm run serve启动（有冲突用--legacy-peer-deps绕过冲突）
2. 后端applicantion修改数据库账户和密码
3. 后端同步一下maven依赖再mvn spring-boot:run启动



## 10.13

完成根据卷烟备注是否存在描述两周一访上浮100%来使用不同的区域客户数表（非双周上浮或双周上浮）---->两周一订的另一种描述



## 10.10

1. 完成导入卷烟投放基本信息表和区域客户数表功能
2. 将操作数据库表从原来的表重构为动态表名查询
3. 重构Entity包，Repository包
4. 改变服务层架构为接口+实现类的架构
5. 重构服务层的数据CRUD功能，分配计算统一接口，表导入服务，新增SQL，RowMapper工具类等
6. 将各类型卷烟分配计算服务的设计模式改为工厂模式，均存放于strategy包下
7. 档位+城乡分类代码部分已重构



## 9.30

1. 前后端新增卷烟投放信息的编码与解码功能
2. 前端新增卷烟的三维图表显示所有目标区域的投放情况



## 9.20

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



****
