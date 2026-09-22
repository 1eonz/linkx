## 简述
    
## 目录结构

```  
.
│─pom.xml
│─README.md
│  
├─CI
│      pack.py
│      smbcopyversion.sh
│      web-build.py
│      
├─cloudcmd-common
│  │  pom.xml
│  │  
│  ├─cloudcmd-common-util
│  ├─cloudcmd-log
│  ├─cloudcmd-mybatis-plus-starter
│  ├─cloudcmd-redis-starter
│  ├─cloudcmd-spring-boot-starter
│  └─cloudcmd-spring-boot-starter-web
├─cloudcmd-framework
│  │  pom.xml
│  │  
│  ├─cloudcmd-gateway
│  │  │  pom.xml
│  │  │  
│  │  ├─bin
│  │  │      pid.sh
│  │  │      start.sh
│  │  │      stop.sh
│  │  │      
│  │  └─src
│  │      └─main
│  │          ├─java
│  │          │  └─com
│  │          │      └─tdtech
│  │          │          └─cloudcmd
│  │          │              └─gateway
│  │          │                  │  GatewayApplication.java
│  │          │                  │  
│  │          │                  ├─config
│  │          │                  └─filter
│  │          │                          CloudcmdGlobalFilter.java
│  │          │                          
│  │          └─resources
│  │                  banner.txt
│  │                  bootstrap.yml
│  │                  logback.xml
│  │                  
│  └─cloudcmd-isp-adapter
│      └─bin
│              pid.sh
│              
├─cloudcmd-pack
│      assembly.xml
│      cloudcmd-pack.iml
│      pom.xml
│      README.md
│      
├─cloudcmd-parent
│      cloudcmd-parent.iml
│      pom.xml
│      
├─cloudcmd-service
│  │  pom.xml
│  │  
│  └─cloudcmd-demo
│      │  pom.xml
│      │  
│      ├─cloudcmd-demo-api
│      │  │  pom.xml
│      │  │  
│      │  └─src
│      │      └─main
│      │          └─java
│      │              └─com
│      │                  └─tdtech
│      │                      └─cloud
│      │                          └─demo
│      │                              └─api
│      │                                  │  EventDubboService.java
│      │                                  │  
│      │                                  └─dto
│      │                                          EventDto.java
│      │                                          
│      ├─cloudcmd-demo-rest
│      │  │  pom.xml
│      │  │  
│      │  └─src
│      │      └─main
│      │          ├─java
│      │          │  └─com
│      │          │      └─tdtech
│      │          │          └─cloud
│      │          │              │  DemoRestApplication.java
│      │          │              │  
│      │          │              └─demo
│      │          │                  └─controller
│      │          │                          DemoController.java
│      │          │                          
│      │          └─resources
│      │                  banner.txt
│      │                  bootstrap.yml
│      │                  logback.xml
│      │                  
│      └─cloudcmd-demo-service
│          │  pom.xml
│          │  
│          ├─bin
│          │      start.sh
│          │      stop.sh
│          │      
│          └─src
│              ├─main
│              │  ├─java
│              │  │  └─com
│              │  │      └─tdtech
│              │  │          └─cloudcmd
│              │  │              │  DemoServiceApplication.java
│              │  │              │  
│              │  │              └─demo
│              │  │                  ├─config
│              │  │                  │      ServiceConfig.java
│              │  │                  │      
│              │  │                  ├─entity
│              │  │                  │      Event.java
│              │  │                  │      
│              │  │                  ├─mapper
│              │  │                  │      EventMapper.java
│              │  │                  │      
│              │  │                  ├─message
│              │  │                  │      CloudcmdPublish.java
│              │  │                  │      CloudcmdReceiver.java
│              │  │                  │      CloudcmdSender.java
│              │  │                  │      
│              │  │                  └─service
│              │  │                      │  IEventService.java
│              │  │                      │  
│              │  │                      └─impl
│              │  │                              EventDubboServiceImpl.java
│              │  │                              EventServiceImpl.java
│              │  │                              
│              │  └─resources
│              │      │  banner.txt
│              │      │  bootstrap.yml
│              │      │  logback.xml
│              │      │  
│              │      └─mapper
│              │          └─demo
│              │                  EventMapper.xml
│              │                  
│              └─test
│                  ├─java
│                  │  └─com
│                  │      └─tdtech
│                  │          └─cloudcmd
│                  │              ├─authentication
│                  │              │  └─message
│                  │              └─demo
│                  │                  └─message
│                  │                          MessageTest.java
│                  │                          
│                  └─resources
├─cloudcmd-web
│  └─cloudcmd-admin-web
├─doc
├─install
│  │  cloudcmd_setup.sh
│  │  eApp.sha512
│  │  install_icpx.py
│  │  name.txt
│  │  pack.list
│  │  sca_install.sh
│  │  setup_ch.sh
│  │  setup_en.sh
│  │  uninstall.sh
│  │  
│  ├─cloudcmd
│  │  ├─common-config
│  │  │      ccmd-boot-config.xml
│  │  │      
│  │  ├─nginx
│  │  │  └─vhost
│  │  │          cloudcmd-web-admin.conf
│  │  │          
│  │  └─third-apps
│  │          erlang.tar.gz
│  │          nacos-server-1.2.1.tar.gz
│  │          rabbitmq.tar.gz
│  │          
│  ├─cloud_adapter
│  │  │  cloud_adapter_setup.sh
│  │  │  
│  │  ├─conf
│  │  │      cloudcmd_admin_service_agent.xml
│  │  │      cloudcmd_alert_rest_agent.xml
│  │  │      cloudcmd_alert_service_agent.xml
│  │  │      cloudcmd_auth_service_agent.xml
│  │  │      cloudcmd_base_service_agent.xml
│  │  │      cloudcmd_cagent_service_agent.xml
│  │  │      cloudcmd_demo_service_agent.xml
│  │  │      cloudcmd_evidence_service_agent.xml
│  │  │      cloudcmd_gateway_service_agent.xml
│  │  │      cloudcmd_lbs_service_agent.xml
│  │  │      cloudcmd_lbs_rest_agent.xml
│  │  │      cloudcmd_resource_rest_agent.xml
│  │  │      cloudcmd_resource_service_agent.xml
│  │  │      cloudcmd_svcgrp_info.txt
│  │  │      cloudcmd_svctype_info.txt
│  │  │      cloudcmd_workflow_service_agent.xml
│  │  │      
│  │  └─db
│  │          nacos-mysql.sql
│  │          
│  └─isp
│          eApp.sha512
│          install_isp_basic.sh
│          ISP_BASIC.tar.gz
│          
└─script
    │  .gitkeep
    │  
    └─sql
        └─init
                test.sql
```               



## 技术选型

### 后端
- JDK：11
- 数据库：mysql
- 项目构建工具：Maven 3.6.1
- springcloud: Hoxton.RELEASE
- spring-boot: 2.2.2.RELEASE
- ORM框架：MyBatis-Plus 3.3.1
- 分布式协调服务：nocos 1.2.1
- 分布式RPC服务：Dubbo 2.7.4.1
- 分布式缓存服务：Redis 5.0
- 分布式消息服务：rabbitMQ-server 3.7.25
- JSON工具：Fastjson 1.2.56
- 日志管理：Logback 1.2.3

### 前端
- VUE：VUE2.6
## 架构图
- 终端：
    ![](doc/20200622-142755(eSpace).png)
## 数据库模型

## 效果展示

### 开发规约
- 禁止在controller捕获异常

### 调度台
### 终端
### 后端管理
### ops
### API文档
### 文档路径

### 增加同步仓库





