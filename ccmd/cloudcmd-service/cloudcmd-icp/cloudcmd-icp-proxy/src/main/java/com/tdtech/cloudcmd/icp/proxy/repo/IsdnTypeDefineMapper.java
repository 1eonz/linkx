package com.tdtech.cloudcmd.icp.proxy.repo;

import com.tdtech.cloudcmd.icp.proxy.entity.IsdnTypeDefine;
import com.tdtech.cloudcmd.mysql.enhance.ExMPJBaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 应用类型定义Mapper
 */
@Mapper
public interface IsdnTypeDefineMapper extends ExMPJBaseMapper<IsdnTypeDefine> {

    /**
     * 批量插入应用类型定义信息
     *
     * @param isdnTypeDefineList 应用类型定义列表
     * @return 插入记录数
     */
    @Insert("<script>" +
        "INSERT INTO tb_isdn_type_define (id, category, subusercategory, apptype, type_name, sub_type_name, kind, remark, involve, gmt_create, gmt_modified) VALUES " +
        "<foreach collection='list' item='item' separator=','>" +
        "(#{item.id}, #{item.category}, #{item.subusercategory}, #{item.apptype}, #{item.typeName}, #{item.subTypeName}, #{item.kind}, #{item.remark}, #{item.involve}, #{item.gmtCreate}, #{item.gmtModified})" +
        "</foreach>" +
        "</script>")
    int insertBatch(@Param("list") List<IsdnTypeDefine> isdnTypeDefineList);

    /**
     * 批量新增或更新应用类型定义信息（根据id判断，存在则更新，不存在则新增）
     *
     * @param isdnTypeDefineList 应用类型定义列表
     * @return 影响记录数
     */
    @Insert("<script>" +
        "INSERT INTO tb_isdn_type_define (id, category, subusercategory, apptype, type_name, sub_type_name, kind, remark, involve, gmt_create, gmt_modified) VALUES " +
        "<foreach collection='list' item='item' separator=','>" +
        "(#{item.id}, #{item.category}, #{item.subusercategory}, #{item.apptype}, #{item.typeName}, #{item.subTypeName}, #{item.kind}, #{item.remark}, #{item.involve}, #{item.gmtCreate}, #{item.gmtModified})" +
        "</foreach>" +
        " ON DUPLICATE KEY UPDATE " +
        "category = VALUES(category), " +
        "subusercategory = VALUES(subusercategory), " +
        "apptype = VALUES(apptype), " +
        "type_name = VALUES(type_name), " +
        "sub_type_name = VALUES(sub_type_name), " +
        "kind = VALUES(kind), " +
        "remark = VALUES(remark), " +
        "involve = VALUES(involve), " +
        "gmt_modified = VALUES(gmt_modified)" +
        "</script>")
    int insertOrUpdateBatch(@Param("list") List<IsdnTypeDefine> isdnTypeDefineList);
}
