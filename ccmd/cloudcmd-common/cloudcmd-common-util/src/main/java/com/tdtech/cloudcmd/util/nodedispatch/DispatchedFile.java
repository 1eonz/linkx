package com.tdtech.cloudcmd.util.nodedispatch;

import lombok.Getter;

/**
 * 跨节点文件下载结果。
 * <p>
 * {@link NodeDispatchClient#dispatchAndDownload} 返回的结果对象，标识流式下载是否成功。
 * 流式模式下文件内容已直接写入调用方传入的 OutputStream，本对象不持有 body；
 * 对端响应头通过 headerConsumer 回调在写流前透传给调用方，本对象也不持有 headers。
 * <p>
 * 调用方根据 {@link #success} 决定后续行为：
 * <ul>
 *   <li>success=true：文件已写入 OutputStream，无需再处理</li>
 *   <li>success=false：未写入任何字节，调用方可自由返回错误响应（如 502 + JSON）</li>
 * </ul>
 */
@Getter
public class DispatchedFile {

    /** 下载是否成功：对端 2xx 且流式写入完成时为 true。 */
    private final boolean success;

    /** 失败时的错误信息（成功时为 null）。 */
    private final String errorMessage;

    private DispatchedFile(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }

    /**
     * 构造成功结果（流式写入已完成）。
     */
    public static DispatchedFile success() {
        return new DispatchedFile(true, null);
    }

    /**
     * 构造失败结果（未写入任何字节到输出流）。
     *
     * @param errorMessage 错误信息
     */
    public static DispatchedFile fail(String errorMessage) {
        return new DispatchedFile(false, errorMessage);
    }
}