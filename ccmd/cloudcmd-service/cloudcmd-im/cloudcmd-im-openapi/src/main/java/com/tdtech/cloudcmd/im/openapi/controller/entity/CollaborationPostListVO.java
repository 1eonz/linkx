package com.tdtech.cloudcmd.im.openapi.controller.entity;

import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationPostVO;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUser;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString(callSuper = true)
public class CollaborationPostListVO extends CollaborationPostVO {

    private List<ImUser> members;

}
