package com.tdtech.cloudcmd.admin.resource.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.admin.resource.entity.Menu;
import com.tdtech.cloudcmd.admin.resource.entity.dto.MenuDto;
import com.tdtech.cloudcmd.admin.resource.entity.dto.MenuMoveDto;
import com.tdtech.cloudcmd.admin.resource.entity.vo.MenuVo;

/**
 * <p>
 * 权限菜单信息表 服务类
 * </p>
 *
 * @author mWX556161
 * @since 2020-07-27
 */
public interface IMenuService extends IService<Menu> {

    /**
     * 获取菜单树
     * 
     * @return
     */
    List<Menu> getMenuTree(MenuVo menuVo);

    /**
     * 更新菜单
     * 
     * @param menuDto
     * @return
     */
    void updateMenu(MenuDto menuDto);

    /**
     * 删除菜单
     * 
     * @param id
     * @return
     */
    void deleteMenu(Long id);

    /**
     * 创建菜单
     * 
     * @param menu
     * @return
     */
    void createMenu(Menu menu);

    /**
     * 根据id查询菜单详情
     * 
     * @param id
     * @return
     */
    MenuDto getMenuById(Long id);

    /**
     * 对移动节点进行处理
     * 
     * @param menuMoveDto
     * @return
     */
    String moveMenuNode(MenuMoveDto menuMoveDto);

    /**
     * 根据菜单编号获取子菜单
     * 
     * @param menuId
     * @param applicationId
     * @return
     */
    List<Menu> getMenuChildren(Long menuId, Long applicationId);

    /**
     * 查询所有菜单
     * 
     * @return
     */
    List<Menu> getMenuList();

    /**
     * 批量删除菜单
     * 
     * @param menuIdList
     */
    void batchDeleteMenuByIds(List<Long> menuIdList);

    /**
     * 批量插入菜单
     * 
     * @param menuList
     */
    void batchInsertMenu(List<Menu> menuList);
}
