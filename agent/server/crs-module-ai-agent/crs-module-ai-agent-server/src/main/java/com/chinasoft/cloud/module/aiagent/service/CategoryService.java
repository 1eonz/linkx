package com.chinasoft.cloud.module.aiagent.service;

import com.chinasoft.cloud.module.aiagent.controller.admin.co.UpsertCategoryCO;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.Category;

import java.util.List;

public interface CategoryService {
    List<Category> categoryList();

    void saveOrUpdateBatch(List<UpsertCategoryCO> categoryCOList);

    void deleteById(Long id);

    List<Category> findByIdList(List<Long> idList);

}
