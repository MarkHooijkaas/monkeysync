package org.kisst.monkeysync;

public interface CachedObject {
    void load();
    void fetch();
    void save(String filename);
    void save();
}
