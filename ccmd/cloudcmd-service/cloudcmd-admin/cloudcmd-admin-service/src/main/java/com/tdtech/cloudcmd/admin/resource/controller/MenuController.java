package com.tdtech.cloudcmd.admin.resource.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tdtech.cloudcmd.admin.exception.AdminException;
import com.tdtech.cloudcmd.admin.resource.entity.Application;
import com.tdtech.cloudcmd.admin.resource.entity.Menu;
import com.tdtech.cloudcmd.admin.resource.entity.dto.MenuDto;
import com.tdtech.cloudcmd.admin.resource.entity.dto.MenuMoveDto;
import com.tdtech.cloudcmd.admin.resource.entity.vo.MenuChildrenVo;
import com.tdtech.cloudcmd.admin.resource.entity.vo.MenuVo;
import com.tdtech.cloudcmd.admin.resource.service.IApplicationService;
import com.tdtech.cloudcmd.admin.resource.service.IMenuService;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.enums.ResponseCodeEnum;
import com.tdtech.cloudcmd.i18n.I18nUtil;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 权限菜单信息表 前端控制器
 * </p>
 *
 * @author mWX556161
 * @since 2020-07-27
 */
@Tag(name = "菜单")
@Slf4j
@RestController
@RequestMapping("/admin/v1/menu")
public class MenuController {
    @Autowired
    private IMenuService menuService;
    @Autowired
    private IApplicationService applicationService;

    @PostMapping("/list")
    public R getMenuList(@Validated @RequestBody MenuVo menuVo, @RequestHeader("X-CloudCmd-AppKey") String appKey) {
        Application application = applicationService.getApplicationByAppKey(appKey);
        if (menuVo != null && menuVo.getApplicationId() == null) {
            if (application != null) {
                menuVo.setApplicationId(application.getId());
            }
        }
        List<Menu> data = menuService.getMenuTree(menuVo);
        return R.success(ResponseCodeEnum.SUCCESS.getCode(), I18nUtil.get(ResponseCodeEnum.SUCCESS.getMsg()), data);
    }

    @PostMapping("/id")
    public R getMenuById(@RequestParam(name = "id") Long id) {
        MenuDto data = menuService.getMenuById(id);
        return R.success(ResponseCodeEnum.SUCCESS.getCode(), I18nUtil.get(ResponseCodeEnum.SUCCESS.getMsg()), data);
    }

    @PostMapping("/update")
    public R updateMenu(@Validated @RequestBody MenuDto menuDto) {
        try {
            menuService.updateMenu(menuDto);
        } catch (AdminException exception) {
            return R.failure(exception.getCode(), exception.getMessage());
        } catch (Exception e) {
            log.error("updateMenu error", e);
            return R.failure();
        }
        return R.success();
    }

    @PostMapping("/move")
    public R moveMenu(@Validated @RequestBody MenuMoveDto menuMoveDto) {
        menuService.moveMenuNode(menuMoveDto);
        return R.success();
    }

    @PostMapping("/getChildren")
    public R getChildrenById(@Validated @RequestBody MenuChildrenVo menuChildrenVo) {
        List<Menu> data = menuService.getMenuChildren(menuChildrenVo.getId(), menuChildrenVo.getApplicationId());
        return R.success(data);
    }

    @PostMapping("/delete")
    public R deleteMenu(@RequestParam(name = "id") Long id) {
        menuService.deleteMenu(id);
        return R.success();
    }

    @PostMapping("/create")
    public R createMenu(@Validated @RequestBody Menu menu) {
        try {
            menuService.createMenu(menu);
        } catch (AdminException exception) {
            return R.failure(exception.getCode(), exception.getMessage());
        } catch (Exception e) {
            log.error("createMenu error", e);
            return R.failure();
        }
        return R.success();
    }
}
