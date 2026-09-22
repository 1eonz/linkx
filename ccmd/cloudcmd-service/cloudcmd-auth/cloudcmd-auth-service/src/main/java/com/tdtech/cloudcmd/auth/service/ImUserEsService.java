package com.tdtech.cloudcmd.auth.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tdtech.cloudcmd.auth.dto.ImUserDto;
import com.tdtech.cloudcmd.auth.dto.ImUserDeptQO;
import com.tdtech.cloudcmd.auth.dto.ImUserQO;
import com.tdtech.cloudcmd.auth.entity.ImUserDO;
import com.tdtech.cloudcmd.bean.PageResult;

import java.util.List;

/**
 * es查询im的user信息
 */
public interface ImUserEsService {

    /**
     * 分页查询用户
     *
     * @param page 分页信息
     * @param qo 过滤条件
     * @return 分页数据
     */
    IPage<ImUserDO> queryPaged(IPage<ImUserDO> page, ImUserQO qo);

    IPage<ImUserDO> queryPagedWithChildren(IPage<ImUserDO> page, ImUserDeptQO qo);


    /**
     * 分页查询管理员用户
     *
     * @param page 分页信息
     * @param qo 过滤条件
     * @return 分页数据
     */
    IPage<ImUserDO> queryAdmin(IPage<ImUserDO> page, ImUserQO qo);

    /**
     * 根据id删除数据
     * @param ids ids
     */
    void deleteByIds(List<Long> ids);

    /**
     * 新增用户
     * @param insertedUsers 用户列表
     */
    void insert(List<ImUserDO> insertedUsers);

    /**
     * 更新用户-通过id
     * @param userDO 用户
     */
    void update(ImUserDO userDO);

    /**
     * 批量更新用户
     * @param userDOS 用户列表
     */
    void updateByIds(List<ImUserDO> userDOS);

    /**
     * 判断哪些id数据已经存在
     *
     * @param idList id集合
     * @return 已存在的id
     */
    List<Long> existIds(List<Long> idList);

    /**
     * 通过身份证查询数据
     *
     * @param idCard 身份证号
     * @return 用户数据
     */
    ImUserDO findByIdCard(String idCard);

    /**
     * 通过身份证查询数据
     *
     * @param idCard 身份证号
     * @return 用户数据
     */
    ImUserDto getByIdCard(String idCard);

    /**
     * 从MySQL分页查询
     */
    PageResult<ImUserDto> pageByDepartmentIds(List<Long> deptIdList, String keywords,
                                              Integer pageNum, Integer pageSize);
}
