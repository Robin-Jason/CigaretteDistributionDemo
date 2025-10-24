FROM openresty/openresty:alpine-fat

# 复制前端构建产物到nginx html目录
COPY ./dist /usr/share/nginx/html/front

# 复制nginx配置文件
COPY ./ngx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["/usr/local/openresty/bin/openresty", "-g", "daemon off;"]