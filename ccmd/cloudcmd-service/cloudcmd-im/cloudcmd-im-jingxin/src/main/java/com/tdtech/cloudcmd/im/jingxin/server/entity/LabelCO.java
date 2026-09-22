package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.tdtech.cloudcmd.im.jingxin.client.entity.ImDepartment;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUser;
import com.tdtech.cloudcmd.im.jingxin.server.service.IOrganizationService;
import com.tdtech.cloudcmd.util.IdWorker;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class LabelCO extends Label {

    private Map<Long, Binding> orgCollaborations;

    private Map<Long, BindingUser> orgUsers;

    public List<LabelBinding> createBinding(IdWorker idWorker) {
        return Optional.ofNullable(orgCollaborations).stream().map(Map::entrySet).flatMap(Collection::stream).map(o -> {
            var value = o.getValue();
            var labelBinding = new LabelBinding();
            labelBinding.setId(idWorker.nextId());
            labelBinding.setDepartmentId(o.getKey());
            labelBinding.setDepartmentName(value.getOrgName());
            labelBinding.setDepartmentPath(value.getPath());
            labelBinding.setLabelId(this.getId());
            if (value.collaborations == null || value.collaborations.isEmpty()) {
                labelBinding.setPostIds(Collections.emptyList());
            }else {
                labelBinding.setPostIds(
                    value.collaborations.stream().map(CollaborationPost::getId).collect(Collectors.toList()));
            }
            return labelBinding;
        }).collect(Collectors.toList());
    }

    public List<LabelBindingUser> createBindingUser(IdWorker idWorker) {
        return Optional.ofNullable(orgUsers).stream().map(Map::entrySet).flatMap(Collection::stream).map(o -> {
            var value = o.getValue();
            var labelBindingUser = new LabelBindingUser();
            labelBindingUser.setId(idWorker.nextId());
            labelBindingUser.setDepartmentId(o.getKey());
            labelBindingUser.setDepartmentName(value.getOrgName());
            labelBindingUser.setDepartmentPath(value.getPath());
            labelBindingUser.setLabelId(this.getId());
            if (value.userIds == null || value.userIds.isEmpty()) {
                labelBindingUser.setUserIds(Collections.emptyList());
            }else {
                labelBindingUser.setUserIds(
                        value.userIds.stream().map(ImUser::getId).collect(Collectors.toList()));
            }
            return labelBindingUser;
        }).collect(Collectors.toList());
    }

    public static Binding createBindingView(LabelBinding value, List<CollaborationPost> posts, IOrganizationService organizationService) {
        var binding = new Binding();
        ImDepartment oneById = organizationService.findOneById(value.getDepartmentId());
        if (oneById != null) {
            binding.setOrgName(oneById.getName());
        } else {
            binding.setOrgName(value.getDepartmentName());
        }
        var ps = Optional.ofNullable(value.getPostIds()).stream().flatMap(Collection::stream).map(pid -> {
            for (var post : posts) {
                if (Objects.equals(post.getId(), pid)) {
                    return post;
                }
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        binding.setPath(value.getDepartmentPath());
        binding.setCollaborations(ps);
        return binding;
    }

    @Getter
    @Setter
    @ToString
    public static class Binding {
        private String orgName;
        private String path;
        private List<CollaborationPost> collaborations;
    }

    @Getter
    @Setter
    @ToString
    public static class BindingUser {
        private String orgName;
        private String path;
        private List<ImUser> userIds;
    }
}
