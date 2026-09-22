
package com.tdtech.cloudcmd.im.jingxin.server.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.auth.dto.ImUserDto;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.CollaborationAttendance;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUser;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationAttendanceSwitch;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationAttendanceUserDTO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationPost;

/**
 * @author syf
 * @date 2025/7/16
 **/
public interface CollaborationAttendanceService  extends IService<CollaborationAttendance> {
    void heartbeat(Long userId);

    /**
     *
     * @param pageNum 页码
     * @param pageSize 页容量
     * @param postName 协同岗名称
     * @param orgName 组织名称
     * @param personName 人员姓名
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 上下岗记录列表
     */
    Page<CollaborationAttendance> getPage(int pageNum, int pageSize, String postName, String orgName, Long orgId,
        String personName, String startTime, String endTime);

    /**
     * 根据协同岗id统计剩余人数及推荐在岗人员
     * 
     * @param postId 协同岗id
     * @param userId 当前用户id
     * @return 剩余人数
     */
    Map<String, Object> getLastNumByPostId(Long postId, Long userId);

    /**
     * 存储协同岗上下岗记录
     * 
     * @param collaborationPost 记录实体
     */
    boolean save(CollaborationAttendance collaborationPost);

    boolean saveAttendance(List<CollaborationPost> collaborationPosts, CollaborationAttendanceUserDTO user);

    void saveRecord(CollaborationAttendance collaborationPost, int status);

    /**
     * 根据人员id获取开关状态
     * 
     * @param personId
     * @return
     */
    CollaborationAttendanceSwitch getSwitchStatusByPerson(Long personId);

    void sendSwitchStatusToCagent(Map<String, String> dataMap);

    /**
     *
     * @param fileName 文件名
     * @param postName 协同岗名称
     * @param orgName 组织名称
     * @param personName 人员姓名
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @throws IOException
     */
    void exportToExcel(String fileName, String postName, String orgName, String orgIds, String personName,
        String startTime, String endTime, HttpServletResponse response) throws IOException;

    List<ImUserDto> getOnlineByPostId(Long postId);

    boolean adminOffline(List<CollaborationPost> collaborationPosts, @Valid CollaborationAttendanceUserDTO user);
}