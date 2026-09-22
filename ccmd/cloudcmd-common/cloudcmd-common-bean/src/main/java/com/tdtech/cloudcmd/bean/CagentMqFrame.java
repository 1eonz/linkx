package com.tdtech.cloudcmd.bean;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
public class CagentMqFrame implements Serializable {

    private static final String AT = "@";
    private static final String DASH = "-";
    private static final String ASTERISK = "*";
    private static final String AND = "&";
    private static final String MC = "mc:";
    private static final String UC = "uc:";
    private static final String COMMA = ",";
    private static final long serialVersionUID = 1L;

    private short type = 2;
    private String subsystem;
    private String tokenkey = "*-*";
    private Object body;
    private Long privOrg;
    private int sendStrategy = 0;

    public CagentMqFrame(short type, String subsystem, String token, Object body) {
        this.type = type;
        this.subsystem = subsystem;
        this.tokenkey = token;
        this.body = body;
    }

    public Builder toBuilder() {
        return new Builder();
    }

    public class Builder {

        public Builder batchMode() {
            CagentMqFrame.this.sendStrategy = 1;
            return this;
        }

        public Builder body(Object body) {
            CagentMqFrame.this.body = body;
            return this;
        }

        public Builder body(String module, String notifyType, Object data) {
            var msgBody = new MsgBody<Object>();
            msgBody.setModule(module);
            msgBody.setNotifyType(notifyType);
            msgBody.setData(data);
            CagentMqFrame.this.body = msgBody;
            return this;
        }

        public Builder typeUnknown() {
            CagentMqFrame.this.type = 0;
            return this;
        }

        public Builder typeKickOut() {
            CagentMqFrame.this.type = 0x0028;
            return this;
        }

        public Builder typeSubSystemMessage(String subSystem) {
            CagentMqFrame.this.type = 2;
            CagentMqFrame.this.subsystem = subSystem;
            return this;
        }

        public Builder privId(Long orgId) {
            CagentMqFrame.this.privOrg = orgId;
            return this;
        }

        /**
         * 广播
         */
        public Builder broadcast() {
            CagentMqFrame.this.tokenkey = "*-*";
            return this;
        }

        /**
         * 组播
         */
        public MulticastBuilder multicast() {
            return CagentMqFrame.this.new MulticastBuilder(this);
        }

        /**
         * 单播
         */
        public UnicastBuilder unicast() {
            return CagentMqFrame.this.new UnicastBuilder(this);
        }

        public CagentMqFrame build() {
            return CagentMqFrame.this;
        }
    }

    @RequiredArgsConstructor
    public class UnicastBuilder {
        private final Builder source;
        private String header = ASTERISK + DASH + "uc:";
        private List<String> rules = new LinkedList<>();

        public UnicastBuilder executorIds(List<Long> executorIds) {
            var join = join(executorIds, COMMA);
            rules.add("executor" + AT + join);
            return this;
        }

        public UnicastBuilder userIds(List<String> userIds) {
            var join = join(userIds, COMMA);
            rules.add("communication" + AT + join);
            return this;
        }

        public UnicastBuilder accounts(List<String> accounts) {
            var join = join(accounts, COMMA);
            rules.add("isdn" + AT + join);
            return this;
        }

        public UnicastBuilder appKeys(List<String> appKeys) {
            var join = join(appKeys, COMMA);
            rules.add("role" + AT + join);
            return this;
        }

        public Builder build() {
            var pattern = join(rules, AND);
            CagentMqFrame.this.tokenkey = header + pattern;
            return source;
        }
    }

    @RequiredArgsConstructor
    public class MulticastBuilder {
        private final Builder source;
        private String header = ASTERISK + DASH + "mc:";
        private List<String> rules = new LinkedList<>();

        public MulticastBuilder orgs(List<Long> orgs) {
            var join = join(orgs, COMMA);
            rules.add("organization" + AT + join);
            return this;
        }

        public MulticastBuilder appKeys(List<String> appKeys) {
            var join = join(appKeys, COMMA);
            rules.add("role" + AT + join);
            return this;
        }

        public Builder build() {
            var pattern = join(rules, AND);
            CagentMqFrame.this.tokenkey = header + pattern;
            return source;
        }
    }

    public static <T> String join(Collection<T> col, String sep) {
        return join(col, sep, null, null);
    }

    public static <T> String join(Collection<T> col, String sep, String pre, String suf) {
        return col.stream().map(Objects::toString)
                .collect(Collectors.joining(sep, pre == null ? "" : pre, suf == null ? "" : suf));
    }
}
