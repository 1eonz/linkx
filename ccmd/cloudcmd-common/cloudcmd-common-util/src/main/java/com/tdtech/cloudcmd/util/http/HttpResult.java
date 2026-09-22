package com.tdtech.cloudcmd.util.http;

import java.io.Serializable;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author swx181319
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
@Deprecated
public class HttpResult implements Serializable {
    public static final int RESULT_CODE_SUCCESS = 0;
    public static final int RESULT_LOGIN_CHALLENGE_OK = 100;
    private Integer statusCode;
    private Map<String, String> headers;
    private Map<String, String> cookies;
    private String content;

    public HttpResult(Integer statusCode, String content) {
        this.statusCode = statusCode;
        this.content = content;
    }

    public HttpResult(Integer statusCode, Map<String, String> headers, String content) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.content = content;
    }
}
