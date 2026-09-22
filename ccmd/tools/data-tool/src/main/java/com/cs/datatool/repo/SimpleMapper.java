package com.cs.datatool.repo;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SimpleMapper {

    @Select("select * from ${tableName}")
    List<Map<String, Object>> selectAll(@Param("tableName") String tableName);

    @Insert("""
        <script>
        INSERT INTO ${tableName}
        (
            <foreach collection='data[0].keySet()' item='column' separator=','>
            ${column}
            </foreach>
        )
        VALUES
        <foreach collection='data' item='record' separator=','>
        (
            <foreach collection='record.keySet()' item='column' separator=','>
             #{record.${column}}
            </foreach>
        )
        </foreach>
        </script>
        """)
    void insertBatch(@Param("tableName") String tableName, @Param("data") List<Map<String, Object>> data);

    @Select("""
        select te.id_card_num
        from tb_user tu,
             tr_executor_user teu,
             tb_executor te
        where te.id = teu.executor_id
          and teu.user_id = tu.id
          and tu.collaboration = 1
          and te.id_card_num is not null
        """)
    List<Map<String, Object>> selectAllOnlineUser();
}
