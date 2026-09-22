package com.tdtech.cloudcmd.base.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.base.entity.ExtendInfoProperties;
import com.tdtech.cloudcmd.base.mapper.ExtendInfoPropertiesMapper;
import com.tdtech.cloudcmd.base.service.IExtendInfoPropertiesService;
import com.tdtech.cloudcmd.util.StringUtils;

/**
 * <p>
 * 扩展信息属性定义，通过反射实现扩展类 服务实现类
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-07
 */
@Service
public class ExtendInfoPropertiesServiceImpl extends ServiceImpl<ExtendInfoPropertiesMapper, ExtendInfoProperties>
    implements IExtendInfoPropertiesService {

    @Autowired
    ExtendInfoPropertiesMapper extendInfoPropertiesMapper;

    @Override
    public List<ExtendInfoProperties> getExtendInfoPropertiesListByCode(String code) {
        QueryWrapper<ExtendInfoProperties> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ExtendInfoProperties.CODE, code);
        return extendInfoPropertiesMapper.selectList(queryWrapper);
    }

    @Override
    public Map<String, List<ExtendInfoProperties>> getExtendInfoPropertiesMap() {
        List<ExtendInfoProperties> extendInfoProperties = extendInfoPropertiesMapper.selectList(null);
        return extendInfoProperties.stream().collect(Collectors.groupingBy(ExtendInfoProperties::getCode));
    }

    @Override
    public ExtendInfoProperties getExtendInfoPropertiesLabelByCodeAndName(String code, String name) {
        QueryWrapper<ExtendInfoProperties> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ExtendInfoProperties.CODE, code);
        queryWrapper.eq(ExtendInfoProperties.NAME, name);
        return extendInfoPropertiesMapper.selectOne(queryWrapper);
    }

    @Override
    public String getExtendInfoPropertiesLabelByName(String name) {
        String result = "";
        if (StringUtils.isNotBlank(name)) {
            QueryWrapper<ExtendInfoProperties> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq(ExtendInfoProperties.NAME, name);
            ExtendInfoProperties extendInfoProperties = extendInfoPropertiesMapper.selectOne(queryWrapper);
            if (extendInfoProperties != null) {
                result = extendInfoProperties.getLabel();
            }
        }

        return result;
    }
}
