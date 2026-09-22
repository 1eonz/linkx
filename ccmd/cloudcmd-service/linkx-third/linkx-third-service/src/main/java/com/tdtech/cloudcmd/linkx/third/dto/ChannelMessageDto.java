package com.tdtech.cloudcmd.linkx.third.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class ChannelMessageDto {

    private Long notificationId;

    private String url;

    private String text;

    private Long contactId;

    private Long receivedUserId;

    private String data;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date receivedAt;
}
