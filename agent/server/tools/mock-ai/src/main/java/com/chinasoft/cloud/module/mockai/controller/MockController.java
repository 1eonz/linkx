package com.chinasoft.cloud.module.mockai.controller;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class MockController {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Map<String, MockApprovalOrder> APPROVAL_ORDERS = new ConcurrentHashMap<>();
    private static final byte[] data =
        """
            data: {
            "event":"message",
            "answer":"【豆包】全能写作bot-能写豆包AI助你高效豆包帮助你轻松完成各种类型写作任务,无论是撰写文章、润色文案、编写程序代码等.帮助你轻松完成各种类型写作任务,丰富的模板和案例库,为您提供灵感为您的工作带来更多的便利!"
            }
            """
            .replaceAll("\n", "").getBytes(StandardCharsets.UTF_8);
    private static final byte[] end = """
        data: {
            "event":"message_end",
            "answer":"[end]"
        }
        """.replaceAll("\n", "").getBytes(StandardCharsets.UTF_8);

    private static final String XIAOQIAO_TRACE_ID = "t-aaa";
    private static final long XIAOQIAO_MESSAGE_ID = 12345L;
    private static final long XIAOQIAO_PARENT_ID = 12344L;
    private static final String XIAOQIAO_MODEL = "qwen-plus";
    private static final String XIAOQIAO_CHAT_ID = "chat-001";

    private static final String[] XIAOQIAO_CHUNKS = {
        "人工", "智能", "是一门", "交叉学科", "，",
        "它", "致力于", "研究", "和", "开发",
        "模拟", "人类", "智能", "的", "理论",
        "、", "方法", "、", "技术", "和", "应用", "。"
    };

    @PostMapping("/ask")
    public void mock(@RequestBody String req, HttpServletResponse response) throws IOException, InterruptedException {
        log.info("req:{}", req);
        var os = response.getOutputStream();
        for (int i = 0; i < 15; i++) {
            os.write(data);
            os.write("\n".getBytes(StandardCharsets.UTF_8));
            os.flush();
            Thread.sleep(400L);
        }
        os.write(end);
        os.write("\n".getBytes(StandardCharsets.UTF_8));
        os.flush();
    }

    @PostMapping("/xiaoqiao/ask")
    public void mockXiaoQiao(@RequestBody String req, HttpServletResponse response) throws IOException, InterruptedException {
        log.info("xiaoqiao req:{}", req);
        var os = response.getOutputStream();
        for (String chunk : XIAOQIAO_CHUNKS) {
            os.write(buildXiaoQiaoFrame(chunk).getBytes(StandardCharsets.UTF_8));
            os.flush();
            Thread.sleep(400L);
        }
        os.flush();
    }

    private String buildXiaoQiaoFrame(String content) {
        JSONObject delta = new JSONObject();
        delta.put("role", "assistant");
        delta.put("content", content);
        delta.put("type", "text");

        JSONObject choice = new JSONObject();
        choice.put("index", 0);
        choice.put("delta", delta);

        JSONArray choices = new JSONArray();
        choices.add(choice);

        JSONObject dataObj = new JSONObject();
        dataObj.put("id", XIAOQIAO_CHAT_ID);
        dataObj.put("model", XIAOQIAO_MODEL);
        dataObj.put("choices", choices);
        dataObj.put("message_id", XIAOQIAO_MESSAGE_ID);
        dataObj.put("parent_id", XIAOQIAO_PARENT_ID);

        JSONObject frame = new JSONObject();
        frame.put("code", 0);
        frame.put("msg", "操作成功");
        frame.put("data", dataObj);
        frame.put("traceId", XIAOQIAO_TRACE_ID);

        return "data: " + frame.toJSONString() + "\n\n";
    }

    @PostMapping("/approval")
    public JSONObject approval(@RequestBody ApprovalCreateReq req) {
        if (StringUtils.isNoneBlank(req.getUserName()) && "error".equals(req.getUserName())) {
            throw new RuntimeException("mock approval error");
        }
        if (StringUtils.isBlank(req.getApprover()) && StringUtils.isBlank(req.getUserName())) {
            JSONObject result = new JSONObject();
            result.put("code", 400);
            result.put("msg", "approver不能为空");
            return result;
        }
        String approvalNo = "MOCK-APPROVAL-" + System.currentTimeMillis();
        String baseUrl = StringUtils.defaultIfBlank(req.getMockBaseUrl(), "http://127.0.0.1:48099");

        MockApprovalOrder order = new MockApprovalOrder();
        order.setApprovalNo(approvalNo);
        order.setApprovalType(StringUtils.defaultIfBlank(req.getApprovalType(), req.getAgentName()));
        order.setApprover(StringUtils.defaultIfBlank(req.getApprover(), req.getUserName()));
        order.setRecordId(req.getRecordId());
        order.setCallbackUrl(req.getCallbackUrl());
        order.setApprovalStatus("1");
        order.setApproveResult(null);
        order.setApproveUrl(baseUrl + "/approval/h5/launch?approvalNo=" + approvalNo);
        order.setApproveDetailUrl(baseUrl + "/approval/h5/detail?approvalNo=" + approvalNo);
        order.setToLeaderUrl(baseUrl + "/approval/h5/leader-card?approvalNo=" + approvalNo);
        APPROVAL_ORDERS.put(approvalNo, order);

        JSONObject dataObj = new JSONObject();
        dataObj.put("approveNo", approvalNo);
        dataObj.put("approvalNo", approvalNo);
        dataObj.put("approvalStatus", order.getApprovalStatus());
        dataObj.put("approveResult", order.getApproveResult());
        dataObj.put("approveUrl", order.getApproveUrl());
        dataObj.put("approvalUrl", order.getApproveUrl());
        dataObj.put("approveDetailUrl", order.getApproveDetailUrl());
        dataObj.put("approvalDetailUrl", order.getApproveDetailUrl());
        dataObj.put("toLeaderUrl", order.getToLeaderUrl());

        JSONObject result = new JSONObject();
        result.put("code", 0);
        result.put("msg", "success");
        result.put("data", dataObj);
        return result;
    }

    @PostMapping("/approval/trigger-callback")
    public JSONObject triggerApprovalCallback(@RequestBody ApprovalCallbackTriggerReq req) throws Exception {
        MockApprovalOrder order = APPROVAL_ORDERS.get(req.getApprovalNo());
        if (order == null) {
            JSONObject result = new JSONObject();
            result.put("code", 404);
            result.put("msg", "approval order not found");
            return result;
        }
        if (order.getRecordId() == null) {
            JSONObject result = new JSONObject();
            result.put("code", 400);
            return result;
        }
        String approvalStatus = resolveApprovalStatus(req);
        Integer approve = resolveApproveResult(req, approvalStatus);
        boolean approved = Objects.equals(approve, 0);
        order.setApprovalStatus(approvalStatus);
        order.setApproveResult(approve);

        JSONObject callbackBody = new JSONObject();
        callbackBody.put("recordId", order.getRecordId());
        callbackBody.put("approveNo", order.getApprovalNo());
        callbackBody.put("approveDetailUrl", order.getApproveDetailUrl());
        callbackBody.put("toLeaderUrl", StringUtils.defaultIfBlank(req.getToLeaderUrl(), order.getToLeaderUrl()));
        callbackBody.put("approvalStatus", order.getApprovalStatus());
        callbackBody.put("approveResult", approve);
        callbackBody.put("approveDescription", approve == null ? "mock approval pending"
            : approved ? "mock approval passed" : "mock approval rejected");
        callbackBody.put("approveTime", LocalDateTime.now().format(DATE_TIME_FORMATTER));
        callbackBody.put("approveUser", order.getApprover());

        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:48086/app-api/deepseek-zjk/xa/dk/approval/callback"))
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(callbackBody.toJSONString(), StandardCharsets.UTF_8))
            .build();
        HttpResponse<String> response =
            HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        JSONObject result = new JSONObject();
        result.put("code", 0);
        result.put("msg", "success");
        result.put("data", Map.of(
            "approvalNo", order.getApprovalNo(),
            "approvalStatus", order.getApprovalStatus(),
            "approveResult", order.getApproveResult(),
            "callbackStatus", response.statusCode(),
            "callbackResponse", response.body()
        ));
        return result;
    }

    @GetMapping(value = "/approval/h5/launch", produces = "text/html;charset=UTF-8")
    public String approvalLaunchPage(@RequestParam String approvalNo) {
        return "<html><body><h3>Mock Approval Launch</h3><div>approvalNo=" + approvalNo + "</div></body></html>";
    }

    @GetMapping(value = "/approval/h5/detail", produces = "text/html;charset=UTF-8")
    public String approvalDetailPage(@RequestParam String approvalNo) {
        return "<html><body><h3>Mock Approval Detail</h3><div>approvalNo=" + approvalNo + "</div></body></html>";
    }

    @Data
    public static class ApprovalCreateReq {

        private Long recordId;

        private String approvalType;

        private String approver;

        private String agentName;

        private String approvalSubMode;

        private String userName;

        private String callbackUrl;

        private String mockBaseUrl;
    }

    @Data
    public static class ApprovalCallbackTriggerReq {

        private String approvalNo;

        private String approvalStatus;

        private Integer approveResult;

        private String approveResultText;

        private String toLeaderUrl;
    }

    @Data
    private static class MockApprovalOrder {

        private String approvalNo;

        private String approvalType;

        private String approver;

        private Long recordId;

        private String callbackUrl;

        private String approvalStatus;

        private Integer approveResult;

        private String approveUrl;

        private String approveDetailUrl;

        private String toLeaderUrl;
    }

    private Integer resolveApproveResult(ApprovalCallbackTriggerReq req, String approvalStatus) {
        if ("1".equals(approvalStatus)) {
            return null;
        }
        if (req.getApproveResult() != null) {
            return req.getApproveResult();
        }
        for (String s : new String[]{"同意", "通过", "approved", "approve"}) {
            if (StringUtils.equalsIgnoreCase(req.getApproveResultText(), s)) {
                return 0;
            }
        }
        return 1;
    }

    private String resolveApprovalStatus(ApprovalCallbackTriggerReq req) {
        if (StringUtils.isNotBlank(req.getApprovalStatus())) {
            return req.getApprovalStatus();
        }
        return "2";
    }

}