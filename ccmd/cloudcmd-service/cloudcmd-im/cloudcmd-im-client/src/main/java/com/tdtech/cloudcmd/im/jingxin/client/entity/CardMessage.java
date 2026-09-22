package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CardMessage {

    private String cardType = "customCard";
    private Data data;

    @lombok.Data
    @Accessors(chain = true)
    public static class Data {
        private Integer commonCardType = 2;
        private Integer cardType =2;
        private String type;
        private String id;
        private String name;
        private String title;
        private String text;
        private String url;
        private String file;
        private String image;
        private String extension;
        private String thumb;
        private String param;
    }

}
