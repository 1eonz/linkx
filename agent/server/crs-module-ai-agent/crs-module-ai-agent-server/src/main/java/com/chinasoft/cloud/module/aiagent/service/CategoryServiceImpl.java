package com.chinasoft.cloud.module.aiagent.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.chinasoft.cloud.framework.common.exception.util.ServiceExceptionUtil;
import com.chinasoft.cloud.framework.utils.IdWorker;
import com.chinasoft.cloud.module.aiagent.controller.admin.co.UpsertCategoryCO;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.Category;
import com.chinasoft.cloud.module.aiagent.dal.mysql.CategoryMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.chinasoft.cloud.module.aiagent.enums.ErrorCodeConstants.CATEGORY_IN_USE;

@Slf4j
@Service
@Validated
public class CategoryServiceImpl implements CategoryService {
    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private AiAgentConfigService aiAgentConfigService;

    @Resource
    private IdWorker idWorker;

    @Override
    public void saveOrUpdateBatch(List<UpsertCategoryCO> categoryCOList) {
        if (CollectionUtils.isEmpty(categoryCOList)) {
            return;
        }

        List<Category> categoryList = categoryCOList.stream().map(co -> {
            Category category = new Category();
            BeanUtil.copyProperties(co, category);

            if (Objects.isNull(category.getId())) {
                category.setId(idWorker.nextId());
            }
            return category;
        }).collect(Collectors.toList());

        categoryMapper.insertOrUpdate(categoryList);
    }


    @Override
    public void deleteById(Long id) {
        var old = categoryMapper.selectById(id);
        if (old == null) {
            log.warn("old category not found for id {}", id);
            return;
        }
        Long count = aiAgentConfigService.countByCategoryId(id);
        if (Objects.nonNull(count) && count.longValue() > 0) {
            throw ServiceExceptionUtil.exception(CATEGORY_IN_USE);
        }
        categoryMapper.deleteById(id);
    }

    @Override
    public List<Category> findByIdList(List<Long> idList) {
        return categoryMapper.selectByIds(idList);
    }


    @Override
    public List<Category> categoryList() {
        return categoryMapper.selectList();
    }

}
