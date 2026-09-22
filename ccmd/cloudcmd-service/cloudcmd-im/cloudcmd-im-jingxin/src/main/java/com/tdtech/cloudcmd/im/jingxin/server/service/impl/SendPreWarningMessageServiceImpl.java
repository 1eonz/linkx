package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import cloudcmd.dto.PerWarningRalationDto;
import cloudcmd.service.rpc.PerWarningRalationRpcService;
import com.alibaba.fastjson.JSON;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.*;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationPost;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationTaskCO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CreateGroup;
import com.tdtech.cloudcmd.im.jingxin.server.service.SendPreWarningMessageService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CollaborationPostMapper;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CreateGroupMapper;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author: S063874
 * @date: 2026-01-15 19:16
 */
@Service
@Slf4j
public class SendPreWarningMessageServiceImpl implements SendPreWarningMessageService {

    @Autowired
    private ImHttpClient imHttpClient;

    @Autowired
    private ImService imService;


    @Autowired
    @Qualifier("warningImHttpClient")
    private ImHttpClient warningImHttpClient;

    @Resource
    private CollaborationPostMapper collaborationPostMapper;

    @Resource
    private CreateGroupMapper createGroupMapper;

    @Resource(name = "warningMessageExecutorService")
    private TaskExecutor taskExecutor;

    @DubboReference
    private PerWarningRalationRpcService perWarningRalationRpcService;

    /**
     * 消息格式正则表达式
     */
    private static final Pattern pattern = Pattern.compile("\\[@([^:\\[\\]]+?):(@[^:\\[\\]]+?)\\]");
    /**
     * 发送协同岗无人值守消息
     *
     * @param postId
     */
    @Override
    public void sendUnattendedMessage(Long postId) {
        List<ImMessageRequest> imMessageRequestList = new ArrayList<>();
        // 根据协同岗id查询需要通知的人员或者群组
        List<PerWarningRalationDto> perWarningRalationList = perWarningRalationRpcService.getPerWarningRalationByPostId(postId);
        if (CollectionUtils.isEmpty(perWarningRalationList)) {
            log.warn("未查询到需要通知的人员或者群组,postId:{}", postId);
            return;
        }
        // 根据人员或部门分组;
        PerWarningRalationDto perWarningRalationDto = perWarningRalationList.get(0);
        // 协同岗名称
        String businessName = perWarningRalationDto.getBusinessName();
        // 所属部门
        String orgName = perWarningRalationDto.getOrgName();

        String txtMsg = "【协同岗无人值守预警】：" + "\n"
                + "所属部门：" + orgName + "\n"
                + "协同岗：" + businessName + "\n"
                + "无人值守开始时间："
                + getCurrentDatetime(null);
        Map<Integer, List<PerWarningRalationDto>> perWarningRalationMap = perWarningRalationList.stream().collect(Collectors.groupingBy(PerWarningRalationDto::getTargetType));
        perWarningRalationMap.forEach((k, v) -> {
            // 发给人员
            if (k == 1) {
                buildimMessageRequestByUser(v, imMessageRequestList,txtMsg);
            } else {
                // 发给群组
                buildimMessageRequestByGroup(v, imMessageRequestList,txtMsg);
            }
        });
        // 异步发送消息
        taskExecutor.execute(() -> sendMessage(imMessageRequestList));
    }
    private static String getCurrentDatetime(Date date) {
        return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Objects.requireNonNullElseGet(date, Date::new));
    }
    /**
     * 组装群组消息
     *
     * @param v
     * @param imMessageRequestList
     * @param txtMsg
     */
    private void buildimMessageRequestByGroup(List<PerWarningRalationDto> v,
                                              List<ImMessageRequest> imMessageRequestList,
                                              String txtMsg) {
        if (CollectionUtils.isNotEmpty(v)) {
            v.forEach(perWarningRalationDto -> {
                ImMessageRequest imMessageRequest = new ImMessageRequest<TxtMsgVo>();
                imMessageRequest.setCategory(2);
                imMessageRequest.setMsgType(1);
                imMessageRequest.setFrom(warningImHttpClient.getProxyUserId());
                imMessageRequest.setToIdType(2);
                // 发给群组用群组id
                imMessageRequest.setTo(perWarningRalationDto.getTargetId().toString());
                imMessageRequest.setPlaintext(1);
                TxtMsgVo txtMsgVo = new TxtMsgVo();
                txtMsgVo.setText(txtMsg);
                imMessageRequest.setMsg(txtMsgVo);
                imMessageRequestList.add(imMessageRequest);
            });
        }
    }

    /**
     * 组装用户消息
     *
     * @param v
     * @param imMessageRequestList
     * @param txtMsg
     */
    private void buildimMessageRequestByUser(List<PerWarningRalationDto> v, List<ImMessageRequest> imMessageRequestList,
                                             String txtMsg) {


        if (CollectionUtils.isNotEmpty(v)) {
            v.forEach(perWarningRalationDto -> {
                ImMessageRequest imMessageRequest = new ImMessageRequest<TxtMsgVo>();
                imMessageRequest.setCategory(1);
                imMessageRequest.setMsgType(1);
                imMessageRequest.setFrom(warningImHttpClient.getProxyUserId());

                imMessageRequest.setToIdType(1);
                // 发给用户用身份证号码
                imMessageRequest.setTo(perWarningRalationDto.getIdCard());
                imMessageRequest.setPlaintext(1);
                TxtMsgVo txtMsgVo = new TxtMsgVo();
                txtMsgVo.setText(txtMsg);
                imMessageRequest.setMsg(txtMsgVo);
                imMessageRequestList.add(imMessageRequest);
            });
        }

    }

    /**
     * 发送任务过期消息
     *
     * @param collaborationTaskCO
     */
    @Override
    public void sendTaskexpiredMessage(CollaborationTaskCO collaborationTaskCO) {
        List<ImMessageRequest> imMessageRequestList = new ArrayList<>();
        log.info("发送任务过期消息,collaborationTaskCO:{}", JSON.toJSONString(collaborationTaskCO));
        Long userId = collaborationTaskCO.getUserId();
        Long postId = collaborationTaskCO.getPostId();
        String text = collaborationTaskCO.getText();
        Long groupId = collaborationTaskCO.getGroupId();
        Date gmtCreated = collaborationTaskCO.getGmtCreated();

        String userName = getUserName(userId);
        String groupName;
        if(StringUtils.isBlank(userName)){
            log.warn("未查询到用户名称,userId:{}", userId);
            return;
        }

        // 查询逾期任务具体协同岗信息
        CollaborationPost collaborationPost = collaborationPostMapper.selectById(postId);
        if(collaborationPost == null){
            log.warn("未查询到逾期任务对应协同岗信息,postId:{}", postId);
            return;
        }
        // 查询任务逾期群组名称

        try {
            CreateGroup createGroup = createGroupMapper.selectByGroupId(groupId);
            if(createGroup !=null){
                groupName = createGroup.getGroupName() ;
            }else{
                groupName = collaborationTaskCO.getGroupName();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Long orgId = collaborationPost.getOrgId();
        String orgName = collaborationPost.getOrgName();
        String postName = collaborationPost.getPostName();

        //根据orgId查询到所有部门以及上级部门
        List<ImDepartment> imDepartments = imService.queryDepartmentWithParents(orgId);
        if(CollectionUtils.isEmpty(imDepartments)){
            log.warn("未查询到对应id的部门信息,orgId:{}", orgId);
            return;
        }
        List<Long> deptIds = imDepartments.stream().map(ImDepartment::getId).collect(Collectors.toList());
        // 查询逾期任务对应的发送预警信息的人员和群组;
        List<PerWarningRalationDto> perWarningRalationList = perWarningRalationRpcService.getPerWarningRalationByDeptId(deptIds);

        if (CollectionUtils.isEmpty(perWarningRalationList)) {
            log.warn("未查询到需要通知的人员或者群组,postId:{}", postId);
            return;
        }

        String txtMsg = "【协同任务逾期预警】：" + "\n"
                + "任务部门：" + orgName + "\n"
                + "协同岗：" + postName + "\n"
                + "支撑人员：" + userName + "\n"
                + "群组名称：" + groupName + "\n"
                + "任务消息：" + parseAtMessage(text) + "\n"
                + "任务创建：" + getCurrentDatetime(gmtCreated) + "\n"
                + "逾期时间：" + getCurrentDatetime(null);
        Map<Integer, List<PerWarningRalationDto>> perWarningRalationMap = perWarningRalationList.stream().collect(Collectors.groupingBy(PerWarningRalationDto::getTargetType));
        perWarningRalationMap.forEach((k, v) -> {
            // 发给人员
            if (k == 1) {
                buildimMessageRequestByUser(v, imMessageRequestList,txtMsg);
            } else {
                // 发给群组
                buildimMessageRequestByGroup(v, imMessageRequestList,txtMsg);
            }
        });
        // 异步发送消息
        taskExecutor.execute(() -> sendMessage(imMessageRequestList));
    }

    private void sendMessage(List<ImMessageRequest> imMessageRequest) {
        imMessageRequest.forEach(imMessageRequest1 -> {
            try {

                IMMsgRspVo imMsgRspVo = warningImHttpClient.sendMsg(imMessageRequest1);
                String msgId = imMsgRspVo.getMsgId();
                if (StringUtils.isNotEmpty(msgId)) {
                    log.info("预警消息发送成功,imMessageRequest:{}", imMessageRequest1);
                }
            } catch (Exception e) {
                log.error("预警消息发送失败,imMessageRequest:{}", imMessageRequest1);
            }
        });
    }

    private String getUserName(Long userId){
        List<UserIDNameInfo> userIDNameInfos = imHttpClient.queryUserDetail(userId.toString());
        if(CollectionUtils.isNotEmpty(userIDNameInfos)){
            return userIDNameInfos.get(0).getName();
        }
        return "";
    }

    /**
     * 解析@消息
     *
     * @param text
     * @return
     */
    private static String parseAtMessage(String text){
        if(StringUtils.isBlank(text) || !text.contains("[@")){
            return text;
        }
        String remainingText = pattern.matcher(text).replaceAll("");
        // 匹配@all消息
        if(text.startsWith("[@all")){
            return "@所有人" + " " + remainingText;
        }
        if(text.contains("@all]")){
            text = text.replace("@all]", "@所有人]");
        }
        // 匹配所有[@xxx:@xxx]格式的块
        Matcher matcher = pattern.matcher(text);
        StringBuilder usernames = new StringBuilder();

        // 提取所有用户名并拼接
        while (matcher.find()) {
            String usernameWithAt = matcher.group(2);
            usernames.append(usernameWithAt);
            if(!usernameWithAt.endsWith(" ")){
                usernames.append(" ");
            }
        }
        String allName = usernames.toString();
        if(StringUtils.isNotBlank(allName) && !allName.endsWith(" ")){
            allName = allName + " ";
        }
        // 移除所有[@xxx:@xxx]部分，获取剩余文本
        return pattern.matcher(text).replaceAll("#{allName}").replaceAll("(#\\{allName\\})+", allName);
    }
}
