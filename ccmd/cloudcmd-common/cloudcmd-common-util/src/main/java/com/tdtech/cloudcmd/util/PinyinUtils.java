package com.tdtech.cloudcmd.util;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

/**
 * 汉字转为拼音
 */
public class PinyinUtils {
    /**
     * 汉字转全拼
     *
     * @param str
     * @return
     * @throws Exception
     */
    public static String getPinyin(String str) throws Exception {
        if (str == null || str.isEmpty()) {
            return "";
        }
        char[] t1 = str.toCharArray();
        // 设置汉字拼音输出的格式
        HanyuPinyinOutputFormat t3 = new HanyuPinyinOutputFormat();
        t3.setCaseType(HanyuPinyinCaseType.LOWERCASE);// 小写
        t3.setToneType(HanyuPinyinToneType.WITHOUT_TONE);// 不带声调
        t3.setVCharType(HanyuPinyinVCharType.WITH_V);

        StringBuilder t4 = new StringBuilder(t1.length * 5);
        try {
            for (char c : t1) {
                // 判断是否为汉字字符
                if (Character.toString(c).matches("[\\u4E00-\\u9FA5]+")) {
                    // 将汉字的几种全拼都存到t2数组中
                    String[] t2 = PinyinHelper.toHanyuPinyinStringArray(c, t3);
                    // 生僻字 pinyin4j 未收录时返回 null，跳过该字避免 NPE，保证整串拼音仍可生成
                    if (t2 != null && t2.length > 0) {
                        t4.append(t2[0]);// 取出该汉字全拼的第一种读音并连接到字符串t4后
                    }
                } else {
                    // 如果不是汉字字符，直接取出字符并连接到字符串t4后
                    t4.append(c);
                }
            }
        } catch (BadHanyuPinyinOutputFormatCombination e) {
            throw e;
        }
        return t4.toString();
    }

    /**
     * 汉字转简拼
     *
     * @return String
     */
    public static String getPinYinHeadChar(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        StringBuilder convert = new StringBuilder(str.length());
        for (int j = 0; j < str.length(); j++) {
            char word = str.charAt(j);
            // 提取汉字的首字母
            String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(word);
            if (pinyinArray != null) {
                convert.append(pinyinArray[0].charAt(0));
            } else {
                convert.append(word);
            }
        }
        return convert.toString().toUpperCase();
    }

    /**
     * 一次遍历同时生成全拼和首字母，复用同一次 pinyin4j 查表，避免对同一名字查表两次。
     * 全拼为小写、首字母为小写，均已做生僻字跳过处理（未收录字跳过，不抛异常）。
     * 非汉字字符（如间隔号"·"）会原样保留在结果中，调用方按需用 {@link #normalize(String)} 清理。
     *
     * @param str 汉字串
     * @return 长度为 2 的数组：[fullPinyin(小写), headChar(小写)]；入参为空返回 ["", ""]
     * @throws BadHanyuPinyinOutputFormatCombination pinyin4j 格式异常
     */
    public static String[] getPinyinAndHead(String str) throws BadHanyuPinyinOutputFormatCombination {
        if (str == null || str.isEmpty()) {
            return new String[]{"", ""};
        }
        char[] chars = str.toCharArray();
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        format.setVCharType(HanyuPinyinVCharType.WITH_V);

        StringBuilder fullPinyin = new StringBuilder(chars.length * 5);
        StringBuilder headChar = new StringBuilder(chars.length);
        for (char c : chars) {
            if (Character.toString(c).matches("[\\u4E00-\\u9FA5]+")) {
                String[] t2 = PinyinHelper.toHanyuPinyinStringArray(c, format);
                if (t2 != null && t2.length > 0) {
                    fullPinyin.append(t2[0]);
                    headChar.append(Character.toLowerCase(t2[0].charAt(0)));
                }
            } else {
                fullPinyin.append(c);
                headChar.append(c);
            }
        }
        return new String[]{fullPinyin.toString(), headChar.toString()};
    }

    /**
     * 规范化拼音：去除所有非字母字符（如少数民族名字中的间隔号"·"、空格等），保证拼音连续。
     * 例如 "mamaimaiti·tuerxun" → "mamaimaitituerxun"，"MMYT·TEX" → "MMYTTEX"
     *
     * @param pinyin 原始拼音串（可能含非字母字符）
     * @return 仅保留字母的拼音串
     */
    public static String normalize(String pinyin) {
        if (pinyin == null || pinyin.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(pinyin.length());
        for (int i = 0; i < pinyin.length(); i++) {
            char c = pinyin.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                sb.append(c);
            }
        }
        return sb.toString();
    }


}