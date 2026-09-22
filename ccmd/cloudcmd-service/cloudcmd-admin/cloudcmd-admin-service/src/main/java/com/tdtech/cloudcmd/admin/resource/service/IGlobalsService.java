package com.tdtech.cloudcmd.admin.resource.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.admin.resource.entity.Globals;
import com.tdtech.cloudcmd.admin.resource.entity.vo.AiDeployConfigVO;

/**
 * <p>
 * 全局变量信息表 服务类
 * </p>
 *
 * @author mWX556161
 * @since 2020-07-16
 */
public interface IGlobalsService extends IService<Globals> {
    /**
     * 查询全局配置列表
     * 
     * @return
     */
    List<Globals> getGlobalsList(String keyword);

    /**
     * 创建全局配置项
     * 
     * @param globals
     * @return
     */
    void createGlobals(Globals globals);

    /**
     * 修改全局配置项
     * 
     * @param globals
     * @return
     */
    void updateGlobals(Globals globals);

    void enableGlobals(Globals globals);

    /**
     * 批量删除字典配置项
     * 
     * @param id
     * @return
     */
    String deleteGlobalsById(Long id);

    String deleteGlobals(Globals globals);

    /**
     * 批量删除扩展信息配置
     * 
     * @param globalsIds
     */
    void batchDeleteGlobalsByIds(List<Long> globalsIds);

    /**
     * 批量插入字典类型
     * 
     * @param globalsList
     */
    void batchInsertGlobals(List<Globals> globalsList);

    public String getValueByName(String name);

    AiDeployConfigVO getAiDeployConfig();

    void updateAiDeployConfig(AiDeployConfigVO aiDeployConfigVO);
}
