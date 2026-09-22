# 使用官方 Maven 镜像作为基础镜像进行构建
FROM 10.28.64.59:5000/maven:3.9.11-eclipse-temurin-21 AS build

# 设置工作目录
WORKDIR /app

# 复制 Maven 依赖文件
COPY . .

RUN mkdir -p /repo && \
    mvn \
    -DskipTests=true \
    -s /app/settings.xml \
    package

FROM 127.0.0.1:5000/jre:21.msjdk.minideb.trixie.amd64

WORKDIR /app
COPY --from=build /app/target/knife4j-gateway-0.0.1-SNAPSHOT.jar ./app.jar

CMD java -jar /app/app.jar