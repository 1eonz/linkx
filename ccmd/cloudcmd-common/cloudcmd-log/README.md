## Logback-Mask

日志匿名化工具。通过对属性字段进行设置，可以进行全匿名化或者半匿名化。

## 使用方式

### 1.在服务的pom.xml引入组件：

```xml
        <dependency>
            <groupId>com.tdtech.cloudcmd</groupId>
            <artifactId>cloudcmd-log</artifactId>
            <version>${cloudcmd.version}</version>
        </dependency>
```

### 2.在类上引入注解@Slf4j：

```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MaskTest {
    public static void main(String[] args) {
        String message = "User(id=1, name=zhangsan, passwd= eRR@com, phone=13177777777, email=test@qq.com)";
        log.info(message);
    }
}

//打印结果： User(id=1, name=***, passwd =***, phone=***7777, email=***.com)
```

说明：

* 属性字段=value,或value) eg:User(id=1, name=zhangsan)
* 属性字段:value,或value) eg:User(id:1, name:zhangsan)
  属性字段和=或者:中间可以有空格。:为英文的冒号(colon)。

### 3.在cloudcmd-log的logback.xml增加需要屏蔽的属性字段（不区分大小写）：

目前已经增加屏蔽字段包括：

 ```xml
                 <rules>
                     <!-- full mask -->
                     <rule>
                         <property>name|username|password|pwd|passwd|latitude|lat|lontitude|lon</property>
                     </rule>
                     <!-- partial mask -->
                     <rule>
                         <property>idcard|email|phone</property>
                         <unmasked>4</unmasked>
                     </rule>
                 </rules>
```

如需增加新的屏蔽属性字段请在相应的rule的property中增加。