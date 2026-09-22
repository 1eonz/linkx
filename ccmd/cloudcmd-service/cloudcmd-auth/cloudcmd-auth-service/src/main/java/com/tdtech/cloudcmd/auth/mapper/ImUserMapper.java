package com.tdtech.cloudcmd.auth.mapper;

import com.tdtech.cloudcmd.auth.entity.ImUserDto;
import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.auth.entity.ImUserDO;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface ImUserMapper extends BaseMapper<ImUserDO> {

    /**
     * 查询用户数据-不会被拦截器自动解密
     *
     * @param minId 最小id,不包含
     * @param maxTime 最大时间
     * @param limit 多少条数据
     * @return 用户数据
     */
    List<ImUserDto> getList(@Param("minId") Long minId, @Param("maxTime")Date maxTime, @Param("limit") Integer limit);

    /**
     * 根据ID列表查询用户数据（不会被拦截器自动解密）
     * 返回DTO，避免触发解密拦截器
     *
     * @param ids 用户ID列表
     * @return 用户DTO列表
     */
    List<ImUserDto> listByIds(@Param("ids") List<Long> ids);

    /**
     * 批量更新用户敏感字段（不会被拦截器自动加密）
     * 用于解密操作，绕过加密拦截器
     *
     * @param users 用户DTO列表
     * @return 更新数量
     */
    int batchUpdateDecryptedData(@Param("users") List<ImUserDO> users);

    /**
     * 批量 upsert（仅更新 IM 属性字段，敏感字段需调用方手动加密）
     * 注意：自定义 XML 方法不触发 EncryptionInnerInterceptor；
     *       ON DUPLICATE KEY UPDATE 不覆盖 password/type/status/pwd_time/gmt_created。
     *
     * @param list 用户列表（name/mobile/idCard 已加密）
     * @return 受影响行数
     */
    int upsertBatch(@Param("list") List<ImUserDO> list);
}