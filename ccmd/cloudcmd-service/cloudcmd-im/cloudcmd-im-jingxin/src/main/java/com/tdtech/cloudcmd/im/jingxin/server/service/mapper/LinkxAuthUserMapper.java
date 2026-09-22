package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.im.jingxin.server.entity.dto.LinkxAuthUserDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * linkx_auth.tb_user_im 的 Mapper
 * 注意：加密字段由 EncryptionInnerInterceptor 自动处理，
 *      但自定义 XML 方法（upsertBatch）不经过拦截器，需在 Service 层手动加密后再传入。
 */
@Mapper
public interface LinkxAuthUserMapper extends BaseMapper<LinkxAuthUserDO> {

    /**
     * 批量 upsert（不存在则插入，存在则更新）
     * 注意：调用方需在传入前手动加密敏感字段（name/mobile/idCard），
     *      因为自定义 XML 方法不会触发 EncryptionInnerInterceptor。
     *
     * @param list 用户列表（敏感字段已加密）
     * @return 受影响行数
     */
    int upsertBatch(@Param("list") List<LinkxAuthUserDO> list);

    /**
     * 根据部门ID集合查询用户（走 MyBatis-Plus selectList，自动解密）
     *
     * @param deptIds 部门ID集合
     * @return 用户列表（敏感字段已解密）
     */
    List<LinkxAuthUserDO> selectByDeptIds(@Param("deptIds") List<String> deptIds);
}