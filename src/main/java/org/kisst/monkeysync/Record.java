package org.kisst.monkeysync;

public interface Record {
    String getKey();
    boolean isActive();

    Iterable<String> fieldNames();
    String getField(String name);
}
