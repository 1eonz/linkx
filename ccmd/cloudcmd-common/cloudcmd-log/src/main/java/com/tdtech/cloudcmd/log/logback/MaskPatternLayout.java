package com.tdtech.cloudcmd.log.logback;

import com.tdtech.cloudcmd.log.mask.MaskRules;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * @author zWX446107
 * @Description 敏感数据日志脱敏
 *              <p>
 *              1、按照安全红线要求，对敏感数据日志进行脱敏。其原理：对产生的日志信息，进行正则匹配和替换。
 *              </p>
 *              <p>
 *              2、敏感数据包括但不限于：姓名、密码、身份证号码、电话号码、Email、银行卡号等。
 *              </p>
 * @create 2020-12-02 17:00
 */
public class MaskPatternLayout extends PatternLayout {
    /**
     * Mask rule
     */
    private MaskRules rules;

    public MaskPatternLayout() {}

    @Override
    public String doLayout(ILoggingEvent event) {
        return rules.apply(super.doLayout(event));
    }

    @SuppressWarnings("unused")
    public void setRules(MaskRules rules) {
        this.rules = rules;
    }
}
