package com.tdtech.cloudcmd.im.jingxin.server.util;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransactionWrapper {

    @Transactional(rollbackFor = Exception.class)
    public void wrap(Runnable runnable){
        runnable.run();
    }

}
