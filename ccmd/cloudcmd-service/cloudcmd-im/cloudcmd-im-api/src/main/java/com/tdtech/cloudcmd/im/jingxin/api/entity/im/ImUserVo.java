package com.tdtech.cloudcmd.im.jingxin.api.entity.im;

import com.tdtech.cloudcmd.im.jingxin.api.entity.virtualUser.UserDepartmentVo;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class ImUserVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String code;

    private String name;

    private String avatar;

    private String gender;

    private String mobile;

    private String email;

    private String idCard;

    private String directLeaderId;

    private List<UserDepartmentVo> userDepartments;

    private String status;

}
