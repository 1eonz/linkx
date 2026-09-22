package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.client.entity.IMOfflineMsgItemVo;
import com.tdtech.cloudcmd.im.jingxin.server.entity.*;
import com.tdtech.cloudcmd.im.jingxin.server.service.GroupExtendsService;
import com.tdtech.cloudcmd.im.jingxin.server.service.ITasksService;
import com.tdtech.cloudcmd.im.jingxin.server.service.PoliceTicketService;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.DateFormatUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 协同群组管理控制器
 * 提供群组归档、群组标签、群组评价等功能
 *
 * @author ChinasoftPortal
 * @date 2025/9/10
 */
@Slf4j
@Tag(name = "协同群组归档")
@RestController
@RequestMapping("/collaboration/v1/groups")
public class GroupExtendsController {


    @Resource
    private GroupExtendsService groupExtendsService;
    @Resource
    private PoliceTicketService policeTicketService;

    @Resource
    private ITasksService tasksService;

    @Resource
    private ReportUtil reportUtil;

    @GetMapping("/{groupId}")
    public R<GroupExtends> getByGroupId(@PathVariable Long groupId) throws Exception {
        return R.success(groupExtendsService.getByGroupId(groupId));
    }

    @PostMapping
    public R<Boolean> saveOrUpdate(@RequestBody GroupExtends groupExtends) throws Exception {
        return R.success(groupExtendsService.saveOrUpdateGroupExtends(groupExtends));
    }

    @PutMapping("/{groupId}/archive")
    public R<Boolean> updateArchive(@PathVariable Long groupId, @RequestBody GroupExtends groupExtends) throws Exception {
        return R.success(groupExtendsService.updateArchiveInfo(groupId, groupExtends));
    }


    @PutMapping("/archive")
    public R<Integer> InitiateArchive(@RequestParam Long groupId,
                                      @RequestParam(required = false) Long userId) {
        return R.success(groupExtendsService.updateArchive(groupId, userId));
    }

    @GetMapping("/get/{id}")
    public R getById(@PathVariable Long id) {
        return R.success(groupExtendsService.getById(id));
    }

    /**
     * [GET] /api/v1/groups/page? &page=&pageSize=	获取我关注的群组列表
     * 获取已经收藏的已经归档或者未归档
     *
     * @param pageNum
     * @param pageSize
     * @param keywords
     * @param archived
     * @return
     */
    @GetMapping("/page")
    public R<IPage<UserCareGroupDTO>> getPageList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer archived,
            @RequestParam(required = false) String archivedTime,
            @RequestParam(required = false) String keywords,
            @RequestParam(required = false) Integer type) throws Exception {
        IPage<UserCareGroupDTO> page = groupExtendsService.getPageList(pageNum, pageSize, archived, archivedTime, keywords, type);
        var records = page.getRecords();
        if (records != null && !records.isEmpty()) {
            var groupIdList = records.stream().map(UserCareGroupDTO::getGroupId).collect(Collectors.toList());
            var cntMap = policeTicketService.countBinding(groupIdList);
            var tasksGroupCntMap = tasksService.countBinding(groupIdList);
            log.debug("getPageList,{}", cntMap);
            log.debug("tasksGroupCntMap: {}", tasksGroupCntMap);
            for (var record : records) {
                record.setPolTicketCnt(cntMap.getOrDefault(record.getGroupId(), 0));
                record.setTasksCnt(tasksGroupCntMap.getOrDefault(record.getGroupId(), 0));
            }
        }
        return R.success(page);
    }

    /**
     * 标签群组管理,未归档	[GET] /api/v1/tags/{tagId}/groups/page?keywords=&page=&pageSize=	通过标签查询群组列表（支持群组名称的关键字搜索）
     * [GET] /api/v1/groups/{groupId}/msgs/archive/page?keywords=&page=&pageSize=  查询已归档群组列表
     *
     * @param pageNum
     * @param pageSize
     * @param archived
     * @param archivedTime
     * @param groupName
     * @param tagName
     * @return
     */
    @GetMapping("/tags/page")
    public R<IPage<UserCareGroupDTO>> getTagsPageList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer archived,
            @RequestParam(required = false) String archivedTime,
            @RequestParam(required = false) String keywords,
            @RequestParam(required = false) String groupName,
            @RequestParam(required = false) String tagName,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer scope) throws Exception {
        IPage<UserCareGroupDTO> page = groupExtendsService.getTagsPageList(pageNum, pageSize, archived, archivedTime, keywords, groupName, tagName, type, scope);

        if (page != null) {
            var records = page.getRecords();
            if (records != null && !records.isEmpty()) {
                List<Long> groupIdList = records.stream().map(UserCareGroupDTO::getGroupId).filter(Objects::nonNull).collect(Collectors.toList());
                var policeTicketCntMap = policeTicketService.countBinding(groupIdList);
                var tasksGroupCntMap = tasksService.countBinding(groupIdList);
                log.debug("policeTicketCntMap: {}", policeTicketCntMap);
                log.debug("tasksGroupCntMap: {}", tasksGroupCntMap);
                for (var record : records) {
                    record.setPolTicketCnt(policeTicketCntMap.getOrDefault(record.getGroupId(), 0));
                    record.setTasksCnt(tasksGroupCntMap.getOrDefault(record.getGroupId(), 0));
                }
            }
        }
        return R.success(page);
    }

    /**
     * 归档时间轴（支持根据关键字进行统计）
     *
     * @param userId
     * @param keywords 搜索关键词，支持搜索create_time、archived_time、group_name、user_names
     * @return 归档时间轴上的数量
     */
    @GetMapping("/archive/timeline")
    public R<List<ArchiveTimelineDTO>> getArchivedPageList(@RequestParam(required = false) Long userId, @RequestParam(required = false) String keywords) {
        List<ArchiveTimelineDTO> archiveTimeline = groupExtendsService.getArchiveTimeline(keywords);
        return R.success(archiveTimeline);
    }


    /**
     * 已经收藏归档时间轴
     *
     * @param userId
     * @return
     */
    @GetMapping("/cared/archive/timeline")
    public R<List<ArchiveTimelineDTO>> getCaredArchivedPageList(@RequestParam(required = false) Long userId, @RequestParam(required = false) String keywords) {
        List<ArchiveTimelineDTO> archiveTimeline = groupExtendsService.getCaredArchivedPageList(keywords);
        return R.success(archiveTimeline);
    }

    /**
     * 群组标签管理 [PUT] /api/v1/groups/{groupId}/tags	更新群组标签。传空则为清空标签。
     *
     * @param groupId
     * @param tagId
     * @param userId
     * @return
     */
    @PutMapping("/tags")
    public R<Void> updateGroupTags(
            @RequestParam Long groupId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) Long userId) throws Exception {
        groupExtendsService.updateGroupTags(groupId, tagId, userId, true);
        return R.success();
    }

    @PutMapping("/batch/tags")
    public R<Void> batchUpdateGroupTags(
            @RequestBody GroupTagsVO groupTagsVO) throws Exception {
        groupExtendsService.batchUpdateGroupTags(groupTagsVO.getGroupId(), groupTagsVO.getTagIds(), null, true);
        return R.success();
    }

    @GetMapping("/count")
    public R<List<TagCountDTO>> getCountGroupTags(
            @RequestParam(required = false) Long userId, @RequestParam(required = false) String departmentCode, @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return R.success(groupExtendsService.getCountGroupTags(userId, departmentCode, startTime, endTime));
    }

    /**
     * 归档群组历史消息列表接口
     */
    @GetMapping("/{groupId}/msgs/archive/page")
    public R<IPage<IMOfflineMsgItemVo>> getArchiveMsgPage(
            @PathVariable("groupId") Long groupId, // 路径参数：群组ID
            @RequestParam(value = "keywords", required = false) String keywords, // 可选关键词
            @RequestParam(value = "pageNum", required = false) Integer pageNum, // 可选页码
            @RequestParam(value = "pageSize", required = false) Integer pageSize, // 可选每页条数
            @RequestParam(value = "startTime", required = false) @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS) Date startTime,
            @RequestParam(value = "endTime", required = false) @DateTimeFormat(pattern = DateFormatUtil.YYYY_MM_DD_HH_MM_SS) Date endTime,
            @RequestParam(value = "from", required = false) String from // 发送者ID（tb_chat_member_xxxx中id）
    ) {
        //对keywords进行解码 因为前端加密了两次
        keywords = URLDecoder.decode(keywords, StandardCharsets.UTF_8);
        IPage<IMOfflineMsgItemVo> pageList = groupExtendsService.getArchiveMsgPage(groupId, keywords, pageNum, pageSize, startTime, endTime, from);
        return R.success(pageList);
    }

    /**
     * 归档群组成员列表接口
     */
    @GetMapping("/{groupId}/members/archive/page")
    public R<IPage<TbChatMember>> getArchiveMemberPage(
            @PathVariable("groupId") Long groupId,
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize
    ) {
        IPage<TbChatMember> pageList = groupExtendsService.getArchiveMemberPage(groupId, pageNum, pageSize);
        return R.success(pageList);
    }

    @GetMapping("/{groupId}/{messageId}")
    public R getGroupMessageById(
            @PathVariable("groupId") Long groupId,
            @PathVariable("messageId") Long messageId) {

        return R.success(groupExtendsService.getGroupMessageById(groupId, messageId));
    }

    /**
     * 获取归档后的列表
     */
    @GetMapping("/archive/list")
    public R<IPage<GroupExtendsVO>> getArchiveList(@RequestParam(value = "startTime", required = false) String startTime,
                                                   @RequestParam(value = "endTime", required = false) String endTime,
                                                   @RequestParam(value = "keywords", required = false) String keywords,
                                                   @RequestParam(value = "pageNum", required = false) Integer pageNum,
                                                   @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return R.success(groupExtendsService.getArchiveList(startTime, endTime, keywords, pageNum, pageSize));
    }

    /**
     * 根据群组id批量下载归档后的文件
     */
    @PostMapping("/archive/download")
    public void downloadArchiveFiles(@RequestBody List<Long> groupIds,
                                     HttpServletResponse response) {
        List<GroupExtendsVO> groupExtendsList = groupExtendsService.getGroupExtendsList(groupIds);
        groupExtendsService.downloadArchivedGroupFiles(groupExtendsList, response);
    }

    /**
     * 删除归档的文件，并且删除归档的群组
     */
    @PostMapping("/archive/delete")
    public R deleteArchiveFiles(@RequestBody List<Long> groupIds) {
        List<GroupExtendsVO> groupExtendsList = groupExtendsService.getGroupExtendsList(groupIds);
        if (CollectionUtils.isEmpty(groupExtendsList)) {
            reportUtil.saveOperationLog(OperationTypeEnum.GROUP_ARCHIVE_DELETE, "删除归档群组：[ids=" + groupIds + "]不存在");
            return R.failure("群组不存在");
        }
        groupExtendsService.deleteArchivedGroupFiles(groupExtendsList);
        return R.success();
    }

    @PostMapping("/group/list")
    public R<List<GroupExtends>> groupList(@RequestBody List<Long> groupIds) {
        return R.success(groupExtendsService.findListByGroupIds(groupIds));
    }
}