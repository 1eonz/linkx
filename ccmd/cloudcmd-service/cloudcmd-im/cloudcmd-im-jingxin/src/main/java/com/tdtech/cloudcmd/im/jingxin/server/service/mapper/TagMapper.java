package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.server.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author ChinasoftPortal
 * @date 2025/9/10
 * @Describe：
 */
@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    /**
     * Get tag list by page with keywords
     *
     * @param page     pagination object
     * @param keywords search keywords
     * @return paginated tag list
     */
    IPage<Tag> selectPageByKeywords(Page<Tag> page, @Param("name") String name);

    /**
     * Count tags by name (for uniqueness check)
     *
     * @param name      tag name
     * @param excludeId ID to exclude (for update scenario)
     * @return count of matching tags
     */
    int countByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    List<Tag> selectAllList();

    List<Tag> selectBatchByIds(@Param("ids") List<Long> ids);

    Integer deleteBatchByIds(@Param("tags") List<Tag> tags);
}
