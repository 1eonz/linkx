package com.tdtech.cloudcmd.auth.Utils;

import com.tdtech.cloudcmd.auth.PwdUtil;
import com.tdtech.cloudcmd.auth.exception.OAuthException;
import com.tdtech.cloudcmd.base.api.service.GlobalsRpcService;
import com.tdtech.cloudcmd.i18n.I18nUtil;
import com.tdtech.cloudcmd.redis.RedisUtil;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

import static com.tdtech.cloudcmd.auth.enums.CommonErrorEnum.COMMON_ERROR_123;

/**
 * 历史密码校验工具类（全局共用）
 * 禁止复用最近 N 次历史密码，默认 3 次
 */
@Component
public class HistoryPwdUtils {

    @Resource
    private RedisUtil redisUtil;

    //历史密码key
    private final String ADMIN_PWD_HISTORY_KEY = "ADMIN_PWD_HISTORY_KEY";

    /**
     * 校验新密码不能与最近N次历史密码相同
     *
     * @param userId           用户ID
     * @param newPlainPassword 新密码（明文）
     * @param historyLimit     历史密码限制次数
     */
    public void checkHistoryPassword(Long userId, String newPlainPassword, int historyLimit) {
        if (historyLimit <= 0) {
            return;
        }
        String redisKey = ADMIN_PWD_HISTORY_KEY + userId;
        List<String> historyPasswords = redisUtil.lRange(redisKey, 0, historyLimit - 1, String.class);
        for (String historyPwd : historyPasswords) {
            if (PwdUtil.checkPwdMatches(historyPwd, newPlainPassword)) {
                throw new OAuthException(COMMON_ERROR_123.getCode(), String.format("新密码不能与最近%d次历史密码相同", historyLimit));
            }
        }
    }

    /**
     * 保存新密码到历史记录，仅保留最近N次
     *
     * @param userId             用户ID
     * @param newEncryptPassword 新密码（加密后）
     * @param historyLimit       历史密码限制次数
     */
    public void saveHistoryPassword(Long userId, String newEncryptPassword, int historyLimit) {
        if (historyLimit <= 0) {
            return;
        }
        String redisKey = ADMIN_PWD_HISTORY_KEY + userId;
        redisUtil.lPush(redisKey, newEncryptPassword);
        redisUtil.lTrim(redisKey, 0, historyLimit - 1);
    }
}
