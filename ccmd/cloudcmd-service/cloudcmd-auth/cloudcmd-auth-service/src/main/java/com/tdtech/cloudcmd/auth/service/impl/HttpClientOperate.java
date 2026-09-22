package com.tdtech.cloudcmd.auth.service.impl;

import com.tdtech.cloudcmd.auth.entity.HttpDeleteWithBody;
import com.tdtech.cloudcmd.util.http.HttpResult;
import com.tdtech.cloudcmd.util.http.SslUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;


/**
 * @author zWX446107
 */
@Slf4j
@Component
public class HttpClientOperate implements BeanFactoryAware {
    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    /**
     * 将required设置为false:
     * 为了避免RequestConfig没被注进来的时候其他方法都不能用,报createbeanfailedexception
     */
    @Autowired(required = false)
    private RequestConfig requestConfig;

    private CloseableHttpClient getHttpClient() {
        return this.beanFactory.getBean(CloseableHttpClient.class);
    }

    /**
     * 无参get请求
     *
     * @param url
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     * @autho
     * @time 2017年5月8日 下午3:30:08
     */
    public HttpResult doGet(String url, Map<String,String> headers) throws ClientProtocolException, IOException {
//        url = URLDecoder.decode(url,"UTF-8");
        //log.info("get RequestUrl:{}",url);
        // 创建http GET请求
        HttpGet httpGet = new HttpGet(url);
        // 设置请求参数
        httpGet.setConfig(requestConfig);

        if (headers != null) {
            for (String key : headers.keySet()) {
                httpGet.setHeader(key, headers.get(key));
            }
        }
        return this.rspClient(httpGet);
    }

    private HttpResult rspClient(HttpUriRequest httpMethod) throws IOException {

        try (CloseableHttpClient client = SslUtils.sslHttpClientBuilder().build();
             CloseableHttpResponse response = client.execute(httpMethod)) {
            // 执行请求
            Map<String, String> responseHeaders = new HashMap<>(response.getAllHeaders().length);
            for (Header header : response.getAllHeaders()) {
                responseHeaders.put(header.getName(), header.getValue());
            }

            return new HttpResult(response.getStatusLine().getStatusCode(), responseHeaders,
                    EntityUtils.toString(response.getEntity(), "UTF-8"));
        }
    }
    /**
     * 有参get请求
     *
     * @param url
     * @return
     * @throws URISyntaxException
     * @throws IOException
     * @throws ClientProtocolException
     */
    public HttpResult doGet(String url, Map<String,String> headers, Map<String, String> params)
            throws URISyntaxException, ClientProtocolException, IOException {
        URIBuilder uriBuilder = new URIBuilder(url);
        if (params != null) {
            for (String key : params.keySet()) {
                if(StringUtils.isNotEmpty(key)){
                    uriBuilder.setParameter(key, URLDecoder.decode(params.get(key),"UTF-8"));
                }
            }
        }
        return this.doGet(uriBuilder.build().toString(),headers);
    }





    public HttpResult doPost(String url) throws ClientProtocolException, IOException {
        return doPost(url,null);
    }

    public HttpResult doPost(String url, Map<String, String> params) throws ClientProtocolException, IOException {
        return doPost(url,null,params);
    }

    /**
     * 有参post请求
     *
     * @param url
     * @param params
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     * @autho
     * @time 2017年5月8日 下午3:32:48
     */
    public HttpResult doPost(String url, Map<String,String> headers, Map<String, String> params) throws ClientProtocolException, IOException {
        // 创建http POST请求
        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(requestConfig);

        if (headers != null) {
            for (String key : headers.keySet()) {
                httpPost.setHeader(key, headers.get(key));
            }
        }

        if (params != null) {
            // 设置2个post参数，一个是scope、一个是q
            List<NameValuePair> parameters = new ArrayList<NameValuePair>(0);
            for (String key : params.keySet()) {
                parameters.add(new BasicNameValuePair(key, params.get(key)));
            }
            // 构造一个form表单式的实体
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters);
            // 将请求实体设置到httpPost对象中
            httpPost.setEntity(formEntity);
        }

        return this.rspClient(httpPost);
    }

    /**
     * 有参post请求,json交互
     *
     * @param url
     * @param body
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     * @autho
     * @time 2017年5月8日 下午3:33:01
     */
    public HttpResult doPostJson(String url, Map<String,String> headers, String body) throws ClientProtocolException, IOException {
        // 创建http POST请求
        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(requestConfig);

        if (headers != null) {
            for (String key : headers.keySet()) {
                httpPost.addHeader(key, headers.get(key));
            }
        }

        if (StringUtils.isNotBlank(body)) {
            // 标识出传递的参数是 application/json
            StringEntity stringEntity = new StringEntity(body, ContentType.APPLICATION_JSON);
            httpPost.setEntity(stringEntity);
        }
        return this.rspClient(httpPost);
    }

    /**
     * 有参post请求,json交互
     *
     * @param url
     * @param body
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     * @autho
     * @time 2017年5月8日 下午3:33:01
     */
    public HttpResult doPostJson(String url, String body) throws ClientProtocolException, IOException {
        return doPostJson(url,null, body);
    }
    /**
     * 有参post请求,text/plain,application/xml交互
     *
     * @param url
     * @param
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     * @autho
     * @time 2017年5月8日 下午3:33:01
     */
    public HttpResult doPostXml(String url, Map<String,String> headers, String body) throws ClientProtocolException, IOException {
        // 创建http POST请求
        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(requestConfig);

        if (headers != null) {
            for (String key : headers.keySet()) {
                httpPost.setHeader(key, headers.get(key));
            }
        }

        if (StringUtils.isNotBlank(body)) {
            StringEntity stringEntity = new StringEntity(body, ContentType.create("application/xml", Consts.UTF_8));
            httpPost.setEntity(stringEntity);
        }

        try (final CloseableHttpClient httpClient = SslUtils.sslHttpClientBuilder().build();
             CloseableHttpResponse response = httpClient.execute(httpPost)) {
            // 执行请求

            return new HttpResult(response.getStatusLine().getStatusCode(),
                    EntityUtils.toString(response.getEntity(), "UTF-8"));
        }
    }



    /**
     * 有参post请求,json交互
     *
     * @param url
     * @param json
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     * @autho
     * @time 2017年5月8日 下午3:33:01
     */
    public HttpResult doPostJsonRetCookies(String url, String json) throws ClientProtocolException, IOException {
        CookieStore cookieStore = new BasicCookieStore();

        // 创建http POST请求
        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(requestConfig);

        if (StringUtils.isNotBlank(json)) {
            // 标识出传递的参数是 application/json
            StringEntity stringEntity = new StringEntity(json, ContentType.APPLICATION_JSON);
            httpPost.setEntity(stringEntity);
        }

        try (final CloseableHttpClient httpClient = SslUtils.sslHttpClientBuilder().setDefaultCookieStore(cookieStore).build();
             CloseableHttpResponse response = httpClient.execute(httpPost)) {
            // 执行请求

            // 获取cookies信息
            List<Cookie> cookies = cookieStore.getCookies();
            Map<String, String> cookieMap = new HashMap<>(cookies.size());
            for (Cookie cookie : cookies) {
                cookieMap.put(cookie.getName(), cookie.getValue());
                //log.debug("cookies: key= " + cookie.getName() + "  value= " + cookie.getValue());
            }
            // 判断返回状态是否为200
            return new HttpResult(response.getStatusLine().getStatusCode(), null, cookieMap,
                    EntityUtils.toString(response.getEntity(), "UTF-8"));
        }
        // httpclient.close();
    }

    /**
     * 有参post请求,text/plain,application/xml交互  专用于登陆返回
     *
     * @param url
     * @param
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     * @autho
     * @time 2017年5月8日 下午3:33:01
     */
    public HttpResult doPostRetCookies(String url, Map<String, String> params) throws ClientProtocolException, IOException {
        CookieStore cookieStore = new BasicCookieStore();

        // 创建http POST请求
        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(requestConfig);

        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");

        if (params != null) {
            // 设置2个post参数，一个是scope、一个是q
            List<NameValuePair> parameters = new ArrayList<NameValuePair>(0);
            for (String key : params.keySet()) {
                parameters.add(new BasicNameValuePair(key, params.get(key)));
            }
            // 构造一个form表单式的实体
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters);
            // 将请求实体设置到httpPost对象中
            httpPost.setEntity(formEntity);
        }

        try(CloseableHttpClient httpClient = SslUtils.sslHttpClientBuilder().setDefaultCookieStore(cookieStore).build();
            CloseableHttpResponse response = httpClient.execute(httpPost)) {
            // 执行请求

            // 获取cookies信息
            List<Cookie> cookies = cookieStore.getCookies();
            Map<String,String> cookieMap = new HashMap<>(cookies.size());
            for (Cookie cookie : cookies) {
                cookieMap.put(cookie.getName(),cookie.getValue());
                //log.debug("cookies: key= "+ cookie.getName() + "  value= " + cookie.getValue());
            }
            // 判断返回状态是否为200
            return new HttpResult(response.getStatusLine().getStatusCode(),null, cookieMap,
                    EntityUtils.toString(response.getEntity(), "UTF-8"));
        }
    }


    /**
     * 有参put请求,json交互
     *
     * @param url
     * @param body
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     */
    public HttpResult doPutJson(String url, Map<String, String> headers, String body) throws ClientProtocolException, IOException {
        // 创建HttpPut请求
        HttpPut httpPut = new HttpPut(url);
        httpPut.setConfig(requestConfig);

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpPut.setHeader(entry.getKey(), entry.getValue());
            }
        }

        if (StringUtils.isNotBlank(body)) {
            // 标识出传递的参数是 application/json
            StringEntity stringEntity = new StringEntity(body, ContentType.APPLICATION_JSON);
            httpPut.setEntity(stringEntity);
        }
        return this.rspClient(httpPut);
    }

    public HttpResult doDelete(String url, Map<String,String> headers) throws ClientProtocolException, IOException {
        // 创建http POST请求
        HttpDelete httpDelete = new HttpDelete(url);
        if (Objects.nonNull(headers)) {
            for (String key : headers.keySet()) {
                httpDelete.setHeader(key, headers.get(key));
            }
        }

        try (final CloseableHttpClient httpClient = SslUtils.sslHttpClientBuilder().build();
             CloseableHttpResponse response = httpClient.execute(httpDelete)) {
            // 执行请求
            return new HttpResult(response.getStatusLine().getStatusCode(),
                    EntityUtils.toString(response.getEntity(), "UTF-8"));
        }
    }

    public HttpResult doDelete(String url, Map<String,String> headers, Map<String,String> params) throws ClientProtocolException, IOException{
        // 创建http POST请求
        HttpDeleteWithBody httpDelete = new HttpDeleteWithBody(url);
        httpDelete.setConfig(requestConfig);

        if (headers != null) {
            for (String key : headers.keySet()) {
                httpDelete.addHeader(key, headers.get(key));
            }
        }

        if (params != null) {
            // 设置2个post参数，一个是scope、一个是q
            List<NameValuePair> parameters = new ArrayList<NameValuePair>(0);
            for (String key : params.keySet()) {
                parameters.add(new BasicNameValuePair(key, params.get(key)));
            }
            // 构造一个form表单式的实体
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters);
            // 将请求实体设置到httpPost对象中
            httpDelete.setEntity(formEntity);
        }

        return this.rspClient(httpDelete);

    }

    /**
     * 有参httypdelete请求,text/plain,application/xml交互
     *
     * @param url
     * @param
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     * @autho
     * @time 2017年5月8日 下午3:33:01
     */
    public HttpResult doDeleteXml(String url, Map<String,String> headers, String body) throws ClientProtocolException, IOException {
        // 创建http POST请求
        HttpDeleteWithBody httpDelete = new HttpDeleteWithBody(url);
        httpDelete.setConfig(requestConfig);

        if (headers != null) {
            for (String key : headers.keySet()) {
                httpDelete.setHeader(key, headers.get(key));
            }
        }

        if (StringUtils.isNotBlank(body)) {
            // 标识出传递的参数是 application/json
            StringEntity stringEntity = new StringEntity(body, ContentType.create("application/xml", Consts.UTF_8));
            httpDelete.setEntity(stringEntity);
        }
        try (CloseableHttpClient httpClient = SslUtils.sslHttpClientBuilder().build();
             CloseableHttpResponse response = httpClient.execute(httpDelete)) {
            // 执行请求
            return new HttpResult(response.getStatusLine().getStatusCode(),
                    EntityUtils.toString(response.getEntity(), "UTF-8"));
        }
    }

    /**
     * 不显示的使用HttpPost就会默认提交请求的方式为post, 传递的参数为以key:value的形式来传递参数
     *
     * @param httpUrl
     * @param picStream 可以传递图片等文件,以字节数组的形式传递
     * @throws IOException
     */
    @SuppressWarnings({"deprecation"})
    public HttpResult doPost(String httpUrl, String alarmId, byte[] picStream, String imageName) throws IOException {
        HttpPost httpPost = new HttpPost(httpUrl);

        // 传递图片的时候可以通过此处上传image.jpg随便给出即可
        ByteArrayBody image = new ByteArrayBody(picStream, imageName);

        HttpEntity reqEntity = MultipartEntityBuilder.create()
                .addPart("filename",image)
                .addPart("alarmid", new StringBody(alarmId, ContentType.create("text/plain", Consts.UTF_8)))
                .build();

        httpPost.setEntity(reqEntity);
        try(final CloseableHttpClient httpClient = SslUtils.sslHttpClientBuilder().build();

            CloseableHttpResponse response =httpClient.execute(httpPost)) {
            // 执行请求
            return new HttpResult(response.getStatusLine().getStatusCode(),
                    EntityUtils.toString(response.getEntity(), "UTF-8"));
        }
    }

    /**
     * 不显示的使用HttpPost就会默认提交请求的方式为post, 传递的参数为以key:value的形式来传递参数
     *
     * @param httpUrl
     * @param multipartFiles 可以传递图片等文件,以字节数组的形式传递
     * @throws IOException
     */
    @SuppressWarnings({"deprecation"})
    public HttpResult doPost(String httpUrl, String body, List<MultipartFile> multipartFiles, String token) throws IOException {
        HttpPost httpPost = new HttpPost(httpUrl);

        // 传递图片的时候可以通过此处上传image.jpg随便给出即可

        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        List<File> files =transferToFile(multipartFiles);
        Charset charset = StandardCharsets.UTF_8;
        multipartEntityBuilder.setCharset(charset);
        List<NameValuePair> parameters = new ArrayList<NameValuePair>(0);
        parameters.add(new BasicNameValuePair("body", body));
        multipartEntityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        if(files.size()>1) {
            multipartEntityBuilder.addPart("files", new FileBody(files.get(0), ContentType.APPLICATION_OCTET_STREAM))
                    .addPart("files", new FileBody(files.get(1), ContentType.APPLICATION_OCTET_STREAM))
                    .addPart("body", new StringBody(body, ContentType.create("text/plain", Consts.UTF_8)));
        }else {
            multipartEntityBuilder.addPart("files", new FileBody(files.get(0), ContentType.APPLICATION_OCTET_STREAM))
                    .addPart("body", new StringBody(body, ContentType.create("text/plain", Consts.UTF_8)));
        }
        HttpEntity fileEntity = multipartEntityBuilder.build();

        httpPost.setEntity(fileEntity);
        BasicCookieStore cookie = new BasicCookieStore();
        BasicClientCookie jsessionid = new BasicClientCookie("JSESSIONID", token);
        cookie.addCookie(jsessionid);
        try (final CloseableHttpClient httpClient = SslUtils.sslHttpClientBuilder().setDefaultCookieStore(cookie).build();
             CloseableHttpResponse response = httpClient.execute(httpPost)) {
            // 执行请求
            return new HttpResult(response.getStatusLine().getStatusCode(),
                    EntityUtils.toString(response.getEntity(), "UTF-8"));

        }
    }

    private List<File> transferToFile(List<MultipartFile> multipartFiles) {
//        选择用缓冲区来实现这个转换即使用java 创建的临时文件 使用 MultipartFile.transferto()方法 。
        List<File> files = new ArrayList<>();
        for (MultipartFile multipartFile : multipartFiles) {

            File file = null;
            try {
                String originalFilename = multipartFile.getOriginalFilename();
                log.info("the OriginalFilename is {}",originalFilename);
                file = new File(originalFilename);
                multipartFile.transferTo(file);
                file.deleteOnExit();
            } catch (IOException e) {
                log.error("transfer error {}", e.getMessage());
            }
            files.add(file);
        }
        return files;
    }

}
