package com.chinasoft.cloud.module.aiagent.excel.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.util.ListUtils;
import com.chinasoft.cloud.module.aiagent.excel.bo.AgentCommonBO;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Slf4j
public class AgentCommonReadListener<T extends AgentCommonBO> implements ReadListener<T> {
    /**
     * Default single handle the amount of data
     */
    private static final int BATCH_COUNT = 300;

    /**
     * Temporary storage of data
     */
    private final List<T> cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);

    @Getter
    private boolean isEmpty = true;

    /**
     * batch consumer
     */
    private final Consumer<List<T>> consumer;

    /**
     * predicate
     */
    private final Predicate<T> predicate;

    public AgentCommonReadListener(Predicate<T> predicate, Consumer<List<T>> consumer) {
        this.predicate = predicate;
        this.consumer = consumer;
    }

    @Override
    public void invoke(T data, AnalysisContext context) {
        data.setRowIndex(context.readRowHolder().getRowIndex() + 1);
        isEmpty = false;
        log.info("invoke data: {}", data);
        if (predicate.test(data)) {
            cachedDataList.add(data);
            log.info("verify successfully data: {}", data);
        }

        if (cachedDataList.size() >= BATCH_COUNT) {
            consumer.accept(cachedDataList);
            log.info("invoke batch data: {}", cachedDataList);
            cachedDataList.clear();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        if (CollectionUtils.isNotEmpty(cachedDataList)) {
            consumer.accept(cachedDataList);
            log.info("do after all analysed data: {}", cachedDataList);
        }
    }

}
