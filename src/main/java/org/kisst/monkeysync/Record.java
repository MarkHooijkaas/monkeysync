package org.kisst.monkeysync;

public interface Record {
    String getKey();
    Iterable<String> fieldNames();
    String getField(String name);
    void setField(String name, String value);
    String toJson();
}
