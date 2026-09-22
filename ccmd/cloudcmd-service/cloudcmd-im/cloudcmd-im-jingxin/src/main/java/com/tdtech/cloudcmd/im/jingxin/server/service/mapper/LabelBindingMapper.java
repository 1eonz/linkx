package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.im.jingxin.server.entity.LabelBinding;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface LabelBindingMapper extends BaseMapper<LabelBinding> {

    @Insert("<script>" +//
        "INSERT INTO tb_label_binding (" +//
        "id, " +//
        "label_id, " +//
        "department_id, " +//
        "department_name, " +//
        "department_path, " +//
        "post_ids) VALUES " +//
        "<foreach collection='list' item='item' separator=','>" +//
        "(" +//
        "#{item.id}, " +//
        "#{item.labelId}, " +//
        "#{item.departmentId}, " +//
        "#{item.departmentName}, " +//
        "#{item.departmentPath}, " +//
        "#{item.postIds, typeHandler=com.tdtech.cloudcmd.im.jingxin.server.entity.LabelBinding$PostIdsTypeHandler}" +//
        ")" +//
        "</foreach>" +//
        "</script>")
    int insertBatch(@Param("list") List<LabelBinding> labelBindings);

    @Update("<script>" +//
        "UPDATE tb_label_binding " +//
        "SET post_ids = JSON_REMOVE(post_ids, JSON_UNQUOTE(JSON_SEARCH(post_ids, 'one', #{postId}))) " +//
        "WHERE JSON_CONTAINS(post_ids, JSON_ARRAY(#{postId})) " +//
        "</script>")
    void removePostIdFromBindings(@Param("postId") Long postId);
    
    @Select("<script>" +//
        "SELECT * FROM tb_label_binding b " +//
        "WHERE JSON_CONTAINS(b.post_ids, JSON_ARRAY(#{postId})) " +//
        "<if test='type != null'> " +//
            "   AND EXISTS (SELECT 1 FROM tb_label l WHERE l.id = b.label_id AND l.is_deleted = 0 AND l.type = #{type}) " +//
        "</if> " +//
        "<if test='departmentId != null'> " +//
        "   AND b.department_id = #{departmentId} " +//
        "</if> " +//
        "</script>")
    List<LabelBinding> findByPostId(@Param("postId") Long postId, @Param("type") Integer type, @Param("departmentId") Long departmentId);
}
