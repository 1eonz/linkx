package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@NoArgsConstructor
@Data
public class IMOfflineMsgItemVo {
    // IM消息类型。1-文本消息；2-彩信消息；3-位置共享；4-已读回执；5-名片；6-群接龙；7-撤回消息；8-合并转发
    private Integer msgType;
    // 消息分类：1-点对点消息；2-群组消息；3-公众号；4-多人转发；5-在线客服消息；8.服务器通知消息；9.终端发起的通知消息。注意：“终端发起的通知消息”不会持久化，不会在历史消息中查询到，也不会在终端不在线时进行通知
    private Integer category;

    private String clientMsgId;

    private Boolean forwardMsg;

    private String from;

    private String fromRealUserId;

    private Integer fromIdType;

    private String fromIsdn;

    private String to;

    private Integer toIdType;

    private Boolean toType;

    private Boolean fromType;

    private String toIsdn;

    /****** 此处省略一万个字段 *****/

    private Object msg;

    private String msgId;

    private Boolean oneByOneMsg;

    private Integer plaintext;

    private Boolean read;

    private Integer seq;

    private Integer sessionSeqId;

    private Long time;

    private Boolean withdraw;

    public IMOfflineMsgItemVo(Map<String, Object> data) {
        if (data.get("category") != null) {
            this.setCategory((Integer) data.get("category"));
        }
        if (data.get("msg_type") != null) {
            this.setMsgType((Integer) data.get("msg_type"));
        }
        if (data.get("forward_msg") != null) {
            Integer forwardMsgInt = Integer.valueOf(data.get("forward_msg").toString());
            if (forwardMsgInt == 0) {
                this.setForwardMsg(false);
            } else if (forwardMsgInt == 1) {
                this.setForwardMsg(true);
            }
        }
        if (data.get("from") != null) {
            this.setFrom(data.get("from").toString());
        }
        if (data.get("from_real_user_id") != null) {
            this.setFromRealUserId(data.get("from_real_user_id").toString());
        }
        if (data.get("from_isdn") != null) {
            this.setFromIsdn((String) data.get("from_isdn"));
        }
        if (data.get("to") != null) {
            this.setTo(data.get("to").toString());
        }
        if (data.get("to_type") != null) {
            Integer toTypeInt = Integer.valueOf(data.get("to_type").toString());
            if (toTypeInt == 0) {
                this.setToType(false);
            } else if (toTypeInt == 1) {
                this.setToType(true);
            }
        }
        if (data.get("from_id_type") != null) {
            this.setFromIdType((Integer) data.get("from_id_type"));
        }
        if (data.get("to_isdn") != null) {
            this.setToIsdn((String) data.get("to_isdn"));
        }
        if (data.get("msg_id") != null) {
            this.setMsgId((String) data.get("msg_id"));
        }
        if (data.get("one_by_one_msg") != null) {
            Integer oneByOneMsgInt = Integer.valueOf(data.get("one_by_one_msg").toString());
            if (oneByOneMsgInt == 0) {
                this.setOneByOneMsg(false);
            } else if (oneByOneMsgInt == 1) {
                this.setOneByOneMsg(true);
            }
        }
        if (data.get("plaintext") != null) {
            this.setPlaintext((Integer) data.get("plaintext"));
        }
        if (data.get("read") != null) {
            Integer readInt = Integer.valueOf(data.get("read").toString());
            if (readInt == 0) {
                this.setRead(false);
            } else if (readInt == 1) {
                this.setRead(true);
            }
        }
        if (data.get("seq") != null) {
            this.setSeq((Integer) data.get("seq"));
        }
        if (data.get("session_seq_id") != null) {
            this.setSessionSeqId((Integer) data.get("session_seq_id"));
        }
        if (data.get("time") != null) {
            this.setTime((Long) data.get("time"));
        }
        if (data.get("withdraw") != null) {
            Integer withdrawInt = Integer.valueOf(data.get("withdraw").toString());
            if (withdrawInt == 0) {
                this.setWithdraw(false);
            } else if (withdrawInt == 1) {
                this.setWithdraw(true);
            }
        }
        if (data.get("msg") != null) {
            this.setMsg(data.get("msg"));
        }
        if (data.get("client_msg_id") != null) {
            this.setClientMsgId((String) data.get("client_msg_id"));
        }
    }

    @Data
    public static class MMSMsgVo {
        private Integer fileType;//文件类型。1-image（图片）；2-audio（语音）；3-video（视频）; 4- general（其它类型文件）;5-word;6-excel;7-pdf;8-txt;9-sms;10-ppt;11-location;
        private Long fileSize;//文件大小，单位KB。最大100M。
        private String fileName;//文件名称，必选。携带文件后缀。
        private String fileKey;//必选。文件管理服务分配的filekey。
        private String imageSize;//图片尺寸，格式为w_h，可选。图片时携带。
        private String videoThumb;//视频封面图片，可选。视频时携带。
        private Long duration;//时长（秒），可选。语音/视频时携带。
        private Boolean selfDestruct;//是否阅后即焚。携带即表示该文件需阅后即焚
        private String filePath;
    }

    @Data
    public static class MergeForwardMsgVo {
        /**
         * 文件，可选
         */
        private String title;

        private List<ForwardMsg> forwardMsgs;
    }

    @Data
    public static class TxtOfflineMsgVo {
        /**
         * 文件，可选
         */
        private String text;

        private String srcMsgId;

    }

    @Data
    public static class UserCardVo {
        /**
         * 文件，可选
         */
        private String userId;

        private String userName;

        private String department;

        /**
         * 头像
         */
        private String  avatar;
    }

    @Data
    public static class OfficialAccountsVo {
        /**
         * 文件，可选
         */
        private String numCode;

        private String label;

        private String newsId;

        private String newsTitle;
        private String newsUrl;

        private String avatar;
    }

    @Data
    public static class SharedMsgVo {
        /**
         * 文件，可选
         */
        private String sharedAppName;

        private String shareMsgTitle;

        private List<ShareMsgDataVo> shareMsgList;
    }

    @Data
    public static class ShareMsgDataVo {
        /**
         * 文件，可选
         */
        private String time;

        private String from;

        private String type;
        private String data;

        private String officialname;

        private String url;

        private String avatar;
    }

    @Data
    public static class OfficialAccountsCodeData {
        /**
         * 文件，可选
         */
        private String numCode;

        private String label;

        private String avatar;
    }

    @Data
    public static class ShareConfCardData {
        /**
         * 文件，可选
         */
        private String id;

        private String externalId;

        private String name;
        private Integer confType;

        private String scheduleStartTime;

        private String chairmanPassword;

        private String guestPassword;

        private String accesscode;

        private String timeZoneId;

        private String timeZoneName;

        private String chairman;

        private String chairmanName;

        private String controlPassword;
    }

    @Data
    public static class CustomCardVo {

        private String emergencyLevel;

        private String thumb;
        private String title;
        private String describe;
        private String url;
    }

    @Data
    public static class SelfDefineCardVo {
        private String msgType;
        private String selfDefineType;
        private String callee;
        private String calleeName;
        private String department;
        private String deviceType;
        private String thumb;
        private String ptzControl;
        private String confirm;
        private String deleteState;
    }

    @Data
    public static class BCOfflineMsgVo {
        /**
         * 文件，可选
         */
        private String cardType;

        private UserCardVo userCardData;

        private OfficialAccountsVo officialAccountsData;

        private SharedMsgVo sharedMsgData;

        private OfficialAccountsCodeData officialAccountsCodeData;

        private ShareConfCardData shareConfCardData;

        private CustomCardVo customCardData;

        private SelfDefineCardVo selfDefineCard;
    }

    @Data   // lombok，省去 getter/setter
    public static class Property {
        private String propertyName;
        private String propertyType;
        private String propertyValue;
    }

    @Data
    public static class GroupNoteMsgVo {
        /**
         * 文件，可选
         */
        private String groupNoteId;

        private Integer type;

        private Long expire;
        private String creationTime;

        private String creator;

        private String text;

        private String example;

        private List<UserTxts> userTxts;
    }

    @Data
    public static class ForwardMsg {
        private String from;
        private String fromIsdn;
        private Integer msgType;
        private String text;
        private Integer fileType;
        private String fileName;
        private Long fileKey;
        private Long fileSize;
        private String imageSize;
        private String duration;
        private Long lon;
        private Long lat;
        private Long alt;
        private String locDesc;
        private String userId;
        private String nickName;
        private List<UserTxts> userTxts;
        private String filePath;
        private Long fromRealUserId;
        private String to;
        private Long fromIdType;
        private Long toIdType;
        private Integer category;
        private Long seq;
        private Long sessionSeqId;
        private Long time;
        private String thumbnail;
        private String thumbnailSize;
        private boolean selfDestruct;
        private String videoThumb;
        private String cardType;
        private Object data;
        private String userTxt;
        private UserCardVo userCardData;

        private OfficialAccountsVo officialAccountsData;

        private SharedMsgVo sharedMsgData;

        private OfficialAccountsCodeData officialAccountsCodeData;

        private ShareConfCardData shareConfCardData;

        private CustomCardVo customCardData;
    }

    @Data
    public static class UserTxts {
        private String timeStamp;
        private String userId;
        private String txt;
    }
}
