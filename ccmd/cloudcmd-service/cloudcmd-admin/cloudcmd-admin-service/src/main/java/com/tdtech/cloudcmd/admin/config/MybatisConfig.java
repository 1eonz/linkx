package com.tdtech.cloudcmd.admin.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author : mWX556161
 * @date : 2020-05-27 14:46
 */
@Configuration
@MapperScan({"com.tdtech.cloudcmd.admin.**.mapper", "com.tdtech.cloudcmd.admin.offlinemap.repo"})
public class MybatisConfig {

}
