package com.tdtech.cloudcmd.auth;

import static com.tdtech.cloudcmd.auth.enums.CommonErrorEnum.COMMON_ERROR_135;
import static com.tdtech.cloudcmd.auth.enums.CommonErrorEnum.COMMON_ERROR_141;
import static com.tdtech.cloudcmd.auth.enums.CommonErrorEnum.COMMON_ERROR_148;
import static com.tdtech.cloudcmd.auth.enums.CommonErrorEnum.COMMON_ERROR_149;
import static com.tdtech.cloudcmd.auth.enums.CommonErrorEnum.COMMON_ERROR_150;

import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tdtech.cloudcmd.i18n.I18nUtil;
import com.tdtech.cloudcmd.util.PBKDF2Util;

import com.tdtech.cloudcmd.util.StringUtils;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PwdUtil {

    /**
     * 特殊字符集合：`~!@#$%^&*()-_=+\|[{}];:'",<.>/? 和空格
     */
    private static final String SPECIAL_CHARS = "`~!@#$%^&*()-_=+\\|[{}];:'\",<.>/? ";

    private PwdUtil() {
        throw new UnsupportedOperationException();
    }

    public static void checkPwdNotEmpty(String pwd) {
        if (StringUtils.isNullBlank(pwd)) {
            throw new PwdException(COMMON_ERROR_149.getCode(), I18nUtil.get(COMMON_ERROR_149.getMsg()));
        }
    }
    public static void checkPwdFormat(String pwd) {
        checkPwdFormat(pwd, null);
    }

    /**
     * 1）口令长度至少8个字符；
     * 2）口令必须包含如下至少两种字符的组合:
     * 至少一个小写字母；
     * 至少一个大写字母；
     * 至少一个数字；
     * 至少一个特殊字符：`~!@#$%^&*()-_=+\\|[{}];:'\",<.>/?  和空格
     * 3）口令不能和账号一样；
     */
    public static void checkPwdFormat(String pwd, String account) {
        // 校验密码不能与用户名相同
        if (!StringUtils.isNullBlank(account) && Objects.equals(pwd, account)) {
            throw new PwdException(COMMON_ERROR_148.getCode(), I18nUtil.get(COMMON_ERROR_148.getMsg()));
        }
//        String pattern = "^(?![A-Za-z0-9]+$)(?![a-z0-9\\W]+$)(?![A-Za-z\\W]+$)(?![A-Z0-9\\W]+$)[a-zA-Z0-9\\W]{8,}$";
        // 校验密码长度至少8个字符
        if (pwd.length() < 8) {
            throw new PwdException(COMMON_ERROR_150.getCode(), I18nUtil.get(COMMON_ERROR_150.getMsg()));
        }
        
        // 校验密码必须包含至少两种字符类型
        int typeCount = 0;
        boolean hasLower = false;   // 小写字母
        boolean hasUpper = false;   // 大写字母
        boolean hasDigit = false;   // 数字
        boolean hasSpecial = false; // 特殊字符
        
        for (char c : pwd.toCharArray()) {
            if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (SPECIAL_CHARS.indexOf(c) >= 0) {
                hasSpecial = true;
            }
        }
        
        // 计算包含的字符类型数量
        if (hasLower) typeCount++;
        if (hasUpper) typeCount++;
        if (hasDigit) typeCount++;
        if (hasSpecial) typeCount++;
        
        // 至少包含两种字符类型
        if (typeCount < 2) {
            throw new PwdException(COMMON_ERROR_150.getCode(), I18nUtil.get(COMMON_ERROR_150.getMsg()));
        }
        
        // 校验不能包含连续三个相同的字符
//        for (int i = 0; i < pwd.length() - 2; i++) {
//            if (pwd.charAt(i) == pwd.charAt(i + 1) && pwd.charAt(i + 1) == pwd.charAt(i + 2)) {
//                throw new PwdException(COMMON_ERROR_141.getCode(), I18nUtil.get(COMMON_ERROR_141.getMsg()));
//            }
//        }
    }

    /**
     * @return
     */
    public static boolean checkPwdMatches(String encrypted, String input) {
        String salt = PBKDF2Util.getSaltFromStandardPass(encrypted);
        // 关键字被扫描
        String repwd = PBKDF2Util.PBKDF2ForPassStandard(input, salt);
        return repwd != null && !repwd.isBlank() && repwd.equals(encrypted);
    }

    @SneakyThrows(NoSuchAlgorithmException.class)
    public static String encryptPwd(String pwd) {
        return PBKDF2Util.PBKDF2ForPassStandard(pwd, PBKDF2Util.generateSalt());
    }

    @Getter
    public static class PwdException extends RuntimeException {
        private final int code;

        public PwdException(int code, String message) {
            super(message);
            this.code = code;
        }
    }
}
