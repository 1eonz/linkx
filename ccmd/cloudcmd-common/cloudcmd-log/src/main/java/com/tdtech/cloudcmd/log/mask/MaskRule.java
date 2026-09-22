package com.tdtech.cloudcmd.log.mask;

import static java.util.regex.Pattern.compile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author zWX446107
 * @Description Rule to masks sensitive information in logs.
 *              <p>
 *              1.full mask sensitive information.eg: password=222@sdsad ,the mask result:password=***
 *              </p>
 *              <p>
 *              2.partial mask sensitive information and leave the number of characters to unmasked.
 *              eg:idcard=322232323323,the mask result: idcard=***3323
 *              </p>
 * @create 2020-12-07 17:31
 */
@EqualsAndHashCode
public class MaskRule {
    private final String property;
    private final int unmasked;
    private final Pattern maskPattern;

    /**
     * @param property the model property to be mask, split '|'.
     * @param unmasked the number of characters to leave unmasked.
     */
    MaskRule(String property, int unmasked) {
        this.property = parse(property);
        this.maskPattern = parse(property, unmasked);
        this.unmasked = unmasked;
    }

    /**
     * @param property the model property to be mask, split '|'.
     * @param unmasked the number of characters to leave unmasked.
     */
    private static Pattern parse(String property, int unmasked) {
        final StringBuilder patternStr = new StringBuilder();
        // partial mask
        if (unmasked == 0) {
            patternStr.append("(?<=(");
            patternStr.append(property);
            patternStr.append(")[\\s]*(=|:)).*?(?=(,|\\)|\\n))");
        } else {
            patternStr.append("(?<=(");
            patternStr.append(property);
            patternStr.append(")[\\s]*(=|:)).*?(?=[\\S\\s]{0,");
            patternStr.append(unmasked);
            patternStr.append("}(,|\\)|\\n))");
        }
        return compile(patternStr.toString(), Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    }

    private String parse(String property) {
        if (property == null || "".equals(property.trim())) {
            throw new IllegalArgumentException("Mask property cannot be null blank!");
        }
        return property.trim();
    }

    /**
     * Applies the masking rule to the message.
     *
     * @param message the message that needs to be masked.
     * @return the masked of the message.
     */
    public String apply(String message) {
        if (maskPattern != null) {
            Matcher matcher = maskPattern.matcher(message);
            return matcher.replaceAll("***");
        }

        return message;
    }

    /**
     * Helper to create a new rule instance.
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MaskRuleBuilder {
        private String property;
        private int unmasked = 0;

        public MaskRuleBuilder(String property) {
            this(property, 0);
        }

        public MaskRule rule() {
            return new MaskRule(property, unmasked);
        }
    }
}
