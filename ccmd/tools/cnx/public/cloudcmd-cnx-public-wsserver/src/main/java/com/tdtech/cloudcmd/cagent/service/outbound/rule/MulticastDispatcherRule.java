package com.tdtech.cloudcmd.cagent.service.outbound.rule;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.tdtech.cloudcmd.cagent.service.entity.BaseChannel;

@Component
public class MulticastDispatcherRule implements DispatcherRule {

    private static final String ORG_TYPE = "organization";
    private static final String COMMADNCENTER_TYPE = "commandcenter";
    private static final String ROLE_TYPE = "role";

    @Override
    public boolean match(String patternFrom, String patternTo) {
        return ASTERISK.equals(patternFrom) && patternTo.indexOf(MC) == 0;
    }

    @Override
    public <T extends BaseChannel> Stream<T> pick(String patternFrom, String patternTo, Stream<T> source) {
        String[] item = patternTo.substring(3).split(AND);
        for (String s : item) {
            if (StringUtils.isNotBlank(s)) {
                source = pick(s, source);
            }
        }
        return source;
    }

    private <T extends BaseChannel> Stream<T> pick(String pattern, Stream<T> source) {
        String[] split = pattern.split(AT);
        assertStringArrayAndNotBlank(split, 2);
        String[] ps = split[1].split(COMMA);
        switch (split[0].toLowerCase()) {
            case ORG_TYPE: {
                var orgIds =
                    Arrays.stream(ps).filter(a -> !a.isBlank()).map(Long::parseLong).collect(Collectors.toList());
                return source.filter(a -> orgIds.contains(a.getUserInfo().getOrganizationId()));
            }
            case COMMADNCENTER_TYPE: {
                // TODO Deprecated
                return source;
            }
            case ROLE_TYPE: {
                List<String> clientIds = Arrays.asList(ps);
                return source.filter(a -> clientIds.contains(a.getUserInfo().getAppKey()));
            }
            default: {
                throw new UnsupportedOperationException("not supported yet");
            }
        }
    }
}
