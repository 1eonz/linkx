package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.toolkit.MPJWrappers;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.server.entity.PoliceTicketType;
import com.tdtech.cloudcmd.im.jingxin.server.entity.PoliceTicketTypeQO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TrPoliceTicketTypePost;
import com.tdtech.cloudcmd.im.jingxin.server.service.PoliceTicketTypeService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.PoliceTicketTypeMapper;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.TrPoliceTicketTypePostMapper;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PoliceTicketTypeServiceImpl implements PoliceTicketTypeService {

    private final PoliceTicketTypeMapper policeTicketTypeMapper;
    private final TrPoliceTicketTypePostMapper trPoliceTicketTypePostMapper;
    private final IdWorker idWorker;

    /**
     * 创建PoliceTicketType记录
     */
    @Override
    public int create(PoliceTicketType policeTicketType) {
        var cnt = policeTicketTypeMapper.selectCount(Wrappers.lambdaQuery(PoliceTicketType.class)
                .eq(PoliceTicketType::getTag, policeTicketType.getTag()));
        if (cnt != 0) {
            throw new BusinessException("名称不允许重复");
        }

        log.info("Creating police ticket type: {}", policeTicketType);
        var user = SecurityUtils.getUser();
        if (Objects.isNull(user)) {
            throw new SecurityUtils.UnAuthException("access token invalid");
        }
        policeTicketType.setId(idWorker.nextId());
        policeTicketType.setCreator(user.getUserName());
        policeTicketType.setCreatorId(user.getUserId());
        policeTicketType.setGmtCreated(new Date());
        return policeTicketTypeMapper.insert(policeTicketType);
    }

    /**
     * 根据ID查询PoliceTicketType记录
     */
    @Override
    public PoliceTicketType findById(Long id) {
        log.info("Finding police ticket type by id: {}", id);
        return policeTicketTypeMapper.selectById(id);
    }

    @Override
    public List<PoliceTicketType> findByIdList(List<Long> idList) {
        return policeTicketTypeMapper.selectBatchIds(idList);
    }

    /**
     * 更新PoliceTicketType记录
     */
    @Override
    public int update(PoliceTicketType policeTicketType) {
        log.info("Updating police ticket type: {}", policeTicketType);
        var cnt = policeTicketTypeMapper.selectCount(Wrappers.lambdaQuery(PoliceTicketType.class)
                .eq(PoliceTicketType::getTag, policeTicketType.getTag())
                .ne(PoliceTicketType::getId, policeTicketType.getId()));
        if (cnt != 0) {
            throw new BusinessException("名称不允许重复");
        }
        return policeTicketTypeMapper.updateById(policeTicketType);
    }

    /**
     * 根据ID删除PoliceTicketType记录
     */
    @Override
    public int deleteById(Long id) {
        log.info("Deleting police ticket type by id: {}", id);
        return policeTicketTypeMapper.deleteById(id);
    }

    /**
     * 查询所有PoliceTicketType记录
     */
    @Override
    public List<PoliceTicketType> findAll(@NotNull PoliceTicketTypeQO query) {
        var wrapper = MPJWrappers.lambdaJoin(PoliceTicketType.class)
                .selectAll(PoliceTicketType.class).distinct();
        if (query.getPostId() != null && !query.getPostId().isEmpty()) {
            wrapper = wrapper
                    .innerJoin(TrPoliceTicketTypePost.class, TrPoliceTicketTypePost::getTypeId,
                            PoliceTicketType::getId)//
                    .in(TrPoliceTicketTypePost::getPostId, query.getPostId());
        }
        return policeTicketTypeMapper.selectList(wrapper);
    }

    /**
     * 分页查询PoliceTicketType记录
     */
    @Override
    public Page<PoliceTicketType> findPage(Page<PoliceTicketType> page,
                                           PoliceTicketType policeTicketType) {
        var queryWrapper = Wrappers.lambdaQuery(PoliceTicketType.class);
        // 可根据policeTicketType的属性构建查询条件
        if (policeTicketType != null) {
            // 示例：添加查询条件
            queryWrapper.like(
                    policeTicketType.getTag() != null && !policeTicketType.getTag().isBlank(),
                    PoliceTicketType::getTag, policeTicketType.getTag());
        }
        return policeTicketTypeMapper.selectPage(page, queryWrapper);
    }

    @Override
    public void bindPost(@NotNull Long postId, @NotNull List<Long> typeIds) {
        if (typeIds == null || typeIds.isEmpty()) {
            return;
        }
        deleteBinding(postId);
        trPoliceTicketTypePostMapper.insertBatch(typeIds.stream()
                .map(a -> new TrPoliceTicketTypePost(idWorker.nextId(), a, postId))
                .collect(Collectors.toList()));
    }

    @Override
    public void deleteBinding(Long postId) {
        trPoliceTicketTypePostMapper
                .delete(Wrappers.lambdaQuery(TrPoliceTicketTypePost.class)
                        .eq(TrPoliceTicketTypePost::getPostId, postId));
    }

    @Override
    public List<Long> getPosts(String tag) {
        var policeTicketTypes = policeTicketTypeMapper.selectList(
            Wrappers.lambdaQuery(PoliceTicketType.class).eq(PoliceTicketType::getTag, tag));
        if (policeTicketTypes == null || policeTicketTypes.isEmpty()) {
            return Collections.emptyList();
        }
        return trPoliceTicketTypePostMapper.selectPostIdsByTag(
            policeTicketTypes.stream().map(PoliceTicketType::getId).collect(Collectors.toList()));
    }

    @Override
    public List<String> getTag(Long postId) {
        var trPoliceTicketTypePosts = trPoliceTicketTypePostMapper.selectList(Wrappers.lambdaQuery(TrPoliceTicketTypePost.class)
                .eq(TrPoliceTicketTypePost::getPostId, postId));
        if (trPoliceTicketTypePosts == null || trPoliceTicketTypePosts.isEmpty()) {
            log.warn("post bind not found:{}", postId);
            return Collections.emptyList();
        }
        var typeIds = trPoliceTicketTypePosts.stream()
                .map(TrPoliceTicketTypePost::getTypeId)
                .collect(Collectors.toList());
        var policeTicketTypes = policeTicketTypeMapper.selectList(Wrappers.lambdaQuery(PoliceTicketType.class)
                .in(PoliceTicketType::getId, typeIds));
        log.debug("getTag policeTicketTypes:{}", policeTicketTypes);
        return policeTicketTypes == null ? Collections.emptyList() : policeTicketTypes.stream()
                .map(PoliceTicketType::getTag).collect(Collectors.toList());
    }

    @Override
    public List<Long> getTypeIds(Long postId) {
        var trPoliceTicketTypePosts = trPoliceTicketTypePostMapper.selectList(Wrappers.lambdaQuery(TrPoliceTicketTypePost.class)
                .eq(TrPoliceTicketTypePost::getPostId, postId));
        if (trPoliceTicketTypePosts == null || trPoliceTicketTypePosts.isEmpty()) {
            log.warn("post bind not found:{}", postId);
            return Collections.emptyList();
        }
        return trPoliceTicketTypePosts.stream()
                .map(TrPoliceTicketTypePost::getTypeId)
                .collect(Collectors.toList());
    }
}
