package com.tdtech.cloudcmd.icp.proxy.repo;

import com.tdtech.cloudcmd.icp.proxy.entity.IsdnType;
import com.tdtech.cloudcmd.mysql.enhance.ExMPJBaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * ISDN账号类型信息Mapper
 */
@Mapper
public interface IsdnTypeMapper extends ExMPJBaseMapper<IsdnType> {

    /**
     * 批量插入ISDN账号类型信息
     *
     * @param isdnTypeList ISDN类型列表
     * @return 插入记录数
     */
    @Insert("<script>" +
        "INSERT INTO tb_isdn_type (id, name, category, subusercategory, apptype, priority, icon, icon_uri, is_show) VALUES " +
        "<foreach collection='list' item='item' separator=','>" +
        "(#{item.id}, #{item.name}, #{item.category}, #{item.subusercategory}, #{item.apptype}, #{item.priority}, #{item.icon}, #{item.iconUri}, #{item.isShow})" +
        "</foreach>" +
        "</script>")
    int insertBatch(@Param("list") List<IsdnType> isdnTypeList);

    /**
     * 批量新增或更新ISDN账号类型信息（根据id判断，存在则更新，不存在则新增）
     *
     * @param isdnTypeList ISDN类型列表
     * @return 影响记录数
     */
    @Insert("<script>" +
        "INSERT INTO tb_isdn_type (id, name, category, subusercategory, apptype, icon, icon_uri, is_show) VALUES " +
        "<foreach collection='list' item='item' separator=','>" +
        "(#{item.id}, #{item.name}, #{item.category}, #{item.subusercategory}, #{item.apptype}, #{item.icon}, #{item.iconUri}, #{item.isShow})" +
        "</foreach>" +
        " ON DUPLICATE KEY UPDATE " +
        "name = VALUES(name), " +
        "category = VALUES(category), " +
        "subusercategory = VALUES(subusercategory), " +
        "apptype = VALUES(apptype), " +
        "icon = VALUES(icon), " +
        "icon_uri = VALUES(icon_uri), " +
        "is_show = VALUES(is_show)" +
        "</script>")
    int insertOrUpdateBatch(@Param("list") List<IsdnType> isdnTypeList);
}