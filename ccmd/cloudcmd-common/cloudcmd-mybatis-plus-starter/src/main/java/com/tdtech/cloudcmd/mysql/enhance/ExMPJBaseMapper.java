package com.tdtech.cloudcmd.mysql.enhance;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.interfaces.MPJBaseJoin;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.tdtech.cloudcmd.mysql.entity.CcmdPage;
import com.tdtech.cloudcmd.mysql.entity.CcmdPageParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public interface ExMPJBaseMapper<T> extends MPJBaseMapper<T>, ExBaseMapper<T> {

    Logger log = LoggerFactory.getLogger(ExMPJBaseMapper.class);

    default T selectJoinFirst(MPJLambdaWrapper<T> wrapper) {
        List<T> list = this.selectJoinList(wrapper.last("LIMIT 1"));
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    default <DTO> CcmdPage<DTO> selectJoinPage(CcmdPageParam pageParam, Class<DTO> clazz, MPJBaseJoin<T> wrapper) {
        var page = new Page<DTO>(pageParam.getPageNum(), pageParam.getPageSize());
        page.setOptimizeCountSql(false);
        var result = selectJoinPage(page, clazz, wrapper);
        // 转换返回
        return new CcmdPage<>(pageParam.getPageNum(), pageParam.getPageSize(), result.getTotal(), result.getRecords());
    }
}