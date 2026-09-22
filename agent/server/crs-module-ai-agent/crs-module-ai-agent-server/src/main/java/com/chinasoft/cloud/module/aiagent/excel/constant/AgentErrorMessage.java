package com.chinasoft.cloud.module.aiagent.excel.constant;

public interface AgentErrorMessage {
    String ERROR_MESSAGE_NAME_NOT_EMPTY = "“AI智能体”导入表，第%s行“智能体名称”不能为空";

    String ERROR_MESSAGE_NAME_NOT_REPEATED = "“AI智能体”导入表，第%s行“名称”不能重复";

    String ERROR_MESSAGE_URL = "“AI智能体”导入表，第%s行“服务地址”不能为空";

    String ERROR_MESSAGE_PRIORITY = "“AI智能体”导入表，第%s行“优先级”不能为空，只能为“高”、“中”、“低”其中的一个值";

    String ERROR_MESSAGE_CATEGORIES = "“AI智能体”导入表，第%s行“智能体分类”不能为空，且有多个智能体分类时，需用“,”隔开";

    String ERROR_MESSAGE_AUDIO_TYPE = "“AI智能体”导入表，第%s行“音频支持文件格式”只能为“.mp3”、“.webm”、“.aac”、“.pcm”、“.wav”、" +
            "“.amr”、“.m4a”其中的值，且有多个文件格式时，需用“/”隔开";

    String ERROR_MESSAGE_VIDEO_TYPE = "“AI智能体”导入表，第%s行“视频支持文件格式”只能为“.mp4”、“.mov”、“.webm”、“.mpeg”、“.mpga”、" +
            "其中的值，且有多个文件格式时，需用“/”隔开";

    String ERROR_MESSAGE_IMAGE_TYPE = "“AI智能体”导入表，第%s行“图片支持文件格式”只能为“.jpg”、“.svg”、“.jpeg”、“.gif”、“.png”、" +
            "“.bmp”、“.webp”其中的值，且有多个文件格式时，需用“/”隔开";

    String ERROR_MESSAGE_DOCUMENT_TYPE = "“AI智能体”导入表，第%s行“文档支持文件格式”只能为“.xml”、“.ppt”、“.md”、“.doc”、“.pptx”、" +
            "“.epub”、“.msg”、“.txt”、“.docx”、“.pdf”、“.html”、“.markdown”、“.csv”、“.xlsx”、“.eml”、“.msg”其中的值，" +
            "且有多个文件格式时，需用“/”隔开";

    String ERROR_MESSAGE_METHOD = "“AI智能体”导入表，第%s行“请求方法”不能为空，只能为“GET”、“POST”、“PUT”、“DELETE”其中的一个值";

    String ERROR_MESSAGE_RECEIVE_IM = "“AI智能体”导入表，第%s行“是否接收IM消息”只能为“是”、“否”其中的一个值";

    String ERROR_MESSAGE_SCOPE = "“AI智能体”导入表，第%s行“智能体作用域”只能为“所有”、“仅AI智能体问答”、“仅IM”其中的一个值";

    String ERROR_MESSAGE_HEADER = "“AI智能体”导入表，第%s行“Header参数”只能为JSON格式";

    String ERROR_MESSAGE_QUERY = "“AI智能体”导入表，第%s行“Query参数”只能为JSON格式";

    String ERROR_MESSAGE_BODY = "“AI智能体”导入表，第%s行“Body参数”只能为JSON格式";

    String ERROR_MESSAGE_FILE_INTERFACE = "“AI智能体”导入表，第%s行“文件上传接口”只能填一个存在的AI智能体文件接口名称";

    String ERROR_MESSAGE_VIRTUAL_USER = "“AI智能体”导入表，第%s行“关联用户”不存在，请填写模板下拉选项中的虚拟用户名称";

    String ERROR_MESSAGE_VIRTUAL_USER_BIND = "“AI智能体”导入表，第%s行“关联用户”绑定关系建立失败";

    String ERROR_MESSAGE_VIRTUAL_USER_BOUND = "“AI智能体”导入表，第%s行“关联用户”已被其他智能体绑定，一个虚拟用户不能重复绑定";
}