package com.tdtech.cloudcmd.admin.offlinemap.repo;

import java.io.Serializable;
import java.util.Date;

import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Component
@TableName("tb_base_map")
public class BaseMap implements Serializable {
    /**
     * 主键
     */
    private Long id;

    /**
     * 序号
     */
    private Long number;

    /**
     * 底图文件名称
     */
    private String name;

    /**
     * 底图文件大小
     */
    private String size;

    /**
     * 创建时间
     */
    private Date created;

    /**
     * 瓦片地址
     */
    private String tiles;
}
