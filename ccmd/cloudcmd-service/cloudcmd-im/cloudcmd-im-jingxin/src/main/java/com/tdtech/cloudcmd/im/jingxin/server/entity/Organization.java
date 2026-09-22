package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImDepartment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_organization")
public class Organization implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 部门编号
     */
    private String code;

    /**
     * 部门名称
     */
    private String name;

    /**
     * 部门简称
     */
    private String shortName;

    /**
     * 上级部门ID
     */
    private Long parentId;

    /**
     * 上级部门编号
     */
    private String parentCode;

    /**
     * 上级部门名称
     */
    private String parentName;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 部门ID全路径，逗号隔开
     */
    private String fullPath;

    /**
     * 部门编码全路径，逗号隔开
     */
    private String fullPathCode;

    /**
     * 部门名称全路径，逗号隔开
     */
    private String fullPathName;

    /**
     * 创建时间
     */
    private Date gmtCreated;

    /**
     * 修改时间
     */
    private Date gmtModified;

    public Organization() {
        super();
    }

    public Organization(ImDepartment imDepartment) {
        if (Objects.isNull(imDepartment)) {
            return;
        }
        this.id = imDepartment.getId();
        this.code = imDepartment.getCode();
        this.name = imDepartment.getName();
        this.shortName = imDepartment.getShortName();
        this.parentId = imDepartment.getParentId();
        this.parentCode = imDepartment.getParentCode();
        this.parentName = imDepartment.getParentName();
        this.sort = imDepartment.getSort();
        this.fullPath = imDepartment.getFullPath();
        this.fullPathCode = imDepartment.getFullPathCode();
        this.fullPathName = imDepartment.getFullPathName();
        this.gmtCreated = new Date(imDepartment.getGmtCreated());
        this.gmtModified = new Date(imDepartment.getGmtModified());
    }
}
