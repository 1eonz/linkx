package com.tdtech.cloudcmd.im.jingxin.server.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tdtech.cloudcmd.base.api.service.GlobalsRpcService;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImToken;
import com.tdtech.cloudcmd.im.jingxin.server.entity.GroupExtends;
import com.tdtech.cloudcmd.im.jingxin.server.service.GroupExtendsService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.GroupExtendsMapper;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.util.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * @author lsc
 * @date 2025/9/24
 **/
@Slf4j
@Configuration
public class GroupArchiveScheduler {

    @DubboReference
    private GlobalsRpcService globalsRpcService;
    @Resource
    private GroupExtendsMapper groupExtendsMapper;
    @Resource
    private GroupExtendsService groupExtendsService;
    @Resource
    private ImHttpClient imHttpClient;
    @Resource
    private RedisUtil redisUtil;
    private static final String TARGETDATETIME = "cloudcmd:im:group:archive:target:time";
    private static final String GROUPARCHIVETIME = "cloudcmd:im:group:archive:time";


    @Scheduled(initialDelay = 10000L, fixedDelay = 60L * 1000L)
    public void scheduled() {
        //查找到每多少小时群组归档
        String groupArchiveIntervalTime = globalsRpcService.getGlobalsValueByName("GROUP_ARCHIVE_INTERVAL_TIME");
        LocalDateTime now = LocalDateTime.now();
        log.info("group archive scheduled now: {}", now);
        //获取下次任务执行时间
        LocalDateTime targetDateTime = redisUtil.get(TARGETDATETIME, LocalDateTime.class);
        log.info("next execute time: {}", targetDateTime);
        //获取上次设置的任务执行周期
        String oldGroupArchiveTime = redisUtil.get(GROUPARCHIVETIME, String.class);

        if(targetDateTime == null || !groupArchiveIntervalTime.equals(oldGroupArchiveTime)){
            // 如果没有目标时间，或者任务周期改变，则马上执行任务且设置下一次执行时间
            executeArchive();
            targetDateTime = now.plusHours(Integer.valueOf(groupArchiveIntervalTime));
            redisUtil.set(TARGETDATETIME, targetDateTime);
            redisUtil.set(GROUPARCHIVETIME, groupArchiveIntervalTime);
        }else{
            // 如果当前时间已经超过任务目标时间，执行任务，并计算下次时间
            if (now.isAfter(targetDateTime)) {
                log.info("群组归档任务已开始");
                // 计算下一次执行时间
                targetDateTime = targetDateTime.plusHours(Integer.valueOf(groupArchiveIntervalTime));
                redisUtil.set(TARGETDATETIME, targetDateTime);
                executeArchive();
            }
        }
    }

    private void executeArchive() {
        try {
            QueryWrapper queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("archived", 0);
            //查找未归档的群组，获取到群组id
            List<GroupExtends> list = groupExtendsMapper.selectList(queryWrapper);
            log.info("群组归档任务开始处理群组数量:{}",list.size());
            ImToken token = imHttpClient.getToken();

            //循环群组id，处理群组
            if(CollectionUtils.isEmpty(list)){
                log.info("没有需要归档消息的群组！");
                return;
            }
            long l = System.currentTimeMillis();
            for(GroupExtends groupExtend : list){
                groupExtendsService.asyncArchive(groupExtend.getGroupId(), token.getProxyUser().getId(), null, groupExtend,false);
            }
            log.info("群组归档任务处理完成，归档消息群组数量:{},归档耗时{}ms",list.size(),System.currentTimeMillis() - l);
        } catch (Exception e) {
            log.error("群组归档任务执行失败", e);
        }
    }

}
