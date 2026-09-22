package com.tdtech.cloudcmd.constant;

public class BizConstants {
	/**
	 * 类型
	 */
	//终端
	public static final String CATEGORY_TERMINAL = "500006";

	//客户端
	public static final Integer CAPP = 0;
    public static final Byte NOT_DELETE = 0;
    public static final Byte DELETED = 1;

    public static class EventCategory{
        public static final Integer EMERGENCY_SMS = 300005;
    }

	//布控任务由我创建
	public static final Integer SUSPECT_OWN = 0;
	//布控任务由小队创建
	public static final Integer SUSPECT_GROUP = 1;

	/**
	 * redis
	 */
	public static final String ORGANIZATION = "cloudcmd:resource:organization:%s";

    public static class MissionCategory{
        public static final Integer EMERGENCY_SMS = 400005;
    }

    public static class EventType{
        public static final String EMERGENCY_SMS = "130000";
    }

    public static final Integer TERMINAL = 500006;
    public static final Integer RECORDER = 500005;
	public static final Integer PDT = 500008;
}
