package org.kisst.monkeysync.json;

import org.kisst.monkeysync.Record;
import org.kisst.monkeysync.Table;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class JsonHelper {
    public static void writeJson(Table table, String filename) {
        try (FileWriter file = new FileWriter(filename)) {
            for (Record rec: table.records()) {
                file.write(toJson(rec));
                file.write('\n');
            }
        }
        catch (IOException e) { throw new RuntimeException(e);}
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
