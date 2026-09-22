package com.tdtech.cloudcmd.sqlrunner.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RunnerMapper {

    @Insert("drop index ${idx} on ${table}")
    void dropIndex(@Param("idx") String idx, @Param("table") String table);

    @Insert("create index ${idx} on ${table}  (${columns})")
    void createIndex(@Param("idx") String idx, @Param("table") String table ,@Param("columns") String columns);
}
