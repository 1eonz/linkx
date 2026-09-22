package com.tdtech.cloudcmd.icp.proxy.repo;

import com.tdtech.cloudcmd.icp.proxy.entity.IsdnType;
import com.tdtech.cloudcmd.icp.proxy.entity.User;
import com.tdtech.cloudcmd.mysql.enhance.ExMPJBaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Mapper
public interface UserMapper extends ExMPJBaseMapper<User> {

    Logger log = LoggerFactory.getLogger(UserMapper.class);

    /**
     * 批量插入用户信息
     * @param userList 用户列表
     * @return 插入记录数
     */
//    @Insert("<script>" +
//            "INSERT INTO tb_user (id, alias, apptype, category, departmentid, isdn, name, priority, subusercategory, uetype, vpnid, vpnin, vpnout, create_time) VALUES " +
//            "<foreach collection='list' item='item' separator=','>" +
//            "(#{item.id}, #{item.alias}, #{item.apptype}, #{item.category}, #{item.departmentid}, #{item.isdn}, #{item.name}, #{item.priority}, #{item.subusercategory}, #{item.uetype}, #{item.vpnid}, #{item.vpnin}, #{item.vpnout}, #{item.createTime})" +
//            "</foreach>" +
//            "</script>")

    @Insert("<script>" +
            "INSERT INTO tb_isdn (id, alias, apptype, category, departmentid, isdn, name, priority, subusercategory, uetype, remark, status) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.id}, #{item.alias}, #{item.apptype}, #{item.category}, #{item.departmentid}, #{item.isdn}, #{item.name}, #{item.priority}, #{item.subusercategory}, #{item.uetype}, #{item.remark}, #{item.status})" +
            "</foreach>" +
            "</script>")

    int insertBatch(@Param("list") List<User> userList);

    /**
     * 批量新增或更新用户信息（根据isdn判断，存在则更新，不存在则新增）
     * @param userList 用户列表
     * @return 影响记录数
     */
//    @Insert("<script>" +
//            "INSERT INTO tb_user (id, alias, apptype, category, departmentid, isdn, name, priority, subusercategory, uetype, vpnid, vpnin, vpnout, create_time) VALUES " +
//            "<foreach collection='list' item='item' separator=','>" +
//            "(#{item.id}, #{item.alias}, #{item.apptype}, #{item.category}, #{item.departmentid}, #{item.isdn}, #{item.name}, #{item.priority}, #{item.subusercategory}, #{item.uetype}, #{item.vpnid}, #{item.vpnin}, #{item.vpnout}, #{item.createTime})" +
//            "</foreach>" +
//            " ON DUPLICATE KEY UPDATE " +
//            "alias = VALUES(alias), " +
//            "apptype = VALUES(apptype), " +
//            "category = VALUES(category), " +
//            "departmentid = VALUES(departmentid), " +
//            "name = VALUES(name), " +
//            "priority = VALUES(priority), " +
//            "subusercategory = VALUES(subusercategory), " +
//            "uetype = VALUES(uetype), " +
//            "vpnid = VALUES(vpnid), " +
//            "vpnin = VALUES(vpnin), " +
//            "vpnout = VALUES(vpnout), " +
//            "create_time = VALUES(create_time)" +
//            "</script>")
    @Insert("<script>" +
            "INSERT INTO tb_isdn (id, alias, apptype, category, departmentid, isdn, name, priority, subusercategory, uetype, remark, status) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.id}, #{item.alias}, #{item.apptype}, #{item.category}, #{item.departmentid}, #{item.isdn}, #{item.name}, #{item.priority}, #{item.subusercategory}, #{item.uetype}, #{item.remark}, #{item.status})" +
            "</foreach>" +
            " ON DUPLICATE KEY UPDATE " +
            "alias = VALUES(alias), " +
            "apptype = VALUES(apptype), " +
            "category = VALUES(category), " +
            "departmentid = VALUES(departmentid), " +
            "name = VALUES(name), " +
            "priority = VALUES(priority), " +
            "subusercategory = VALUES(subusercategory), " +
            "uetype = VALUES(uetype), " +
            "remark = VALUES(remark), " +
            "status = VALUES(status)" +
            "</script>")
    int insertOrUpdateBatch(@Param("list") List<User> userList);

    /**
     * 根据ISDN查询用户信息（包含逻辑删除的记录）
     * 用于处理逻辑删除后重新插入的场景
     * @param isdn ISDN号码
     * @return 用户信息（包含status=1的记录）
     */
    @Select("SELECT id, isdn, status FROM tb_isdn WHERE isdn = #{isdn}")
    User selectByIsdnIncludeDeleted(@Param("isdn") String isdn);

    /**
     * 批量根据ISDN查询用户信息（包含逻辑删除的记录）
     * 用于处理逻辑删除后重新插入的场景
     * @param isdnList ISDN列表
     * @return 用户信息列表（包含status=1的记录）
     */
    @Select("<script>" +
            "SELECT id, isdn, status FROM tb_isdn WHERE isdn IN " +
            "<foreach collection='list' item='item' open='(' separator=',' close=')'>" +
            "#{item}" +
            "</foreach>" +
            "</script>")
    List<User> selectByIsdnListIncludeDeleted(@Param("list") List<String> isdnList);

    /**
     * 查询tb_isdn表中category, subusercategory, apptype去重后的结果
     * @return 去重后的ISDN类型列表（仅包含category, subusercategory, apptype字段）
     */
    @Select("SELECT DISTINCT category, subusercategory, apptype FROM tb_isdn WHERE status = 0")
    List<IsdnType> selectDistinctKey();

}