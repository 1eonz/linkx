package com.tdtech.cloudcmd.web.filter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RequestWrapper extends HttpServletRequestWrapper {
    /**
     * 请求体
     */
    private final String mBody;

    public RequestWrapper(HttpServletRequest request) {
        super(request);
        // 将body数据存储起来
        mBody = getBody(request);
    }

    /**
     * 获取请求体
     * 
     * @param request 请求
     * @return 请求体
     */
    private String getBody(HttpServletRequest request) {
        try (InputStream is = request.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr)) {
            StringBuffer sb = new StringBuffer();
            String s;
            while ((s = br.readLine()) != null) {
                sb.append(s);
            }
            return sb.toString();
        } catch (IOException e) {
            log.info("IOException:", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取请求体
     * 
     * @return 请求体
     */
    public String getBody() {
        return mBody;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        // 创建字节数组输入流
        final ByteArrayInputStream bais = new ByteArrayInputStream(mBody.getBytes(Charset.defaultCharset()));

        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {

            }

            @Override
            public int read() throws IOException {
                return bais.read();
            }
        };
    }
}