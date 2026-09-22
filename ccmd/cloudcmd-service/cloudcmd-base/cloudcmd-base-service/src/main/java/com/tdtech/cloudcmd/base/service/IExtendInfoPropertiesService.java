package com.tdtech.cloudcmd.base.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.base.entity.ExtendInfoProperties;

/**
 * <p>
 * 扩展信息属性定义，通过反射实现扩展类 服务类
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-07
 */
public interface IExtendInfoPropertiesService extends IService<ExtendInfoProperties> {

    /**
     * 根据code查询相关的 扩展属性
     * 
     * @param code
     * @return
     */
    List<ExtendInfoProperties> getExtendInfoPropertiesListByCode(String code);

    /**
     * 获取所有的属性分类
     * 
     * @return
     */
    Map<String, List<ExtendInfoProperties>> getExtendInfoPropertiesMap();

    /**
     * 根据扩展属性编号和名称查询标签
     * 
     * @return
     * @param code
     * @param name
     */
    ExtendInfoProperties getExtendInfoPropertiesLabelByCodeAndName(String code, String name);

    /**
     * 根据扩展名称查找标签
     * 
     * @param name
     * @return
     */
    String getExtendInfoPropertiesLabelByName(String name);
}
