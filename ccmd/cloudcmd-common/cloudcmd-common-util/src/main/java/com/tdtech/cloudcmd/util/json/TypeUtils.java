package com.tdtech.cloudcmd.util.json;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.util.IOUtils;

public class TypeUtils {

    public static String castToString(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    public static Byte castToByte(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal) {
            return byteValue((BigDecimal)value);
        }

        if (value instanceof Number) {
            return ((Number)value).byteValue();
        }

        if (value instanceof String) {
            String strVal = (String)value;
            if (strVal.isEmpty() //
                || "null".equals(strVal) //
                || "NULL".equals(strVal)) {
                return null;
            }
            try {
                return Byte.parseByte(strVal);
            } catch (NumberFormatException e) {
                throw new JSONException("can not cast to byte, value : " + value, e);
            }
        }
        throw new JSONException("can not cast to byte, value : " + value);
    }

    public static Character castToChar(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Character) {
            return (Character)value;
        }
        if (value instanceof String) {
            String strVal = (String)value;
            if (strVal.isEmpty()) {
                return null;
            }
            if (strVal.length() != 1) {
                throw new JSONException("can not cast to char, value : " + value);
            }
            return strVal.charAt(0);
        }
        throw new JSONException("can not cast to char, value : " + value);
    }

    public static Short castToShort(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal) {
            return shortValue((BigDecimal)value);
        }

        if (value instanceof Number) {
            return ((Number)value).shortValue();
        }

        if (value instanceof String) {
            String strVal = (String)value;
            if (strVal.isEmpty() //
                || "null".equals(strVal) //
                || "NULL".equals(strVal)) {
                return null;
            }
            try {
                return Short.parseShort(strVal);
            } catch (NumberFormatException e) {
                throw new JSONException("can not cast to short, value : " + value, e);
            }
        }
        throw new JSONException("can not cast to short, value : " + value);
    }

    public static BigDecimal castToBigDecimal(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Float) {
            if (Float.isNaN((Float)value) || Float.isInfinite((Float)value)) {
                return null;
            }
        } else if (value instanceof Double) {
            if (Double.isNaN((Double)value) || Double.isInfinite((Double)value)) {
                return null;
            }
        } else if (value instanceof BigDecimal) {
            return (BigDecimal)value;
        } else if (value instanceof BigInteger) {
            return new BigDecimal((BigInteger)value);
        } else if (value instanceof Map && ((Map)value).isEmpty()) {
            return null;
        }

        String strVal = value.toString();

        if (strVal.isEmpty() || strVal.equalsIgnoreCase("null")) {
            return null;
        }

        if (strVal.length() > 65535) {
            throw new JSONException("decimal overflow");
        }
        return new BigDecimal(strVal);
    }

    public static BigInteger castToBigInteger(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Float) {
            var floatValue = (Float)value;
            if (Float.isNaN(floatValue) || Float.isInfinite(floatValue)) {
                return null;
            }
            return BigInteger.valueOf(floatValue.longValue());
        } else if (value instanceof Double) {
            var doubleValue = (Double)value;
            if (Double.isNaN(doubleValue) || Double.isInfinite(doubleValue)) {
                return null;
            }
            return BigInteger.valueOf(doubleValue.longValue());
        } else if (value instanceof BigInteger) {
            return (BigInteger)value;
        } else if (value instanceof BigDecimal) {
            BigDecimal decimal = (BigDecimal)value;
            int scale = decimal.scale();
            if (scale > -1000 && scale < 1000) {
                return ((BigDecimal)value).toBigInteger();
            }
        }

        String strVal = value.toString();

        if (strVal.isEmpty() || strVal.equalsIgnoreCase("null")) {
            return null;
        }

        if (strVal.length() > 65535) {
            throw new JSONException("decimal overflow");
        }
        return new BigInteger(strVal);
    }

    public static Float castToFloat(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number)value).floatValue();
        }
        if (value instanceof String) {
            String strVal = value.toString();
            if (strVal.isEmpty() //
                || "null".equals(strVal) //
                || "NULL".equals(strVal)) {
                return null;
            }
            try {
                return Float.parseFloat(strVal);
            } catch (NumberFormatException ex) {
                throw new JSONException("can not cast to float, value : " + value, ex);
            }
        }

        throw new JSONException("can not cast to float, value : " + value);
    }

    public static Double castToDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number)value).doubleValue();
        }
        if (value instanceof String) {
            String strVal = value.toString();
            if (strVal.isEmpty() //
                || "null".equals(strVal) //
                || "NULL".equals(strVal)) {
                return null;
            }
            try {
                return Double.parseDouble(strVal);
            } catch (NumberFormatException ex) {
                throw new JSONException("can not cast to double, value : " + value, ex);
            }
        }
        throw new JSONException("can not cast to double, value : " + value);
    }

    public static long longExtractValue(Number number) {
        if (number instanceof BigDecimal) {
            return ((BigDecimal)number).longValueExact();
        }

        return number.longValue();
    }

    public static Long castToLong(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal) {
            return longValue((BigDecimal)value);
        }

        if (value instanceof Number) {
            return ((Number)value).longValue();
        }

        if (value instanceof String) {
            String strVal = (String)value;
            if (strVal.isEmpty() //
                || "null".equals(strVal) //
                || "NULL".equals(strVal)) {
                return null;
            }
            try {
                return Long.parseLong(strVal);
            } catch (NumberFormatException ex) {
                throw new JSONException("can not cast to long, value : " + value, ex);
            }
        }
        throw new JSONException("can not cast to long, value : " + value);
    }

    public static byte byteValue(BigDecimal decimal) {
        if (decimal == null) {
            return 0;
        }

        int scale = decimal.scale();
        if (scale >= -100 && scale <= 100) {
            return decimal.byteValue();
        }

        return decimal.byteValueExact();
    }

    public static short shortValue(BigDecimal decimal) {
        if (decimal == null) {
            return 0;
        }

        int scale = decimal.scale();
        if (scale >= -100 && scale <= 100) {
            return decimal.shortValue();
        }

        return decimal.shortValueExact();
    }

    public static int intValue(BigDecimal decimal) {
        if (decimal == null) {
            return 0;
        }

        int scale = decimal.scale();
        if (scale >= -100 && scale <= 100) {
            return decimal.intValue();
        }

        return decimal.intValueExact();
    }

    public static long longValue(BigDecimal decimal) {
        if (decimal == null) {
            return 0;
        }

        int scale = decimal.scale();
        if (scale >= -100 && scale <= 100) {
            return decimal.longValue();
        }

        return decimal.longValueExact();
    }

    public static Integer castToInt(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Integer) {
            return (Integer)value;
        }

        if (value instanceof BigDecimal) {
            return intValue((BigDecimal)value);
        }

        if (value instanceof Number) {
            return ((Number)value).intValue();
        }

        if (value instanceof String) {
            String strVal = (String)value;
            if (strVal.isEmpty() //
                || "null".equals(strVal) //
                || "NULL".equals(strVal)) {
                return null;
            }
            try {
                return Integer.parseInt(strVal);
            } catch (NumberFormatException ex) {
                throw new JSONException("can not cast to int, value : " + value, ex);
            }
        }
        throw new JSONException("can not cast to int, value : " + value);
    }

    public static byte[] castToBytes(Object value) {
        if (value instanceof byte[]) {
            return (byte[])value;
        }
        if (value instanceof String) {
            return IOUtils.decodeBase64((String)value);
        }
        throw new JSONException("can not cast to byte[], value : " + value);
    }

    public static Boolean castToBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean)value;
        }

        if (value instanceof String) {
            String strVal = (String)value;
            if (strVal.isEmpty() //
                || "null".equals(strVal) //
                || "NULL".equals(strVal)) {
                return null;
            }
            if ("true".equalsIgnoreCase(strVal)) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(strVal)) {
                return Boolean.FALSE;
            }
        }
        throw new JSONException("can not cast to boolean, value : " + value);
    }
}
