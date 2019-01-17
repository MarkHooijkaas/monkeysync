package org.kisst.monkeysync.json;

import org.kisst.monkeysync.Record;

import java.util.Map;

public class JsonBuilder {
    private final StringBuilder result= new StringBuilder("{");
    private String sep="";


    public String toString() { return result.toString(); }

    public void addStringField(String fieldName, String value) {
        if (value==null)
            return;
        result.append('"');
        addUnescapedField(fieldName, JsonHelper.escapeJsonString(value));
        result.append('"');
    }

    public void addUnescapedField(String fieldName, String value) {
        if (value==null)
            return;
        result.append(sep);
        result.append("\""+fieldName+"\":");
        sep=",";
    }


}
