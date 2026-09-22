package com.tdtech.cloudcmd.auth.entity;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

import java.net.URI;

/**
 * @author zWX446107
 * @Description HttpDelete 使用 body 传递参数
 * @create 2020-06-02 17:05
 */
public class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {
    public static final String METHOD_NAME = "DELETE";

    @Override
    public String getMethod() { return METHOD_NAME; }

    public HttpDeleteWithBody(final String uri) {
        super();
        setURI(URI.create(uri));
    }
    public HttpDeleteWithBody(final URI uri) {
        super();
        setURI(uri);
    }
    public HttpDeleteWithBody() { super(); }
}
