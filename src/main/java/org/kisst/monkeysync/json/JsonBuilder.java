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
        addUnescapedField(fieldName, escapeJsonString(value));
        result.append('"');
    }

    public void addUnescapedField(String fieldName, String value) {
        if (value==null)
            return;
        result.append(sep);
        result.append("\""+fieldName+"\":");
        sep=",";
    }



    public static String toJson(Record rec) {
        JsonBuilder builder = new JsonBuilder();
        for (String fieldName : rec.fieldNames())
            builder.addStringField(fieldName, rec.getField(fieldName));
        return builder.toString();
    }

    public static String toJson(Map<String, String> map) {
        JsonBuilder builder = new JsonBuilder();
        for (String fieldName : map.keySet())
            builder.addStringField(fieldName, map.get(fieldName));
        return builder.toString();
    }

    public static String escapeJsonString(String str) {
        return str.replace("\\","\\\\")
                .replace("\b","\\b")
                .replace("\n","\\n")
                .replace("\r","\\r")
                .replace("\f","\\f")
                .replace("\t","\\t")
                .replace("\"","\\\"");
    }
}
