package com.tdtech.cloudcmd.admin.resource.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.tdtech.cloudcmd.msip.enums.LicenseEnum;
import com.tdtech.cloudcmd.msip.util.LicenseUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.admin.cache.NodeConstant;
import com.tdtech.cloudcmd.admin.exception.AdminErrorEnum;
import com.tdtech.cloudcmd.admin.exception.AdminException;
import com.tdtech.cloudcmd.admin.resource.entity.Application;
import com.tdtech.cloudcmd.admin.resource.entity.Menu;
import com.tdtech.cloudcmd.admin.resource.entity.dto.MenuDto;
import com.tdtech.cloudcmd.admin.resource.entity.dto.MenuMoveDto;
import com.tdtech.cloudcmd.admin.resource.entity.vo.MenuVo;
import com.tdtech.cloudcmd.admin.resource.mapper.MenuMapper;
import com.tdtech.cloudcmd.admin.resource.service.IApplicationService;
import com.tdtech.cloudcmd.admin.resource.service.IMenuService;
import com.tdtech.cloudcmd.base.api.service.GlobalsRpcService;
import com.tdtech.cloudcmd.i18n.I18nUtil;
import com.tdtech.cloudcmd.license.LicensePredicate;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.ListUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 权限菜单信息表 服务实现类
 * </p>
 *
 * @author mWX556161
 * @since 2020-07-27
 */
@Service
@Slf4j
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements IMenuService {

    private final List<Long> EXCEPT_LIST = Arrays.asList(1522392406668870008L);

    @Autowired
    private MenuMapper menuMapper;
    @Autowired
    private IdWorker idWorker;
    @Autowired
    private IApplicationService applicationService;
    @Autowired
    private RedisUtil redisUtil;
    @DubboReference
    private GlobalsRpcService globalsService;
    @Resource
    private LicensePredicate licensePredicate;

    @Resource
    private LicenseUtil licenseUtil;

    @Override
    public List<Menu> getMenuTree(MenuVo menuVo) {
        String carBreaker = globalsService.getGlobalsValueByName("CAR_BREAKER");
        String edgegatewayBreaker = globalsService.getGlobalsValueByName("EDGEGATEWAY_BREAKER");
        String customizedLayer = globalsService.getGlobalsValueByName("CUSTOMIZED_LAYER");
        String applicationBreaker = globalsService.getGlobalsValueByName("APPLICATION_BREAKER");
        QueryWrapper<Menu> wrapper = new QueryWrapper<>();
        wrapper.eq(Menu.APPLICATION_ID, menuVo.getApplicationId());
        List<Menu> menus = menuMapper.selectList(wrapper);

        // 判断license AI协同功能是否可用
        boolean aiIsAvailable = licenseUtil.availableByCode(LicenseEnum.AI_COLLABORATION.getCode());
        if (!aiIsAvailable) {
            menus = menus.stream().filter(obj -> (!EXCEPT_LIST.contains(obj.getId()))).collect(Collectors.toList());
        }

        log.debug("menus:{}", menus);
        var menuStream = Optional.ofNullable(menus).stream().flatMap(Collection::stream);
        // 初始化数据 去除视频会议 和 辖区管理
        menuStream = menuStream.filter(
            a -> !Objects.equals(1522392406668869632L, a.getId()) && !Objects.equals(1394907389584474115L, a.getId()));

        List<Menu> roots = new ArrayList<>();
        menus = menuStream.collect(Collectors.toList());
        for (Menu menu : menus) {
            // 找到根
            if (menu.getParentId() == -1) {
                roots.add(menu);
            }
            // 找到子
            for (Menu menuNode : menus) {
                if (menuNode.getParentId().equals(menu.getId())) {
                    menuNode.setName(I18nUtil.get(menuNode.getName()));
                    if (menu.getChildren() == null) {
                        menu.setChildren(new ArrayList<>());
                    }
                    if ("0".equals(carBreaker) && menuNode.getId().equals(1394907819479662592L)) {
                        continue;
                    }
                    if ("0".equals(edgegatewayBreaker) && menuNode.getId().equals(1421391566475362304L)) {
                        continue;
                    }
                    if ("0".equals(customizedLayer) && menuNode.getId().equals(1421390646945513472L)) {
                        continue;
                    }
                    if ("0".equals(applicationBreaker) && (menuNode.getId().equals(1394908432246505472L)
                        || menuNode.getId().equals(1394908523799773184L)
                        || menuNode.getId().equals(1394908629525594112L))) {
                        continue;
                    }
                    menu.getChildren().add(menuNode);
                }
            }
            if (ListUtils.isNotBlankList(menu.getChildren())) {
                List<Menu> children = menu.getChildren();
                children = children.stream().sorted(Comparator.comparing(Menu::getSort)).collect(Collectors.toList());
                menu.setChildren(children);
            }
        }
        if (ListUtils.isNotBlankList(roots)) {
            roots = roots.stream().sorted(Comparator.comparing(Menu::getSort)).collect(Collectors.toList());
        }
        return roots;
    }

    @Override
    public void updateMenu(MenuDto menuDto) {
        Menu existMenu = menuMapper.selectById(menuDto.getId());
        if (!existMenu.getName().equals(menuDto.getName())) {
            var count = menuMapper.selectCount(new QueryWrapper<Menu>().eq(Menu.NAME, menuDto.getName())
                .eq(Menu.APPLICATION_ID, menuDto.getApplicationId()).eq(Menu.PARENT_ID, menuDto.getParentId()));
            if (count > 0) {
                throw new AdminException(AdminErrorEnum.COMMON_ERROR_514.getCode(),
                    I18nUtil.get(AdminErrorEnum.COMMON_ERROR_514.getMsg()));
            }
        }
        Menu menuRes = new Menu();
        menuDto.setGmtModified(new Date());
        BeanUtils.copyProperties(menuDto, menuRes);
        // 判断是否改变上级菜单层级
        Menu menuTmp = menuMapper.selectById(menuDto.getId());
        if (menuTmp != null && !menuTmp.getParentId().equals(menuDto.getParentId())) {
            // 如果上级层级发生了变化
            menuRes.setParentId(menuDto.getParentId());
            Menu parMenu = menuMapper.selectById(menuDto.getParentId());
            if (parMenu != null) {
                menuRes.setLevel(parMenu.getLevel() + 1);
            } else {
                menuRes.setLevel(1);
            }
            List<Menu> all = menuMapper.selectList(null);
            List<Long> menuIds = all.stream().map(Menu::getId).collect(Collectors.toList());
            this.updateDeptMenuList(menuRes, all, menuIds);
        }
        UpdateWrapper<Menu> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq(Menu.ID, menuRes.getId());
        menuMapper.update(menuRes, updateWrapper);
        if (existMenu.getStatus() == 1 && menuDto.getStatus() == 0) {
            List<Menu> menus = menuMapper.selectList(null);
            List<Menu> deptMenuList = this.getDeptMenuList(menuDto.getId(), menus);
            deptMenuList.forEach(menu -> menu.setStatus(0));
            this.saveOrUpdateBatch(deptMenuList);
        }
    }

    /**
     * 更新子层级
     *
     * @param root
     * @param all
     */
    private void updateDeptMenuList(Menu root, List<Menu> all, List<Long> menuIds) {
        all.stream().filter(menu -> menu.getParentId().equals(root.getId())).forEach(menu -> {
            if (!menuIds.contains(menu.getParentId())) {
                menu.setIsleaf(1);
            }
            UpdateWrapper<Menu> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq(Menu.ID, menu.getId());
            menuMapper.update(menu, updateWrapper);
            this.updateDeptMenuList(menu, all, menuIds);
        });
    }

    @Override
    public void deleteMenu(Long id) {
        List<Menu> menus = menuMapper.selectList(null);
        List<Menu> deptMenuList = this.getDeptMenuList(id, menus);
        List<Long> idList = deptMenuList.stream().map(Menu::getId).collect(Collectors.toList());
        idList.add(id);
        menuMapper.batchDeleteMenu(idList);
    }

    /**
     * 递归查找子菜单层级
     *
     * @param id
     * @param list
     * @return
     */
    private List<Menu> getDeptMenuList(long id, List<Menu> list) {
        List<Menu> menus = new ArrayList<>();
        for (Menu menu : list) {
            if (menu.getParentId().equals(id)) {
                menus.add(menu);
                getDeptMenuList(menu.getId(), list);
            }
        }
        return menus;
    }

    /**
     * 创建菜单
     *
     * @param menu
     * @return
     */
    @Override
    public void createMenu(Menu menu) {
        // 同一运用下，同一菜单下级不能包含相同菜单名称
        var count = menuMapper.selectCount(new QueryWrapper<Menu>().eq(Menu.NAME, menu.getName())
            .eq(Menu.APPLICATION_ID, menu.getApplicationId()).eq(Menu.PARENT_ID, menu.getParentId()));
        if (count > 0) {
            throw new AdminException(AdminErrorEnum.COMMON_ERROR_514.getCode(),
                I18nUtil.get(AdminErrorEnum.COMMON_ERROR_514.getMsg()));
        }
        Long menuId = idWorker.nextId();
        menu.setId(menuId);
        Menu parMenu = menuMapper.selectById(menu.getParentId());
        if (parMenu != null) {
            menu.setLevel(parMenu.getLevel() + 1);
        } else {
            menu.setLevel(1);
        }
        if (menu.getParentId() <= 0L) {
            menu.setParentId(-1L);
        }
        menuMapper.insert(menu);
    }

    @Override
    public MenuDto getMenuById(Long id) {
        Menu menu = menuMapper.selectById(id);
        MenuDto menuDto = new MenuDto();
        menuDto.buildMenuDto(menu);
        Menu parMenu = menuMapper.selectById(menu.getParentId());
        Application application = applicationService.getApplicationById(menu.getApplicationId());
        if (parMenu != null) {
            menuDto.setParentName(parMenu.getName());
        }
        if (application != null) {
            menuDto.setApplicationName(application.getName());
        }
        menuDto.setApplicationName(I18nUtil.get(menuDto.getApplicationName()));
        menuDto.setName(I18nUtil.get(menuDto.getName()));
        return menuDto;
    }

    @Override
    public String moveMenuNode(MenuMoveDto menuMoveDto) {
        // 获取当前节点的兄弟节点
        List<Menu> currentBrotherMenuList = this.getSonMenuList(menuMoveDto.getCurrentParentId());
        if (currentBrotherMenuList.size() == 1) {
            Menu curParMenu = new Menu();
            curParMenu.setIsleaf(1);
            curParMenu.setId(menuMoveDto.getCurrentParentId());
            menuMapper.updateById(curParMenu);
        } else if (currentBrotherMenuList.size() > 1) {
            // 获取到比当前节点排序大的
            List<Menu> currentList = currentBrotherMenuList.stream()
                .filter(menu -> menu.getSort() > menuMoveDto.getCurrentSort()).collect(Collectors.toList());
            if (ListUtils.isNotBlankList(currentList)) {
                List<Long> idList = currentList.stream().map(Menu::getId).collect(Collectors.toList());
                menuMapper.batchUpdateMenuSortDecrement(idList);
            }
        }
        if (NodeConstant.NODE_MOVE_INNER.equals(menuMoveDto.getLocation())) {
            // 获取目标节点的所有子节点，子节点的排序不需要变化
            List<Menu> currentDeptMenuList = this.getSonMenuList(menuMoveDto.getTargetId());
            Menu menu = new Menu();
            menu.setId(menuMoveDto.getCurrentId());
            menu.setParentId(menuMoveDto.getTargetId());
            int level = menuMoveDto.getTargetLevel() == null ? 1 : menuMoveDto.getTargetLevel();
            menu.setLevel(level + 1);
            if (ListUtils.isNotBlankList(currentDeptMenuList)) {
                int maxSort = currentDeptMenuList.stream().mapToInt(Menu::getSort).max().getAsInt();
                menu.setSort(maxSort + 1);
            } else {
                menu.setSort(1);
                Menu parMenu = new Menu();
                parMenu.setId(menuMoveDto.getTargetId());
                parMenu.setIsleaf(0);
                menuMapper.updateById(parMenu);
            }
            menuMapper.updateById(menu);
        } else {
            // 获取目标节点的兄弟节点
            List<Menu> targetBrotherMenuList = this.getSonMenuList(menuMoveDto.getTargetParentId());
            List<Menu> targetList = new ArrayList<>();
            List<Long> idList = new ArrayList<>();
            Menu curMenu = new Menu();
            curMenu.setId(menuMoveDto.getCurrentId());
            curMenu.setParentId(menuMoveDto.getTargetParentId());
            int level = menuMoveDto.getTargetLevel() == null ? 1 : menuMoveDto.getTargetLevel();
            curMenu.setLevel(level);
            int curSort = 0;
            if (NodeConstant.NODE_MOVE_AFTER.equals(menuMoveDto.getLocation())) {
                if (menuMoveDto.getTargetParentId().equals(menuMoveDto.getCurrentParentId())
                    && menuMoveDto.getTargetSort() > menuMoveDto.getCurrentSort()) {
                    targetList = targetBrotherMenuList.stream()
                        .filter(menu -> menu.getSort() >= menuMoveDto.getTargetSort()).collect(Collectors.toList());
                    curSort = menuMoveDto.getTargetSort();
                } else {
                    targetList = targetBrotherMenuList.stream()
                        .filter(menu -> menu.getSort() > menuMoveDto.getTargetSort()).collect(Collectors.toList());
                    curSort = menuMoveDto.getTargetSort() + 1;
                }
            } else if (NodeConstant.NODE_MOVE_BEFORE.equals(menuMoveDto.getLocation())) {
                if (menuMoveDto.getTargetParentId().equals(menuMoveDto.getCurrentParentId())
                    && menuMoveDto.getTargetSort() > menuMoveDto.getCurrentSort()) {
                    targetList = targetBrotherMenuList.stream()
                        .filter(menu -> menu.getSort() >= menuMoveDto.getTargetSort() - 1).collect(Collectors.toList());
                    curSort = menuMoveDto.getTargetSort() - 1;
                } else {
                    targetList = targetBrotherMenuList.stream()
                        .filter(menu -> menu.getSort() >= menuMoveDto.getTargetSort()).collect(Collectors.toList());
                    curSort = menuMoveDto.getTargetSort();
                }
            }
            curMenu.setSort(curSort);
            idList = targetList.stream().map(Menu::getId).collect(Collectors.toList());
            log.info("-------------:{}", idList);
            if (ListUtils.isNotBlankList(idList)) {
                idList.remove(menuMoveDto.getCurrentId());
                menuMapper.batchUpdateMenuSortIncrement(idList);
            }
            menuMapper.updateById(curMenu);
        }
        // 更新子层级的level
        return "success";
    }

    @Override
    public List<Menu> getMenuChildren(Long menuId, Long applicationId) {
        return this.getSonMenu(menuId, applicationId);
    }

    /**
     * 获取所有的子节点(带应用ID)
     *
     * @param menuId
     * @return
     */
    private List<Menu> getSonMenu(Long menuId, Long applicationId) {
        QueryWrapper<Menu> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(Menu.PARENT_ID, menuId);
        queryWrapper.eq(Menu.APPLICATION_ID, applicationId);
        return menuMapper.selectList(queryWrapper);
    }

    /**
     * 获取所有的子节点
     *
     * @param menuId
     * @return
     */
    private List<Menu> getSonMenuList(Long menuId) {
        QueryWrapper<Menu> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(Menu.PARENT_ID, menuId);
        return menuMapper.selectList(queryWrapper);
    }

    @Override
    public List<Menu> getMenuList() {
        return menuMapper.selectList(null);
    }

    @Override
    public void batchDeleteMenuByIds(List<Long> menuIdList) {
        menuMapper.deleteBatchIds(menuIdList);
    }

    @Override
    public void batchInsertMenu(List<Menu> menuList) {
        menuMapper.batchInsertMenu(menuList);
    }
}
