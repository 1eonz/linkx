package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.LabelVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.Label;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author lsc
 * @date 2025/7/15
 **/
@Mapper
public interface LabelMapper extends BaseMapper<Label> {

    List<LabelVO> getLabelVOs(@Param("name") String name, @Param("type") Integer type, @Param("scope") Integer scope, @Param("level") int level);

    List<Label> getLabelsByCollaborationPostId(@Param("collaborationPostId") Long collaborationPostId);

    Label selectLabelById(@Param("id") Long id);

    List<Label> selectBatchByIds(@NotNull @Param("ids") List<String> ids);

    List<Label> getAllChildrenLabels(@Param("ids") List<Long> ids);

    Integer deleteBatchByIds(@Param("ids") List<Long> ids);

    List<Label> selectAllByIds(@Param("ids") List<Long> ids);

    void deleteLabelById(@Param("id") Long id);

    IPage<Label> getLabelPage(Page<Label> page, @Param("name") String name);

    List<Label> selectAllList(Integer scope);

    List<Label> selectByDepartmentIds(@Param("departmentIds") List<Long> departmentIds);

}
