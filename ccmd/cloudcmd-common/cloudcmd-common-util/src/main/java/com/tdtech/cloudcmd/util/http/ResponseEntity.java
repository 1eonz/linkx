package com.tdtech.cloudcmd.util.http;

// import com.fasterxml.jackson.annotation.JsonInclude;
// import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.io.Serializable;

import lombok.Data;

// @JsonInclude(Include.NON_NULL)
@Data
@Deprecated
public class ResponseEntity implements Serializable {
    private int status;
    private long code;
    private String message;
    private String developerMessage;

    private String moreInfoUrl;
    private String throwable;
    private String capbility;

}
