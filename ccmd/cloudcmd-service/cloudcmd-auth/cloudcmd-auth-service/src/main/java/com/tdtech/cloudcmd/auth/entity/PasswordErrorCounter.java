package com.tdtech.cloudcmd.auth.entity;

import java.util.Objects;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PasswordErrorCounter {

    private Integer count;
    private Long time;

    public boolean shouldBlock(Integer unlockTime,Integer errorCount, Long now) {
        Objects.requireNonNull(unlockTime);
        Objects.requireNonNull(errorCount);
        return count != null && count >= errorCount && time != null && now < time + unlockTime * 60L * 1000L;
    }

    public PasswordErrorCounter increaseCount() {
        count = count == null ? 1 : count + 1;
        return this;
    }
}
