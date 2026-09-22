package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.im.jingxin.api.entity.department.OrganizationVO;
import com.tdtech.cloudcmd.im.jingxin.server.service.IOrganizationService;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.json.JsonArray;
import com.tdtech.cloudcmd.util.nodedispatch.NodeDispatchClient;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Tag(name = "组织部门信息")
@Slf4j
@RestController
@RequestMapping("/collaboration/v1/organization")
@RequiredArgsConstructor
@Validated
public class OrganizationController {

    @Resource
    private IOrganizationService organizationService;
    @Resource
    private NodeDispatchClient nodeDispatchClient;

    @GetMapping("/tree")
    @Operation(summary = "查询部门树状信息", description = "根据条件查询部门树状信息；传入 peerId 时查询对端节点组织树")
    public R<OrganizationVO> tree(@Parameter(description = "父级组织代码") @RequestParam(required = false,
            value = "parentCode") String parentCode,
                                        @Parameter(description = "父级ID") @RequestParam(required = false, value = "parentId") Long parentId,
                                        @Parameter(description = "目标节点 peerId，传入则查询对端节点组织树") @RequestParam(required = false, value = "peerId") String peerId) {
        // 跨节点查询：通过 linkx-node dispatch 透传到对端
        if (StringUtils.isNotBlank(peerId)) {
            Map<String, String> params = new HashMap<>(2);
            if (parentCode != null) params.put("parentCode", parentCode);
            if (parentId != null) params.put("parentId", String.valueOf(parentId));
            return nodeDispatchClient.dispatchAndParse(
                    peerId, "/collaboration/v1/organization/tree", params, R.class);
        }
        if (Objects.nonNull(parentId)) {
            return R.success(organizationService.tree(parentId));
        } else {
            return R.success(organizationService.tree(parentCode));
        }
    }

    @GetMapping("/user/tree")
    @Operation(summary = "获取用户授权部门树", description = "获取用户授权部门树")
    public R<Object> getUserInfoOrgTree() {
        UserInfo user = SecurityUtils.getUser();
        if(user == null){
            return R.success(new JsonArray());
        }else{
            if(CollectionUtils.isNotEmpty(user.getImOrgPrivs())){
                return R.success(user.getImOrgPrivs());
            }else {
                return R.success(new JsonArray());
            }
        }
    }
}
