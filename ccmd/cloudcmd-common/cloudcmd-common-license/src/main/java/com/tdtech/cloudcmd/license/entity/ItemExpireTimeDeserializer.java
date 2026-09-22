package com.tdtech.cloudcmd.license.entity;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class ItemExpireTimeDeserializer extends JsonDeserializer<Date> {

    private static final Date MAX_DATE = new Date(Long.MAX_VALUE);

    @Override
    public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.hasToken(JsonToken.VALUE_STRING)) {
            String str = p.getText().trim();
            if (str.length() == 0) {
                return (Date)getEmptyValue(ctxt);
            }
            if ("PERMANENT".equals(str)) {
                return MAX_DATE;
            }
            try {
                var simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                return simpleDateFormat.parse(str);
            } catch (ParseException e) {
                return (Date)ctxt.handleWeirdStringValue(handledType(), str,
                    "expected format \"yyyy-MM-dd\"");
            }
        }

        return (Date)getEmptyValue(ctxt);
    }
}
