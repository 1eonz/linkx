package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationPost;

import java.util.List;

public interface CollaborationPostGroupService {

    void deleteByPostId(List<Long> postIds);

    void changeUserStatus(Integer status, Long userId);

    void changeUserStatus(Integer status, Long userId, List<CollaborationPost> collaborationPosts);

    void addSupport(List<Long> userId, CollaborationPost post);

    void addSupport(Long userId);

    void addSupport(Long userId, List<CollaborationPost> collaborationPosts);

    void removeSupport(Long userId);

    List<Long> postsByGroupId(Long groupId);

    void removeSupport(Long userId, Long postId);
}