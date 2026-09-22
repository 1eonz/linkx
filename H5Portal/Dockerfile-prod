## AdoptOpenJDK 停止发布 OpenJDK 二进制，而 Eclipse Temurin 是它的延伸，提供更好的稳定性
## 感谢复旦核博士的建议！灰子哥，牛皮！
FROM nginx:latest

## 创建目录，并使用它作为工作目录
RUN mkdir -p /usr/share/nginx/app
WORKDIR /usr/share/nginx/app
## 将前端编译后的文件复制到 /usr/share/nginx/app
COPY ./unpackage/dist/build/web /usr/share/nginx/app
COPY ./docker/nginx.conf /etc/nginx/
# 暴露80端口，因为Nginx默认使用80端口

EXPOSE 80

# 启动Nginx服务器
CMD ["nginx", "-g", "daemon off;"]