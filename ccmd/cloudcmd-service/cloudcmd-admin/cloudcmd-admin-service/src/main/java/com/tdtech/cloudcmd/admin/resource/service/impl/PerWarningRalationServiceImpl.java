package com.tdtech.cloudcmd.admin.resource.service.impl;

import cloudcmd.dto.PerWarningRalationDto;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.admin.resource.entity.Carousel;
import com.tdtech.cloudcmd.admin.resource.entity.PerWarningRalation;
import com.tdtech.cloudcmd.admin.resource.entity.dto.CarouselDO;
import com.tdtech.cloudcmd.admin.resource.entity.dto.PerWarningRalationDO;
import com.tdtech.cloudcmd.admin.resource.entity.vo.PerWarningRalationPageReqVO;
import com.tdtech.cloudcmd.admin.resource.entity.vo.PerWarningRalationSaveReqVO;
import com.tdtech.cloudcmd.admin.resource.mapper.PerWarningRalationMapper;
import com.tdtech.cloudcmd.admin.resource.service.IPerWarningRalationService;
import com.tdtech.cloudcmd.bean.PageResult;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: S063874
 * @date: 2026-01-13 14:59
 */
@Service
@Slf4j
public class PerWarningRalationServiceImpl implements IPerWarningRalationService {

    @Resource
    private PerWarningRalationMapper perWarningRalationMapper;

    @Autowired
    private IdWorker idWorker;

    private static final String SPLIT_CHAR = ",";

    /**
     * 创建推送预警
     *
     * @param perWarningRalationSaveReqVO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int createPerWaringRalation(PerWarningRalationSaveReqVO perWarningRalationSaveReqVO) {
        // 获取参数
        String targetIds = perWarningRalationSaveReqVO.getTargetIds();
        List<String> targetNameList = perWarningRalationSaveReqVO.getTargetNames();
        String idCards = perWarningRalationSaveReqVO.getIdCard();
        //拆分多条数据
        List<String> targetIdList = Arrays.stream(targetIds.split(SPLIT_CHAR)).collect(Collectors.toList());
        List<String> idCardList = null;
        if (StringUtils.isNotBlank(idCards)) {
            idCardList = Arrays.stream(idCards.split(SPLIT_CHAR)).collect(Collectors.toList());
        }
        // 先删除相同配置的数据 在统一入库，避免重复配置入库
        int nums = deletePerWaringList(perWarningRalationSaveReqVO);
        log.info("删除相同配置的数据条数：{}", nums);
        List<PerWarningRalation> perWarningRalationList = new ArrayList<>();
        for (int i = 0; i < targetIdList.size(); i++) {
            long id = idWorker.nextId();
            PerWarningRalation perWarningRalation = BeanCopyUtils.copyBean(perWarningRalationSaveReqVO, PerWarningRalation::new);
            perWarningRalation.setId(id);
            perWarningRalation.setTargetId(Long.parseLong(targetIdList.get(i)));
            perWarningRalation.setTargetName(targetNameList.get(i));
            if (CollectionUtils.isNotEmpty(idCardList)) {
                perWarningRalation.setIdCard(idCardList.get(i));
            }
            perWarningRalationList.add(perWarningRalation);
        }
        // 执行批量插入
        perWarningRalationMapper.batchInsert(perWarningRalationList);
        // 返回插入id
        return targetIdList.size();
    }

    /**
     * 查询单挑推送预警
     *
     * @param perWarningRalationSaveReqVO
     * @return
     */
    public PerWarningRalation selectPerWaringOne(PerWarningRalationSaveReqVO perWarningRalationSaveReqVO) {
        Integer classify = perWarningRalationSaveReqVO.getClassify();
        Integer targetType = perWarningRalationSaveReqVO.getTargetType();
        Long targetId = perWarningRalationSaveReqVO.getTargetId();
        Long businessId = perWarningRalationSaveReqVO.getBusinessId();
        QueryWrapper<PerWarningRalation> wrapper = new QueryWrapper<>();

        wrapper.eq("classify", classify);
        wrapper.eq("business_id", businessId);
        wrapper.eq("target_type", targetType);
        wrapper.eq("target_id", targetId);
        wrapper.ne("id", perWarningRalationSaveReqVO.getId());

        return perWarningRalationMapper.selectOne(wrapper);
    }

    /**
     *
     * @param perWarningRalationSaveReqVO
     * @return
     */
    public int deletePerWaringList(PerWarningRalationSaveReqVO perWarningRalationSaveReqVO) {
        Integer classify = perWarningRalationSaveReqVO.getClassify();
        Integer targetType = perWarningRalationSaveReqVO.getTargetType();
        Long businessId = perWarningRalationSaveReqVO.getBusinessId();
        String targetIds = perWarningRalationSaveReqVO.getTargetIds();
        List<Long> targetIdList = Arrays.stream(targetIds.split(SPLIT_CHAR)).map(Long::parseLong).collect(Collectors.toList());
        QueryWrapper<PerWarningRalation> wrapper = new QueryWrapper<>();

        wrapper.eq("classify", classify);
        wrapper.eq("business_id", businessId);
        wrapper.eq("target_type", targetType);
        wrapper.in("target_id", targetIdList);
        return perWarningRalationMapper.delete(wrapper);
    }

    /**
     * 更新推送预警
     *
     * @param perWarningRalationSaveReqVO
     */
    @Override
    public boolean updatePerWaringRalation(PerWarningRalationSaveReqVO perWarningRalationSaveReqVO) {
        // 更新
        PerWarningRalation perWarningRalation = BeanCopyUtils.copyBean(perWarningRalationSaveReqVO, PerWarningRalation::new);
        if (perWarningRalation.getId() == null) {
            throw new RuntimeException("id不能为空");
        }
        // 更新前先查询是否已经存在同一类别下相同配置,有相同配置直接返回
        PerWarningRalation exsitWarningRalation = selectPerWaringOne(perWarningRalationSaveReqVO);
        if (exsitWarningRalation != null) {
            log.info("配置已存在，请重新配置");
            return false;
        }
        perWarningRalationMapper.updateById(perWarningRalation);
        return true;
    }

    @Override
    public void deletePerWaringRalation(Long id) {
        perWarningRalationMapper.deleteById(id);
    }

    @Override
    public void deletePerWaringRalationListByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new RuntimeException("ids不能为空");
        }
        perWarningRalationMapper.deleteBatchIds(ids);
    }

    @Override
    public PageResult<PerWarningRalationDO> getPerWarningRalationPage(PerWarningRalationPageReqVO pageReqVO) {
        var page = new Page<PerWarningRalation>(pageReqVO.getPageNum(), pageReqVO.getPageSize());
        Page<PerWarningRalationDO> perWarningRalationDOPage = perWarningRalationMapper.selectPerWarningRalationPage(page, pageReqVO);

        var pageResult = new PageResult<PerWarningRalationDO>();
        pageResult.setPages(perWarningRalationDOPage.getPages());
        pageResult.setSize(perWarningRalationDOPage.getSize());
        pageResult.setTotal(perWarningRalationDOPage.getTotal());
        pageResult.setCurrent(perWarningRalationDOPage.getCurrent());
        pageResult.setRecords(perWarningRalationDOPage.getRecords());
        return pageResult;
    }

    @Override
    public List<PerWarningRalation> getPerWarningRalationByPostId(Long postId) {
        QueryWrapper<PerWarningRalation> wrapper = new QueryWrapper<>();
        wrapper.eq("classify", 2);
        wrapper.eq("business_id", postId);
        return perWarningRalationMapper.selectList(wrapper);
    }

    @Override
    public List<PerWarningRalation> getPerWarningRalationByDeptId(List<Long> deptIds) {
        QueryWrapper<PerWarningRalation> wrapper = new QueryWrapper<>();
        wrapper.eq("classify", 1);
        wrapper.in("business_id", deptIds);
        return perWarningRalationMapper.selectList(wrapper);
    }

    @Override
    public int deleteGroupWarningRalation(Long groupId) {
        QueryWrapper<PerWarningRalation> wrapper = new QueryWrapper<>();
        wrapper.eq("target_type", 2);
        wrapper.eq("target_id", groupId);
        int deleted = perWarningRalationMapper.delete(wrapper);
        log.info("delete group warning relation, groupId:{}, deleted:{}", groupId, deleted);
        return deleted;
    }

    @Override
    public void updateTargetName(Long targetId, String newName) {
        LambdaUpdateWrapper<PerWarningRalation> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PerWarningRalation::getTargetId, targetId)
               .eq(PerWarningRalation::getTargetType, 1)
               .set(PerWarningRalation::getTargetName, newName);
        perWarningRalationMapper.update(null, wrapper);
    }

}