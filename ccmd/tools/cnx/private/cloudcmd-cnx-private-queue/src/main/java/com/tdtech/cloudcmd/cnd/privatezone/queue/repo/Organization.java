package com.tdtech.cloudcmd.cnd.privatezone.queue.repo;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nullable;

@Serdeable
@MappedEntity("tb_organization")
public record Organization(
    /*      id唯一不变,传给前台id的精度会丢失，使用字符串进行序列化。     */
    @Id Long id,
    /*      组织code     */
    @Nullable String code,
    /*      名称     */
    @Nullable String name,
    /*      分类ID     */
    @Nullable Long typeId,
    /*      上级组织机构ID     */
    @Nullable Long parentId,
    /*      组织机构简称     */
    @Nullable String shortName,
    /*      所属部门全路径(组织可以重名)。上级部门全路径—+本级name。方便查询，不显示。     */
    @Nullable String fullPathName,
    /*      行政区划。     */
    @Nullable String administrativeArea,
    /*      组织所在层级    */
    @Nullable Integer level,
    /*      排序     */
    @Nullable Integer sort,
    /*      备注     */
    @Nullable String remark,
    /*      状态：0-正常,1-禁用     */
    @Nullable Integer status,
    /*      创建时间     */
    @Nullable @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date gmtCreated,
    /*      绝对组织路径，使用逗号隔开     */
    @Nullable String fullPath,
    /*      修改时间     */
    @Nullable @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date gmtModified) {
}