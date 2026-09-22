package com.tdtech.cloudcmd.im.jingxin.server.util;

import com.tdtech.cloudcmd.util.IdWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imdecode;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;

@Slf4j
@RequiredArgsConstructor
@Component
public class FaceUtil {

    private final IdWorker idWorker;
    @Value("${cloudcmd.imjx.faceDetect.cascade:haarcascade_frontalface_alt2.xml}")
    private String haarcascade;
    @Value("${cloudcmd.imjx.faceDetect.resizeThreadhold:204800}")
    private Long resize;

    public byte[] pickFirstFace(byte[] sourcePic) throws IOException {
        var cascadePath = Path.of("/opt/app/haarcascade", haarcascade);
        log.info("process image size:{} with:{}", sourcePic.length, cascadePath);
        Path tempImage = null;
        Mat image = null, grayImage = null, faceROI = null, buf = null, resizedFace = null;
        CascadeClassifier faceDetector = null;
        RectVector faceDetections = null;
        try {
            faceDetector = new CascadeClassifier(cascadePath.toString());
            faceDetections = new RectVector();
            buf = new Mat(sourcePic.length, 1, opencv_core.CV_8UC1);

            buf.data().put(sourcePic);
            image = imdecode(buf, IMREAD_COLOR);
            // 检查图像是否成功加载
            if (image.empty()) {
                log.error("Failed to decode image data");
                return null;
            }
            // 转换为灰度图像
            grayImage = new Mat();
            cvtColor(image, grayImage, COLOR_BGR2GRAY);

            // 添加检查确保灰度图像不为空
            if (grayImage.empty()) {
                log.error("Failed to convert image to grayscale");
                return null;
            }

            // 3. 进行人脸检测
            faceDetector.detectMultiScale(grayImage, faceDetections);
            // 4. 遍历所有检测到的人脸，并保存为图片
            if (faceDetections.empty()) {
                log.warn("no face detected");
                return null;
            }
            var rect = Arrays.stream(faceDetections.get())
                .max(Comparator.comparingLong(a -> (long)a.height() * (long)a.width())).get();

            //         从原图中截取人脸区域
            faceROI = new Mat(image, rect);
            int width = faceROI.cols();
            int height = faceROI.rows();
            long imgSize = faceROI.total() * faceROI.elemSize();
            if (imgSize > resize) {
                // 在 imwrite 之前添加以下代码进行 resize
                resizedFace = new Mat();
                var targetSize =
                    new Size(new BigDecimal(resize).divide(new BigDecimal(height), 2, RoundingMode.DOWN).intValue(),
                        new BigDecimal(resize).divide(new BigDecimal(width), 2, RoundingMode.DOWN)
                            .intValue()); // 设置目标尺寸
                resize(faceROI, resizedFace, targetSize);
                faceROI = resizedFace;
            }
            tempImage = Files.createTempFile(idWorker.nextId() + "", ".jpg");
            // 将人脸区域保存为图片
            int[] params = {opencv_imgcodecs.IMWRITE_JPEG_QUALITY, 90};
            imwrite(tempImage.toString(), faceROI, params);
            var bytes = Files.readAllBytes(tempImage);
            log.info("face detected size:{} with:{}", bytes.length, cascadePath);
            return bytes;
            //            // 创建适当大小的字节数组
            //            byte[] imageData = new byte[(int)(faceROI.channels() * faceROI.total())];
            //            // 将Mat数据复制到字节数组中
            //            faceROI.data().get(imageData);
            //            return imageData;
        } finally {
            if (tempImage != null) {
                try {
                    Files.deleteIfExists(tempImage);
                } catch (Exception e) {
                    log.warn("Failed to close resource: {}", tempImage, e);
                }
            }
            closeIfNotNull(image);
            closeIfNotNull(grayImage);
            closeIfNotNull(faceROI);
            closeIfNotNull(buf);
            closeIfNotNull(resizedFace);
            closeIfNotNull(faceDetector);
            closeIfNotNull(faceDetections);
        }
    }

    private void closeIfNotNull(Pointer pointer) {
        if (pointer != null) {
            try {
                pointer.close();
            } catch (Exception e) {
                log.warn("Failed to close resource: {}", pointer.getClass().getSimpleName(), e);
            }
        }
    }

}
