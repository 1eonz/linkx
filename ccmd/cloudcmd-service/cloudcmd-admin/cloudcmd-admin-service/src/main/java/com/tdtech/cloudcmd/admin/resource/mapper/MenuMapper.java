package com.tdtech.cloudcmd.admin.resource.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.admin.resource.entity.Menu;

/**
 * <p>
 * 权限菜单信息表 Mapper 接口
 * </p>
 *
 * @author mWX556161
 * @since 2020-07-27
 */
@Mapper
public interface MenuMapper extends BaseMapper<Menu> {

    /**
     * 批量插入菜单
     * 
     * @param menuList
     */
    void batchInsertMenu(@Param("menuList") List<Menu> menuList);

    /**
     * 批量逻辑删除
     * 
     * @param idList
     */
    Integer batchLogicDeleteMenu(@Param("idList") List<Long> idList);

    /**
     * 批量删除
     * 
     * @param idList
     */
    Integer batchDeleteMenu(@Param("idList") List<Long> idList);

    /**
     * 批量将排序加一
     * 
     * @param menuIdList
     */
    void batchUpdateMenuSortIncrement(@Param("idList") List<Long> menuIdList);

    /**
     * 批量将排序减一
     * 
     * @param menuIdList
     */
    void batchUpdateMenuSortDecrement(@Param("idList") List<Long> menuIdList);
}
