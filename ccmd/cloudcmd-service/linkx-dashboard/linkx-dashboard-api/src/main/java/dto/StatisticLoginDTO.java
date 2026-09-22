package dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Date;

/**
 * 客户端登录信息记录
 */
@Slf4j
@Data
@Accessors(chain = false)
@NoArgsConstructor
@AllArgsConstructor
public class StatisticLoginDTO implements Serializable {

    /**
     * 警信用户ID
     */
    private String userId;

    /**
     * AppId
     */
    private String appId;

    /**
     * 客户端类型(1:BS PC;2:CS PC;3:App H5;4:Admin;5:RESTful;6:JS-SDK)
     * 默认0，用于兼容情况，标识数据有问题，需要定位
     */
    private Integer clientType = 0;

    /**
     * 操作系统(如 Windows 10, iOS 15, Linux)
     */
    private String os;

    /**
     * 浏览器信息(Chrome 120/Safari 17，Server类型可为空)
     */
    private String browser;

    /**
     * 屏幕窗口物理长宽像素
     */
    private String screen;

    /**
     * 登录时间
     */
    private Date loginTime;

    /**
     * 登录结果(0：成功；1：失败)
     */
    private Integer loginResult;

    private static StatisticLoginDTO getInstance(HttpServletRequest request, Date loginTime) {
        StatisticLoginDTO dto = new StatisticLoginDTO();
        String userId = request.getHeader("X-User-Id");
        dto.setUserId(StringUtils.isBlank(userId) || "undefined".equalsIgnoreCase(userId) ? null : userId);

        String os = request.getHeader("X-OS");
        dto.setOs(StringUtils.isBlank(os) ? null : os);

        String browser = request.getHeader("X-Browser");
        dto.setBrowser(StringUtils.isBlank(browser) ? null : browser);

        String screen = request.getHeader("X-Screen");
        dto.setScreen(StringUtils.isBlank(screen) ? null : screen);

        dto.setLoginTime(loginTime);

        String clientType = request.getHeader("X-Client-Type");
        if (StringUtils.isNotBlank(clientType)) {
            try {
                dto.setClientType(Integer.parseInt(clientType));
            } catch (NumberFormatException exception) {
                log.error("Set StatisticLoginDTO clientType error. StatisticLoginDTO is {}", dto);
            }
        }
        return dto;
    }

    public static StatisticLoginDTO getSuccessInstance(HttpServletRequest request, Date loginTime) {
        StatisticLoginDTO dto = getInstance(request, loginTime);
        dto.setLoginResult(0);
        return dto;
    }

    public static StatisticLoginDTO getFailureInstance(HttpServletRequest request, Date loginTime) {
        StatisticLoginDTO dto = getInstance(request, loginTime);
        dto.setLoginResult(1);
        return dto;
    }

    public void setClientTypeByUserId(String userId) {
        this.setClientType(StringUtils.isBlank(userId) ? 5 : 6);
    }
}
