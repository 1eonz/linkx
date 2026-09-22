package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.im.jingxin.server.entity.LabelBindingUser;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface LabelBindingUserMapper extends BaseMapper<LabelBindingUser> {

    @Insert("<script>" +//
        "INSERT INTO tb_label_binding_user (" +//
        "id, " +//
        "label_id, " +//
        "department_id, " +//
        "department_name, " +//
        "department_path, " +//
        "user_ids) VALUES " +//
        "<foreach collection='list' item='item' separator=','>" +//
        "(" +//
        "#{item.id}, " +//
        "#{item.labelId}, " +//
        "#{item.departmentId}, " +//
        "#{item.departmentName}, " +//
        "#{item.departmentPath}, " +//
        "#{item.userIds, typeHandler=com.tdtech.cloudcmd.im.jingxin.server.entity.LabelBindingUser$UserIdsTypeHandler}" +//
        ")" +//
        "</foreach>" +//
        "</script>")
    int insertBatch(@Param("list") List<LabelBindingUser> labelBindingUsers);

    @Update("<script>" +//
        "UPDATE tb_label_binding_user " +//
        "SET user_ids = JSON_REMOVE(user_ids, JSON_UNQUOTE(JSON_SEARCH(user_ids, 'one', #{userId}))) " +//
        "WHERE JSON_CONTAINS(user_ids, JSON_ARRAY(#{userId})) " +//
        "</script>")
    void removeUserIdFromBindings(@Param("userId") Long userId);
    
    @Select("<script>" +//
        "SELECT * FROM tb_label_binding_user b " +//
        "WHERE JSON_CONTAINS(b.user_ids, JSON_ARRAY(#{userId})) " +//
        "<if test='type != null'> " +//
            "   AND EXISTS (SELECT 1 FROM tb_label l WHERE l.id = b.label_id AND l.is_deleted = 0 AND l.type = #{type}) " +//
        "</if> " +//
        "<if test='departmentId != null'> " +//
        "   AND b.department_id = #{departmentId} " +//
        "</if> " +//
        "</script>")
    List<LabelBindingUser> findByUserId(@Param("userId") Long userId, @Param("type") Integer type, @Param("departmentId") Long departmentId);
}
