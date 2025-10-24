FROM openjdk:21

# 设置时区为上海
RUN cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo 'Asia/Shanghai' > /etc/timezone

WORKDIR /app

# 复制jar包
COPY app.jar /app/app.jar

# 复制生产环境配置文件
COPY application-prod.yml /app/application-prod.yml

# 创建日志目录
RUN mkdir -p /app/logs

EXPOSE 28080

CMD ["java", "-Xmx1024m", "-Xms512m", "-jar", "app.jar", "--spring.profiles.active=prod"]