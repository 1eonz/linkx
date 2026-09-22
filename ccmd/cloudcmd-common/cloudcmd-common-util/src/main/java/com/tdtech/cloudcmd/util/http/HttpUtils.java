package com.tdtech.cloudcmd.util.http;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;

/**
 * @author zhuangzl
 * @date 2020-02-03 17:13
 */
@Slf4j
@Deprecated
public class HttpUtils {

    // 默认超时20s
    public static final int DEFALUT_TIMEOUT = 20000;
    public static final int MIN_TIMEOUT = 2000;
    private static HttpUtils httpUtils;
    /**
     * cookie表
     */
    private final Map<String, String> cookieMap = new HashMap<>();
    /**
     * 请求超时时间,默认20000ms
     */
    private int timeout = 20000;
    /**
     * 等待异步JS执行时间,默认20000ms
     */
    private int waitForBackgroundJavaScript = 20000;
    /**
     * 请求编码(处理返回结果)，默认UTF-8
     */
    private String charset = "UTF-8";

    private HttpUtils() {}

    /**
     * 获取实例
     *
     * @return
     */
    public static HttpUtils getInstance() {
        if (httpUtils == null)
            httpUtils = new HttpUtils();
        return httpUtils;
    }

    /**
     * 创建 SSL连接
     *
     * @return
     * @throws GeneralSecurityException
     */
    private static CloseableHttpClient createSSLInsecureClient() throws GeneralSecurityException {
        try {
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, (chain, authType) -> true).build();
            SSLConnectionSocketFactory sslConnectionSocketFactory =
                new SSLConnectionSocketFactory(sslContext, (s, sslContextL) -> true);
            return HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory).build();
        } catch (GeneralSecurityException e) {
            throw e;
        }
    }

    /**
     * 清空cookieMap
     */
    public void invalidCookieMap() {
        cookieMap.clear();
    }

    public int getTimeout() {
        return timeout;
    }

    /**
     * 设置请求超时时间
     *
     * @param timeout
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getCharset() {
        return charset;
    }

    /**
     * 设置请求字符编码集
     *
     * @param charset
     */
    public void setCharset(String charset) {
        this.charset = charset;
    }

    public int getWaitForBackgroundJavaScript() {
        return waitForBackgroundJavaScript;
    }

    /**
     * 设置获取完整HTML页面时等待异步JS执行的时间
     *
     * @param waitForBackgroundJavaScript
     */
    public void setWaitForBackgroundJavaScript(int waitForBackgroundJavaScript) {
        this.waitForBackgroundJavaScript = waitForBackgroundJavaScript;
    }

    /**
     * 执行get请求
     *
     * @param url
     * @return
     * @throws Exception
     */
    public String executeGet(String url) throws Exception {
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Cookie", convertCookieMapToString(cookieMap));
        httpGet.setConfig(RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout).build());
        CloseableHttpClient httpClient = null;
        String str = "";
        try {
            httpClient = HttpClientBuilder.create().build();
            HttpClientContext context = HttpClientContext.create();
            CloseableHttpResponse response = httpClient.execute(httpGet, context);
            getCookiesFromCookieStore(context.getCookieStore(), cookieMap);
            int state = response.getStatusLine().getStatusCode();
            if (state == 404) {
                str = "";
            }
            try {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    str = EntityUtils.toString(entity, charset);
                }
            } finally {
                response.close();
            }
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (httpClient != null) {
                    httpClient.close();
                }
            } catch (IOException e) {
                log.error("Failed to close HttpClient", e);
            }
        }
        return str;
    }

    public Integer executeGetForStatusCodeWithSSL(String url) throws Exception {
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Cookie", convertCookieMapToString(cookieMap));
        httpGet.setConfig(RequestConfig.custom().setSocketTimeout(500).setConnectTimeout(500).build());
        try (CloseableHttpClient httpClient = createSSLInsecureClient()) {
            HttpClientContext context = HttpClientContext.create();
            CloseableHttpResponse response = httpClient.execute(httpGet, context);
            getCookiesFromCookieStore(context.getCookieStore(), cookieMap);
            return response.getStatusLine().getStatusCode();
        }
    }

    public Integer executeGetForStatusCode(String url) throws Exception {
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Cookie", convertCookieMapToString(cookieMap));
        httpGet.setConfig(RequestConfig.custom().setSocketTimeout(500).setConnectTimeout(500).build());
        CloseableHttpClient httpClient = null;
        try {
            httpClient = HttpClientBuilder.create().build();
            HttpClientContext context = HttpClientContext.create();
            CloseableHttpResponse response = httpClient.execute(httpGet, context);
            getCookiesFromCookieStore(context.getCookieStore(), cookieMap);
            return response.getStatusLine().getStatusCode();
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (httpClient != null) {
                    httpClient.close();
                }
            } catch (IOException e) {
                log.error("Failed to close HttpClient", e);
            }
        }
    }

    /**
     * 用https执行get请求
     *
     * @param url
     * @return
     * @throws Exception
     */
    public String executeGetWithSSL(String url) throws Exception {
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Cookie", convertCookieMapToString(cookieMap));
        httpGet.setConfig(RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout).build());
        CloseableHttpClient httpClient = null;
        String str = "";
        try {
            httpClient = createSSLInsecureClient();
            HttpClientContext context = HttpClientContext.create();
            CloseableHttpResponse response = httpClient.execute(httpGet, context);
            getCookiesFromCookieStore(context.getCookieStore(), cookieMap);
            int state = response.getStatusLine().getStatusCode();
            if (state == 404) {
                str = "";
            }
            try {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    str = EntityUtils.toString(entity, charset);
                }
            } finally {
                response.close();
            }
        } catch (IOException e) {
            throw e;
        } catch (GeneralSecurityException ex) {
            throw ex;
        } finally {
            try {
                if (httpClient != null) {
                    httpClient.close();
                }
            } catch (IOException e) {
                log.error("Failed to close HttpClient", e);
            }
        }
        return str;
    }

    /**
     * 执行post请求
     *
     * @param url
     * @param params
     * @return
     * @throws Exception
     */
    public String executePost(String url, JSONObject params) throws Exception {
        String reStr = "";
        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout).build());
        httpPost.setHeader("Cookie", convertCookieMapToString(cookieMap));
        CloseableHttpClient httpclient = HttpClientBuilder.create().build();
        CloseableHttpResponse response = null;
        try {
            HttpEntity httpEntity = new StringEntity(params.toJSONString());
            httpPost.setEntity(httpEntity);
            HttpClientContext context = HttpClientContext.create();
            response = httpclient.execute(httpPost, context);
            getCookiesFromCookieStore(context.getCookieStore(), cookieMap);
            HttpEntity entity = response.getEntity();
            reStr = EntityUtils.toString(entity, charset);
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (response != null) {
                    response.close();
                }

                if (httpclient != null) {
                    httpclient.close();
                }
                httpPost.releaseConnection();

            } catch (Exception e) {
                log.error("Failed to close resources in executePost", e);
            }
        }
        return reStr;
    }

    /**
     * 执行post请求
     *
     * @param url
     * @param params
     * @return
     * @throws Exception
     */
    public String executePost(String url, Map<String, String> params) throws Exception {
        String reStr = "";
        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout).build());
        httpPost.setHeader("Cookie", convertCookieMapToString(cookieMap));
        List<NameValuePair> paramsRe = new ArrayList<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            paramsRe.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        CloseableHttpClient httpclient = null;
        httpclient = HttpClientBuilder.create().build();
        CloseableHttpResponse response = null;
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(paramsRe));
            HttpClientContext context = HttpClientContext.create();
            response = httpclient.execute(httpPost, context);
            getCookiesFromCookieStore(context.getCookieStore(), cookieMap);
            HttpEntity entity = response.getEntity();
            reStr = EntityUtils.toString(entity, charset);
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                httpPost.releaseConnection();
                IOUtils.closeQuietly(httpclient);
                IOUtils.closeQuietly(response);
            } catch (Exception e) {
                log.error("Failed to close resources in executePost", e);
            }
        }
        return reStr;
    }

    /**
     * 用https执行post请求
     *
     * @param url
     * @param params
     * @return
     * @throws Exception
     */
    public String executePostWithSSL(String url, Map<String, String> params) throws Exception {
        String re = "";
        HttpPost post = new HttpPost(url);
        List<NameValuePair> paramsRe = new ArrayList<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            paramsRe.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        post.setHeader("Cookie", convertCookieMapToString(cookieMap));
        post.setConfig(RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout).build());
        CloseableHttpResponse response = null;
        CloseableHttpClient httpClientRe = null;
        try {
            httpClientRe = createSSLInsecureClient();
            HttpClientContext contextRe = HttpClientContext.create();
            post.setEntity(new UrlEncodedFormEntity(paramsRe));
            // 扫描原因未知
            response = httpClientRe.execute(post, contextRe);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                re = EntityUtils.toString(entity, charset);
            }
            getCookiesFromCookieStore(contextRe.getCookieStore(), cookieMap);
        } catch (Exception e) {
            throw e;
        } finally {
            IOUtils.closeQuietly(response);
            IOUtils.closeQuietly(httpClientRe);
        }
        return re;
    }

    /**
     * 发送JSON格式body的POST请求
     *
     * @param url 地址
     * @param jsonBody json body
     * @return
     * @throws Exception
     */
    public String executePostWithJson(String url, String jsonBody) throws Exception {
        String reStr = "";
        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout).build());
        httpPost.setHeader("Cookie", convertCookieMapToString(cookieMap));
        CloseableHttpClient httpclient = HttpClientBuilder.create().build();
        CloseableHttpResponse response = null;
        try {
            httpPost.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
            HttpClientContext context = HttpClientContext.create();
            response = httpclient.execute(httpPost, context);
            getCookiesFromCookieStore(context.getCookieStore(), cookieMap);
            HttpEntity entity = response.getEntity();
            reStr = EntityUtils.toString(entity, charset);
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                IOUtils.closeQuietly(response);
                IOUtils.closeQuietly(httpclient);

                httpPost.releaseConnection();
            } catch (Exception e) {
                log.error("Failed to close resources in executePostWithJson", e);
            }
        }
        return reStr;
    }

    /**
     * 发送JSON格式body的SSL POST请求
     *
     * @param url 地址
     * @param jsonBody json body
     * @return
     * @throws Exception
     */
    public String executePostWithJsonAndSSL(String url, String jsonBody) throws Exception {
        String re = "";
        HttpPost post = new HttpPost(url);
        post.setHeader("Cookie", convertCookieMapToString(cookieMap));
        post.setConfig(RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout).build());
        CloseableHttpResponse response = null;
        CloseableHttpClient httpClientRe = null;
        try {
            httpClientRe = createSSLInsecureClient();
            HttpClientContext contextRe = HttpClientContext.create();
            post.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
            response = httpClientRe.execute(post, contextRe);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                re = EntityUtils.toString(entity, charset);
            }
            getCookiesFromCookieStore(contextRe.getCookieStore(), cookieMap);
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                post.releaseConnection();
                IOUtils.closeQuietly(response);
                IOUtils.closeQuietly(httpClientRe);
            } catch (Exception e) {
                log.error("Failed to close resources in executePostWithJsonAndSSL", e);
            }

        }
        return re;
    }

    private void getCookiesFromCookieStore(CookieStore cookieStore, Map<String, String> cookieMap) {
        List<Cookie> cookies = cookieStore.getCookies();
        for (Cookie cookie : cookies) {
            cookieMap.put(cookie.getName(), cookie.getValue());
        }
    }

    private String convertCookieMapToString(Map<String, String> map) {
        String cookie = "";
        for (Map.Entry<String, String> entry : map.entrySet()) {
            cookie += (entry.getKey() + "=" + entry.getValue() + "; ");
        }
        if (map.size() > 0) {
            cookie = cookie.substring(0, cookie.length() - 2);
        }
        return cookie;
    }

    public HttpResult doPostWithBodyCheck(String url, Map<String, String> headers, String jsonBody)
        throws IOException, GeneralSecurityException {
        CloseableHttpResponse response = null;
        HttpPost httpPost = new HttpPost(url);
        if (headers != null) {
            for (String key : headers.keySet()) {
                httpPost.setHeader(key, headers.get(key));
            }
        }
        httpPost.setHeader("Content-Type", "application/json;charset=utf-8");
        httpPost.setConfig(RequestConfig.custom().setSocketTimeout(500).setConnectTimeout(500).build());
        httpPost.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
        CloseableHttpClient httpClientRe = null;
        try {
            /** 执行请求 */
            httpClientRe = createSSLInsecureClient();
            response = httpClientRe.execute(httpPost);
            Map<String, String> responseHeaders = new HashMap<>(response.getAllHeaders().length);
            for (Header header : response.getAllHeaders()) {
                responseHeaders.put(header.getName(), header.getValue());
            }
            return new HttpResult(response.getStatusLine().getStatusCode(), responseHeaders,
                EntityUtils.toString(response.getEntity(), "UTF-8"));
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                httpPost.releaseConnection();
                if (httpClientRe != null) {
                    httpClientRe.close();
                }
                IOUtils.closeQuietly(response);
            } catch (Exception e) {
                log.error("Failed to close resources in doPostWithBodyCheck", e);
            }
        }
    }

    public HttpResult doPostWithBody(String url, Map<String, String> headers, String jsonBody)
        throws ClientProtocolException, IOException, GeneralSecurityException {
        CloseableHttpResponse response = null;
        HttpPost httpPost = new HttpPost(url);

        if (headers != null) {
            for (String key : headers.keySet()) {
                httpPost.setHeader(key, headers.get(key));
            }
        }
        httpPost.setHeader("Content-Type", "application/json;charset=utf-8");
        // httpPost.setHeader("Cookie", convertCookieMapToString(cookieMap));
        httpPost.setConfig(RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout).build());
        httpPost.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
        CloseableHttpClient httpClientRe = null;
        CloseableHttpClient client = null;
        try {
            httpClientRe = createSSLInsecureClient();
            client = SslUtils.sslHttpClientBuilder().build();
            /** 执行请求 */
            response = client.execute(httpPost);
            // response = httpClientRe.execute(httpPost, contextRe);

            Map<String, String> responseHeaders = new HashMap<>(response.getAllHeaders().length);
            for (Header header : response.getAllHeaders()) {
                responseHeaders.put(header.getName(), header.getValue());
            }

            return new HttpResult(response.getStatusLine().getStatusCode(), responseHeaders,
                EntityUtils.toString(response.getEntity(), "UTF-8"));
        } catch (Exception e) {
            throw e;
        } finally {
            if (httpClientRe != null) {
                httpClientRe.close();
            }
            if (response != null) {
                response.close();
            }
            if (client != null) {
                client.close();
            }
            httpPost.releaseConnection();
        }
    }

    public HttpResult doPostTest(String url, Map<String, String> headers) throws ClientProtocolException, IOException {
        // 创建http POST请求
        HttpPost httpPost = new HttpPost(url);
        // httpPost.setConfig(requestConfig);
        httpPost.setConfig(RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout).build());
        if (headers != null) {
            for (String key : headers.keySet()) {
                httpPost.setHeader(key, headers.get(key));
            }
        }
        httpPost.setEntity(new StringEntity("{}", ContentType.APPLICATION_JSON));

        CloseableHttpResponse response = null;
        CloseableHttpClient client = null;

        try {
            client = SslUtils.sslHttpClientBuilder().build();
            // 执行请求
            response = client.execute(httpPost);

            Map<String, String> responseHeaders = new HashMap<>(response.getAllHeaders().length);
            for (Header header : response.getAllHeaders()) {
                responseHeaders.put(header.getName(), header.getValue());
            }

            return new HttpResult(response.getStatusLine().getStatusCode(), responseHeaders,
                EntityUtils.toString(response.getEntity(), "UTF-8"));
        } finally {
            if (response != null) {
                response.close();
            }
            if (client != null) {
                client.close();
            }
            // httpclient.close();
        }
    }
}