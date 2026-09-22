package com.tdtech.cloudcmd.auth.service;

import com.tdtech.cloudcmd.auth.dto.ImUserDto;
import com.tdtech.cloudcmd.bean.PageResult;

import java.util.List;
public interface IIMUserRPCService {

    List<ImUserDto> findIdCardList(List<Long> deptIdList);

    List<ImUserDto> listByIdcards(List<String> idcards);

    PageResult<ImUserDto> pageByDepartmentIds(List<Long> deptIdList, String keywords, Integer pageNum,
        Integer pageSize);

    List<Long> findOrgByUserId(Long userId);

    List<ImUserDto> listByDeptCodes(List<String> deptCodes);

    /**
     * 通过idCard获取用户
     *
     * @param idCard idCard
     * @return 用户
     */
    ImUserDto getByIdCard(String idCard);

    /**
     * 通过userId获取用户
     *
     * @param userId userId
     * @return 用户
     */
    ImUserDto getById(Long userId);

    /**
     * 批量通过userId获取用户
     *
     * @param userIds userId列表
     * @return 用户列表
     */
    List<ImUserDto> listByIds(List<Long> userIds);

    /**
     * 批量落库 IM 用户到 tb_im_user（存在则更新 IM 属性字段，不存在则插入）。
     * 由 auth 侧 ImUserMapper 执行，敏感字段 name/mobile/idCard 自动加密。
     * 注意：更新时不会覆盖 password/type/status/pwd_time/gmt_created，
     *       避免冲掉登录密码与锁定状态。
     *
     * @param userList 用户列表（明文，敏感字段由 auth 侧加密）
     * @return 受影响行数（语义同 saveOrUpdate，仅作日志参考）
     */
    int upsertImUserBatch(List<ImUserDto> userList);
}