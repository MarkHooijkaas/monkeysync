package org.kisst.monkeysync;

public interface Record {
    String getKey();

    Iterable<String> fieldNames();
    String getField(String name);
}
