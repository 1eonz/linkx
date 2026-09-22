package com.tdtech.cloudcmd.util;

/**
 * @author zhuangzl
 * @date 2018-03-23 17:26
 */

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils extends org.apache.commons.lang3.StringUtils {

    /**
     * 字符长度
     */
    public static final int CHAR_LEN = 1;
    public static final Pattern IP_PATTERN = Pattern.compile(
        "(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d)\\.(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d)\\.(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d)\\.(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d)");
    public static final Pattern INT_NUMBER_PATTERN = Pattern.compile("^[0-9]+$");

    private StringUtils() {}

    public static boolean ipCheck(String ip) {
        return IP_PATTERN.matcher(ip).matches();
    }


    public static boolean isIntNumber(String source) {
        return INT_NUMBER_PATTERN.matcher(source).matches();
    }

    /**
     * 判断字符串是否为null或为空字符串（包括只含空格的字符串）
     *
     * @param str String 待检查的字符串
     * @return boolean 如果为null或空字符串（包括只含空格的字符串）则返回true，否则返回false
     */
    public static boolean isNullBlank(String str) {
        return (null == str || "".equals(str.trim()) || "null".equals(str.trim())) ? true : false;
    }


    public static boolean isAllBlank(String... strs){
        for (var str : strs) {
            if(str!=null&&!str.isBlank()){
                return false;
            }
        }
        return true;
    }
    /**
     * 删除指定的前导、后导子串（大小写敏感）. <br>
     * 例子： <br>
     * StringUtils.trim("and and username = '123' and password = 'abc' and ","and "); <br>
     * 结果：username = '123' and password = 'abc'
     *
     * @param sourceStr String 待删除的字符串
     * @param removeChar char 子串
     * @return String 处理后的字符串
     */
    public static String trim(String sourceStr, char removeChar) {
        if (sourceStr == null) {
            return null;
        }
        sourceStr = sourceStr.trim();
        int loInt_begin = 0, loInt_end = 0;
        int loInt_len = sourceStr.length();
        for (int i = 0; i < loInt_len; i++) {
            if (sourceStr.charAt(i) == removeChar) {
                loInt_begin++;
            } else {
                break;
            }
        }
        for (int i = 0; i < loInt_len; i++) {
            if (sourceStr.charAt(loInt_len - 1 - i) == removeChar) {
                loInt_end++;
            } else {
                break;
            }
        }
        return sourceStr.substring(loInt_begin, loInt_len - loInt_end);
    }

    /**
     * 删除指定的前导、后导子串（大小写敏感）. <br>
     * 例子： <br>
     * StringUtils.trim("and and username = '123' and password = 'abc' and ","and "); <br>
     * 结果：username = '123' and password = 'abc'
     *
     * @param sourceStr String 待删除的字符串
     * @param removeChar char 子串
     * @param end true 只trim末尾，false 只trim头部
     * @return String 处理后的字符串
     */
    public static String trim(String sourceStr, char removeChar, boolean end) {
        if (sourceStr == null) {
            return null;
        }
        sourceStr = sourceStr.trim();
        int loInt_begin = 0, loInt_end = 0;
        int loInt_len = sourceStr.length();
        for (int i = 0; i < loInt_len; i++) {
            if (sourceStr.charAt(i) == removeChar) {
                loInt_begin++;
            } else {
                break;
            }
        }
        for (int i = 0; i < loInt_len; i++) {
            if (sourceStr.charAt(loInt_len - 1 - i) == removeChar) {
                loInt_end++;
            } else {
                break;
            }
        }
        if (end) {
            return sourceStr.substring(0, loInt_len - loInt_end);
        } else {
            return sourceStr.substring(loInt_begin);
        }
    }

    public static String truncate(String str, int len, int lenType) {
        if (str == null) {
            return str;
        }
        // 字符长度
        if (lenType == CHAR_LEN) {
            if (str.length() <= len) {
                return str;
            } else {
                return str.substring(0, len);
            }
        } else // 字节长度
        {
            StringBuffer sb = new StringBuffer();
            int temp = 0;

            for (int i = 0; i < str.length(); i++) {
                if (i < len) // 是否到指定的长度
                {
                    temp = ("" + str.charAt(i)).getBytes().length;
                    if (i + temp <= len) {
                        sb.append(str.charAt(i));
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
            return sb.toString();
        }
    }

    /**
     * 从源字符串中从第一个字符开始取出给定长度的字串. <BR>
     * 源字符串长度大于len时，尾巴追加一个appendStr串
     *
     * @param sourceStr String 源字符串
     * @param len int 取出的长度
     * @param appendStr String 追加的字符串（常用的appendStr为...）
     * @return String 取出的子串
     */
    public static String substring(String sourceStr, int len, String appendStr) {
        if (null == sourceStr || "".equals(sourceStr)) {
            return sourceStr;
        }
        if (len <= 0) {
            return "";
        }

        if (null == appendStr) {
            appendStr = "";
        }

        int sourceLen = sourceStr.length();
        if (len >= sourceLen) {
            return sourceStr;
        } else {
            return sourceStr.substring(0, len) + appendStr;
        }
    }

    /**
     * 产生指定长度随机字符串（包括字母及数字）.
     *
     * @param length int 随机字符串长度
     * @return String 随机字符串
     */
    public static String random(int length) {
        SecureRandom secureRandom = new SecureRandom();
        String retu = "";
        int d1, d2;
        char[] letters = initLetters();
        for (int i = 0; i < length; i++) {
            d1 = ((int)(secureRandom.nextDouble() * 10) % 2);
            if (d1 == 0) { // use a letter
                d2 = ((int)(secureRandom.nextDouble() * 100) % 52);
                retu += letters[d2];
            } else if (d1 == 1) { // use a number
                retu += (int)(secureRandom.nextDouble() * 10);
            }
        }
        return retu;
    }


    /**
     * 内部方法，产生字母数组
     *
     * @return char[]
     */
    private static char[] initLetters() {
        char[] ca = new char[52];
        for (int i = 0; i < 26; i++) {
            ca[i] = (char)(65 + i);
        }
        for (int i = 26; i < 52; i++) {
            ca[i] = (char)(71 + i);
        }
        return ca;
    }

    /**
     *
     */
    public static String join(Object[] array, String separator) {
        return org.apache.commons.lang3.StringUtils.join(array, separator);
    }




    /**
     * 四舍五入 用这个方法rounds()当值大于上千会出错
     *
     * @param value
     * @param fractionDigits 保留几位小数
     * @return
     */
    public static double parseDouble(double value, int fractionDigits) {
        double d = new BigDecimal(value).setScale(fractionDigits, BigDecimal.ROUND_HALF_UP).doubleValue();
        return d;
    }

    /**
     * 是否含有数字
     *
     * @param content
     * @return
     */
    public static boolean hasDigit(String content) {
        boolean flag = false;
        Pattern p = Pattern.compile(".*\\d+.*");
        Matcher m = p.matcher(content);
        if (m.matches()) {
            flag = true;
        }
        return flag;
    }

    /**
     * 截取所有数字
     *
     * @param content
     * @return
     */
    public static String getDigit(String content) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            return matcher.group(0);
        }
        return "";
    }

    /**
     * 截取所有中文文字
     *
     * @param content
     * @return
     */
    public static String getCharacter(String content) {
        String reg = "[^\u4e00-\u9fa5]";
        content = content.replaceAll(reg, " ");
        return content;
    }

    public static String getIdCardNum(String content) {
        String regex =
            "([1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx])|([1-9]\\d{5}\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3})";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(content);
        if (m.find()) {
            return m.group();
        }
        return null;
    }

    private static final String SPECIAL_CHARACTERS_PATTERN = "([%_/])";
    private static final String ESCAPE_CHARACTER = "/";

    public static String escapeSpecialCharacters(String input) {
        if (input == null) {
            return null;
        }
        Pattern pattern = Pattern.compile(SPECIAL_CHARACTERS_PATTERN);
        Matcher matcher = pattern.matcher(input);
        return matcher.replaceAll(ESCAPE_CHARACTER + "$1");
    }

    public static boolean isNullOrEmptyOrUndefined (String str) {
        if (str == null) {
            return true;
        }

        String trimmedStr = str.trim();
        if (trimmedStr.isEmpty()) {
            return true;
        }

        if ("undefined".equalsIgnoreCase(trimmedStr)) {
            return true;
        }

        return false;
    }
}
