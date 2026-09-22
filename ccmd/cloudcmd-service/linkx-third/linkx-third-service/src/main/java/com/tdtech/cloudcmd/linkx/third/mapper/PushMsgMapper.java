package com.tdtech.cloudcmd.linkx.third.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.linkx.third.entity.PushMsg;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PushMsgMapper extends BaseMapper<PushMsg> {

    Page<PushMsg> messageRecordPage(@Param("userId") Long userId, @Param("keywords") String keywords, @Param("start") Long start, @Param("end") Long end, Page<PushMsg> of);
}