package com.tdtech.cloudcmd.cagent.service.outbound.rule;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.tdtech.cloudcmd.cagent.service.entity.BaseChannel;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class UserCastDispatcherRule implements DispatcherRule {
    private static final String EXECUTOR_TYPE = "executor";
    private static final String COMMUNICATION_TYPE = "communication";
    private static final String ISDN = "isdn";
    private static final String ROLE_TYPE = "role";

    @Override
    public boolean match(String patternFrom, String patternTo) {
        return ASTERISK.equals(patternFrom) && patternTo.indexOf(UC) == 0;
    }

    @Override
    public <T extends BaseChannel> Stream<T> pick(String patternFrom, String patternTo, Stream<T> source) {
        var targets = patternTo.substring(3).split(AND);
        for (var target : targets) {
            if(target.isBlank()){
                log.warn("strange to pattern:{}",patternTo);
                continue;
            }
            source = pick(patternFrom, patternTo, source, target);
        }
        return source;
    }

    private <T extends BaseChannel> @NotNull Stream<T> pick(String patternFrom, String patternTo, Stream<T> source, String target) {
        String[] item = target.split(AT);
        assertStringArrayAndNotBlank(item, 2);
        String[] ps = item[1].split(COMMA);

        switch (item[0].toLowerCase()) {
            case EXECUTOR_TYPE: {
                var ids = Arrays.stream(ps).filter(a -> !a.isBlank()).map(Long::parseLong).collect(Collectors.toList());
                return source.filter(a -> ids.contains(a.getUserInfo().getExecutorId()));
            }
            case COMMUNICATION_TYPE: {
                var ids = Arrays.stream(ps).filter(a -> !a.isBlank()).map(Long::parseLong).collect(Collectors.toList());
                return source.filter(a -> ids.contains(a.getUserInfo().getUserId()));
            }
            case ISDN: {
                return source.filter(a -> anyEquals(ps, a.getUserInfo().getAccount()));
            }
            case ROLE_TYPE: {
                List<String> clientIds = Arrays.asList(ps);
                return source.filter(a -> clientIds.contains(a.getUserInfo().getAppKey()));
            }
            default: {
                throw new UnsupportedOperationException("not supported yet,from:" + patternFrom + ",patternTo:" + patternTo);
            }
        }
    }
}
