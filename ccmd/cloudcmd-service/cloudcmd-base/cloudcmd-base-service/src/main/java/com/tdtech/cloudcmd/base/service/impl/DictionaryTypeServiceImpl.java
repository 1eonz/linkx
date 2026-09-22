package com.tdtech.cloudcmd.base.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.base.entity.DictionaryType;
import com.tdtech.cloudcmd.base.mapper.DictionaryTypeMapper;
import com.tdtech.cloudcmd.base.service.IDictionaryTypeService;

/**
 * <p>
 * 数据字典类型表 服务实现类
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-07
 */
@Service
public class DictionaryTypeServiceImpl extends ServiceImpl<DictionaryTypeMapper, DictionaryType>
    implements IDictionaryTypeService {

    @Autowired
    DictionaryTypeMapper dictionaryTypeMapper;

    @Override
    public List<DictionaryType> getDictionaryTypeList() {
        return dictionaryTypeMapper.selectList(null);
    }
}
