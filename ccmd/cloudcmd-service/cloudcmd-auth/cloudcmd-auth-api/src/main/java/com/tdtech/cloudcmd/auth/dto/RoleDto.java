package com.tdtech.cloudcmd.auth.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tdtech.cloudcmd.auth.controller.admin.vo.OrgPrivDto;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.json.JsonArray;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author mWX556161
 * @date 2020/9/27 14:13
 */
@Data
public class RoleDto implements Serializable {

    /**
     * 名称
     */
    private String name;

    /**
     * 角色类型
     */
    private Integer type;

    private List<String> iccPrivJson;

    private List<String> cappPrivJson;

    private List<String> adminPrivJson;

    private JsonArray imOrgPrivJson;

    private List<OrgPermissionDto> imOrgPriv;

    /**
     * 角色数据权限列表
     */
    private List<OrgPrivDto> orgPrivList;

    /**
     * 角色描述
     */
    private String remark;

    /**
     * 状态 0-可用,1-禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date gmtCreated;

    /**
     * 修改时间
     */
    private Date gmtModified;

    public void convertOrgJson() {
        if (imOrgPrivJson != null) {
            imOrgPriv = JsonUtil.convert(imOrgPrivJson, new TypeReference<>() {
            });
        }
    }

    public Set<String> getOrgPermissionIds() {
        Set<String> result = new HashSet<>();
        if (CollectionUtils.isEmpty(orgPrivList)) {
            return result;
        }
        for (OrgPrivDto org : orgPrivList) {
            result.add(String.valueOf(org.getId()));
        }
        return result;
    }
}
