package com.tdtech.cloudcmd.cagent.service.outbound.rule;

import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.tdtech.cloudcmd.cagent.service.entity.BaseChannel;

@Component
public class PublishDispatcherRule implements DispatcherRule {

    @Override
    public boolean match(String patternFrom, String patternTo) {
        return ASTERISK.equals(patternFrom) && ASTERISK.equals(patternTo);
    }

    @Override
    public <T extends BaseChannel> Stream<T> pick(String patternFrom, String patternTo, Stream<T> source) {
        return source;
    }
}
