package com.tdtech.cloudcmd.util.nodedispatch;

import com.tdtech.cloudcmd.util.json.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 节点数据转发客户端。
 * 供 cloudcmd-im-jingxin / linkx-dashboard-service 等业务微服务调用 linkx-node dispatch 接口。
 * 通过 K8s service name 内网直连 linkx-node-service，不走 APISIX。
 */
@Slf4j
public class NodeDispatchClient {

    /**
     * dispatch 接口路径模板：/node/v1/p2p/{peerId}/dispatch/{originURI}
     */
    private static final String DISPATCH_PATH = "/node/v1/p2p/%s/dispatch/%s";

    private final RestTemplate restTemplate;

    public NodeDispatchClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 调用本端 linkx-node 的 dispatch 接口，转发请求到对端节点，并将响应反序列化为指定类型。
     * 便捷方法，等价于 {@code dispatch(peerId, originUri, params)} + {@code JsonUtil.parseJson(body, clazz)}。
     *
     * @param peerId    目标节点 peerId
     * @param originUri 原始业务 URI（如 /collaboration/v1/post/queryDepartment）
     * @param params    业务参数（可为 null）
     * @param clazz     响应类型
     * @param <T>       响应泛型
     * @return 反序列化后的响应对象；dispatch 失败或反序列化失败返回 null
     */
    public <T> T dispatchAndParse(String peerId, String originUri, Map<String, ?> params, Class<T> clazz) {
        ResponseEntity<byte[]> response = dispatch(peerId, originUri, params);
        if (response == null || response.getBody() == null) {
            return null;
        }
        try {
            String body = new String(response.getBody(), StandardCharsets.UTF_8);
            return JsonUtil.parseJson(body, clazz);
        } catch (Exception e) {
            log.error("dispatchAndParse: parse failed, peerId={}, originUri={}", peerId, originUri, e);
            return null;
        }
    }

    /**
     * 调用本端 linkx-node 的 dispatch 接口，转发请求到对端节点，并将响应体以流式方式写入指定输出流。
     * 供跨节点大文件下载场景使用（如跨节点导出 Excel），与 {@link #dispatchAndParse} 的区别：
     * <ul>
     *   <li>{@code dispatchAndParse} 把响应当 JSON 解析成对象，适用于普通业务接口</li>
     *   <li>{@code dispatchAndDownload} 流式透传二进制响应体，不将整个文件加载进内存，适用于大文件下载</li>
     * </ul>
     *
     * <p>实现方式：用 {@code RestTemplate.execute} + {@code ResponseExtractor} 拿到对端 InputStream，
     * 边读边写到调用方的 OutputStream，避免大文件 OOM。
     *
     * <p>本方法不直接依赖 Servlet API（保持工具模块纯净），调用方需自行传入 OutputStream
     * （通常是 {@code response.getOutputStream()}）和响应头设置回调。
     *
     * <p>响应头处理约定：
     * <ul>
     *   <li>对端 2xx：先调用 {@code headerConsumer} 让调用方设置 Content-Type / Content-Disposition 等下载头，
     *       再流式写入 body。调用方在 headerConsumer 里能拿到对端的完整响应头。</li>
     *   <li>对端非 2xx 或网络异常：不向 output 写入任何字节（避免把错误信息当文件内容），
     *       返回 {@code DispatchedFile.fail(msg)}，调用方据 success 字段自行写错误响应</li>
     * </ul>
     *
     * <p>注意：流式写入开始后，output 对应的 response 即 committed，后续无法再改为错误响应。
     * 因此对端非 2xx 时本方法不会写 output，调用方仍可自由返回错误响应。
     *
     * @param peerId         目标节点 peerId
     * @param originUri      原始业务 URI（如 /dashboard/v1/static/export）
     * @param params         业务参数（可为 null）
     * @param output         输出流（通常是 HttpServletResponse.getOutputStream()），成功时写入文件内容
     * @param headerConsumer 响应头回调（可为 null），在写 body 前调用，传入对端响应头供调用方透传
     * @return 下载结果（success=true 表示已流式写入完成；success=false 表示失败，未写入 output）
     */
    public DispatchedFile dispatchAndDownload(String peerId, String originUri, Map<String, ?> params,
                                              OutputStream output, Consumer<HttpHeaders> headerConsumer) {
        URI uri = buildDispatchUri("linkx-node-service", peerId, originUri, params);
        log.info("NodeDispatchClient.dispatchAndDownload: peerId={}, originUri={}, params={}", peerId, originUri, params);
        try {
            return restTemplate.execute(uri, HttpMethod.GET, request -> {
                // GET 请求无 body，仅设置空 header 保持与 dispatch 一致
                request.getHeaders().addAll(new HttpHeaders());
            }, response -> {
                // 2xx 路径：先让调用方设置响应头，再流式写 body
                HttpHeaders headers = response.getHeaders();
                if (headerConsumer != null) {
                    headerConsumer.accept(headers);
                }
                // try-with-resources 确保响应流在任何路径下（含写入 output 抛 IOException）都会关闭
                try (InputStream in = response.getBody()) {
                    byte[] buf = new byte[8192];
                    int n;
                    long total = 0;
                    while ((n = in.read(buf)) > 0) {
                        output.write(buf, 0, n);
                        total += n;
                    }
                    output.flush();
                    log.info("NodeDispatchClient.dispatchAndDownload: success, peerId={}, originUri={}, size={}",
                            peerId, originUri, total);
                    return DispatchedFile.success();
                }
            });
        } catch (HttpStatusCodeException e) {
            // 对端返回 4xx/5xx：RestTemplate 默认 ResponseErrorHandler 会抛此异常，body 在异常里
            String msg = e.getResponseBodyAsString();
            log.warn("NodeDispatchClient.dispatchAndDownload: non-2xx, peerId={}, originUri={}, status={}, msg={}",
                    peerId, originUri, e.getStatusCode(), msg);
            return DispatchedFile.fail("跨节点下载失败: 对端状态=" + e.getStatusCode() + ", msg=" + msg);
        } catch (RestClientException e) {
            // 网络异常、连接超时等
            log.error("NodeDispatchClient.dispatchAndDownload: failed, peerId={}, originUri={}", peerId, originUri, e);
            return DispatchedFile.fail("跨节点下载失败: " + e.getMessage());
        }
    }

    /**
     * 调用本端 linkx-node 的 dispatch 接口，转发请求到对端节点。
     * 默认使用 "linkx-node-service" 作为 K8s service name。
     *
     * @param peerId    目标节点 peerId
     * @param originUri 原始业务 URI（如 /collaboration/v1/post/queryDepartment）
     * @param params    业务参数（可为 null）
     * @return 对端响应（透传，含状态码、响应体、响应头）
     */
    public ResponseEntity<byte[]> dispatch(String peerId, String originUri, Map<String, ?> params) {
        return dispatch("linkx-node-service", peerId, originUri, params);
    }

    /**
     * 调用本端 linkx-node 的 dispatch 接口，转发请求到对端节点。
     *
     * @param linkxNodeServiceName linkx-node 在 K8s 中的 service name（默认 linkx-node-service）
     * @param peerId               目标节点 peerId
     * @param originUri            原始业务 URI（如 /collaboration/v1/post/queryDepartment）
     * @param params               业务参数（可为 null）
     * @return 对端响应
     */
    public ResponseEntity<byte[]> dispatch(String linkxNodeServiceName, String peerId,
                                           String originUri, Map<String, ?> params) {
        URI uri = buildDispatchUri(linkxNodeServiceName, peerId, originUri, params);
        log.info("NodeDispatchClient.dispatch: peerId={}, originUri={}, params={}", peerId, originUri, params);
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    uri, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), byte[].class);
            log.info("NodeDispatchClient.dispatch: completed, peerId={}, originUri={}, statusCode={}",
                    peerId, originUri, response.getStatusCode());
            return response;
        } catch (Exception e) {
            log.error("NodeDispatchClient.dispatch: failed, peerId={}, originUri={}", peerId, originUri, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(("跨节点查询失败: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * 构建 dispatch 接口的完整 URI。
     * <p>
     * originUri 不做 URL 编码：业务路径中含 /，编码会变成 %2F，对端 Tomcat 默认拒绝（400）。
     * 去掉前导 /，让 originUri 作为多级 path 自然拼接（如 collaboration/v1/post/queryDepartment），
     * A 端 dispatch 接口用 /{peerId}/dispatch/** 匹配，提取后再加上前导 / 还原。
     * build(false) 不再额外编码，避免对路径二次编码；保留 / 原样传递。
     */
    private URI buildDispatchUri(String linkxNodeServiceName, String peerId,
                                 String originUri, Map<String, ?> params) {
        String normalizedOriginUri = stripLeadingSlashes(originUri);
        String path = String.format(DISPATCH_PATH, peerId, normalizedOriginUri);
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUri(URI.create("http://" + linkxNodeServiceName + ":8080"))
                .path(path);
        if (params != null && !params.isEmpty()) {
            builder.queryParams(convertToMultiValueMap(params));
        }
        return builder.build(false).toUri();
    }

    /**
     * 去掉前导 /，避免 path 拼接时出现 //。
     * originUri 不做 URL 编码：业务路径中含 /，编码成 %2F 后对端 Tomcat 默认拒绝（400）。
     */
    private String stripLeadingSlashes(String segment) {
        if (segment == null) {
            return "";
        }
        while (segment.startsWith("/")) {
            segment = segment.substring(1);
        }
        return segment;
    }

    private MultiValueMap<String, String> convertToMultiValueMap(Map<String, ?> params) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        if (params != null) {
            params.forEach((k, v) -> {
                if (v != null) {
                    if (v instanceof Iterable) {
                        for (Object item : (Iterable<?>) v) {
                            map.add(k, String.valueOf(item));
                        }
                    } else if (v instanceof Object[]) {
                        for (Object item : (Object[]) v) {
                            map.add(k, String.valueOf(item));
                        }
                    } else {
                        map.add(k, String.valueOf(v));
                    }
                }
            });
        }
        return map;
    }
}
