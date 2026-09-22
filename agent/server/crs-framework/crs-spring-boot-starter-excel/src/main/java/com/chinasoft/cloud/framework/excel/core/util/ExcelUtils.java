package com.chinasoft.cloud.framework.excel.core.util;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.converters.longconverter.LongStringConverter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.chinasoft.cloud.framework.excel.core.handler.SelectSheetWriteHandler;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

/**
 * Excel 工具类
 *
 * @author 芋道源码
 */
public class ExcelUtils {

    /**
     * 将列表以 Excel 响应给前端
     *
     * @param response  响应
     * @param filename  文件名
     * @param sheetName Excel sheet 名
     * @param head      Excel head 头
     * @param data      数据列表哦
     * @param <T>       泛型，保证 head 和 data 类型的一致性
     * @throws IOException 写入失败的情况
     */
    public static <T> void write(HttpServletResponse response, String filename, String sheetName,
                                 Class<T> head, List<T> data) throws IOException {
        // 输出 Excel
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encodedFileName = URLEncoder.encode(filename, "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + encodedFileName);
        ServletOutputStream outputStream = response.getOutputStream();
        var writer = EasyExcel.write(outputStream, head)
                .autoCloseStream(false) // 不要自动关闭，交给 Servlet 自己处理
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy()) // 基于 column 长度，自动适配。最大 255 宽度
                .registerWriteHandler(new SelectSheetWriteHandler(head)) // 基于固定 sheet 实现下拉框
                .registerConverter(new LongStringConverter()) // 避免 Long 类型丢失精度
                .build();
        WriteSheet writeSheet = EasyExcel.writerSheet(sheetName).build();
        writer.write(data, writeSheet);
        writer.finish();
        outputStream.flush();
    }

    public static <T> List<T> read(MultipartFile file, Class<T> head) throws IOException {
        return EasyExcel.read(file.getInputStream(), head, null)
                .autoCloseStream(false)  // 不要自动关闭，交给 Servlet 自己处理
                .doReadAllSync();
    }

}
