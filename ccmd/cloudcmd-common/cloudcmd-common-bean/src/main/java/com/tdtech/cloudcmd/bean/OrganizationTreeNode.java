package com.tdtech.cloudcmd.bean;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author cangPeng
 * @date 2023/6/14
 */
@Data
public class OrganizationTreeNode implements Serializable {
    private Long orgId;
    private Long pid;
    private List<Long> children;
}
