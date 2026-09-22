package com.tdtech.cloudcmd.admin.offlinemap.repo;

import java.io.Serializable;

import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Component
@TableName("tb_division")
public class Division implements Serializable {
    /**
     * 主键
     */
    private Long id;

    /**
     * 行政划分文件名称
     */
    private String name;

    /**
     * 行政划分文件地址
     */
    private String address;

    /**
     * 行政划分id
     */
    private Long code;
}
